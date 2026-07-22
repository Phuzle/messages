#!/usr/bin/env bash
# Adds a handful of test contacts to the connected emulator/device so the app's real
# ContactsContract.PhoneLookup-based classifier can genuinely resolve some senders as
# Category.Personal (rather than falling back to Unknown for every unsaved number).
#
# Usage: ./scripts/provision_contacts.sh [adb serial]

set -eo pipefail

ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"
SERIAL_ARGS=()
if [ "${1:-}" != "" ]; then
  SERIAL_ARGS=(-s "$1")
fi

adb_shell() {
  "$ADB" "${SERIAL_ARGS[@]}" shell "$1"
}

# name|phone (E.164-ish, matches the numbers the message simulation script sends "Personal" texts from)
CONTACTS=(
  "Jordan Reyes|+15550100001"
  "Aria Kapoor|+15550100002"
  "Mira Chen|+15550100003"
  "Sam Whitfield|+15550100004"
  "Priya Nair|+15550100005"
  "Liam Connor|+15550100006"
  "Noah Bennett|+15550100007"
  "Zara Ahmed|+15550100008"
)

echo "Provisioning ${#CONTACTS[@]} test contacts..."

for entry in "${CONTACTS[@]}"; do
  name="${entry%%|*}"
  phone="${entry##*|}"

  # Skip if a contact already resolves for this number (idempotent re-runs).
  existing=$(adb_shell "content query --uri content://com.android.contacts/phone_lookup/$phone --projection display_name" || true)
  if [[ "$existing" == *"$name"* ]]; then
    echo "  already present: $name ($phone)"
    continue
  fi

  adb_shell "content insert --uri content://com.android.contacts/raw_contacts --bind account_type:s:null --bind account_name:s:null" > /dev/null

  raw_id=$(adb_shell "content query --uri content://com.android.contacts/raw_contacts --projection _id" \
    | tail -1 | sed -E 's/.*_id=([0-9]+).*/\1/')

  adb_shell "content insert --uri content://com.android.contacts/data --bind raw_contact_id:i:$raw_id --bind mimetype:s:vnd.android.cursor.item/name --bind data1:s:'$name'" > /dev/null
  adb_shell "content insert --uri content://com.android.contacts/data --bind raw_contact_id:i:$raw_id --bind mimetype:s:vnd.android.cursor.item/phone_v2 --bind data1:s:'$phone' --bind data2:i:2" > /dev/null

  echo "  added: $name ($phone) -> raw_contact_id=$raw_id"
done

echo "Done."
