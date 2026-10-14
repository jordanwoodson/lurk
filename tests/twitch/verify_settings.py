#!/usr/bin/env python3
"""Verify emitted startup, settings registry and React Native loader integration."""
import argparse
import json
from pathlib import Path
import re


def verify(root, hermes=False):
    files = list(root.glob("smali*/tv/twitch/android/app/consumer/TwitchApplication.smali"))
    assert len(files) == 1
    text = files[0].read_text()
    method = re.search(r"\.method[^\n]* onCreate\(\)V\n(.*?)\.end method", text, re.S).group(1)
    startup = [line.strip() for line in method.splitlines()
               if line.strip() and not line.lstrip().startswith((".", "#", ":"))]
    assert "TwitchPrivacy;->initSettingsOnce(Landroid/content/Context;)V" in startup[0], "Settings not initialized first"
    files = list(root.glob("smali*/io/github/jordanwoodson/extension/shared/settings/Settings.smali"))
    assert len(files) == 1, "Missing settings extension"
    text = files[0].read_text()
    registry_method = re.search(r"\.method[^\n]* registryJson\(\)Ljava/lang/String;\n(.*?)\.end method", text, re.S).group(1)
    literal = re.search(r'const-string(?:/jumbo)? \w+, (".*")', registry_method).group(1)
    # Smali escapes apostrophes even inside a double-quoted string; JSON does not.
    registry = json.loads(json.loads(literal.replace("\\'", "'")))
    assert registry["app"] == "Twitch" and registry["settings"], "Wrong or empty settings registry"
    keys = [s["key"] for s in registry["settings"]]
    assert len(keys) == len(set(keys)), "Duplicate setting keys"
    if hermes:
        files = list(root.glob("smali*/com/facebook/react/runtime/ReactInstance$loadJSBundle$1.smali"))
        assert len(files) == 1
        text = files[0].read_text()
        method = re.search(r"\.method[^\n]* loadScriptFromAssets\([^\n]*\n(.*?)\.end method", text, re.S).group(1)
        assert "TwitchBundle;->prepare(" in method, "Missing Hermes runtime loader"
        assert "access$loadJSBundleFromFile" in method and "access$loadJSBundleFromAssets" in method
        assert "TwitchBundle;->init(" in "\n".join(startup)
        manifest = json.loads((root / "assets/lurk-hermes.json").read_text())
        assert manifest["version"] == 98
        assert {e["key"] for e in manifest["edits"]} <= set(keys), "Hermes edit lacks runtime control"
    print("PASS Twitch startup, settings registry" + (" and Hermes loader" if hermes else ""))


if __name__ == "__main__":
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("decoded", type=Path)
    p.add_argument("--hermes", action="store_true")
    args = p.parse_args()
    verify(args.decoded, args.hermes)
