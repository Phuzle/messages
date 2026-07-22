/**
 * Layer 3 of the PRD's pipeline: a PII-scrubbed cloud fallback for messages the on-device Layer 1
 * regex classifier (app/src/main/assets/regex_rules.json) couldn't confidently place — it only
 * ever sees a message after the client has already redacted account numbers, card numbers, OTP
 * codes, emails, and phone numbers (see the Android PiiScrubber). This is a weighted keyword
 * heuristic, not a trained model — there's no labeled dataset to train on here, and overclaiming
 * "AI" for a keyword scorer would be dishonest. It's deliberately a *superset* of the client's
 * rules (more keywords, weighted combinations) so it's worth the round trip.
 */

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

const OTP_RULES: WeightedRule[] = [
  { keyword: "otp", weight: 3 },
  { keyword: "one-time password", weight: 3 },
  { keyword: "one time password", weight: 3 },
  { keyword: "verification code", weight: 3 },
  { keyword: "security code", weight: 2.5 },
  { keyword: "authentication code", weight: 2.5 },
  { keyword: "login code", weight: 2.5 },
  { keyword: "access code", weight: 2 },
  { keyword: "passcode", weight: 2 },
  { keyword: "confirmation code", weight: 2 },
  { keyword: "do not share", weight: 1.5 },
  { keyword: "never share", weight: 1.5 },
  { keyword: "valid for", weight: 1 },
  { keyword: "expires in", weight: 1 },
];
const OTP_CODE_PATTERN = /\b\d{4,8}\b/;

const TRANSACTION_RULES: WeightedRule[] = [
  { keyword: "debited", weight: 3 },
  { keyword: "credited", weight: 3 },
  { keyword: "spent", weight: 2.5 },
  { keyword: "payment of", weight: 2.5 },
  { keyword: "transaction of", weight: 2.5 },
  { keyword: "withdrawn", weight: 2.5 },
  { keyword: "balance", weight: 1.5 },
  { keyword: "account ending", weight: 2 },
  { keyword: "card ending", weight: 2 },
  { keyword: "available limit", weight: 1.5 },
  { keyword: "minimum due", weight: 2 },
  { keyword: "statement", weight: 1 },
  { keyword: "emi", weight: 1.5 },
  { keyword: "upi", weight: 1.5 },
  { keyword: "invoice", weight: 1 },
  { keyword: "refund", weight: 1.5 },
];
const AMOUNT_PATTERN = /(?:\$|rs\.?|inr|usd|€|£)\s?\d[\d,]*\.?\d*/i;

const PROMOTION_RULES: WeightedRule[] = [
  { keyword: "% off", weight: 3 },
  { keyword: "percent off", weight: 3 },
  { keyword: "discount", weight: 2 },
  { keyword: "coupon", weight: 2.5 },
  { keyword: "promo code", weight: 2.5 },
  { keyword: "free shipping", weight: 2 },
  { keyword: "clearance", weight: 2 },
  { keyword: "flash sale", weight: 2.5 },
  { keyword: "limited time", weight: 1.5 },
  { keyword: "exclusive offer", weight: 2 },
  { keyword: "unsubscribe", weight: 1.5 },
  { keyword: "reply stop", weight: 1.5 },
  { keyword: "sale ends", weight: 1.5 },
  { keyword: "shop now", weight: 1.5 },
];

const OTHERS_RULES: WeightedRule[] = [
  { keyword: "advisory", weight: 2 },
  { keyword: "alert", weight: 1.5 },
  { keyword: "notice", weight: 1 },
  { keyword: "reminder:", weight: 2 },
  { keyword: "has shipped", weight: 2 },
  { keyword: "out for delivery", weight: 2.5 },
  { keyword: "delivered", weight: 1.5 },
  { keyword: "appointment", weight: 2 },
  { keyword: "scheduled maintenance", weight: 2.5 },
  { keyword: "service outage", weight: 2.5 },
  { keyword: "weather warning", weight: 2.5 },
  { keyword: "evacuation", weight: 2.5 },
  { keyword: "school closing", weight: 2 },
  { keyword: "flight status", weight: 2 },
  { keyword: "gate change", weight: 2 },
  { keyword: "tracking number", weight: 2 },
  { keyword: "your order", weight: 1 },
];

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

  const otp = score(text, OTP_RULES);
  if (otp.total > 0 && OTP_CODE_PATTERN.test(rawBody)) {
    return { category: "OTP", confidence: confidenceFrom(otp.total + 1), matchedKeywords: otp.matched };
  }

  const transaction = score(text, TRANSACTION_RULES);
  if (transaction.total > 0 && AMOUNT_PATTERN.test(rawBody)) {
    return { category: "TRANSACTIONS", confidence: confidenceFrom(transaction.total + 1), matchedKeywords: transaction.matched };
  }

  const promo = score(text, PROMOTION_RULES);
  const others = score(text, OTHERS_RULES);

  if (promo.total > 0 || others.total > 0) {
    if (promo.total >= others.total) {
      return { category: "PROMOTIONS", confidence: confidenceFrom(promo.total), matchedKeywords: promo.matched };
    }
    return { category: "OTHERS", confidence: confidenceFrom(others.total), matchedKeywords: others.matched };
  }

  return { category: "UNKNOWN", confidence: 0, matchedKeywords: [] };
}
