package com.mychangli.app.feature.common.viewModel

import androidx.lifecycle.viewModelScope
import com.mychangli.app.core.mvi.MviViewModel
import com.mychangli.app.core.security.SchoolSessionCoordinator
import com.mychangli.app.core.security.SchoolSessionResult
import com.mychangli.app.core.security.SchoolSessionState
import com.mychangli.app.feature.common.contract.ExamArrangementContract
import com.mychangli.app.feature.common.data.local.mmkv.ExamArrangementCache
import com.mychangli.app.feature.common.ui.adapter.model.Exam
import com.dcelysia.csust_spider.core.Resource
import com.dcelysia.csust_spider.education.data.remote.model.ExamArrange
import com.dcelysia.csust_spider.education.data.remote.services.ExamArrangeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExamArrangementViewModel :
    MviViewModel<ExamArrangementContract.Intent, ExamArrangementContract.State>() {
    private val cache by lazy { ExamArrangementCache() }
    private val _effect = Channel<ExamArrangementContract.Effect>(Channel.BUFFERED)
    val effect: Flow<ExamArrangementContract.Effect> = _effect.receiveAsFlow()

    override fun initialState(): ExamArrangementContract.State = ExamArrangementContract.State()

    override fun processIntent(intent: ExamArrangementContract.Intent) {
        when (intent) {
            is ExamArrangementContract.Intent.LoadExamArrangement -> {
                fetchExamArrangement(intent.termTime)
            }
        }
    }

    private fun fetchExamArrangement(termTime: String) {
        updateState { copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (SchoolSessionCoordinator.instance.ensureReady()) {
                    SchoolSessionResult.MissingCredentials -> {
                        withContext(Dispatchers.Main) {
                            updateState { copy(isLoading = false) }
                            _effect.send(
                                ExamArrangementContract.Effect.ShowBindingRequired(
                                    "本地未检测到完整的学号绑定信息。"
                                )
                            )
                        }
                        return@launch
                    }

                    SchoolSessionResult.TemporarilyUnavailable -> {
                        val cachedExams = cache.getExamArrangement().orEmpty().toUiExamList()
                        withContext(Dispatchers.Main) {
                            updateState {
                                copy(
                                    isLoading = false,
                                    exams = cachedExams.ifEmpty { exams }
                                )
                            }
                            _effect.send(
                                ExamArrangementContract.Effect.ShowErrorDialog(
                                    "教务会话暂时无法恢复，请稍后重试。"
                                )
                            )
                        }
                        return@launch
                    }

                    SchoolSessionResult.Ready -> Unit
                }

                var examResponse = ExamArrangeService.getExamArrange(termTime)
                if (
                    examResponse is Resource.Error &&
                    SchoolSessionState.isAuthenticationFailure(examResponse.msg)
                ) {
                    SchoolSessionState.invalidate()
                    if (
                        SchoolSessionCoordinator.instance.ensureReady(
                            forceRefresh = true
                        ) == SchoolSessionResult.Ready
                    ) {
                        examResponse = ExamArrangeService.getExamArrange(termTime)
                    }
                }
                if (examResponse is Resource.Success) {
                    val middleList = examResponse.data
                    cache.saveExamArrangement(middleList)
                    val examModels = middleList.toUiExamList()

                    withContext(Dispatchers.Main) {
                        updateState { copy(isLoading = false, exams = examModels) }
                        _effect.send(ExamArrangementContract.Effect.ShowToast("加载成功"))
                    }
                } else {
                    val errorMessage = when {
                        examResponse is Resource.Error -> examResponse.msg
                        else -> "未知错误"
                    }
                    val combined = cache.getExamArrangement()
                    if (!combined.isNullOrEmpty()) {
                        val examModels = combined.toUiExamList()
                        if (examModels.isEmpty()) {
                            cache.clearCache()
                            withContext(Dispatchers.Main) {
                                updateState { copy(isLoading = false) }
                                _effect.send(ExamArrangementContract.Effect.ShowErrorDialog("本地缓存已失效，请重试"))
                            }
                            return@launch
                        }
                        withContext(Dispatchers.Main) {
                            updateState { copy(isLoading = false, exams = examModels) }
                            _effect.send(ExamArrangementContract.Effect.ShowErrorDialog("${errorMessage},加载本地缓存"))
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            updateState { copy(isLoading = false) }
                            _effect.send(ExamArrangementContract.Effect.ShowErrorDialog(errorMessage))
                        }
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    updateState { copy(isLoading = false) }
                    _effect.send(
                        ExamArrangementContract.Effect.ShowErrorDialog(
                            "查询暂时不可用，请稍后重试。"
                        )
                    )
                }
            }
        }
    }

    private fun List<ExamArrange>.toUiExamList(): List<Exam> {
        return mapNotNull { exam ->
            runCatching {
                val courseName = exam.courseNameval
                val examTime = exam.examTime
                val campus = exam.campus
                val examRoom = exam.examRoomval
                if (courseName.isBlank() || examTime.isBlank()) {
                    null
                } else {
                    Exam(courseName, examTime, campus, examRoom)
                }
            }.getOrNull()
        }.distinctBy { listOf(it.name, it.time, it.place, it.room) }
    }
}
