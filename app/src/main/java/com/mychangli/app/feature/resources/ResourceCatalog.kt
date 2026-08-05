package com.mychangli.app.feature.resources

import java.net.URI
import java.util.Locale

enum class ResourceCategory(val displayName: String) {
    EXAM_MATERIALS("考试资料"),
    LIBRARY("图书馆"),
    ACADEMIC_AFFAIRS("教务信息"),
    STUDENT_SERVICES("学生服务")
}

data class ResourceLink(
    val id: String,
    val title: String,
    val description: String,
    val category: ResourceCategory,
    val sourceLabel: String,
    val url: String,
    val keywords: Set<String> = emptySet()
)

/**
 * Curated links verified against their public pages on 2026-07-21.
 *
 * Only exact entries from this catalog can be opened by the resource-library UI.
 */
object ResourceCatalog {
    val resources: List<ResourceLink> = listOf(
        ResourceLink(
            id = "computer_exam_archive",
            title = "计算机学院往年试题",
            description = "由校友维护的计通学院课程题库与复习资料归档。",
            category = ResourceCategory.EXAM_MATERIALS,
            sourceLabel = "GitHub",
            url = "https://github.com/MedivhTirisfal/csust_test_database",
            keywords = setOf("题库", "真题", "复习", "计算机", "计通")
        ),
        ResourceLink(
            id = "university_library",
            title = "长沙理工大学图书馆",
            description = "查看开放安排、数字资源、读者服务与图书馆通知。",
            category = ResourceCategory.LIBRARY,
            sourceLabel = "学校图书馆",
            url = "https://www.csust.edu.cn/tsg/index.htm",
            keywords = setOf("图书", "论文", "数据库", "开放时间")
        ),
        ResourceLink(
            id = "library_catalog_search",
            title = "馆藏书目检索",
            description = "按题名、作者、ISBN 等条件查询学校纸质与馆藏文献。",
            category = ResourceCategory.LIBRARY,
            sourceLabel = "馆藏检索系统",
            url = "https://opac.csust.edu.cn/index",
            keywords = setOf("馆藏", "借阅", "书目", "ISBN", "OPAC")
        ),
        ResourceLink(
            id = "academic_affairs_notices",
            title = "教务处通知公告",
            description = "获取选课、考试、学籍、竞赛与教学运行相关通知。",
            category = ResourceCategory.ACADEMIC_AFFAIRS,
            sourceLabel = "长沙理工大学教务处",
            url = "https://www.csust.edu.cn/jwc/index/tzgg.htm",
            keywords = setOf("教务", "选课", "考试", "学籍", "竞赛", "通知")
        ),
        ResourceLink(
            id = "current_student_services",
            title = "在校学生服务",
            description = "学校汇总的教务、校园生活、学生事务与公共服务入口。",
            category = ResourceCategory.STUDENT_SERVICES,
            sourceLabel = "长沙理工大学",
            url = "https://www.csust.edu.cn/fldh/zxxs1.htm",
            keywords = setOf("学生", "一卡通", "就业", "资助", "维修", "医疗", "校历")
        )
    )

    val categories: List<ResourceCategory> = ResourceCategory.entries

    private val allowedHosts = setOf(
        "github.com",
        "www.csust.edu.cn",
        "opac.csust.edu.cn"
    )

    init {
        check(resources.map(ResourceLink::id).distinct().size == resources.size) {
            "Resource ids must be unique"
        }
        check(resources.all { isAllowedRemoteUrl(it.url) }) {
            "Resource catalog contains a URL outside the verified host allowlist"
        }
    }

    fun contains(resource: ResourceLink): Boolean =
        resource in resources && isAllowedRemoteUrl(resource.url)

    fun filter(
        query: String,
        category: ResourceCategory?
    ): List<ResourceLink> {
        val normalizedQuery = query.trim().lowercase(Locale.ROOT)
        return resources.filter { resource ->
            val matchesCategory = category == null || resource.category == category
            val matchesQuery = normalizedQuery.isEmpty() || resource.searchText()
                .lowercase(Locale.ROOT)
                .contains(normalizedQuery)
            matchesCategory && matchesQuery
        }
    }

    private fun ResourceLink.searchText(): String = buildString {
        append(title)
        append(' ')
        append(description)
        append(' ')
        append(category.displayName)
        append(' ')
        append(sourceLabel)
        append(' ')
        append(keywords.joinToString(" "))
    }

    private fun isAllowedRemoteUrl(url: String): Boolean = runCatching {
        val uri = URI(url)
        val host = uri.host?.lowercase(Locale.ROOT)
        uri.scheme.equals("https", ignoreCase = true) &&
            host != null &&
            host in allowedHosts
    }.getOrDefault(false)
}
