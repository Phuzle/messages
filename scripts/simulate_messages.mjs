#!/usr/bin/env node
// Feeds a realistic, cross-category batch of SMS into the connected emulator via the console's
// `sms send` command (real inbound path: SmsDeliverReceiver -> CategoryClassifier -> Room ->
// MessageNotifier) plus, for the two categories the app actually lets you reply to (Personal and
// Unknown — see Category.isReplyable), a genuine outgoing reply through DebugSimulationReceiver,
// which calls the same ThreadRepository.composeOutgoingThread() the real Compose screen uses.
//
// Unlike the old bash version, message *content* isn't drawn from a small fixed array of whole
// sentences — @faker-js/faker fills in the concrete details (names, merchants, amounts, cities,
// dates, tracking numbers, ...) into each scenario's template, so two runs read differently even
// with the same category mix. Pass --seed to make a run reproducible (faker.seed()); omit it and
// a random seed is picked and printed so you can reproduce that exact run later.
//
// Usage:
//   npm install                              (once, installs @faker-js/faker)
//   node simulate_messages.mjs [options]
//
// Options (all optional):
//   --seed <n>       reproducible faker seed (default: random, printed at the start of the run)
//   --exchanges <n>  two-way exchanges per Personal/Unknown contact (default: 15)
//   --oneway <n>     one-way messages per OTP/Transactions/Promotions/Others sender (default: 30)
//   --serial <id>    adb device serial, for when more than one is attached
//   --categories <list>  comma-separated subset to run, e.g. "otp,transactions" (default: all)

import { execFileSync } from "node:child_process";
import { parseArgs } from "node:util";
import { faker } from "@faker-js/faker";

const { values } = parseArgs({
  options: {
    seed: { type: "string" },
    exchanges: { type: "string", default: "15" },
    oneway: { type: "string", default: "30" },
    serial: { type: "string", default: "" },
    categories: { type: "string", default: "personal,unknown,otp,transactions,promotions,others" },
  },
});

const seed = values.seed ? Number(values.seed) : Math.floor(Math.random() * 1_000_000);
faker.seed(seed);
console.log(`Using seed ${seed} — pass --seed ${seed} to reproduce this exact run.`);

const EXCHANGES = Number(values.exchanges);
const ONE_WAY = Number(values.oneway);
const ACTIVE_CATEGORIES = new Set(values.categories.split(",").map((s) => s.trim().toLowerCase()));

const ANDROID_HOME = process.env.ANDROID_HOME ?? `${process.env.HOME}/Library/Android/sdk`;
const ADB = `${ANDROID_HOME}/platform-tools/adb`;
const SERIAL_ARGS = values.serial ? ["-s", values.serial] : [];
const PKG = "com.phuzle.labs.messages";

function adb(args) {
  execFileSync(ADB, [...SERIAL_ARGS, ...args], { stdio: "ignore" });
}

/** `adb shell <args>` re-joins all args into a single string and runs it via `sh -c` on the
 * *device*, not the host — so any argument execFileSync would otherwise pass through untouched
 * (spaces, apostrophes, quotes) needs to be quoted for that remote shell ourselves, or generated
 * text like "what's up?" silently truncates the command at the apostrophe. */
