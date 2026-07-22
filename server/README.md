# Phuzle Messages — server

The PRD's "Layer 3" cloud fallback: a small stateless HTTP API the app calls only for messages its
on-device Layer 1 regex classifier couldn't confidently place, and only after the client has
already redacted PII (account numbers, card numbers, OTP codes, emails, phone numbers) from the
body. Two endpoints:

- `POST /v1/classify` — `{ "body": "..." }` → `{ "category", "confidence", "matchedKeywords" }`.
  A weighted keyword heuristic (see `src/classifier/categoryClassifier.ts`), not a trained model —
  there's no labeled dataset here to train one on.
- `POST /v1/reminders/extract` — `{ "body", "receivedAtEpochMillis", "timezoneOffsetMinutes" }` →
  `{ "isReminder", "title", "detail", "dueAtEpochMillis" }`. Regex/keyword date parsing (see
  `src/classifier/reminderExtractor.ts`) — recognizes a deliberately narrow set of phrasings
  ("tomorrow at 10am", "due on Friday", "25 July", ...) and only reports `isReminder: true` when it
  both found a trigger phrase *and* resolved an actual due date.
- `GET /healthz` — liveness check.

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
  without adding auth first.
- **Not a trained ML classifier.** Both endpoints are hand-written heuristics. They're a genuine
  superset of the on-device rules (worth the network round trip) but they will misclassify things
  a real model wouldn't, same as the rest of this project is upfront about Layer 2 (on-device NPU)
  and a real Layer 3 model not existing yet.

## Local dev without Docker

```bash
npm install
npm run dev   # ts-node-dev, restarts on change
```
