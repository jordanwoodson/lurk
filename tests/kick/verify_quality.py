#!/usr/bin/env python3
"""Check quality defaults and execute the actual reset/selection bytecode with store doubles.

Requires hermes-dec. Usage:
    python tests/kick/verify_quality.py stock.bundle patched.apk [--height 1080]
    python tests/kick/verify_quality.py stock.bundle patched.bundle manifest.json

This checks the emitted artifact, including the first-load regression and explicit selections.
The limited interpreter rejects unknown instructions. It does not replace playback device tests.
"""
import argparse
import ast
from io import BytesIO
import json
from pathlib import Path
import re
import runpy
import struct
from zipfile import is_zipfile, ZipFile

from hermes_dec.parsers.hbc_bytecode_parser import parse_hbc_bytecode
from hermes_dec.parsers.hbc_file_parser import HBCReader
from hermes_dec.parsers.serialized_literal_parser import unpack_slp_array

UNDEFINED = object()
KEY = "default_video_quality"


def verify(stock, embedded, manifest, height):
    structural = runpy.run_path(str(Path(__file__).with_name("test_hermes.py")))
    structural["verify"](stock, embedded, manifest)
    reader = HBCReader()
    reader.read_whole_file(BytesIO(stock))

    def header(name):
        matches = [h for h in reader.function_headers if reader.strings[h.functionName] == name]
        assert len(matches) == 1, f"Ambiguous function: {name}"
        return matches[0]

    def instructions(name, data):
        reader.file_buffer = BytesIO(data)
        return list(parse_hbc_bytecode(header(name), reader))

    # Find every real property read independently of the patch's anchors or function-ID list.
    property_id = reader.strings.index("defaultMaxAutoQualityHeight")
    candidates = re.finditer(b"\x45..." + re.escape(struct.pack("<H", property_id)), stock, re.DOTALL)
    sites = set()
    owners = {}
    for match in candidates:
        offset = match.start()
        for h in reader.function_headers:
            if h.offset <= offset < h.offset + h.bytecodeSizeInBytes:
                code = structural["decoded"](reader, h, stock)
                if any(i.original_pos + h.offset == offset and i.inst.name == "GetById" and
                       i.arg4 == property_id for i in code):
                    sites.add(offset)
                    owners[offset] = h
    assert len(sites) == 10, f"Expected ten default reads, found {len(sites)}"

    selected = bytearray(stock)
    quality_edits = [edit for edit in manifest["edits"] if edit["key"] == KEY]
    assert len(quality_edits) == 10, "Quality patch must cover all ten consumers"
    covered = set()
    for edit in quality_edits:
        before, after = bytes.fromhex(edit["original"]), bytes.fromhex(edit["replacement"])
        offset = edit["offset"]
        assert len(before) == len(after) == 12 and before[:6] == after[:6]
        assert offset + 6 in sites, "Edit is not a default read"
        assert after[6:8] == bytes([0x8c, before[7]]), "Changed destination or instruction"
        assert struct.unpack("<i", after[8:])[0] == height, "Wrong quality option"
        selected[offset:offset + len(after)] = after
        covered.add(offset + 6)
    assert covered == sites, "An unpatched default would reset the chosen ceiling"
    for h in owners.values():
        structural["decoded"](reader, h, selected)

    def config_read(name, data):
        original = next(i for i in instructions(name, stock)
                        if i.inst.name == "GetById" and i.arg4 == property_id)
        current = next(i for i in instructions(name, data) if i.original_pos == original.original_pos)
        if current.inst.name == "GetById":
            return 720
        assert current.inst.name == "LoadConstInt"
        return current.arg2

    def literal(value):
        if value == "undefined":
            return UNDEFINED
        if value in ("null", "true", "false"):
            return {"null": None, "true": True, "false": False}[value]
        return ast.literal_eval(value)

    def equal(a, b, loose=False):
        if loose and (a is None or a is UNDEFINED) and (b is None or b is UNDEFINED):
            return True
        return a is b or a == b

    def execute(name, data, environments, params=()):
        code = {i.original_pos: i for i in instructions(name, data)}
        registers, pc = {}, 0
        for _ in range(1000):
            i = code[pc]
            op = i.inst.name
            a = [getattr(i, f"arg{j}") for j in range(1, len(i.inst.operands) + 1)]
            pc = i.next_pos
            r = registers
            if op == "GetParentEnvironment":
                r[a[0]] = environments[a[1]]
            elif op == "LoadFromEnvironment":
                r[a[0]] = r[a[1]][a[2]]
            elif op == "LoadParam":
                r[a[0]] = params[a[1] - 1]
            elif op == "LoadConstUndefined":
                r[a[0]] = UNDEFINED
            elif op == "LoadConstNull":
                r[a[0]] = None
            elif op in ("LoadConstInt", "LoadConstUInt8"):
                r[a[0]] = a[1]
            elif op in ("LoadConstString", "LoadConstStringLongIndex"):
                r[a[0]] = reader.strings[a[1]]
            elif op in ("Call1", "Call2"):
                r[a[0]] = r[a[1]](*[r[x] for x in a[2:]])
            elif op in ("GetById", "GetByIdShort"):
                r[a[0]] = r[a[1]].get(reader.strings[a[3]], UNDEFINED)
            elif op in ("NewObjectWithBuffer", "NewObjectWithBufferLong"):
                keys = reader.object_shape_keys[a[1]]
                values = unpack_slp_array(reader.literal_values[a[2]:], len(keys), 99).to_strings(reader.strings)
                r[a[0]] = {literal(k): literal(v) for k, v in zip(keys, values)}
            elif op == "PutOwnBySlotIdx":
                r[a[0]][list(r[a[0]])[a[2]]] = r[a[1]]
            elif op == "Mov":
                r[a[0]] = r[a[1]]
            elif op == "LessEq":
                r[a[0]] = r[a[1]] <= r[a[2]]
            elif op == "Eq":
                r[a[0]] = equal(r[a[1]], r[a[2]], loose=True)
            elif op == "DelByVal":
                r[a[1]].pop(r[a[2]], None)
                r[a[0]] = True
            elif op in ("JmpFalse", "JmpTrue"):
                truth = r[a[1]] is not UNDEFINED and bool(r[a[1]])
                if truth == (op == "JmpTrue"):
                    pc = i.original_pos + a[0]
            elif op in ("JStrictEqual", "JNotEqual"):
                same = equal(r[a[1]], r[a[2]], loose=op == "JNotEqual")
                if same == (op == "JStrictEqual"):
                    pc = i.original_pos + a[0]
            elif op == "Ret":
                return r[a[0]]
            else:
                raise AssertionError(f"Unsupported executed instruction: {op}")
        raise AssertionError("Unexpected bytecode loop")

    config = {7: {"PlayerConfig": {"defaultMaxAutoQualityHeight": 720}}}

    def reset(data, current):
        initial = {"autoMaxQuality": config_read("getInitialStateFor", data),
                   "userSelectedQuality": UNDEFINED, "autoQualityMode": True,
                   "selectedQualityGroupPreset": "auto"}
        state = {**current, "getInitialStateFor": lambda this, keys: initial.copy()}
        # Mock only the Zustand get boundary and its selection of initial-state keys.
        envs = [{1: lambda this: state}, {**config, 16: list(initial)}]
        changes = execute("_getResetStreamLoadingStates", data, envs)
        return {**current, **changes}

    first = {"autoMaxQuality": config_read("usePlayerStoreIVSModuleSlice", selected),
             "userSelectedQuality": UNDEFINED, "autoQualityMode": True,
             "selectedQualityGroupPreset": "auto"}
    assert reset(stock, dict(first, autoMaxQuality=1080))["autoMaxQuality"] == 720, "Missing regression control"
    assert first["autoMaxQuality"] == height and reset(selected, first) == first, "First load lost ceiling"
    lower = dict(first, autoMaxQuality=240, userSelectedQuality={"height": 240},
                 autoQualityMode=False, selectedQualityGroupPreset="advanced")
    assert reset(selected, lower) == lower, "Explicit lower selection was reset"
    higher = dict(lower, autoMaxQuality=height + 360, userSelectedQuality={"height": height + 360})
    assert reset(selected, higher) == first, "Original above-default automatic reset changed"

    state = dict(lower)
    setter = lambda this, values: state.update(values)
    envs = [{0: setter}, config]
    execute("setSelectedQualityGroupPreset", selected, envs, ("auto",))
    assert state == first, "Selecting Auto did not restore chosen ceiling"
    for explicit in (240, 2160):
        choice = {"height": explicit}
        execute("setUserSelectedQuality", selected, envs, (choice,))
        assert state["autoMaxQuality"] == explicit and state["userSelectedQuality"] is choice
    execute("setUserSelectedQuality", selected, envs, (None,))
    assert state["autoMaxQuality"] == height, "Clearing explicit selection used old default"
    print(f"PASS: ten quality consumers; {height}p first load and Auto; explicit lower/higher selections; "
          "original automatic reset behavior; prior 1080-to-720 regression reproduced as control")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("stock", type=Path)
    parser.add_argument("patched", type=Path)
    parser.add_argument("manifest", nargs="?", type=Path)
    parser.add_argument("--height", type=int, default=1080, choices=(1080, 720, 480, 360, 240))
    args = parser.parse_args()
    if is_zipfile(args.patched):
        with ZipFile(args.patched) as apk:
            embedded = apk.read("assets/index.android.bundle")
            manifest = json.loads(apk.read("assets/lurk-hermes.json"))
    else:
        if args.manifest is None:
            parser.error("manifest is required for a raw bytecode file")
        embedded = args.patched.read_bytes()
        manifest = json.loads(args.manifest.read_text())
    verify(args.stock.read_bytes(), embedded, manifest, args.height)
