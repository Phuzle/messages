#!/usr/bin/env bash
# Feeds a realistic, cross-category batch of SMS into the connected emulator via the console's
# `sms send` command (real inbound path: SmsDeliverReceiver -> CategoryClassifier -> Room ->
# MessageNotifier) plus, for the two categories the app actually lets you reply to (Personal and
# Unknown — see Category.isReplyable), a genuine outgoing reply through DebugSimulationReceiver,
# which calls the same ThreadRepository.composeOutgoingThread() the real Compose screen uses.
# `HeadlessSmsSendService` (the OS's own "quick text reply" hook) only calls SmsManager and never
# writes to Room, so there was previously no way to simulate the *other side* of a conversation.
#
# Usage: ./scripts/simulate_messages.sh [exchanges_per_contact] [adb serial]
#   exchanges_per_contact defaults to 15 (one inbound + one outgoing each = 30 messages per
#   contact). Personal uses the first 4 numbers provisioned by provision_contacts.sh (run that
#   first, or those texts just land in Unknown). Fewer, richer senders reads like real ongoing
#   relationships instead of a one-off blast from a dozen strangers.
#
# Note on `printf`: bash's printf *recycles* its format string for as long as there are leftover
# arguments, even ones the format has no conversion for — so every template within a category
# below is written with the exact same number of %s placeholders, and each category's loop always
# passes exactly that many arguments. Don't add a template with a different placeholder count
# without updating the printf call to match.

set -eo pipefail

ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"
EXCHANGES="${1:-15}"
ONE_WAY_COUNT="${3:-30}"
SERIAL="${2:-}"
SERIAL_ARGS=()
if [ -n "$SERIAL" ]; then
  SERIAL_ARGS=(-s "$SERIAL")
fi

PKG="com.phuzle.labs.messages"

send_sms() { # inbound, real SMS_DELIVER path
  local number="$1" text="$2"
  "$ADB" "${SERIAL_ARGS[@]}" emu sms send "$number" "$text" > /dev/null
}

send_reply() { # outgoing, real ThreadRepository path via the debug-only broadcast receiver
  local number="$1" text="$2"
  "$ADB" "${SERIAL_ARGS[@]}" shell am broadcast \
    -n "$PKG/.core.debug.DebugSimulationReceiver" \
    -a com.phuzle.labs.messages.debug.SIMULATE_OUTGOING \
    --es number "$number" --es body "$text" > /dev/null
}

rand_between() { # $1=min $2=max
  echo $(( $1 + RANDOM % ($2 - $1 + 1) ))
}

rand_digits() { # $1=how many digits
  local n="$1" out=""
  for ((i = 0; i < n; i++)); do out="${out}$((RANDOM % 10))"; done
  echo "$out"
}

# --- Personal: known contacts (provisioned by provision_contacts.sh), real two-way exchanges. ---
PERSONAL_NUMBERS=(+15550100001 +15550100002 +15550100003 +15550100004)
# Each entry is "inbound line|reply line" — a believable one-turn exchange, not just a keyword blast.
PERSONAL_EXCHANGES=(
  "Are we still on for dinner tonight?|Yes! See you at 7."
  "Thanks for the notes, really helpful!|Anytime, glad it helped."
  "See you tomorrow at the usual spot.|Sounds good, I'll be there."
  "Can you send me that file when you get a chance?|Sending it over now."
  "Running about 10 minutes late, sorry!|No worries, take your time."
  "Happy birthday! Hope it's a great one.|Thank you so much!"
  "Let's catch up this weekend.|I'm in, Saturday works for me."
  "Did you see the game last night?|Yeah, what an ending!"
  "Can you pick up milk on your way home?|Sure, anything else you need?"
  "Are you free for a call later?|Should be free after 5."
  "Loved that restaurant you recommended.|So glad you tried it!"
  "Traffic is brutal, might be 20 late.|All good, drive safe."
  "Did the package arrive yet?|Not yet, I'll let you know."
  "Can we push our meeting to 3pm?|Works for me, see you then."
  "Long day, talk tomorrow?|Get some rest, talk soon."
)

# --- Unknown: not saved contacts, but the app still lets you reply — same two-way treatment. ---
UNKNOWN_NUMBERS=(18005555101 18005555102 18005555103)
UNKNOWN_EXCHANGES=(
  "Hey is this available?|Yes, still available!"
  "Call me when you get a chance.|Will do, give me a bit."
  "Are you around this weekend?|Should be, what's up?"
  "Thanks for reaching out, appreciate it.|Of course, happy to help."
  "Got your message, will follow up shortly.|Sounds good, thank you."
  "Is the price negotiable?|A little, what did you have in mind?"
  "Can we meet up tomorrow?|Tomorrow works, what time?"
)

# --- OTP: needs an otp keyword + a 4-8 digit code. Every template takes (sender, code). ---
OTP_NUMBERS=(18005551001 18005551002)
OTP_SENDER_NAMES=("Northgate Bank" "Summit Wireless")
OTP_TEMPLATES=(
  "Your %s verification code is %s. Do not share this code."
  "%s: your one-time password is %s. Valid for 10 minutes."
  "%s authentication code: %s. Do not share with anyone."
  "Your %s security code is %s. Never share this with anyone."
  "%s OTP: %s. If you did not request this, ignore this message."
)

