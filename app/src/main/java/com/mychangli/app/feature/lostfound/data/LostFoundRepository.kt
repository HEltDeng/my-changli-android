package com.mychangli.app.feature.lostfound.data

import android.content.Context
import android.util.Base64
import com.mychangli.app.core.network.campustools.CampusToolsApiClient
import com.mychangli.app.core.network.campustools.RemoteLostFoundPost
import com.mychangli.app.core.network.campustools.ReportRequest
import com.mychangli.app.core.network.campustools.UpsertLostFoundRequest
import com.mychangli.app.core.network.campustools.authenticatedCall
import com.mychangli.app.freshNews.data.local.mmkv.CampusCommunityLocalStore
import com.mychangli.app.freshNews.data.local.model.LocalCampusPost
import com.mychangli.app.freshNews.data.local.model.LocalLostFoundInfo
import com.mychangli.app.freshNews.data.local.model.LostFoundCategory
import com.mychangli.app.freshNews.data.local.model.LostFoundStatus
import com.mychangli.app.freshNews.data.local.model.LostFoundType
import com.tencent.mmkv.MMKV
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LostFoundListFilter(val label: String) {
    ALL("全部"),
    LOST("寻物"),
    FOUND("招领"),
}

enum class LostFoundConnectionState { IDLE, LOADING, ONLINE, OFFLINE }

/**
 * Remote-first lost-and-found repository.
 *
 * The existing MMKV post store remains the cache and one-time migration source. App-facing IDs
 * stay equal to legacy local IDs for migrated owner posts, so detail routes and drafts do not
 * break when the server copy arrives.
 */
