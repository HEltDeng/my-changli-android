package com.mychangli.app.core.privacy

import android.content.Context
import com.amap.api.location.AMapLocationClient

/** Keeps third-party location consent explicit and local to this app. */
object PrivacyConsentManager {
    private const val PREFS_NAME = "privacy_consent"
    private const val KEY_AMAP_LOCATION = "amap_location_agreed"
    private const val KEY_AMAP_NOTICE_SHOWN = "amap_location_notice_shown"

    fun isAmapLocationAgreed(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AMAP_LOCATION, false)

    fun setAmapLocationConsent(context: Context, agreed: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AMAP_LOCATION, agreed)
            .putBoolean(KEY_AMAP_NOTICE_SHOWN, true)
            .apply()
        syncAmapLocationConsent(context)
    }

    fun syncAmapLocationConsent(context: Context) {
        val appContext = context.applicationContext
        val noticeShown = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AMAP_NOTICE_SHOWN, false)
        AMapLocationClient.updatePrivacyShow(appContext, noticeShown, true)
        AMapLocationClient.updatePrivacyAgree(appContext, isAmapLocationAgreed(appContext))
    }
}
