# Phuzle Messages — server

The PRD's "Layer 3" cloud fallback: a small stateless HTTP API the app calls only for messages its
on-device Layer 1 regex classifier couldn't confidently place, and only after the client has
already redacted PII (account numbers, card numbers, OTP codes, emails, phone numbers) from the
body. Runs on [Bun](https://bun.sh) + [Hono](https://hono.dev) — no Node/Express/tsc build step.

- `POST /v1/classify` — `{ "body": "..." }` → `{ "category", "confidence", "matchedKeywords" }`.
  A weighted keyword heuristic (see `src/classifier/categoryClassifier.ts`), not a trained model —
  there's no labeled dataset here to train one on.
- `POST /v1/reminders/extract` — `{ "body", "receivedAtEpochMillis", "timezoneOffsetMinutes" }` →
  `{ "isReminder", "title", "detail", "dueAtEpochMillis" }`. Regex/keyword date parsing (see
  `src/classifier/reminderExtractor.ts`) — recognizes a deliberately narrow set of phrasings
  ("tomorrow at 10am", "due on Friday", "25 July", ...) and only reports `isReminder: true` when it
  both found a trigger phrase *and* resolved an actual due date.
- `GET /healthz` — liveness check.

## Keyword/trigger datasets (SQLite, editable via API)

The classifier's keyword→weight rules and the reminder extractor's trigger phrases aren't
hardcoded — they live in a SQLite database (`data/keywords.sqlite`, `bun:sqlite`), seeded once from
`src/db/seedData.ts` the first time the database is empty. That means the ruleset can be tuned from
real-world misclassifications without touching code or redeploying:

- `GET /v1/keywords` — list all rules. `?category=OTP|TRANSACTIONS|PROMOTIONS|OTHERS` to filter.
- `POST /v1/keywords` — `{ "category", "keyword", "weight" }`. Upserts on `(category, keyword)`, so
  posting an existing keyword again just updates its weight.
- `DELETE /v1/keywords/:id` — remove a rule.
- `GET /v1/reminders/triggers` — list trigger phrases.
- `POST /v1/reminders/triggers` — `{ "phrase" }`.
- `DELETE /v1/reminders/triggers/:id` — remove a phrase.

```bash
# Teach the classifier a new OTP phrasing seen in a misclassified message
curl -X POST http://localhost:8080/v1/keywords -H 'content-type: application/json' \
  -d '{"category":"OTP","keyword":"temporary code","weight":2.5}'

# Add a new reminder trigger phrase
curl -X POST http://localhost:8080/v1/reminders/triggers -H 'content-type: application/json' \
  -d '{"phrase":"renewal notice"}'
```

Under Docker, `data/` is a mounted host volume (see `docker-compose.yml`), so these edits survive
`docker compose up --build` instead of being reset back to the seed defaults on every rebuild.

## Running it

```bash
cd server
docker compose up --build
```

Listens on `0.0.0.0:8080` inside the container, published to `8080` on the host — deliberately not
`127.0.0.1`-only, since both of the app's test targets need to reach it from outside the container:

- **Android emulator**: the emulator's virtual router aliases the host machine as `10.0.2.2`, so
  the app's dev config points at `http://10.0.2.2:8080/`.
- **Real device over wifi debugging**: the device reaches the Mac directly over the LAN at
  `http://192.168.29.10:8080/` (or whatever this Mac's current wifi IP is — check with
  `ipconfig getifaddr en0`). Both machines need to be on the same wifi network, and macOS may
  prompt to allow incoming connections the first time — allow it, or `curl` from another device
  will just hang.

Quick check once it's up:

```bash
curl http://localhost:8080/healthz
curl -X POST http://localhost:8080/v1/classify -H 'content-type: application/json' \
  -d '{"body":"Your package is out for delivery and should arrive today."}'
curl -X POST http://localhost:8080/v1/reminders/extract -H 'content-type: application/json' \
  -d '{"body":"Reminder: your appointment is tomorrow at 10am","receivedAtEpochMillis":1721606400000}'
```

## What this is not

- **Not internet-facing.** There's no authentication on these endpoints — this is built for a home
  wifi network during development, matching how the app is configured to reach it (emulator alias
  or LAN IP). Don't port-forward this through a router or deploy it anywhere publicly reachable
  without adding auth first. That includes the keyword/trigger CRUD endpoints — anyone who can
  reach the server can rewrite the classifier's ruleset.
- **Not a trained ML classifier.** Both endpoints are hand-written heuristics. They're a genuine
  superset of the on-device rules (worth the network round trip) but they will misclassify things
  a real model wouldn't, same as the rest of this project is upfront about Layer 2 (on-device NPU)
  and a real Layer 3 model not existing yet.

## Local dev without Docker

```bash
bun install
bun run dev   # bun --watch, restarts on change
```