object LostFoundRepository {
    private const val ACCOUNT_SCOPE_STORE = "lost_found_account_scope_v1"
    private const val KEY_ACCOUNT_ID = "active_account_id"
    private val accountScope by lazy { MMKV.mmkvWithID(ACCOUNT_SCOPE_STORE) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _sourcePosts = MutableStateFlow(
        CampusCommunityLocalStore.posts.value.filter(LostFoundRules::isLostFound)
    )
    val sourcePosts: StateFlow<List<LocalCampusPost>> = _sourcePosts.asStateFlow()
    private val _connectionState = MutableStateFlow(LostFoundConnectionState.IDLE)
    val connectionState: StateFlow<LostFoundConnectionState> = _connectionState.asStateFlow()
    private val _searchResults = MutableStateFlow<List<LocalCampusPost>?>(null)
    val searchResults: StateFlow<List<LocalCampusPost>?> = _searchResults.asStateFlow()
    private val remoteIds = mutableMapOf<String, String>()
    @Volatile
    private var searchGeneration = 0L
    private var activeAccountId: String? = null

    @Synchronized
    fun activateAccount(userId: String) {
        if (activeAccountId == userId) return
        val previous = accountScope.decodeString(KEY_ACCOUNT_ID).orEmpty()
        if (previous.isNotBlank() && previous != userId) {
            CampusCommunityLocalStore.replaceLostFoundPosts(emptyList())
        }
        check(accountScope.encode(KEY_ACCOUNT_ID, userId))
        activeAccountId = userId
        remoteIds.clear()
        _searchResults.value = null
        updateLocalSnapshot()
        _connectionState.value = LostFoundConnectionState.IDLE
    }

    @Synchronized
    fun activateGuest() {
        if (activeAccountId == null && _sourcePosts.value.isEmpty()) return
        activeAccountId = null
        remoteIds.clear()
        _searchResults.value = null
        _sourcePosts.value = emptyList()
        _connectionState.value = LostFoundConnectionState.IDLE
    }

    fun refresh() {
        scope.launch {
            _connectionState.value = LostFoundConnectionState.LOADING
            runCatching {
                if (activeAccountId == null) {
                    val response = CampusToolsApiClient.unauthenticated.lostFound()
                    val remote = response.data.takeIf { response.ok }
                        ?: error(response.error?.message ?: "失物信息加载失败")
                    remoteIds.clear()
                    _sourcePosts.value = remote.map { it.toDisplayLocal() }
                } else {
                    migrateLegacyOwnerPosts()
                    val remote = authenticatedCall { it.lostFound() }
                    applyRemoteSnapshot(remote)
                }
                _connectionState.value = LostFoundConnectionState.ONLINE
            }.onFailure {
                _sourcePosts.value = if (activeAccountId == null) emptyList() else {
                    CampusCommunityLocalStore.posts.value.filter(LostFoundRules::isLostFound)
                }
                _connectionState.value = LostFoundConnectionState.OFFLINE
            }
        }
    }

    fun find(id: String): LocalCampusPost? =
        _sourcePosts.value.firstOrNull { it.id == id }?.takeIf(LostFoundRules::isLostFound)

    fun search(query: String) {
        val normalized = query.trim()
        val generation = ++searchGeneration
        if (normalized.isEmpty()) {
            _searchResults.value = null
            return
        }
        scope.launch {
            runCatching {
                if (activeAccountId == null) {
                    val response = CampusToolsApiClient.unauthenticated.lostFound(query = normalized)
                    response.data.takeIf { response.ok }
                        ?.map { it.toDisplayLocal() }
                        ?: error(response.error?.message ?: "搜索失败")
                } else {
                    authenticatedCall { it.lostFound(query = normalized) }
                        .map { it.toDisplayLocal() }
                }
            }.onSuccess { results ->
                if (generation == searchGeneration) {
                    _searchResults.value = results
                    _connectionState.value = LostFoundConnectionState.ONLINE
                }
            }.onFailure {
                if (generation == searchGeneration) {
                    _searchResults.value = null
                    _connectionState.value = LostFoundConnectionState.OFFLINE
                }
            }
        }
    }

    fun publish(info: LocalLostFoundInfo): LocalCampusPost {
        val local = CampusCommunityLocalStore.publishLostFound(info)
        updateLocalSnapshot()
        syncPost(local)
        return local
    }

    fun update(id: String, info: LocalLostFoundInfo): LocalCampusPost? {
        val local = CampusCommunityLocalStore.updateLostFound(id, info) ?: return null
        updateLocalSnapshot()
        syncPost(local)
        return local
    }

    fun toggleFavorite(id: String): LocalCampusPost? {
        val local = CampusCommunityLocalStore.toggleFavorite(id) ?: return null
        updateLocalSnapshot()
        remoteIds[id]?.let { remoteId ->
            scope.launch {
                runCatching {
                    authenticatedCall { it.toggleFavorite(remoteId) }
                    refreshRemoteOnly()
                }.onFailure { _connectionState.value = LostFoundConnectionState.OFFLINE }
            }
        }
        return local
    }

    fun updateStatus(id: String, status: LostFoundStatus): LocalCampusPost? {
        val local = CampusCommunityLocalStore.updateLostFoundStatus(id, status) ?: return null
        updateLocalSnapshot()
        syncPost(local)
        return local
    }

    fun submitLocalClaim(id: String): LocalCampusPost? {
        val local = CampusCommunityLocalStore.submitLocalClaim(id) ?: return null
        updateLocalSnapshot()
        remoteIds[id]?.let { remoteId ->
            scope.launch {
                runCatching {
                    authenticatedCall { it.claim(remoteId) }
                    refreshRemoteOnly()
                }.onFailure { _connectionState.value = LostFoundConnectionState.OFFLINE }
            }
        }
        return local
    }

    fun report(id: String, reason: String): Boolean {
        if (find(id) == null || reason.isBlank()) return false
        CampusCommunityLocalStore.recordReport(id, reason)
        remoteIds[id]?.let { remoteId ->
            scope.launch {
                runCatching {
                    authenticatedCall { it.report(remoteId, ReportRequest(reason)) }
                }.onFailure { _connectionState.value = LostFoundConnectionState.OFFLINE }
            }
        }
        return true
    }

    fun delete(context: Context, id: String): Boolean {
        val post = find(id) ?: return false
        if (post.lostFound?.isLocalPublisher != true) return false
        val imagePath = post.lostFound.imagePath
        val remoteId = remoteIds[id]
        val deleted = CampusCommunityLocalStore.deleteLocalPost(id)
        if (!deleted) return false
        updateLocalSnapshot()
        if (!imagePath.isNullOrBlank()) deleteUnreferencedPrivateImage(context, imagePath)
        if (remoteId != null) {
            scope.launch {
                runCatching {
                    authenticatedCall { it.deleteLostFound(remoteId) }
                }.onFailure {
                    _connectionState.value = LostFoundConnectionState.OFFLINE
                }
            }
        }
        return true
    }

    private fun syncPost(post: LocalCampusPost) {
        scope.launch {
            val info = post.lostFound ?: return@launch
            runCatching {
                val request = post.toRemoteRequest(includeImage = true)
                val remoteId = remoteIds[post.id]
                val remote = if (remoteId == null) {
                    authenticatedCall { it.createLostFound(request) }
                } else {
                    authenticatedCall { it.updateLostFound(remoteId, request) }
                }
                remoteIds[post.id] = remote.id
                refreshRemoteOnly()
            }.onFailure {
                _connectionState.value = LostFoundConnectionState.OFFLINE
            }
        }
    }

    private suspend fun migrateLegacyOwnerPosts() {
        val localPosts = CampusCommunityLocalStore.posts.value
            .filter(LostFoundRules::isLostFound)
            .filter { it.lostFound?.isLocalPublisher == true }
        localPosts.forEach { post ->
            val remote = authenticatedCall {
                it.createLostFound(post.toRemoteRequest(includeImage = true))
            }
            remoteIds[post.id] = remote.id
        }
    }

    private suspend fun refreshRemoteOnly() {
        val remote = authenticatedCall { it.lostFound() }
        applyRemoteSnapshot(remote)
        _connectionState.value = LostFoundConnectionState.ONLINE
    }

    private fun applyRemoteSnapshot(remote: List<RemoteLostFoundPost>) {
        remoteIds.clear()
        val mapped = remote.map { it.toDisplayLocal() }
        val unsynced = CampusCommunityLocalStore.posts.value
            .filter(LostFoundRules::isLostFound)
            .filter { local -> mapped.none { it.id == local.id } }
        val merged = (mapped + unsynced)
            .distinctBy(LocalCampusPost::id)
            .sortedByDescending(LocalCampusPost::createdAtMillis)
        CampusCommunityLocalStore.replaceLostFoundPosts(merged)
        _sourcePosts.value = merged
    }

    private fun RemoteLostFoundPost.toDisplayLocal(): LocalCampusPost {
        val displayId = clientLocalId.takeIf { isMine && !it.isNullOrBlank() } ?: id
        remoteIds[displayId] = id
        return toLocal(displayId)
    }

    private fun updateLocalSnapshot() {
        _sourcePosts.value = CampusCommunityLocalStore.posts.value
            .filter(LostFoundRules::isLostFound)
            .sortedByDescending(LocalCampusPost::createdAtMillis)
    }

    private fun LocalCampusPost.toRemoteRequest(includeImage: Boolean): UpsertLostFoundRequest {
        val info = checkNotNull(lostFound)
        val image = info.imagePath
            ?.takeIf { includeImage && !it.startsWith("http://") && !it.startsWith("https://") }
            ?.let(::File)
            ?.takeIf { it.isFile && it.length() <= 3L * 1024 * 1024 }
            ?.readBytes()
            ?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
        return UpsertLostFoundRequest(
            clientLocalId = id,
            type = info.effectiveType.name,
            itemName = info.itemName,
            category = info.category.name,
            location = info.location,
            eventDate = info.foundDate,
            approximateTime = info.approximateTime,
            description = info.description,
            claimNote = info.claimNote,
            imageBase64 = image,
            status = info.status.name,
        )
    }

    private fun RemoteLostFoundPost.toLocal(displayId: String): LocalCampusPost {
        val mappedType = enumValueOrDefault(type, LostFoundType.FOUND)
        val mappedCategory = enumValueOrDefault(category, LostFoundCategory.OTHER)
        val mappedStatus = enumValueOrDefault(
            status,
            if (mappedType == LostFoundType.LOST) {
                LostFoundStatus.SEARCHING
            } else {
                LostFoundStatus.WAITING
            },
        )
        val info = LocalLostFoundInfo(
            itemName = itemName,
            category = mappedCategory,
            location = location,
            foundDate = eventDate,
            approximateTime = approximateTime,
            description = description,
            claimNote = claimNote,
            imagePath = CampusToolsApiClient.absoluteUrl(imageUrl),
            type = mappedType,
            status = mappedStatus,
            isLocalPublisher = isMine,
            hasLocalClaimRequest = hasClaimRequest,
            updatedAtMillis = updatedAtMillis,
        )
        return LocalCampusPost(
            id = displayId,
            title = itemName,
            content = buildString {
                append(location)
                append(" · ")
                append(eventDate)
                if (description.isNotBlank()) {
                    append('\n')
                    append(description)
                }
            },
            tag = "失物招领",
            createdAtMillis = createdAtMillis,
            isFavorite = isFavorite,
            lostFound = info,
        )
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(
        raw: String,
        default: T,
    ): T = runCatching { enumValueOf<T>(raw) }.getOrDefault(default)

    private fun deleteUnreferencedPrivateImage(context: Context, imagePath: String) {
        val stillReferenced = sourcePosts.value.any { it.lostFound?.imagePath == imagePath }
        if (stillReferenced || imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            return
        }
        runCatching {
            val image = File(imagePath).canonicalFile
            val privateDirectory = File(context.filesDir, "lost_found_images").canonicalFile
            if (image.parentFile == privateDirectory && image.isFile) image.delete()
        }
    }
}

object LostFoundRules {
    fun isLostFound(post: LocalCampusPost): Boolean =
        post.lostFound != null || post.tag == "失物招领"

    fun filter(
        posts: List<LocalCampusPost>,
        type: LostFoundListFilter,
        query: String,
        favoritesOnly: Boolean = false,
        mineOnly: Boolean = false,
    ): List<LocalCampusPost> {
        val normalizedQuery = query.trim()
        return posts.asSequence()
            .filter(::isLostFound)
            .filter { post ->
                when (type) {
                    LostFoundListFilter.ALL -> true
                    LostFoundListFilter.LOST -> post.lostFound?.effectiveType == LostFoundType.LOST
                    LostFoundListFilter.FOUND -> post.lostFound?.effectiveType == LostFoundType.FOUND
                }
            }
            .filter { !favoritesOnly || it.isFavorite }
            .filter { !mineOnly || it.lostFound?.isLocalPublisher == true }
            .filter { post ->
                normalizedQuery.isEmpty() ||
                    searchableText(post).contains(normalizedQuery, ignoreCase = true)
            }
            .sortedByDescending(LocalCampusPost::createdAtMillis)
            .toList()
    }

    fun initialStatus(type: LostFoundType): LostFoundStatus =
        if (type == LostFoundType.LOST) LostFoundStatus.SEARCHING else LostFoundStatus.WAITING

    fun resolvedStatus(type: LostFoundType): LostFoundStatus =
        if (type == LostFoundType.LOST) LostFoundStatus.FOUND else LostFoundStatus.RETURNED

    fun statusLabel(info: LocalLostFoundInfo): String = when (info.effectiveType) {
        LostFoundType.LOST -> when (info.status) {
            LostFoundStatus.FOUND, LostFoundStatus.RETURNED -> "已找回"
            else -> "寻找中"
        }
        LostFoundType.FOUND -> when (info.status) {
            LostFoundStatus.CLAIMED -> "已认领"
            LostFoundStatus.RETURNED, LostFoundStatus.FOUND -> "已归还"
            else -> "等待认领"
        }
    }

    private fun searchableText(post: LocalCampusPost): String = buildString {
        append(post.title)
        append('\n')
        append(post.content)
        append('\n')
        append(post.tag)
        post.lostFound?.let { info ->
            append('\n')
            append(info.itemName)
            append('\n')
            append(info.category.label)
            append('\n')
            append(info.location)
            append('\n')
            append(info.description)
            append('\n')
            append(info.claimNote)
        }
    }
}
