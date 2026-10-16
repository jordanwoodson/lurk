# Security policy

Lurk patches rewrite Twitch, Kick and Rumble on the user's phone, and some patches talk to
third-party services the user turns on (emote and badge providers, a playlist proxy the user
configures). A mistake here can leak data or weaken an app, so reports are welcome.

## What counts

- A patch that sends data somewhere it shouldn't, or sends more than its description says.
  The descriptions and [docs/](docs/README.md) state what each patch transmits.
- A patch that weakens the app: an exported component that wasn't, cleartext where there was
  TLS, a dropped signature or permission check, a relaxed network security config beyond what
  the patch documents.
- Anything that lets a stream, chat message, emote list or proxy response run code or crash
  the app in a way an attacker controls.
- The build and release pipeline: dependencies, the GitHub workflows, the published bundle.
- The workspace scripts under `bin/` handling an untrusted APK or bundle unsafely.

Ads still showing, a service detecting a modified client, or a feature not working are not
security issues; open a normal bug report for those.

## Reporting

Use GitHub's private reporting: **Security › Report a vulnerability** on
[the repository](https://github.com/jordanwoodson/lurk-morphe-patches/security/advisories/new).
Please don't open a public issue for something exploitable.

Include the app and version, the Lurk version, the patches involved and how to reproduce.
You'll get an acknowledgement within a week. Fixes ship as a new release; the advisory is
published once the release is out, with credit if you want it.

## Supported versions

Only the latest release is supported. Each release targets one version of each app (listed in
the README); older app versions and older Lurk releases don't receive fixes.
