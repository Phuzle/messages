package com.phuzle.labs.messages.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent

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