function shellQuote(value) {
  return `'${value.replace(/'/g, `'\\''`)}'`;
}

/** Inbound, real SMS_DELIVER path. Goes through the emulator console protocol, not a device
 * shell, so it doesn't need shellQuote. */
function sendSms(number, text) {
  adb(["emu", "sms", "send", number, text]);
}

/** Outgoing, real ThreadRepository path via the debug-only broadcast receiver — see
 * DebugSimulationReceiver.kt. HeadlessSmsSendService (the OS's own "quick reply" hook) only calls
 * SmsManager and never writes to Room, so there's no other way to simulate the *other side* of a
 * conversation. */
function sendReply(number, text) {
  adb([
    "shell", "am", "broadcast",
    "-n", `${PKG}/.core.debug.DebugSimulationReceiver`,
    "-a", "com.phuzle.labs.messages.debug.SIMULATE_OUTGOING",
    "--es", "number", shellQuote(number),
    "--es", "body", shellQuote(text),
  ]);
}

function money(min = 5, max = 900) {
  return faker.finance.amount({ min, max, dec: 2 });
}

function last4() {
  return faker.string.numeric(4);
}

// ---------------------------------------------------------------------------
// Personal: known contacts (provisioned by provision_contacts.sh), real two-way exchanges.
// ---------------------------------------------------------------------------
const PERSONAL_NUMBERS = ["+15550100001", "+15550100002", "+15550100003", "+15550100004"];

function personalExchange() {
  const name = faker.person.firstName();
  const place = faker.helpers.arrayElement([faker.location.city(), "the usual spot", faker.company.name()]);
  const time = `${faker.number.int({ min: 1, max: 9 })}${faker.helpers.arrayElement(["am", "pm"])}`;
  const day = faker.helpers.arrayElement(["tonight", "tomorrow", "this weekend", "Friday", "Saturday"]);
  const item = faker.commerce.product();
  return faker.helpers.arrayElement([
    [`Are we still on for ${day}?`, `Yes! See you at ${time}.`],
    [`Thanks for the ${faker.commerce.productAdjective().toLowerCase()} advice, really helpful!`, "Anytime, glad it helped."],
    [`See you ${day} at ${place}.`, "Sounds good, I'll be there."],
    [`Can you send me that ${item} link when you get a chance?`, "Sending it over now."],
    [`Running about ${faker.number.int({ min: 5, max: 30 })} minutes late, sorry!`, "No worries, take your time."],
    [`Happy birthday, ${name}! Hope it's a great one.`, "Thank you so much!"],
    [`Let's catch up ${day}.`, `I'm in, ${faker.helpers.arrayElement(["Saturday", "Sunday", "after work"])} works for me.`],
    ["Did you see the game last night?", "Yeah, what an ending!"],
    [`Can you pick up ${item} on your way home?`, "Sure, anything else you need?"],
    [`Are you free for a call ${day}?`, `Should be free after ${time}.`],
    [`Loved that ${faker.commerce.department().toLowerCase()} place you recommended.`, "So glad you tried it!"],
    [`Traffic near ${place} is brutal, might be ${faker.number.int({ min: 10, max: 30 })} late.`, "All good, drive safe."],
    ["Did the package arrive yet?", "Not yet, I'll let you know."],
    [`Can we push our meeting to ${time}?`, "Works for me, see you then."],
    ["Long day, talk tomorrow?", "Get some rest, talk soon."],
  ]);
}

// ---------------------------------------------------------------------------
// Unknown: not saved contacts, but the app still lets you reply — same two-way treatment.
// ---------------------------------------------------------------------------
const UNKNOWN_NUMBERS = ["18005555101", "18005555102", "18005555103"];

function unknownExchange() {
  const item = faker.commerce.product();
  const price = money(10, 500);
  return faker.helpers.arrayElement([
    [`Hey, is the ${item} still available?`, "Yes, still available!"],
    ["Call me when you get a chance.", "Will do, give me a bit."],
    ["Are you around this weekend?", "Should be, what's up?"],
    ["Thanks for reaching out, appreciate it.", "Of course, happy to help."],
    ["Got your message, will follow up shortly.", "Sounds good, thank you."],
    [`Is the $${price} price negotiable?`, "A little, what did you have in mind?"],
    ["Can we meet up tomorrow?", `Tomorrow works, ${faker.number.int({ min: 1, max: 9 })}pm okay?`],
  ]);
}

// ---------------------------------------------------------------------------
// OTP: needs an otp keyword + a 4-8 digit code.
// ---------------------------------------------------------------------------
const OTP_SENDERS = [
  { number: "18005551001", name: "Northgate Bank" },
  { number: "18005551002", name: "Summit Wireless" },
  { number: "18005551003", name: () => `${faker.company.name()} Pay` },
];

function otpMessage(senderName) {
  const code = faker.string.numeric({ length: faker.number.int({ min: 4, max: 8 }) });
  const template = faker.helpers.arrayElement([
    `Your ${senderName} verification code is ${code}. Do not share this code.`,
    `${senderName}: your one-time password is ${code}. Valid for ${faker.number.int({ min: 5, max: 15 })} minutes.`,
    `${senderName} authentication code: ${code}. Do not share with anyone.`,
    `Your ${senderName} security code is ${code}. Never share this with anyone.`,
    `${senderName} OTP: ${code}. If you did not request this, ignore this message.`,
  ]);
  return template;
}

// ---------------------------------------------------------------------------
// Transactions: needs a transaction keyword + an amount.
// ---------------------------------------------------------------------------
const TRANSACTION_NUMBERS = ["18005552001", "18005552002", "18005552003"];

