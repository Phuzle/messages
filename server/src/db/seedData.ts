/**
 * Bootstrap values only — copied in once when `keyword_rules`/`trigger_phrases` are empty (a
 * fresh `data/keywords.sqlite`). After that first seed, the tables are the source of truth and
 * these arrays are never consulted again; all real edits happen through the /v1/keywords and
 * /v1/reminders/triggers CRUD endpoints, not by changing this file.
 */

export type KeywordCategory = "OTP" | "TRANSACTIONS" | "PROMOTIONS" | "OTHERS";

export interface DefaultKeywordRule {
  category: KeywordCategory;
  keyword: string;
  weight: number;
}

export const DEFAULT_KEYWORD_RULES: DefaultKeywordRule[] = [
  // OTP
  { category: "OTP", keyword: "otp", weight: 3 },
  { category: "OTP", keyword: "one-time password", weight: 3 },
  { category: "OTP", keyword: "one time password", weight: 3 },
  { category: "OTP", keyword: "verification code", weight: 3 },
  { category: "OTP", keyword: "security code", weight: 2.5 },
  { category: "OTP", keyword: "authentication code", weight: 2.5 },
  { category: "OTP", keyword: "login code", weight: 2.5 },
  { category: "OTP", keyword: "access code", weight: 2 },
  { category: "OTP", keyword: "passcode", weight: 2 },
  { category: "OTP", keyword: "confirmation code", weight: 2 },
  { category: "OTP", keyword: "do not share", weight: 1.5 },
  { category: "OTP", keyword: "never share", weight: 1.5 },
  { category: "OTP", keyword: "valid for", weight: 1 },
  { category: "OTP", keyword: "expires in", weight: 1 },

  // Transactions
  { category: "TRANSACTIONS", keyword: "debited", weight: 3 },
  { category: "TRANSACTIONS", keyword: "credited", weight: 3 },
  { category: "TRANSACTIONS", keyword: "spent", weight: 2.5 },
  { category: "TRANSACTIONS", keyword: "payment of", weight: 2.5 },
  { category: "TRANSACTIONS", keyword: "transaction of", weight: 2.5 },
  { category: "TRANSACTIONS", keyword: "withdrawn", weight: 2.5 },
  { category: "TRANSACTIONS", keyword: "balance", weight: 1.5 },
  { category: "TRANSACTIONS", keyword: "account ending", weight: 2 },
  { category: "TRANSACTIONS", keyword: "card ending", weight: 2 },
  { category: "TRANSACTIONS", keyword: "available limit", weight: 1.5 },
  { category: "TRANSACTIONS", keyword: "minimum due", weight: 2 },
  { category: "TRANSACTIONS", keyword: "statement", weight: 1 },
  { category: "TRANSACTIONS", keyword: "emi", weight: 1.5 },
  { category: "TRANSACTIONS", keyword: "upi", weight: 1.5 },
  { category: "TRANSACTIONS", keyword: "invoice", weight: 1 },
  { category: "TRANSACTIONS", keyword: "refund", weight: 1.5 },

  // Promotions
  { category: "PROMOTIONS", keyword: "% off", weight: 3 },
  { category: "PROMOTIONS", keyword: "percent off", weight: 3 },
  { category: "PROMOTIONS", keyword: "discount", weight: 2 },
  { category: "PROMOTIONS", keyword: "coupon", weight: 2.5 },
  { category: "PROMOTIONS", keyword: "promo code", weight: 2.5 },
  { category: "PROMOTIONS", keyword: "free shipping", weight: 2 },
  { category: "PROMOTIONS", keyword: "clearance", weight: 2 },
  { category: "PROMOTIONS", keyword: "flash sale", weight: 2.5 },
  { category: "PROMOTIONS", keyword: "limited time", weight: 1.5 },
  { category: "PROMOTIONS", keyword: "exclusive offer", weight: 2 },
  { category: "PROMOTIONS", keyword: "unsubscribe", weight: 1.5 },
  { category: "PROMOTIONS", keyword: "reply stop", weight: 1.5 },
  { category: "PROMOTIONS", keyword: "sale ends", weight: 1.5 },
  { category: "PROMOTIONS", keyword: "shop now", weight: 1.5 },

  // Others
  { category: "OTHERS", keyword: "advisory", weight: 2 },
  { category: "OTHERS", keyword: "alert", weight: 1.5 },
  { category: "OTHERS", keyword: "notice", weight: 1 },
  { category: "OTHERS", keyword: "reminder:", weight: 2 },
  { category: "OTHERS", keyword: "has shipped", weight: 2 },
  { category: "OTHERS", keyword: "out for delivery", weight: 2.5 },
  { category: "OTHERS", keyword: "delivered", weight: 1.5 },
  { category: "OTHERS", keyword: "appointment", weight: 2 },
  { category: "OTHERS", keyword: "scheduled maintenance", weight: 2.5 },
  { category: "OTHERS", keyword: "service outage", weight: 2.5 },
  { category: "OTHERS", keyword: "weather warning", weight: 2.5 },
  { category: "OTHERS", keyword: "evacuation", weight: 2.5 },
  { category: "OTHERS", keyword: "school closing", weight: 2 },
  { category: "OTHERS", keyword: "flight status", weight: 2 },
  { category: "OTHERS", keyword: "gate change", weight: 2 },
  { category: "OTHERS", keyword: "tracking number", weight: 2 },
  { category: "OTHERS", keyword: "your order", weight: 1 },
];

export const DEFAULT_TRIGGER_PHRASES: string[] = [
  "reminder",
  "due on",
  "due by",
  "payment due",
  "appointment",
  "scheduled for",
  "renew",
  "renewal",
  "expires on",
  "expiring on",
  "meeting",
  "rsvp",
  "confirm your",
  "please confirm",
];
