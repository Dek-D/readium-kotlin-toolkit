package org.readiumd.dekd.shared.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.URLUtil
import androidx.browser.customtabs.CustomTabsIntent
import org.readiumd.dekd.shared.InternalReadiumApi
import org.readiumd.dekd.shared.extensions.tryOrLog

/**
 * Opens the given [uri] with a Chrome Custom Tab or the system browser as a fallback.
 */
@InternalReadiumApi
fun launchWebBrowser(context: Context, uri: Uri) {
    var url = uri
    if (url.scheme == null) {
        url = url.buildUpon().scheme("http").build()
    }

    if (!URLUtil.isNetworkUrl(url.toString())) {
        return
    }

    tryOrLog {
        try {
            CustomTabsIntent.Builder()
                .build()
                .launchUrl(context, url)
        } catch (e: ActivityNotFoundException) {
            context.startActivity(Intent(Intent.ACTION_VIEW, url))
        }
    }
}