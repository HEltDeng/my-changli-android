package com.mychangli.app.feature.mooc.data

import com.dcelysia.csust_spider.core.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

internal suspend fun <T : Any> boundedMoocRefresh(
    timeoutMillis: Long = 30_000L,
    block: suspend () -> T
): T = withTimeoutOrNull(timeoutMillis) { block() }
    ?: throw IllegalStateException("慕课查询超时，请稍后重试；课表和成绩可正常独立查询。")

internal suspend fun <T> moocRequestOrThrow(block: suspend () -> T): T = try {
    block()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    throw IllegalStateException("部分作业或测试查询失败，请稍后重试。")
}

internal fun <T> requireMoocSuccess(result: Resource<T>?): T = when (result) {
    is Resource.Success -> result.data
    else -> throw IllegalStateException("部分作业或测试查询失败，暂不能确认是否有待办。")
}
