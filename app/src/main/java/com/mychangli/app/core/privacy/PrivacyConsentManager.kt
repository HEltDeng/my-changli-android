package com.mychangli.app.core.privacy

import android.content.Context
import com.amap.api.location.AMapLocationClient

/** Keeps third-party location consent explicit and local to this app. */
object PrivacyConsentManager {
    private const val PREFS_NAME = "privacy_consent"
    private const val KEY_APP_CONSENT_VERSION = "privacy_consent_version"
    private const val KEY_APP_DECLINED_VERSION = "privacy_declined_version"
    private const val KEY_AMAP_LOCATION = "amap_location_agreed"
    private const val KEY_AMAP_NOTICE_SHOWN = "amap_location_notice_shown"

    /** Increment when a material policy change requires renewed consent. */
    const val CURRENT_APP_CONSENT_VERSION = 1

    enum class AppConsentState { ACCEPTED, DECLINED, UNDECIDED }

    fun appConsentState(context: Context): AppConsentState {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return when {
            preferences.getInt(KEY_APP_CONSENT_VERSION, 0) >= CURRENT_APP_CONSENT_VERSION ->
                AppConsentState.ACCEPTED
            preferences.getInt(KEY_APP_DECLINED_VERSION, 0) >= CURRENT_APP_CONSENT_VERSION ->
                AppConsentState.DECLINED
            else -> AppConsentState.UNDECIDED
        }
    }

    fun hasAppConsent(context: Context): Boolean =
        appConsentState(context) == AppConsentState.ACCEPTED

    fun acceptAppConsent(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_APP_CONSENT_VERSION, CURRENT_APP_CONSENT_VERSION)
            .remove(KEY_APP_DECLINED_VERSION)
            .apply()
        // App-level consent permits SDK privacy state synchronization. Location remains denied
        // until the user separately enables it from the campus map feature.
        syncAmapLocationConsent(context)
    }

    fun declineAppConsent(context: Context) {
        val previouslyAccepted = hasAppConsent(context)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_APP_CONSENT_VERSION)
            .putInt(KEY_APP_DECLINED_VERSION, CURRENT_APP_CONSENT_VERSION)
            .putBoolean(KEY_AMAP_LOCATION, false)
            .apply()
        // Do not touch a third-party SDK on a first-run refusal. If consent is withdrawn after
        // prior acceptance, explicitly synchronize the revoked state.
        if (previouslyAccepted) syncAmapLocationConsent(context)
    }

    fun clearDeclinedDecision(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_APP_DECLINED_VERSION)
            .apply()
    }

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
