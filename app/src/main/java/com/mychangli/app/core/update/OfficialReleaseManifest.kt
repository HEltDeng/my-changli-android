package com.mychangli.app.core.update

import com.google.gson.annotations.SerializedName
import com.mychangli.app.core.network.campustools.RemoteAppVersion
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/** Public same-origin release metadata used only when the backend source is unavailable. */
data class OfficialReleaseManifest(
    @SerializedName("versionName") val versionName: String = "",
    @SerializedName("versionCode") val versionCode: Int = 0,
    @SerializedName("minimumVersionCode") val minimumVersionCode: Int = 1,
    @SerializedName("downloadUrl") val downloadUrl: String = "",
    @SerializedName("apkSha256") val apkSha256: String? = null,
    @SerializedName("apkSize") val apkSize: Long? = null,
    @SerializedName("updateMessage") val updateMessage: String = "",
    @SerializedName("forceUpdate") val forceUpdate: Boolean = false,
    @SerializedName("publishedAt") val publishedAt: String? = null,
) {
    fun toRemoteAppVersion(): RemoteAppVersion? {
        if (versionCode < 1 || versionName.isBlank()) return null
        if (minimumVersionCode < 1 || minimumVersionCode > versionCode) return null
        if (!UpdateTargetPolicy.isSafeHttps(downloadUrl)) return null
        return RemoteAppVersion(
            latest_version_code = versionCode,
            minimum_version_code = minimumVersionCode,
            latest_version_name = versionName,
            update_message = updateMessage,
            download_url = downloadUrl,
            apk_sha256 = apkSha256,
            apk_size = apkSize,
            force_update = forceUpdate,
        )
    }
}

interface OfficialReleaseManifestApi {
    @GET("releases/latest.json")
    suspend fun latest(@Query("cacheBust") cacheBust: Long? = null): OfficialReleaseManifest
}

object OfficialReleaseManifestClient {
    private const val BASE_URL = "https://mychangliapp.cn/"
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("Cache-Control", "no-cache")
                        .header("Pragma", "no-cache")
                        .build()
                )
            }
            .build()
    }
    private val api: OfficialReleaseManifestApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OfficialReleaseManifestApi::class.java)
    }

    suspend fun latest(forceRefresh: Boolean): OfficialReleaseManifest =
        api.latest(VersionRequestPolicy.cacheBust(forceRefresh))
}

object VersionRequestPolicy {
    fun cacheBust(forceRefresh: Boolean, nowMillis: Long = System.currentTimeMillis()): Long? =
        nowMillis.takeIf { forceRefresh }
}
