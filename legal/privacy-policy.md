# Privacy Policy

**Effective date:** July 24, 2026

This policy covers the Messages Android app ("Messages", "the app"), published by Phuzle Labs.
It is intended to be hosted at `https://docs.phuzle.com/messages/privacy`.

## The short version

Messages is a local-first SMS app. Your messages, contacts, and message content are processed
entirely on your device and are never uploaded to us. The only data that leaves your device is
anonymous crash/usage data (via Firebase) that helps us fix bugs, and — only if you turn it on
yourself — an encrypted backup to your own Google Drive, which we cannot access.

## Data we access, and what happens to it

### SMS messages

As your default SMS app, Messages needs to read, receive, and send SMS messages to function at
all — this is how any SMS app works, not something specific to us. Message content is:

- Stored locally in an encrypted-at-rest app database on your device.
- Categorized (Personal, OTP, Transactions, Promotions) using rules that run entirely on-device.
- **Never uploaded, transmitted, or shared with Phuzle Labs or any third party.**

### Contacts

Messages reads your contacts (name and photo) to label conversations with a name instead of a
raw phone number. This lookup happens entirely on-device. Contact data is not uploaded anywhere.

### Notifications

Messages posts local notifications for incoming texts, including quick actions like copying an
OTP code or replying inline. These are generated on-device and don't involve any external server.

## Data that is collected and transmitted

Messages uses Google Firebase for the following, all covered by
[Google's Privacy Policy](https://policies.google.com/privacy):

- **Firebase Analytics** — anonymous app-interaction data (screens viewed, features used) and
  basic device information (device model, OS version, general region), to help us understand
  how the app is used.
- **Firebase Crashlytics** — crash reports and basic device information, so we can find and fix
  bugs. Crash reports do not include your message content.
- **Firebase Cloud Messaging** — every install is assigned a push-notification token, used only
  to deliver app-update and maintenance notices. This token is not linked to your message content
  or contacts.
- **Firebase Authentication** — an anonymous device identifier used internally to support the
  above; it is not linked to your name, email, phone number, or any personal information.

None of the above ever includes the content of your messages, your contacts, or your phone
number.

## Optional, user-initiated backup

Messages includes an optional backup feature you must explicitly turn on:

- **Local backup**: an encrypted snapshot of your message database, stored only on your device
  (or wherever you choose to export it to).
- **Google Drive backup**: if you connect your Google account and enable this, an encrypted
  backup is stored in your own Google Drive's private app folder (`appDataFolder`), which is not
  visible in your regular Drive files and which Phuzle Labs cannot access. You can disconnect and
  delete this backup at any time from Settings.

If we add other server-based features in the future (for example, an optional cloud-assisted
categorization step), this policy will be updated first, and any such feature will remain off by
default and clearly disclosed before you can turn it on.

## What we don't do

- We do not sell your data.
- We do not share your message content, contacts, or phone number with third parties.
- We do not use your message content for advertising.
- We do not run ads in the app.

## Data retention and deletion

- Messages, threads, and contacts data live in the app's local database until you delete them.
- Deleted messages/threads move to a Recycle Bin and are automatically purged after 30 days,
  or immediately if you empty it yourself.
- Uninstalling the app removes all locally stored data (except any backup you explicitly created
  in your own Google Drive or exported elsewhere).
- You can request deletion of any Firebase-collected analytics/crash data associated with your
  install by contacting us at the email below.

## Children's privacy

Messages is not directed at children under 13, and we do not knowingly collect personal
information from children under 13.

## Changes to this policy

We may update this policy as the app changes. Material changes will be reflected here with an
updated effective date.

## Contact us

Questions about this policy: **support@phuzle.com**

See also our [Terms of Service](https://docs.phuzle.com/messages/terms).
