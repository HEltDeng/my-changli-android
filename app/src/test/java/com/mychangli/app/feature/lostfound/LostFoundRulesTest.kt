package com.mychangli.app.feature.lostfound

import com.mychangli.app.feature.lostfound.data.LostFoundListFilter
import com.mychangli.app.feature.lostfound.data.LostFoundRules
import com.mychangli.app.freshNews.data.local.model.LocalCampusPost
import com.mychangli.app.freshNews.data.local.model.LocalLostFoundInfo
import com.mychangli.app.freshNews.data.local.model.LostFoundCategory
import com.mychangli.app.freshNews.data.local.model.LostFoundStatus
import com.mychangli.app.freshNews.data.local.model.LostFoundType
import org.junit.Assert.assertEquals
import org.junit.Test

class LostFoundRulesTest {
    @Test
    fun `legacy lost-found payload defaults to found type`() {
        val post = post("legacy", type = null, favorite = true)

        val result = LostFoundRules.filter(
            posts = listOf(post),
            type = LostFoundListFilter.FOUND,
            query = "",
            favoritesOnly = true,
        )

        assertEquals(listOf("legacy"), result.map(LocalCampusPost::id))
        assertEquals("等待认领", LostFoundRules.statusLabel(post.lostFound!!))
    }

    @Test
    fun `search covers structured item location category and description`() {
        val lost = post("lost", LostFoundType.LOST)
        val ordinary = LocalCampusPost(
            id = "ordinary",
            title = "图书馆",
            content = "蓝色耳机",
            tag = "校园动态",
            createdAtMillis = 1L,
        )

        listOf("蓝色耳机", "图书馆", "电子设备", "保护壳").forEach { query ->
            assertEquals(
                query,
                listOf("lost"),
                LostFoundRules.filter(
                    listOf(ordinary, lost),
                    LostFoundListFilter.ALL,
                    query,
                ).map(LocalCampusPost::id),
            )
        }
    }

    @Test
    fun `lost and found status labels remain type specific`() {
        val lost = post("lost", LostFoundType.LOST).lostFound!!
        val found = post("found", LostFoundType.FOUND).lostFound!!

        assertEquals("寻找中", LostFoundRules.statusLabel(lost))
        assertEquals("已找回", LostFoundRules.statusLabel(lost.copy(status = LostFoundStatus.FOUND)))
        assertEquals("等待认领", LostFoundRules.statusLabel(found))
        assertEquals("已归还", LostFoundRules.statusLabel(found.copy(status = LostFoundStatus.RETURNED)))
    }

    private fun post(
        id: String,
        type: LostFoundType?,
        favorite: Boolean = false,
    ) = LocalCampusPost(
        id = id,
        title = "蓝色耳机",
        content = "物品信息",
        tag = "失物招领",
        createdAtMillis = 2L,
        isFavorite = favorite,
        lostFound = LocalLostFoundInfo(
            itemName = "蓝色耳机",
            category = LostFoundCategory.ELECTRONIC,
            location = "图书馆",
            foundDate = "2026-07-28",
            description = "带透明保护壳",
            type = type,
            status = if (type == LostFoundType.LOST) {
                LostFoundStatus.SEARCHING
            } else {
                LostFoundStatus.WAITING
            },
        ),
    )
}
