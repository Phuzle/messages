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

# name|phone|color (E.164-ish numbers matching the message simulation script's "Personal" senders;
# color is a plain 0xRRGGBB used by DebugSimulationReceiver's SEED_CONTACT_PHOTO to generate each
# contact a distinct solid-color placeholder photo, so AvatarBubble's real-photo render path — as
# opposed to its initials fallback, which is all these contacts got before — can actually be tested).
CONTACTS=(
  "Jordan Reyes|+15550100001|0xE17055"
  "Aria Kapoor|+15550100002|0x6C5CE7"
  "Mira Chen|+15550100003|0x00B894"
  "Sam Whitfield|+15550100004|0x0984E3"
  "Priya Nair|+15550100005|0xFDCB6E"
  "Liam Connor|+15550100006|0xD63031"
  "Noah Bennett|+15550100007|0x00CEC9"
  "Zara Ahmed|+15550100008|0xE84393"
)

echo "Provisioning ${#CONTACTS[@]} test contacts..."

# WRITE_CONTACTS is only declared in the debug manifest (app/src/debug/AndroidManifest.xml) — grant
# it (and READ_CONTACTS, normally requested at runtime) so DebugSimulationReceiver's photo seeding
# below doesn't need an interactive permission prompt.
adb_shell "pm grant com.phuzle.labs.messages android.permission.READ_CONTACTS" > /dev/null 2>&1 || true
adb_shell "pm grant com.phuzle.labs.messages android.permission.WRITE_CONTACTS" > /dev/null 2>&1 || true

for entry in "${CONTACTS[@]}"; do
  name="${entry%%|*}"
  rest="${entry#*|}"
  phone="${rest%%|*}"
  color="${rest##*|}"

  # Skip creation if a contact already resolves for this number (idempotent re-runs) — but still
  # (re-)seed the photo, since an earlier run of this script predates SEED_CONTACT_PHOTO.
  existing=$(adb_shell "content query --uri content://com.android.contacts/phone_lookup/$phone --projection display_name" || true)
  if [[ "$existing" == *"$name"* ]]; then
    echo "  already present: $name ($phone)"
  else
    adb_shell "content insert --uri content://com.android.contacts/raw_contacts --bind account_type:s:null --bind account_name:s:null" > /dev/null

    raw_id=$(adb_shell "content query --uri content://com.android.contacts/raw_contacts --projection _id" \
      | tail -1 | sed -E 's/.*_id=([0-9]+).*/\1/')

    adb_shell "content insert --uri content://com.android.contacts/data --bind raw_contact_id:i:$raw_id --bind mimetype:s:vnd.android.cursor.item/name --bind data1:s:'$name'" > /dev/null
    adb_shell "content insert --uri content://com.android.contacts/data --bind raw_contact_id:i:$raw_id --bind mimetype:s:vnd.android.cursor.item/phone_v2 --bind data1:s:'$phone' --bind data2:i:2" > /dev/null

    echo "  added: $name ($phone) -> raw_contact_id=$raw_id"
  fi

  adb_shell "am broadcast -n com.phuzle.labs.messages/.core.debug.DebugSimulationReceiver -a com.phuzle.labs.messages.debug.SEED_CONTACT_PHOTO --es number '$phone' --ei color $color" > /dev/null
  echo "  photo seeded: $name ($phone)"
done

echo "Done."
