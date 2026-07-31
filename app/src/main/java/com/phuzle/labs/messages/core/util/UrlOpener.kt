package com.phuzle.labs.messages.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import com.phuzle.labs.messages.BuildConfig

/** Opens [url] via Chrome Custom Tabs when the user has [inAppBrowser] on, otherwise a plain
 * ACTION_VIEW to whatever browser they've set as default. Shared by anywhere in the app that
 * opens an external link (detected URLs in a message, the About screen's legal links, ...) so
 * they all honor the same setting instead of each picking their own behavior. */
fun openUrl(context: Context, inAppBrowser: Boolean, url: String) {
    val fullUrl = if (url.startsWith("http", ignoreCase = true)) url else "https://$url"
    val uri = Uri.parse(fullUrl)
    val launched = runCatching {
        if (inAppBrowser) {
            CustomTabsIntent.Builder().build().launchUrl(context, uri)
        } else {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }.isSuccess
    if (!launched) Toast.makeText(context, "Couldn't open that link", Toast.LENGTH_SHORT).show()
}

private const val PRODUCTION_SUPPORT_EMAIL = "support@phuzle.com"

// Testers Google Group — same one release.yml's tag-suffix routes to internal/closed testing
// (see RELEASING.md). A plain "vX.Y.Z" tag/versionName means production; any "-alpha"/"-beta"/
// "-rc" suffix means this build only ever reaches testers, so route feedback to their group
// instead of the production support inbox.
private const val TESTERS_SUPPORT_EMAIL = "testers-messages-phuzle-labs@googlegroups.com"

val SUPPORT_EMAIL: String
    get() = if (BuildConfig.VERSION_NAME.contains('-')) TESTERS_SUPPORT_EMAIL else PRODUCTION_SUPPORT_EMAIL

/** Same mailto action as the About screen's "Email support" row — shared so the closed-testing
 * feedback banner (DashboardScreen) sends to the exact same address instead of drifting out of
 * sync with its own copy. */
fun openSupportEmail(context: Context, subject: String? = null) {
    val uri = if (subject != null) Uri.parse("mailto:$SUPPORT_EMAIL?subject=${Uri.encode(subject)}") else Uri.parse("mailto:$SUPPORT_EMAIL")
    val intent = Intent(Intent.ACTION_SENDTO, uri)
    val launched = runCatching { context.startActivity(Intent.createChooser(intent, "Email support")) }.isSuccess
    if (!launched) Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
}