function transactionMessage() {
  const amount = money();
  const merchant = faker.company.name();
  const l4 = last4();
  return faker.helpers.arrayElement([
    `You spent $${amount} at ${merchant} using your card ending ${l4}.`,
    `A payment of $${amount} for ${merchant} has posted to your account ending ${l4}.`,
    `Your account was debited $${amount} for ${merchant}, card ending ${l4}.`,
    `$${amount} was credited to your account ending ${l4} from ${merchant}.`,
    `Available balance is $${money(100, 5000)} on your ${faker.finance.accountName()} account ending ${l4}.`,
    `Your refund of $${amount} from ${merchant} has been processed to your account ending ${l4}.`,
  ]);
}

// ---------------------------------------------------------------------------
// Promotions: needs a promo keyword.
// ---------------------------------------------------------------------------
const PROMO_NUMBERS = ["18005553001", "18005553002"];

function promoMessage() {
  const brand = faker.company.name();
  const pct = faker.number.int({ min: 10, max: 70 });
  const code = faker.string.alphanumeric({ length: 6, casing: "upper" });
  return faker.helpers.arrayElement([
    `${brand}: get ${pct}% off your next order today only!`,
    `Exclusive offer from ${brand}: save big with code ${code}.`,
    `Limited time deal at ${brand}, free shipping on all orders this week.`,
    `Flash sale! Up to ${pct}% off select items at ${brand}, reply STOP to unsubscribe.`,
    `Clearance event at ${brand} starts now, exclusive deal for members.`,
  ]);
}

// ---------------------------------------------------------------------------
// Others: needs an "other" keyword (alerts/advisories/delivery/reminders).
// ---------------------------------------------------------------------------
const OTHERS_NUMBERS = ["18005554001", "18005554002", "18005554003"];

function othersMessage() {
  const ref = faker.string.alphanumeric({ length: 8, casing: "upper" });
  const city = faker.location.city();
  const time = `${faker.number.int({ min: 1, max: 12 })}${faker.helpers.arrayElement(["am", "pm"])}`;
  return faker.helpers.arrayElement([
    `Severe weather advisory in effect for ${city} until ${time}. Ref ${ref}.`,
    `Your package is out for delivery and should arrive today. Tracking ${ref}.`,
    `Reminder: your appointment is scheduled for tomorrow at ${time}. Confirmation ${ref}.`,
    `Service outage notice: scheduled maintenance tonight from 12-3 AM. Ticket ${ref}.`,
    `Your order has shipped from ${faker.company.name()}. Tracking number ${ref}.`,
    `Reminder: payment due by ${faker.date.soon({ days: 7 }).toDateString()}. Reference ${ref}.`,
  ]);
}

// ---------------------------------------------------------------------------

function runTwoWay(label, numbers, exchangeFn) {
  console.log(`[${label}] (2-way)...`);
  for (const number of numbers) {
    for (let i = 0; i < EXCHANGES; i++) {
      const [inbound, reply] = exchangeFn();
      sendSms(number, inbound);
      sendReply(number, reply);
    }
  }
}

function runOneWay(label, numbers, messageFn) {
  console.log(`[${label}] (inbound)...`);
  for (let i = 0; i < ONE_WAY; i++) {
    const number = numbers[i % numbers.length];
    sendSms(number, messageFn());
  }
}

if (ACTIVE_CATEGORIES.has("personal")) runTwoWay("Personal", PERSONAL_NUMBERS, personalExchange);
if (ACTIVE_CATEGORIES.has("unknown")) runTwoWay("Unknown", UNKNOWN_NUMBERS, unknownExchange);
if (ACTIVE_CATEGORIES.has("otp")) {
  runOneWay(
    "OTP",
    OTP_SENDERS.map((s) => s.number),
    () => otpMessage(faker.helpers.arrayElement(OTP_SENDERS.map((s) => (typeof s.name === "function" ? s.name() : s.name)))),
  );
}
if (ACTIVE_CATEGORIES.has("transactions")) runOneWay("Transactions", TRANSACTION_NUMBERS, transactionMessage);
if (ACTIVE_CATEGORIES.has("promotions")) runOneWay("Promotions", PROMO_NUMBERS, promoMessage);
if (ACTIVE_CATEGORIES.has("others")) runOneWay("Others", OTHERS_NUMBERS, othersMessage);

console.log("Done.");
