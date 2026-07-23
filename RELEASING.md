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
tag), CI runs two independent jobs — a failure in one never blocks the other:

**`build-and-attach`** (GitHub release asset):
1. Checks out the exact tag.
2. Derives `versionName` from the tag (strips the leading `v` — so tag `v1.2.0-beta.1` becomes
   app `versionName` `1.2.0-beta.1`).
3. Derives `versionCode` from the CI run number (monotonically increasing for the life of this
   workflow file — never needs manual bumping, never collides).
4. Builds and signs `assembleRelease` using the `RELEASE_KEYSTORE_BASE64` /
   `RELEASE_KEYSTORE_PASSWORD` / `RELEASE_KEY_ALIAS` / `RELEASE_KEY_PASSWORD` repo secrets.
5. Attaches `Messages-<tag>.apk` to the release.

**`publish-play-store`** (Play Console):
1. Same tag/version derivation as above.
2. Picks the Play Console track from the tag's suffix:
   - `-alpha*` → **internal testing**
   - `-beta*` or `-rc*` → **closed testing**
   - no suffix (a plain `vX.Y.Z`) → **open testing**
   - **Production is never selected automatically** — promoting a build there is a manual step
     in the Play Console, given the review stakes of a public release.
3. Builds and signs `bundleRelease` (an `.aab`, which Play Console requires — separate from the
   `.apk` built above for the GitHub release asset).
4. Uploads it via [`r0adkll/upload-google-play`](https://github.com/r0adkll/upload-google-play)
   using the `PLAY_SERVICE_ACCOUNT_JSON` secret, with the release notes taken from the GitHub
   release body (truncated to Play's 500-character-per-locale limit).

You do not need to bump anything in the repo before tagging — just pick the next tag per the
convention above and draft the release on GitHub.

## Required secrets

| Secret | Used by | Notes |
|---|---|---|
| `RELEASE_KEYSTORE_BASE64` | both jobs | `base64 -i messages-release.keystore \| gh secret set RELEASE_KEYSTORE_BASE64` |
| `RELEASE_KEYSTORE_PASSWORD` | both jobs | |
| `RELEASE_KEY_ALIAS` | both jobs | |
| `RELEASE_KEY_PASSWORD` | both jobs | |
| `GOOGLE_SERVICES_JSON_BASE64` | both jobs | `base64 -i app/google-services.json \| gh secret set GOOGLE_SERVICES_JSON_BASE64` |
| `PLAY_SERVICE_ACCOUNT_JSON` | `publish-play-store` only | The Google Cloud service-account JSON key (plain text, not base64) with **Release manager** access to this app in Play Console — see the Play Store rollout plan for setup steps. |

## Play Store rollout plan

See the Play Store planning notes for the full pre-launch checklist (prominent disclosure,
Data Safety form, Permissions Declaration + video, closed-testing requirement for new developer
accounts, target API deadlines). In short, the intended progression is:

`internal (-alpha)` → `closed (-beta/-rc, ≥12 testers/14 days)` → `open (stable tag)` → manual
promotion to `production` once you're satisfied.
