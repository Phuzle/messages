package com.phuzle.labs.messages.core.sms

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony

/** Wraps the two ways Android lets an app ask to become the default SMS handler. */
object DefaultSmsAppHelper {

    /** [Telephony.Sms.getDefaultSmsPackage] can lag behind for a beat right after
     * [RoleManager]'s own grant flow finishes — the role holder list itself updates
     * immediately (confirmed via `adb shell dumpsys role`), but the legacy Telephony-side
     * bookkeeping that this API reads from is a separate subsystem that catches up
     * asynchronously. Checked right after the request-role Activity returns a result, this
     * mismatch made the app think it had been denied the role it had just been granted,
     * silently skipping the one-time SMS history import. RoleManager.isRoleHeld() queries the
     * same system that just performed the grant, so it's immediately consistent — use it as
     * the source of truth on API 29+, where the role-based flow applies at all. */
    fun isDefaultSmsApp(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            return roleManager?.isRoleHeld(RoleManager.ROLE_SMS) == true
        }
        return Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
    }

    fun requestRoleIntent(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            return roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
        }
        return Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
            .putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
    }
}
