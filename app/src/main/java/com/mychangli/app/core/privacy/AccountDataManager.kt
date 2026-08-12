package com.mychangli.app.core.privacy

import android.webkit.CookieManager
import com.dcelysia.csust_spider.core.RetrofitUtils as CsustRetrofitUtils
import com.mychangli.app.common.data.local.mmkv.StudentInfoManager
import com.mychangli.app.core.PlanetApplication
import com.mychangli.app.core.security.SchoolSessionState
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/** Local-only account and school-data operations exposed from Settings. */
object AccountDataManager {
    fun clearSchoolBinding() {
        StudentInfoManager.clear()
        SchoolSessionState.invalidate()
        runBlocking(Dispatchers.IO) {
            runCatching { CsustRetrofitUtils.ClearClient("moocClient") }
            runCatching { CsustRetrofitUtils.ClearClient("EducationClient") }
        }
        MMKV.mmkvWithID("education_cache").clearAll()
        MMKV.mmkvWithID("csust_cookie_jar", MMKV.MULTI_PROCESS_MODE).clearAll()
        MMKV.mmkvWithID("MoocCookiejar").clearAll()
        MMKV.mmkvWithID("import_cache").clearAll()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        PlanetApplication.clearSchoolDataCacheAll()
    }

    fun clearLocalLearningData() {
        MMKV.mmkvWithID("content_cache").clearAll()
        MMKV.mmkvWithID("mooc_local_cache").clearAll()
        MMKV.mmkvWithID("TimeTableAppWidget").clearAll()
        PlanetApplication.clearSchoolDataCacheAll()
    }
}