# --- Transactions: needs a transaction keyword + an amount. Every template takes (amount, merchant, last4). ---
TRANSACTION_NUMBERS=(18005552001 18005552002)
MERCHANTS=("Corner Cafe" "Fleet Deliveries" "MegaMart" "Downtown Parking" "QuickMerchant" "City Transit")
TRANSACTION_TEMPLATES=(
  "You spent \$%s at %s using your card ending %s."
  "A payment of \$%s for %s has posted to your account ending %s."
  "Your account was debited \$%s for %s, card ending %s."
  "\$%s was credited to your account ending %s from %s."
  "Available balance is \$%s on your %s account ending %s."
)

# --- Promotions: needs a promo keyword. Used verbatim, no placeholders. ---
PROMO_NUMBERS=(18005553001 18005553002)
PROMO_TEMPLATES=(
  "Get 50% off your next order today only!"
  "Exclusive offer: save big with coupon code SAVE20."
  "Limited time deal, free shipping on all orders this week."
  "Flash sale! Up to 70% off select items, reply STOP to unsubscribe."
  "Clearance event starts now, exclusive deal for our members."
)

# --- Others: needs an "other" keyword (alerts/advisories/delivery/reminders). Every template takes (refCode). ---
OTHERS_NUMBERS=(18005554001 18005554002)
OTHERS_TEMPLATES=(
  "Severe weather advisory in effect for your area until 8 PM. Ref %s."
  "Your package is out for delivery and should arrive today. Tracking %s."
  "Reminder: your appointment is scheduled for tomorrow at 10 AM. Confirmation %s."
  "Service outage notice: scheduled maintenance tonight from 12-3 AM. Ticket %s."
  "Your order has shipped. Tracking number %s."
)

total_two_way=$(( (${#PERSONAL_NUMBERS[@]} + ${#UNKNOWN_NUMBERS[@]}) * EXCHANGES * 2 ))
total_one_way=$(( (${#OTP_NUMBERS[@]} + ${#TRANSACTION_NUMBERS[@]} + ${#PROMO_NUMBERS[@]} + ${#OTHERS_NUMBERS[@]}) * ONE_WAY_COUNT ))
echo "Sending $EXCHANGES exchanges/contact for Personal+Unknown ($total_two_way messages, real 2-way)"
echo "and $ONE_WAY_COUNT one-way messages/sender for OTP/Transactions/Promotions/Others ($total_one_way messages)..."

echo "[1/6] Personal (2-way)..."
for number in "${PERSONAL_NUMBERS[@]}"; do
  for ((i = 0; i < EXCHANGES; i++)); do
    exchange="${PERSONAL_EXCHANGES[$((RANDOM % ${#PERSONAL_EXCHANGES[@]}))]}"
    inbound="${exchange%%|*}"
    reply="${exchange##*|}"
    send_sms "$number" "$inbound"
    send_reply "$number" "$reply"
  done
done

echo "[2/6] Unknown (2-way)..."
for number in "${UNKNOWN_NUMBERS[@]}"; do
  for ((i = 0; i < EXCHANGES; i++)); do
    exchange="${UNKNOWN_EXCHANGES[$((RANDOM % ${#UNKNOWN_EXCHANGES[@]}))]}"
    inbound="${exchange%%|*}"
    reply="${exchange##*|}"
    send_sms "$number" "$inbound"
    send_reply "$number" "$reply"
  done
done

echo "[3/6] OTP..."
for ((i = 0; i < ONE_WAY_COUNT; i++)); do
  idx=$((i % ${#OTP_NUMBERS[@]}))
  number="${OTP_NUMBERS[$idx]}"
  senderName="${OTP_SENDER_NAMES[$idx]}"
  template="${OTP_TEMPLATES[$((RANDOM % ${#OTP_TEMPLATES[@]}))]}"
  code=$(rand_digits "$(rand_between 4 8)")
  text=$(printf "$template" "$senderName" "$code")
  send_sms "$number" "$text"
done

echo "[4/6] Transactions..."
for ((i = 0; i < ONE_WAY_COUNT; i++)); do
  number="${TRANSACTION_NUMBERS[$((i % ${#TRANSACTION_NUMBERS[@]}))]}"
  merchant="${MERCHANTS[$((RANDOM % ${#MERCHANTS[@]}))]}"
  template="${TRANSACTION_TEMPLATES[$((RANDOM % ${#TRANSACTION_TEMPLATES[@]}))]}"
  amount="$(rand_between 5 900).$(printf '%02d' $((RANDOM % 100)))"
  last4=$(rand_digits 4)
  text=$(printf "$template" "$amount" "$merchant" "$last4")
  send_sms "$number" "$text"
done

echo "[5/6] Promotions..."
for ((i = 0; i < ONE_WAY_COUNT; i++)); do
  number="${PROMO_NUMBERS[$((i % ${#PROMO_NUMBERS[@]}))]}"
  text="${PROMO_TEMPLATES[$((RANDOM % ${#PROMO_TEMPLATES[@]}))]}"
  send_sms "$number" "$text"
done

echo "[6/6] Others..."
for ((i = 0; i < ONE_WAY_COUNT; i++)); do
  number="${OTHERS_NUMBERS[$((i % ${#OTHERS_NUMBERS[@]}))]}"
  template="${OTHERS_TEMPLATES[$((RANDOM % ${#OTHERS_TEMPLATES[@]}))]}"
  refCode=$(rand_digits 6)
  text=$(printf "$template" "$refCode")
  send_sms "$number" "$text"
done

echo "Done — sent $((total_two_way + total_one_way)) messages total."
