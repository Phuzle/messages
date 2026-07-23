import { Hono } from "hono";
import { db } from "../db/database";

/**
 * CRUD over the classifier's keyword/weight dataset (see ../db/database.ts) — this is what lets
 * the ruleset be tuned ("upgraded") from real-world misclassifications without a redeploy: add a
 * keyword you've seen in a misclassified message, or bump/lower a weight, straight through the
 * API. Defaults live in ../db/seedData.ts and are only ever used to bootstrap an empty database.
 */
const app = new Hono();

const CATEGORIES = new Set(["OTP", "TRANSACTIONS", "PROMOTIONS", "OTHERS"]);

app.get("/", (c) => {
  const category = c.req.query("category");
  const rows =
    category && CATEGORIES.has(category)
      ? db.query("SELECT id, category, keyword, weight FROM keyword_rules WHERE category = ? ORDER BY weight DESC").all(category)
      : db.query("SELECT id, category, keyword, weight FROM keyword_rules ORDER BY category, weight DESC").all();
  return c.json(rows);
});

/** Upserts on (category, keyword) — posting an existing keyword again just updates its weight,
 * which is the natural way to "correct" a rule that's scoring too strong or too weak. */
app.post("/", async (c) => {
  const payload = await c.req.json().catch(() => null);
  const category = payload?.category;
  const keyword = typeof payload?.keyword === "string" ? payload.keyword.trim().toLowerCase() : "";
  const weight = Number(payload?.weight);

  if (!CATEGORIES.has(category) || keyword.length === 0 || !Number.isFinite(weight) || weight <= 0) {
    return c.json(
      { error: "category (OTP|TRANSACTIONS|PROMOTIONS|OTHERS), keyword (non-empty string), weight (positive number) are required" },
      400,
    );
  }

  db.query(
    "INSERT INTO keyword_rules (category, keyword, weight) VALUES (?, ?, ?) " +
      "ON CONFLICT(category, keyword) DO UPDATE SET weight = excluded.weight",
  ).run(category, keyword, weight);

  const row = db.query("SELECT id, category, keyword, weight FROM keyword_rules WHERE category = ? AND keyword = ?").get(category, keyword);
  return c.json(row, 201);
});

app.delete("/:id", (c) => {
  const id = Number(c.req.param("id"));
  if (!Number.isFinite(id)) return c.json({ error: "invalid id" }, 400);
  db.query("DELETE FROM keyword_rules WHERE id = ?").run(id);
  return c.body(null, 204);
});

export default app;
