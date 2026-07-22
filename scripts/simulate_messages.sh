#!/usr/bin/env bash
# Feeds a large, realistic, cross-category batch of SMS into the connected emulator via the
# console's `sms send` command — this exercises the *real* production path end to end
# (SmsDeliverReceiver -> CategoryClassifier -> Room -> MessageNotifier), not a mocked shortcut.
#
# Usage: ./scripts/simulate_messages.sh [count_per_category] [adb serial]
#   count_per_category defaults to 100. There are 6 categories, so the default run sends 600
#   messages total. Personal messages are sent from the numbers provisioned by
#   provision_contacts.sh (run that first, or Personal texts just land in Unknown instead).
#
# Note on `printf`: bash's printf *recycles* its format string for as long as there are
# leftover arguments, even ones the format has no conversion for — so every template within a
# category below is written with the exact same number of %s placeholders, and each category's
# loop always passes exactly that many arguments. Don't add a template with a different
# placeholder count without updating the printf call to match.

set -eo pipefail

ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"
COUNT="${1:-100}"
SERIAL="${2:-}"
SERIAL_ARGS=()
if [ -n "$SERIAL" ]; then
  SERIAL_ARGS=(-s "$SERIAL")
fi

send_sms() {
  local number="$1" text="$2"
  "$ADB" "${SERIAL_ARGS[@]}" emu sms send "$number" "$text" > /dev/null
}

rand_between() { # $1=min $2=max
  echo $(( $1 + RANDOM % ($2 - $1 + 1) ))
}

rand_digits() { # $1=how many digits
  local n="$1" out=""
  for ((i = 0; i < n; i++)); do out="${out}$((RANDOM % 10))"; done
  echo "$out"
}

# --- Personal: known contacts provisioned by provision_contacts.sh (no placeholders) ---
PERSONAL_NUMBERS=(+15550100001 +15550100002 +15550100003 +15550100004 +15550100005 +15550100006 +15550100007 +15550100008)
PERSONAL_TEMPLATES=(
  "Are we still on for dinner tonight?"
  "Thanks for the notes, really helpful!"
  "See you tomorrow at the usual spot."
  "Can you send me that file when you get a chance?"
  "Sounds good, talk soon!"
  "Running about 10 minutes late, sorry!"
  "Happy birthday! Hope it's a great one."
  "Let's catch up this weekend."
  "Did you see the game last night?"
  "Can you pick up milk on your way home?"
)

# --- OTP: needs an otp keyword + a 4-8 digit code. Every template takes (sender, code). ---
OTP_NUMBERS=(18005551001 18005551002 18005551003)
OTP_SENDER_NAMES=("Northgate Bank" "Summit Wireless" "Horizon Card Services")
OTP_TEMPLATES=(
  "Your %s verification code is %s. Do not share this code."
  "%s: your one-time password is %s. Valid for 10 minutes."
  "%s authentication code: %s. Do not share with anyone."
  "Your %s security code is %s. Never share this with anyone."
  "%s OTP: %s. If you did not request this, ignore this message."
)

# --- Transactions: needs a transaction keyword + an amount. Every template takes (amount, merchant, last4). ---
TRANSACTION_NUMBERS=(18005552001 18005552002 18005552003)
MERCHANTS=("Corner Cafe" "Fleet Deliveries" "MegaMart" "Downtown Parking" "QuickMerchant" "City Transit")
TRANSACTION_TEMPLATES=(
  "You spent \$%s at %s using your card ending %s."
  "A payment of \$%s for %s has posted to your account ending %s."
  "Your account was debited \$%s for %s, card ending %s."
  "\$%s was credited to your account ending %s from %s."
  "Available balance is \$%s on your %s account ending %s."
)

# --- Promotions: needs a promo keyword. Used verbatim, no placeholders. ---
PROMO_NUMBERS=(18005553001 18005553002 18005553003)
PROMO_TEMPLATES=(
  "Get 50% off your next order today only!"
  "Exclusive offer: save big with coupon code SAVE20."
  "Limited time deal, free shipping on all orders this week."
  "Flash sale! Up to 70% off select items, reply STOP to unsubscribe."
  "Clearance event starts now, exclusive deal for our members."
)

