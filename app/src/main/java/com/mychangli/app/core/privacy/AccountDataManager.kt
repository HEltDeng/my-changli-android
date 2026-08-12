package com.mychangli.app.core.privacy

import android.webkit.CookieManager
import com.mychangli.app.core.PlanetApplication
import com.tencent.mmkv.MMKV

/** Local-only data operations exposed from Settings in V1. */
object AccountDataManager {
    /**
     * Removes legacy school credentials/session data left by an upgrade from the full build.
     * V1 has no code path that reads or uses these stores.
     */
    fun clearSchoolBinding() {
        MMKV.mmkvWithID("stu_info_cache").clearAll()
        MMKV.mmkvWithID("education_cache").clearAll()
        MMKV.mmkvWithID("csust_cookie_jar", MMKV.MULTI_PROCESS_MODE).clearAll()
        MMKV.mmkvWithID("MoocCookiejar").clearAll()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    fun clearLocalLearningData() {
        MMKV.mmkvWithID("content_cache").clearAll()
        MMKV.mmkvWithID("mooc_local_cache").clearAll()
        MMKV.mmkvWithID("TimeTableAppWidget").clearAll()
        PlanetApplication.clearSchoolDataCacheAll()
    }
}
