/**
 * Best-effort "is this a reminder, and when is it due" extractor — the piece Passbook's Reminders
 * tab was missing (see PassbookRepository's doc comment on the Android side: it stayed genuinely
 * empty rather than seeded with fake data because this needed Layer 2/3 AI this project didn't
 * have yet). This is regex/keyword date parsing, not an LLM — it only recognizes a deliberately
 * narrow set of phrasings, and only returns `isReminder: true` when it found both a trigger phrase
 * *and* a due date/time it could actually resolve to a timestamp; a trigger phrase alone isn't
 * enough to be useful to the user.
 */

export interface ReminderExtraction {
  isReminder: boolean;
  title?: string;
  detail?: string;
  dueAtEpochMillis?: number;
}

const TRIGGER_PHRASES = [
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

const WEEKDAYS = ["sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"];
const MONTHS = [
  "january", "february", "march", "april", "may", "june",
  "july", "august", "september", "october", "november", "december",
];

function hasTrigger(text: string): boolean {
  return TRIGGER_PHRASES.some((p) => text.includes(p));
}

/** Extracts hour/minute (24h) from a "10am" / "10:30 pm" / "22:00" style fragment, if present. */
function extractTime(text: string): { hour: number; minute: number } | null {
  const match = text.match(/\b(\d{1,2})(?::(\d{2}))?\s?(am|pm)?\b/i);
  if (!match) return null;
  let hour = parseInt(match[1], 10);
  const minute = match[2] ? parseInt(match[2], 10) : 0;
  const meridiem = match[3]?.toLowerCase();
  if (hour > 23 || minute > 59) return null;
  if (meridiem === "pm" && hour < 12) hour += 12;
  if (meridiem === "am" && hour === 12) hour = 0;
  if (!meridiem && hour > 12) return null; // ambiguous 24h-looking value without context, skip
  return { hour, minute };
}

/**
 * Resolves a due date relative to [receivedAt] using [timezoneOffsetMinutes] (minutes east of
 * UTC, e.g. -330 is not valid but +330 is IST) so "tomorrow at 10am" means 10am in the sender's
 * timezone, not the server's. Defaults to UTC when the client doesn't supply one.
 */
export function extractReminder(
  rawBody: string,
  receivedAtEpochMillis: number,
  timezoneOffsetMinutes = 0,
): ReminderExtraction {
  const text = rawBody.toLowerCase();
  if (!hasTrigger(text)) return { isReminder: false };

  const offsetMillis = timezoneOffsetMinutes * 60_000;
  const localReceived = new Date(receivedAtEpochMillis + offsetMillis);
  const time = extractTime(text) ?? { hour: 9, minute: 0 }; // sensible default when no time is stated

  let localDue: Date | null = null;

  if (text.includes("today")) {
    localDue = new Date(localReceived);
  } else if (text.includes("tomorrow")) {
    localDue = new Date(localReceived);
    localDue.setUTCDate(localDue.getUTCDate() + 1);
  } else {
    const weekdayIdx = WEEKDAYS.findIndex((day) => text.includes(day));
    if (weekdayIdx >= 0) {
      localDue = new Date(localReceived);
      const currentDay = localDue.getUTCDay();
      let daysAhead = weekdayIdx - currentDay;
      if (daysAhead <= 0) daysAhead += 7;
      localDue.setUTCDate(localDue.getUTCDate() + daysAhead);
    } else {
      // "25 July" or "July 25"
      const dayMonth = text.match(/\b(\d{1,2})(?:st|nd|rd|th)?\s+([a-z]+)\b/);
      const monthDay = text.match(/\b([a-z]+)\s+(\d{1,2})(?:st|nd|rd|th)?\b/);
      let day: number | null = null;
      let monthIdx: number | null = null;
      if (dayMonth && MONTHS.includes(dayMonth[2])) {
        day = parseInt(dayMonth[1], 10);
        monthIdx = MONTHS.indexOf(dayMonth[2]);
      } else if (monthDay && MONTHS.includes(monthDay[1])) {
        day = parseInt(monthDay[2], 10);
        monthIdx = MONTHS.indexOf(monthDay[1]);
      }
      if (day !== null && monthIdx !== null && day >= 1 && day <= 31) {
        localDue = new Date(localReceived);
        localDue.setUTCMonth(monthIdx, day);
        if (localDue.getTime() < localReceived.getTime()) {
          localDue.setUTCFullYear(localDue.getUTCFullYear() + 1);
        }
      }
    }
  }

  if (!localDue) return { isReminder: false };

  localDue.setUTCHours(time.hour, time.minute, 0, 0);
  const dueAtEpochMillis = localDue.getTime() - offsetMillis;

  return {
    isReminder: true,
    title: summarize(rawBody),
    detail: rawBody.trim().slice(0, 160),
    dueAtEpochMillis,
  };
}

function summarize(body: string): string {
  const firstSentence = body.split(/[.!?\n]/)[0]?.trim() ?? body.trim();
  return firstSentence.length > 60 ? `${firstSentence.slice(0, 57)}...` : firstSentence;
}
