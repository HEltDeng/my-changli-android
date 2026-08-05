package com.mychangli.app.freshNews.viewModel

import com.mychangli.app.freshNews.data.local.model.LocalCampusPost
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalCampusUiStateFilterTest {
    @Test fun `category filter is applied when search is empty`() {
        val lost = LocalCampusPost("lost", "校园卡", "内容", "失物招领", 2L)
        val general = LocalCampusPost("general", "动态", "内容", "校园动态", 1L)
        val state = LocalCampusUiState(posts = listOf(lost, general), selectedCategory = "失物招领")
        assertEquals(listOf("lost"), state.visiblePosts.map(LocalCampusPost::id))
    }

    @Test fun `search remains scoped to the selected category`() {
        val lost = LocalCampusPost("lost", "校园卡", "图书馆", "失物招领", 2L)
        val general = LocalCampusPost("general", "校园卡", "教学楼", "校园动态", 1L)
        val state = LocalCampusUiState(posts = listOf(lost, general), selectedCategory = "失物招领", searchQuery = "校园卡")
        assertEquals(listOf("lost"), state.visiblePosts.map(LocalCampusPost::id))
    }
}
