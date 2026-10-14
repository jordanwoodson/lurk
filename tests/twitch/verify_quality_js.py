#!/usr/bin/env python3
"""Execute Twitch 31.6.0's actual decompiled RN initial-quality effect, with boundary doubles.

Usage: python3 tests/twitch/verify_quality_js.py /path/to/hermes/decompiled.js
No bundle is modified. The effect consumes the preference returned by the native storage fallback.
"""
from pathlib import Path
import re
import subprocess
import sys

source = Path(sys.argv[1]).read_text()
start = source.index('Original name: usePreferredVideoQuality,')
start = source.index('r7 = function() { // Original name: t1', start) + len('r7 = ')
end = source.index('};\n                r0 = 3;', start) + 1
effect = source[start:end]
# Hermes decompiler names each function's register file r0...; retain that lexical isolation.
declarations = 'var ' + ','.join(f'r{i}' for i in range(32)) + ';'
effect = re.sub(r'function\([^)]*\) \{', lambda m: m[0] + declarations, effect)
runner = r'''
const assert = require('node:assert/strict');
function run(preference, names, repeat=false) {
    const calls=[], writes=[];
    const _closure1_slot7={current:false};
    const _closure1_slot0={names};
    const _closure1_slot1={current:{setQuality:q=>calls.push(q)}};
    const _closure1_slot3=preference;
    const _closure1_slot4=q=>writes.push(q);
    const effect=EFFECT;
    effect();
    if (repeat) effect();
    return {calls,writes};
}
// Stock absent/null preference never chooses a named rendition: negative control.
assert.deepEqual(run(null,['chunked','720p60']),{calls:[],writes:[]});
assert.deepEqual(run('chunked',['chunked','720p60'],true),{calls:['chunked'],writes:[]});
assert.deepEqual(run('720p60',['chunked','720p60']),{calls:['720p60'],writes:[]});
// Twitch accepts p30 and p as the same frame-rate rendition.
assert.deepEqual(run('720p',['chunked','720p30']),{calls:['720p30'],writes:[]});
// Missing rendition resets the existing stored preference to Auto, once per player instance.
assert.deepEqual(run('1080p60',['chunked','720p60'],true),{calls:[],writes:[null]});
assert.deepEqual(run('chunked',[]),{calls:[],writes:[]});
console.log('PASS: original RN initial-quality effect: stock negative, Source, explicit rendition, p30 normalization, unavailable Auto fallback, no repeat reset');
'''.replace('EFFECT', effect)
subprocess.run(['node', '-'], input=runner, text=True, check=True)
