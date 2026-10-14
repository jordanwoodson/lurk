#!/usr/bin/env python3
"""Compile production playback/link helpers; outputs only under ~/scratch/morphe/twitch-playback."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / 'extensions/shared/src/main/java/io/github/jordanwoodson/extension/shared/twitch'

def main():
    for name in ['TwitchPlayback', 'TwitchLinks']:
        assert (JAVA / (name + '.java')).is_file(), f'missing production {name} helper'
    scratch = Path.home() / 'scratch/morphe/twitch-playback'
    scratch.mkdir(parents=True, exist_ok=True)
    output = Path(tempfile.mkdtemp(prefix='contracts-', dir=scratch))
    try:
        stubs = {
            'android/content/Intent.java': '''package android.content;
                public class Intent { public static final String EXTRA_TEXT="android.intent.extra.TEXT";
                public String value; public Intent putExtra(String k,String v){value=v;return this;} }''',
            'io/github/jordanwoodson/extension/shared/settings/Settings.java': '''
                package io.github.jordanwoodson.extension.shared.settings;
                public class Settings { public static boolean autoplay=true,links=true,next=true; public static int quality=1;
                public static boolean isEnabled(String k) { if(k.equals("disable_autoplay"))return autoplay;
                if(k.equals("autoplay_next_video_off"))return next;
                if(k.equals("clean_share_links"))return links;throw new AssertionError(k); }
                public static int getInt(String k){if(!k.equals("video_quality"))throw new AssertionError(k);return quality;} }''',
            'android/content/res/Resources.java': '''package android.content.res;
                public class Resources { public String getResourceEntryName(int id) {
                return new String[]{"autoplay_container","autoplay_progress","autoplay_time_remaining",
                "cancel_autoplay","manual_play","video_title"}[id]; } }''',
            'android/view/View.java': '''package android.view;
                public class View { public static final int NO_ID=-1,VISIBLE=0,GONE=8;
                private final int id; private int visibility;
                public View(int id){this.id=id;} public int getId(){return id;}
                public android.content.res.Resources getResources(){return new android.content.res.Resources();}
                public void setVisibility(int value){visibility=value;} public int getVisibility(){return visibility;} }''',
            'android/view/ViewGroup.java': '''package android.view;
                public class ViewGroup extends View { private final View[] children;
                public ViewGroup(int id,View... children){super(id);this.children=children;}
                public int getChildCount(){return children.length;} public View getChildAt(int i){return children[i];} }''',
        }
        sources = []
        for path, text in stubs.items():
            target = output / path
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(text)
            sources.append(str(target))
        sources += [str(JAVA / (name + '.java')) for name in ['TwitchPlayback', 'TwitchLinks']]
        sources.append(str(ROOT / 'tests/twitch/PlaybackLinksProbe.java'))
        jdk = Path(os.environ.get('JAVA_HOME', '/home/jordan/.local/opt/jdk-21')) / 'bin'
        subprocess.run([str(jdk / 'javac'), '-d', str(output / 'classes'), *sources], check=True)
        subprocess.run([str(jdk / 'java'), '-XX:-UsePerfData', '-cp', str(output / 'classes'), 'PlaybackLinksProbe'], check=True)
    finally:
        shutil.rmtree(output)

if __name__ == '__main__':
    main()
