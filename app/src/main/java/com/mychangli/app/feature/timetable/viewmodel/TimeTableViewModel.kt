package com.mychangli.app.feature.timetable.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mychangli.app.common.cache.CommonInfo
import com.mychangli.app.core.PlanetApplication
import com.mychangli.app.core.network.ApiResponse
import com.mychangli.app.feature.common.data.local.entity.TimeTableMySubject
import com.mychangli.app.feature.common.data.local.room.database.CoursesDataBase
import com.mychangli.app.feature.timetable.ui.entity.TimeTableUiState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Room-only timetable model for V1. No school credential or school network client is reachable. */
class TimeTableViewModel : ViewModel() {
    companion object {
        private const val TAG = "TimeTableViewModel"
    }

    private val mmkv by lazy { MMKV.defaultMMKV() }
    private val courseDao by lazy {
        CoursesDataBase.getDatabase(PlanetApplication.appContext).courseDao()
    }

    private val _uiState = MutableLiveData<TimeTableUiState>()
    val uiState: LiveData<TimeTableUiState> = _uiState

    private val _coursesResponse =
        MutableSharedFlow<ApiResponse<List<TimeTableMySubject>>>(replay = 1)
    val coursesResponse: SharedFlow<ApiResponse<List<TimeTableMySubject>>> = _coursesResponse

    private val _addCourseResponse = MutableLiveData<ApiResponse<Unit>>()
    val addCourseResponse: LiveData<ApiResponse<Unit>> = _addCourseResponse

    private val _deleteCourseResponse = MutableLiveData<ApiResponse<Unit>>()
    val deleteCourseResponse: LiveData<ApiResponse<Unit>> = _deleteCourseResponse

    private val _termStartDate = MutableStateFlow<String?>(null)
    val termStartDate: StateFlow<String?> = _termStartDate

    init {
        _uiState.value = TimeTableUiState(term = getCurrentTerm())
    }

    /** Keeps the historical preference without deleting locally retained courses. */
    fun initFirstLaunch() {
        if (mmkv.getBoolean("isFirstLaunch", true)) {
            mmkv.encode("isFirstLaunch", false)
        }
    }

    fun getCurrentTerm(): String = CommonInfo.getCurrentTerm()

    @Suppress("UNUSED_PARAMETER")
    fun loadCourses(term: String, forceRefresh: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _coursesResponse.tryEmit(ApiResponse.Loading())
            try {
                val courses = courseDao.getCoursesByTerm(term).distinctBy(::courseIdentity)
                val remark = loadRemarkFromLocal(term)
                withContext(Dispatchers.Main) {
                    updateUiState(courses.toMutableList(), term, remark)
                }
                _coursesResponse.tryEmit(ApiResponse.Success(courses))
            } catch (error: Exception) {
                Log.e(TAG, "Local timetable load failed", error)
                _coursesResponse.tryEmit(ApiResponse.Error("本地课表加载失败"))
            }
        }
    }

    fun addCourse(course: TimeTableMySubject) {
        viewModelScope.launch {
            _addCourseResponse.value = ApiResponse.Loading()
            try {
                val insertedId = withContext(Dispatchers.IO) { courseDao.insertCourse(course) }
                if (insertedId == -1L) {
                    _addCourseResponse.value = ApiResponse.Error("该课程已存在，无法重复添加")
                    return@launch
                }
                val current = _uiState.value ?: TimeTableUiState()
                updateUiState(
                    subjects = (current.subjects + course.copy(id = insertedId.toInt())).toMutableList(),
                    term = current.term,
                    remark = current.remark,
                )
                _addCourseResponse.value = ApiResponse.Success(Unit)
                refreshScheduleOutputs()
            } catch (error: Exception) {
                Log.e(TAG, "Local course add failed", error)
                _addCourseResponse.value = ApiResponse.Error("添加课程失败")
            }
        }
    }

    fun deleteCourse(courseId: Int, term: String) {
        viewModelScope.launch {
            _deleteCourseResponse.value = ApiResponse.Loading()
            try {
                val deleted = withContext(Dispatchers.IO) {
                    courseDao.deleteCustomCourseById(courseId)
                }
                if (deleted <= 0) {
                    _deleteCourseResponse.value = ApiResponse.Error("仅能删除本地自定义课程")
                    return@launch
                }
                val current = _uiState.value ?: TimeTableUiState()
                updateUiState(
                    subjects = current.subjects.filterNot { it.id == courseId }.toMutableList(),
                    term = term,
                    remark = current.remark,
                )
                _deleteCourseResponse.value = ApiResponse.Success(Unit)
                refreshScheduleOutputs()
            } catch (error: Exception) {
                Log.e(TAG, "Local course delete failed", error)
                _deleteCourseResponse.value = ApiResponse.Error("删除课程失败")
            }
        }
    }

    fun selectWeek(weekInfo: String) {
        val current = _uiState.value ?: TimeTableUiState()
        _uiState.value = current.copy(weekInfo = weekInfo)
    }

    fun selectTerm(term: String) {
        loadCourses(term)
        ensureTermStartDate(term)
    }

    fun ensureTermStartDate(term: String) {
        _termStartDate.value = CommonInfo.getTermStartDate(term)
    }

    fun getCurWeek(term: String): Int =
        com.mychangli.app.feature.schedule.CourseScheduleRepository.termStart(term)
            ?.let {
                com.mychangli.app.feature.schedule.CourseScheduleEngine.teachingWeek(
                    it,
                    java.time.LocalDate.now(),
                )
            }
            ?.coerceAtLeast(1)
            ?: CommonInfo.getCurrentWeekInt(term)

    fun hasTermStarted(term: String): Boolean = CommonInfo.hasTermStarted(term)

    private fun updateUiState(
        subjects: MutableList<TimeTableMySubject>,
        term: String,
        remark: List<String> = emptyList(),
    ) {
        val currentWeekInfo = _uiState.value?.weekInfo ?: "第1周"
        _uiState.value = TimeTableUiState(
            subjects = subjects,
            term = term,
            weekInfo = currentWeekInfo,
            lastUpdate = System.currentTimeMillis(),
            remark = remark,
        )
    }

    private fun loadRemarkFromLocal(term: String): List<String> {
        val json = mmkv.decodeString("remark_$term", "").orEmpty()
        if (json.isBlank()) return emptyList()
        return runCatching {
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson<List<String>>(json, type)
        }.getOrDefault(emptyList())
    }

    private fun courseIdentity(course: TimeTableMySubject): String = listOf(
        course.courseName,
        course.teacher,
        course.classroom,
        course.weeks,
        course.start,
        course.step,
        course.term,
        course.weekday,
    ).joinToString("|")

    private fun refreshScheduleOutputs() {
        com.mychangli.app.WidgetUpdateCoordinator.updateAll(PlanetApplication.appContext)
        viewModelScope.launch(Dispatchers.IO) {
            com.mychangli.app.feature.schedule.reminder.CourseReminderScheduler.reschedule(
                PlanetApplication.appContext,
            )
        }
    }
}
