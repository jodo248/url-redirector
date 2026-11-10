package com.igredirect

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast

/**
 * Transparent activity that intercepts Instagram URLs and redirects them
 * to the configured Kittygram frontend, then finishes immediately.
 *
 * When launched from the app launcher (no data URI), it forwards to SettingsActivity.
 */
class RedirectActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent?.data
        if (intent?.action == Intent.ACTION_VIEW && uri != null) {
            handleRedirect(uri)
        } else {
            openSettings()
        }
        finish()
    }

    private fun handleRedirect(uri: Uri) {
        val settings = SettingsManager.load(this)
        val uriString = uri.toString()

        // Find the first enabled rule whose regex matches this URL
        val matched = settings.rules.any { rule ->
            rule.enabled && try {
                uriString.matches(Regex(rule.pattern))
            } catch (_: Exception) {
                false
            }
        }

        if (!matched) {
            // No rule matches — pass through to the default browser as-is
            openInBrowser(uri, null)
            return
        }

        val frontend = settings.getActiveFrontend()
        if (frontend == null) {
            Toast.makeText(this, getString(R.string.no_frontend_configured), Toast.LENGTH_LONG).show()
            openSettings()
            return
        }

        // Build the redirected URL: frontend base + original path
        // Optionally strip query params (tracking junk)
        val path = uri.path?.takeIf { it.isNotEmpty() } ?: "/"
        val redirected = if (settings.stripQueryParams) {
            frontend.url.trimEnd('/') + path
        } else {
            val q = uri.query
            frontend.url.trimEnd('/') + path + if (q != null) "?$q" else ""
        }

        openInBrowser(Uri.parse(redirected), settings.preferredBrowserPackage)
    }

    private fun openInBrowser(uri: Uri, pkg: String?) {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (pkg != null) setPackage(pkg)
        }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            if (pkg != null) {
                // Configured browser missing — fall back to system chooser
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    })
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(this, getString(R.string.no_browser_found), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, getString(R.string.no_browser_found), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
