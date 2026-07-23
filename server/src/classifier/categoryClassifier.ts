/**
 * Layer 3 of the PRD's pipeline: a PII-scrubbed cloud fallback for messages the on-device Layer 1
 * regex classifier (app/src/main/assets/regex_rules.json) couldn't confidently place — it only
 * ever sees a message after the client has already redacted account numbers, card numbers, OTP
 * codes, emails, and phone numbers (see the Android PiiScrubber). This is a weighted keyword
 * heuristic, not a trained model — there's no labeled dataset to train on here, and overclaiming
 * "AI" for a keyword scorer would be dishonest. It's deliberately a *superset* of the client's
 * rules (more keywords, weighted combinations) so it's worth the round trip.
 *
 * The keyword/weight rules themselves live in SQLite (see ../db/database.ts), not here — this
 * file only holds the scoring logic and the two structural patterns (an OTP-shaped digit run, an
 * amount-shaped number) that aren't really "keywords" a user would add through the API.
 */
import { db } from "../db/database";

export type Category = "PERSONAL" | "TRANSACTIONS" | "OTP" | "PROMOTIONS" | "OTHERS" | "UNKNOWN";

export interface ClassificationResult {
  category: Category;
  confidence: number; // 0..1
  matchedKeywords: string[];
}

interface WeightedRule {
  keyword: string;
  weight: number;
}

const OTP_CODE_PATTERN = /\b\d{4,8}\b/;
const AMOUNT_PATTERN = /(?:\$|rs\.?|inr|usd|€|£)\s?\d[\d,]*\.?\d*/i;

const rulesStmt = db.query("SELECT keyword, weight FROM keyword_rules WHERE category = ?");
function rulesFor(category: string): WeightedRule[] {
  return rulesStmt.all(category) as WeightedRule[];
}

function score(text: string, rules: WeightedRule[]): { total: number; matched: string[] } {
  let total = 0;
  const matched: string[] = [];
  for (const rule of rules) {
    if (text.includes(rule.keyword)) {
      total += rule.weight;
      matched.push(rule.keyword);
    }
  }
  return { total, matched };
}

/** Confidence is a soft squash of the winning score — enough keyword weight reads as "confident",
 * a lone weak match reads as "not really sure" so the caller can fall back to Unknown itself. */
function confidenceFrom(totalScore: number): number {
  return Math.max(0, Math.min(1, totalScore / (totalScore + 3)));
}

export function classify(rawBody: string): ClassificationResult {
  const text = rawBody.toLowerCase();

  const otp = score(text, rulesFor("OTP"));
  if (otp.total > 0 && OTP_CODE_PATTERN.test(rawBody)) {
    return { category: "OTP", confidence: confidenceFrom(otp.total + 1), matchedKeywords: otp.matched };
  }

  const transaction = score(text, rulesFor("TRANSACTIONS"));
  if (transaction.total > 0 && AMOUNT_PATTERN.test(rawBody)) {
    return { category: "TRANSACTIONS", confidence: confidenceFrom(transaction.total + 1), matchedKeywords: transaction.matched };
  }

  const promo = score(text, rulesFor("PROMOTIONS"));
  const others = score(text, rulesFor("OTHERS"));

  if (promo.total > 0 || others.total > 0) {
    if (promo.total >= others.total) {
      return { category: "PROMOTIONS", confidence: confidenceFrom(promo.total), matchedKeywords: promo.matched };
    }
    return { category: "OTHERS", confidence: confidenceFrom(others.total), matchedKeywords: others.matched };
  }

  return { category: "UNKNOWN", confidence: 0, matchedKeywords: [] };
}
