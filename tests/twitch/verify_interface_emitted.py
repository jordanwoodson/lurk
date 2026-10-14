#!/usr/bin/env python3
"""Check pinned native UI targets and preservation controls in stock/patched apktool output.

Usage: python3 tests/twitch/verify_interface_emitted.py stock-apktool patched-apktool
Version-specific class/field names belong in this independent artifact test, never fingerprints.
"""
from pathlib import Path
import re
import sys

def source(root, relative):
    candidates = list(root.glob("smali*/" + relative))
    assert len(candidates) == 1, (relative, candidates)
    return candidates[0].read_text()

def method(text, signature):
    candidates = [body for declaration, body in re.findall(r"^\.method ([^\n]+)\n(.*?)^\.end method", text, re.M | re.S)
                  if declaration.endswith(signature)]
    assert len(candidates) == 1, signature
    return candidates[0]

def executable_source(text):
    # Dex rewriting can move debug line markers across instruction boundaries. Preserve every
    # instruction, field, annotation and label while ignoring that non-executable metadata.
    return "\n".join(line.strip() for line in text.splitlines()
                     if line.strip() and not line.strip().startswith(".line "))

def main():
    stock, patched = map(Path, sys.argv[1:])
    helper = "Lio/github/jordanwoodson/extension/shared/twitch/TwitchInterface;"
    for filename, flags in [("rr70.smali", ["k", "m"]), ("v9s.smali", ["e", "k"])]:
        original = source(stock, filename)
        emitted = source(patched, filename)
        assert helper not in original, "Stock negative unexpectedly contains Lurk"
        assert emitted.count(helper + "->bitsVisible(Z)Z") == len(flags), filename
        constructor = next(body for declaration, body in re.findall(r"^\.method ([^\n]+)\n(.*?)^\.end method", emitted, re.M | re.S)
                           if declaration.startswith("public constructor <init>"))
        instructions = [line.strip() for line in constructor.splitlines()
                        if line.strip() and not line.lstrip().startswith((".", "#", ":"))]
        for flag in flags:
            position = next(i for i, line in enumerate(instructions) if line.endswith(f"->{flag}:Z") and line.startswith("iput-boolean"))
            register = instructions[position].split()[1].rstrip(",")
            assert instructions[position - 1] == f"move-result {register}"
            assert "->bitsVisible(Z)Z" in instructions[position - 2]
        assert constructor.count("return-void") == 1, "UI-state constructor exit changed"
    profile = source(patched, "omv.smali")
    assert helper + "->clipTabs(Ljava/lang/Object;Landroid/os/Bundle;)Ljava/lang/Object;" in profile
    assert re.search(r"invoke-static \{p1, p4\}, " + re.escape(helper) +
                     r"->clipTabs\(Ljava/lang/Object;Landroid/os/Bundle;\)Ljava/lang/Object;", profile), "Profile tabs lost original Bundle context"
    assert "check-cast p1, Lkotlin/enums/EnumEntries;" in profile or "check-cast v" in profile
    renderer = method(source(patched, "jp20.smali"), "w(Lip20;)V")
    assert "instance-of v0, p1, Lfp20;" in renderer
    assert "instance-of v0, p1, Ldp20;" in renderer
    assert "->setVisibility(I)V" in renderer and "hide_subscription_upsells" in renderer
    assert "hide_subscription_upsells" not in method(source(stock, "jp20.smali"), "w(Lip20;)V")
    # Account entitlement, received chat events and clip deep links are preservation controls.
    for filename in ["tv/twitch/android/models/UserModel.smali", "oqr.smali", "fp20.smali", "dp20.smali"]:
        assert executable_source(source(stock, filename)) == executable_source(source(patched, filename)), filename + " changed unexpectedly"
    assert "hide_subscription_upsells" in source(patched, "o890.smali"), "Turbo app-start route was not guarded"
    account = source(patched, "djj.smali")
    account_section = next(body for declaration, body in re.findall(r"^\.method ([^\n]+)\n(.*?)^\.end method", account, re.M | re.S)
                           if "0x7f151826" in body and "0x7f151827" in body)
    assert "hide_subscription_upsells" in account_section, "Native account Turbo promotion remains"
    assert re.search(r"move/from16 v\d+, p0", account_section), "Must retain actual Turbo subscription input"
    for filename in ["nd50.smali", "hfr.smali"]:
        assert executable_source(source(stock, filename)) == executable_source(source(patched, filename)), filename + " changed unexpectedly"
    # Selection/argument parsing and the existing native clip bridge are preservation controls.
    for filename in ["w72.smali", "kb1.smali", "lov.smali", "qb8.smali", "wv7.smali",
                     "tv/twitch/android/feature/discovery/feed/rn/bridge/TwitchRNHostNavigationModule.smali"]:
        assert executable_source(source(stock, filename)) == executable_source(source(patched, filename)), filename + " clip navigation changed unexpectedly"
    print("PASS: native UI flag writes, profile tabs, selective subscription renderer, Turbo route and stock preservation controls")

if __name__ == "__main__":
    main()
