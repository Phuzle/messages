import { Database } from "bun:sqlite";
import { existsSync, mkdirSync } from "node:fs";
import { dirname } from "node:path";
import { DEFAULT_KEYWORD_RULES, DEFAULT_TRIGGER_PHRASES } from "./seedData";

// Overridable so docker-compose can point this at a mounted volume (see ../../docker-compose.yml)
// — otherwise every `docker compose up --build` would start from a fresh, reseeded database and
// throw away any keywords/triggers the user added through the API.
const DB_PATH = process.env.DB_PATH ?? "data/keywords.sqlite";

const dir = dirname(DB_PATH);
if (dir !== "." && !existsSync(dir)) mkdirSync(dir, { recursive: true });

export const db = new Database(DB_PATH, { create: true });
db.exec("PRAGMA journal_mode = WAL;");

/** Creates the two datasets tables if missing and seeds them once from seedData.ts if empty. Both
 * the classifier's keyword rules and the reminder extractor's trigger phrases live here so they
 * can be tuned via the API (see routes/keywords.ts, routes/triggers.ts) without a code deploy.
 *
 * Called immediately below, at this module's own load time — not left for index.ts to call after
 * its other imports run. ES module imports are fully evaluated before the importing module's own
 * top-level code runs, so categoryClassifier.ts/reminderExtractor.ts's module-level
 * `db.query(...)` prepared statements (which import this file) would otherwise run against tables
 * that don't exist yet if init were deferred until index.ts got a chance to call it. */
function initDb(): void {
  db.exec(`
    CREATE TABLE IF NOT EXISTS keyword_rules (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      category TEXT NOT NULL,
      keyword TEXT NOT NULL,
      weight REAL NOT NULL,
      UNIQUE(category, keyword)
    );
  `);
  db.exec(`
    CREATE TABLE IF NOT EXISTS trigger_phrases (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      phrase TEXT NOT NULL UNIQUE
    );
  `);

  const ruleCount = db.query("SELECT COUNT(*) as count FROM keyword_rules").get() as { count: number };
  if (ruleCount.count === 0) {
    const insert = db.prepare("INSERT INTO keyword_rules (category, keyword, weight) VALUES (?, ?, ?)");
    db.transaction(() => {
      for (const rule of DEFAULT_KEYWORD_RULES) insert.run(rule.category, rule.keyword, rule.weight);
    })();
  }

  const triggerCount = db.query("SELECT COUNT(*) as count FROM trigger_phrases").get() as { count: number };
  if (triggerCount.count === 0) {
    const insert = db.prepare("INSERT INTO trigger_phrases (phrase) VALUES (?)");
    db.transaction(() => {
      for (const phrase of DEFAULT_TRIGGER_PHRASES) insert.run(phrase);
    })();
  }
}

initDb();
