#!/usr/bin/env python3
"""Exercise the real Twitch UI extension with small platform-boundary doubles."""
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

def main():
    repo = Path(__file__).resolve().parents[2]
    root = Path(os.environ.get("MORPHE_WORK", Path.home() / "scratch/morphe")) / "twitch-interface"
    root.mkdir(parents=True, exist_ok=True)
    output = Path(tempfile.mkdtemp(prefix="java-", dir=root))
    try:
        stubs = {
            "android/os/Bundle.java": """
                package android.os;
                public class Bundle {
                    private final java.util.Map<String,String> values = new java.util.HashMap<>();
                    public void putString(String key, String value) { values.put(key, value); }
                    public String getString(String key) { return values.get(key); }
                }
            """,
            "kotlin/enums/EnumEntries.java": "package kotlin.enums; public interface EnumEntries<E extends Enum<E>> extends java.util.List<E> {}",
            "io/github/jordanwoodson/extension/shared/settings/Settings.java": """
                package io.github.jordanwoodson.extension.shared.settings;
                public final class Settings {
                    public static boolean enabled;
                    public static boolean isEnabled(String key) {
                        if (!key.equals("hide_clips") && !key.equals("hide_tipping"))
                            throw new AssertionError("Unexpected key " + key);
                        return enabled;
                    }
                }
            """,
        }
        sources = []
        for name, content in stubs.items():
            path = output / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content)
            sources.append(str(path))
        sources += [str(repo / "tests/twitch/InterfaceProbe.java"), str(repo / "extensions/shared/src/main/java/io/github/jordanwoodson/extension/shared/twitch/TwitchInterface.java")]
        java_bin = Path(os.environ["JAVA_HOME"]) / "bin" if os.environ.get("JAVA_HOME") else None
        def tool(name):
            return str(java_bin / name) if java_bin else shutil.which(name) or name
        subprocess.run([tool("javac"), "-d", str(output / "classes"), *sources], check=True)
        subprocess.run([tool("java"), "-XX:-UsePerfData", "-cp", str(output / "classes"), "InterfaceProbe"], check=True)
    finally:
        if sys.platform == "darwin":
            print(f"Retained on macOS: {output}")
        else:
            shutil.rmtree(output)

if __name__ == "__main__":
    main()
