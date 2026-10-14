package com.mychangli.app.core.security

import com.dcelysia.csust_spider.core.Resource
import com.mychangli.app.feature.mooc.data.boundedMoocRefresh
import com.mychangli.app.feature.mooc.data.moocRequestOrThrow
import com.mychangli.app.feature.mooc.data.requireMoocSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MoocRefreshPolicyTest {
    @Test fun successfulEmptyResponseRemainsEmpty() = runBlocking {
        assertTrue(boundedMoocRefresh { requireMoocSuccess(Resource.Success(emptyList<String>())) }.isEmpty())
    }

    @Test fun stalledRequestExitsWithExplicitTimeout() = runBlocking {
        val error = runCatching {
            boundedMoocRefresh<String>(25) { moocRequestOrThrow { awaitCancellation() } }
        }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
        assertTrue(error!!.message!!.contains("超时"))
    }

    @Test fun parentCancellationIsNotConvertedToEmptyOrFailure() = runBlocking {
        val cancellation = CancellationException("cancelled")
        val error = runCatching {
            boundedMoocRefresh<String> { moocRequestOrThrow { throw cancellation } }
        }.exceptionOrNull()
        assertTrue(error is CancellationException)
        assertEquals(cancellation.message, error!!.message)
    }

    @Test fun failedPartialQueryCannotLookLikeNoAssignments() {
        val error = runCatching { requireMoocSuccess(Resource.Error<List<String>>("failure")) }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
    }
}