# --- Others: needs an "other" keyword (alerts/advisories/delivery/reminders). Every template takes (refCode). ---
OTHERS_NUMBERS=(18005554001 18005554002 18005554003)
OTHERS_TEMPLATES=(
  "Severe weather advisory in effect for your area until 8 PM. Ref %s."
  "Your package is out for delivery and should arrive today. Tracking %s."
  "Reminder: your appointment is scheduled for tomorrow at 10 AM. Confirmation %s."
  "Service outage notice: scheduled maintenance tonight from 12-3 AM. Ticket %s."
  "Your order has shipped. Tracking number %s."
)

# --- Unknown: no keywords, sender not a known contact (no placeholders). ---
UNKNOWN_NUMBERS=(18005555101 18005555102 18005555103 18005555104 18005555105)
UNKNOWN_TEMPLATES=(
  "Hey is this available?"
  "Call me when you get a chance."
  "Are you around this weekend?"
  "Thanks for reaching out, appreciate it."
  "See you soon."
  "Got your message, will follow up shortly."
)

echo "Sending $COUNT messages per category (6 categories, $((COUNT * 6)) total)..."

echo "[1/6] Personal..."
for ((i = 0; i < COUNT; i++)); do
  number="${PERSONAL_NUMBERS[$((i % ${#PERSONAL_NUMBERS[@]}))]}"
  text="${PERSONAL_TEMPLATES[$((RANDOM % ${#PERSONAL_TEMPLATES[@]}))]}"
  send_sms "$number" "$text"
done

echo "[2/6] OTP..."
for ((i = 0; i < COUNT; i++)); do
  idx=$((i % ${#OTP_NUMBERS[@]}))
  number="${OTP_NUMBERS[$idx]}"
  senderName="${OTP_SENDER_NAMES[$idx]}"
  template="${OTP_TEMPLATES[$((RANDOM % ${#OTP_TEMPLATES[@]}))]}"
  code=$(rand_digits "$(rand_between 4 8)")
  text=$(printf "$template" "$senderName" "$code")
  send_sms "$number" "$text"
done

echo "[3/6] Transactions..."
for ((i = 0; i < COUNT; i++)); do
  number="${TRANSACTION_NUMBERS[$((i % ${#TRANSACTION_NUMBERS[@]}))]}"
  merchant="${MERCHANTS[$((RANDOM % ${#MERCHANTS[@]}))]}"
  template="${TRANSACTION_TEMPLATES[$((RANDOM % ${#TRANSACTION_TEMPLATES[@]}))]}"
  amount="$(rand_between 5 900).$(printf '%02d' $((RANDOM % 100)))"
  last4=$(rand_digits 4)
  text=$(printf "$template" "$amount" "$merchant" "$last4")
  send_sms "$number" "$text"
done

echo "[4/6] Promotions..."
for ((i = 0; i < COUNT; i++)); do
  number="${PROMO_NUMBERS[$((i % ${#PROMO_NUMBERS[@]}))]}"
  text="${PROMO_TEMPLATES[$((RANDOM % ${#PROMO_TEMPLATES[@]}))]}"
  send_sms "$number" "$text"
done

echo "[5/6] Others..."
for ((i = 0; i < COUNT; i++)); do
  number="${OTHERS_NUMBERS[$((i % ${#OTHERS_NUMBERS[@]}))]}"
  template="${OTHERS_TEMPLATES[$((RANDOM % ${#OTHERS_TEMPLATES[@]}))]}"
  refCode=$(rand_digits 6)
  text=$(printf "$template" "$refCode")
  send_sms "$number" "$text"
done

echo "[6/6] Unknown..."
for ((i = 0; i < COUNT; i++)); do
  number="${UNKNOWN_NUMBERS[$((i % ${#UNKNOWN_NUMBERS[@]}))]}"
  text="${UNKNOWN_TEMPLATES[$((RANDOM % ${#UNKNOWN_TEMPLATES[@]}))]}"
  send_sms "$number" "$text"
done

echo "Done — sent $((COUNT * 6)) messages."
