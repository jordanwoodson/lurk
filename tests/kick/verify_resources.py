#!/usr/bin/env python3
"""Verify observable resource-patch output in an apktool-decoded Kick APK.

Run against stock first as a negative control, then against the patched APK.
Only the explicitly selected expectations are checked, allowing patch combinations.
"""
import argparse
from pathlib import Path
import xml.etree.ElementTree as ET

ANDROID = "{http://schemas.android.com/apk/res/android}"
TRACKING = {
    "com.google.android.gms.permission.AD_ID",
    "android.permission.ACCESS_ADSERVICES_AD_ID",
    "android.permission.ACCESS_ADSERVICES_ATTRIBUTION",
    "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE",
}


def verify(root, *, label=None, package=None, privacy=False, embedded=False, icon=False, analytics=False):
    manifest = ET.parse(root / "AndroidManifest.xml").getroot()
    application = manifest.find("application")
    assert application is not None, "Application is missing"
    if privacy:
        declared = {e.get(ANDROID + "name") for e in manifest.findall("uses-permission")}
        assert not declared & TRACKING, f"Tracking permissions remain: {sorted(declared & TRACKING)}"
        assert "android.permission.INTERNET" in declared, "Playback internet permission was removed"
    if embedded:
        metadata = {e.get(ANDROID + "name"): e.get(ANDROID + "value")
                    for e in application.findall("meta-data")}
        assert metadata.get("expo.modules.updates.ENABLED") == "false", "Expo updates can replace the patched bundle"
    if analytics:
        metadata = {e.get(ANDROID + "name"): e.get(ANDROID + "value")
                    for e in application.findall("meta-data")}
        assert metadata.get("firebase_analytics_collection_enabled") == "false", \
            "Firebase Analytics starts collecting before the startup hook"
    if label is not None:
        labels = [e.text for e in ET.parse(root / "res/values/strings.xml").getroot()
                  if e.get("name") == "app_name"]
        assert labels == [label], f"Wrong launcher label: {labels}"
    if package is not None:
        assert manifest.get("package") == package, "Clone package was not applied"
        providers = application.findall("provider")
        assert len(providers) == 13, "Providers were removed or added unexpectedly"
        for provider in providers:
            assert all(a.startswith(package + ".") for a in provider.get(ANDROID + "authorities", "").split(";")), "Provider would conflict with stock"
        token = next(e for e in providers if e.get(ANDROID + "name") == "expo.modules.kickauthtokenprovider.KickAuthTokenProvider")
        assert token.get(ANDROID + "readPermission") == package + ".SHARE_TOKEN", "Token access protection changed"
        permissions = {e.get(ANDROID + "name"): e.get(ANDROID + "protectionLevel")
                       for e in manifest.findall("permission")}
        assert permissions.get(package + ".SHARE_TOKEN") == "signature", "Token provider permission is not signature protected"
        assert application.get(ANDROID + "name") == "com.kick.mobile.MainApplication", "Clone renamed the application class"
    if icon:
        assert application.get(ANDROID + "icon") == "@mipmap/lurk_launcher", "Lurk launcher icon was not selected"
        assert application.get(ANDROID + "roundIcon") == "@mipmap/lurk_launcher", "Round launcher icon was not selected"
        adaptive = ET.parse(root / "res/mipmap-anydpi-v26/lurk_launcher.xml").getroot()
        assert adaptive.tag == "adaptive-icon"
        for kind in ("background", "foreground", "monochrome"):
            node = adaptive.find(kind)
            assert node is not None, f"Missing {kind} icon layer"
            reference = node.get(ANDROID + "drawable")
            assert reference.startswith("@drawable/")
            assert (root / "res/drawable" / (reference.split("/")[1] + ".xml")).is_file()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("decoded", type=Path)
    parser.add_argument("--label")
    parser.add_argument("--package")
    parser.add_argument("--privacy", action="store_true")
    parser.add_argument("--embedded", action="store_true")
    parser.add_argument("--analytics", action="store_true")
    parser.add_argument("--icon", action="store_true")
    args = vars(parser.parse_args())
    verify(args.pop("decoded"), **args)
    print("Kick resource expectations passed")
