#!/usr/bin/env python3
"""Verify Insider job guards in actual apktool output and retain the exact disabled path.

Usage: python tests/kick/verify_analytics_jobs.py stock-decoded patched-decoded
No app code is executed and no upload job is started by this test.
"""
import argparse
from pathlib import Path
import re

SERVICES = ("SessionPayloadJobService", "InsiderAmplificationService")
SIGNATURE = "onStartJob(Landroid/app/job/JobParameters;)Z"
SETTINGS = "Lio/github/jordanwoodson/extension/shared/settings/Settings;"


def method(root, service):
    files = list(root.glob(f"smali*/com/useinsider/insider/{service}.smali"))
    assert len(files) == 1, f"Expected one {service} class"
    text = files[0].read_text()
    blocks = [b for b in re.findall(r"(?ms)^\.method\b.*?^\.end method", text)
              if b.splitlines()[0].endswith(" " + SIGNATURE)]
    assert len(blocks) == 1, f"Expected one {service}.{SIGNATURE}"
    block = blocks[0]
    locals_match = re.search(r"(?m)^\s*\.locals (\d+)$", block)
    assert locals_match and int(locals_match[1]) >= 1, "v0 must be a local, not a parameter"
    instructions, labels, catches = [], {}, []
    for line in block.splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith(":"):
            labels[line] = len(instructions)
        elif line.startswith(".catch"):
            catches.append(line)
        elif not line.startswith("."):
            instructions.append(line)
    return int(locals_match[1]), instructions, labels, catches


def normalize(lines, labels, offset):
    def replace(match):
        return "@" + str(labels[match[0]] - offset)
    return [re.sub(r"(?<![A-Za-z0-9_$]):[A-Za-z_][A-Za-z_0-9]*", replace, line) for line in lines]


def verify(stock, patched):
    for service in SERVICES:
        original_locals, original, original_labels, original_catches = method(stock, service)
        patched_locals, code, labels, catches = method(patched, service)
        assert original_locals == patched_locals == 2, f"Changed register lifetime in {service}"
        assert code[:3] == [
            'const-string v0, "disable_analytics"',
            f"invoke-static/range {{v0 .. v0}}, {SETTINGS}->isEnabled(Ljava/lang/String;)Z",
            "move-result v0",
        ], f"Missing runtime setting lookup in {service}"
        branch = re.fullmatch(r"if-eqz v0, (:\w+)", code[3])
        assert branch and labels[branch[1]] == 6, f"Disabled path does not resume original {service}"
        assert code[4:6] == ["const/4 v0, 0x0", "return v0"], f"Enabled path must return false: {service}"
        # Reject hidden side effects, missing work, changed exception handlers or changed off behavior.
        assert normalize(code[6:], labels, 6) == normalize(original, original_labels, 0)
        assert normalize(catches, labels, 6) == normalize(original_catches, original_labels, 0)
    print("PASS: two unique Insider job guards; safe local register; enabled synchronous completion; "
          "disabled instructions, branches and exception handlers preserved")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("stock", type=Path)
    parser.add_argument("patched", type=Path)
    args = parser.parse_args()
    verify(args.stock, args.patched)
