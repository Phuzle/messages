# Releasing

How to cut a GitHub Release for this app. The [`release.yml`](.github/workflows/release.yml)
workflow does the actual build/sign/attach automatically once you publish the release — this
doc is just the naming convention so tags/titles stay consistent across releases.

## Naming convention

- **Tag**: `vX.Y.Z` or `vX.Y.Z-suffix` (e.g. `v1.0.0`, `v1.2.0-beta.1`, `v2.0.0-rc.1`)
  - Always lowercase `v` prefix, semver underneath.
  - Use a `-suffix` (`-alpha`, `-beta`, `-beta.2`, `-rc.1`, …) for anything not meant as a
    stable build.
- **Release title**: `Messages ` + the tag verbatim, e.g. `Messages v1.0.0`, `Messages v1.2.0-beta.1`.
- **Pre-release checkbox**: tick GitHub's "Set as a pre-release" checkbox by hand for any tag
  with a `-suffix`. Nothing in the workflow does this automatically — it's a manual step when
  drafting the release.
- **First release**: `v0.1.0` (current `versionName` in [`app/build.gradle.kts`](app/build.gradle.kts)
  is a placeholder — the workflow overrides it at build time, see below).

## What happens automatically

On publishing a release (or via the workflow's manual `workflow_dispatch` re-run for an existing
tag), CI:

1. Checks out the exact tag.
2. Derives `versionName` from the tag (strips the leading `v` — so tag `v1.2.0-beta.1` becomes
   app `versionName` `1.2.0-beta.1`).
3. Derives `versionCode` from the CI run number (monotonically increasing for the life of this
   workflow file — never needs manual bumping, never collides).
4. Builds and signs `assembleRelease` using the `RELEASE_KEYSTORE_BASE64` /
   `RELEASE_KEYSTORE_PASSWORD` / `RELEASE_KEY_ALIAS` / `RELEASE_KEY_PASSWORD` repo secrets.
5. Attaches `Messages-<tag>.apk` to the release.

You do not need to bump anything in the repo before tagging — just pick the next tag per the
convention above and draft the release on GitHub.
