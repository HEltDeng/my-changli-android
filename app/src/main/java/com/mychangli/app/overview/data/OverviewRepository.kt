package com.mychangli.app.overview.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.mychangli.app.common.cache.CommonInfo
import com.mychangli.app.common.data.local.mmkv.UserInfoManager
import com.mychangli.app.feature.common.data.local.entity.TimeTableMySubject
import com.mychangli.app.feature.common.data.local.room.database.CoursesDataBase
import com.mychangli.app.overview.ui.model.OverviewCourseUiModel
import com.mychangli.app.overview.ui.model.OverviewUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * V1 overview data source.
 *
 * The initial public release deliberately reads only the locally maintained timetable. It does
 * not request school credentials, call a school endpoint, or surface cached grades/exams from a
 * previous full-version installation.
 */
class OverviewRepository(context: Context) {
    private val courseDao = CoursesDataBase.getDatabase(context.applicationContext).courseDao()

    suspend fun loadLocalState(): OverviewUiState = withContext(Dispatchers.IO) {
        val term = CommonInfo.getCurrentTerm()
        val week = CommonInfo.getCurrentWeekInt(term)
        val targetCalendar = Calendar.getInstance()
        val isShowingTomorrow = targetCalendar.get(Calendar.HOUR_OF_DAY) >= 21
        if (isShowingTomorrow) targetCalendar.add(Calendar.DAY_OF_MONTH, 1)

        val weekday = when (targetCalendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> 7
            else -> targetCalendar.get(Calendar.DAY_OF_WEEK) - 1
        }
        val targetWeek = if (isShowingTomorrow && weekday == 1) week + 1 else week
        val courses = courseDao.getCoursesByTerm(term)
            .filter { it.studentId.isBlank() || it.isCustom }
            .toDayCourses(weekday, targetWeek)

        OverviewUiState(
            isRefreshing = false,
            isSilentSyncing = false,
            isBoundStudent = false,
            isOnline = false,
            accountName = UserInfoManager.username.ifBlank {
                UserInfoManager.account.ifBlank { "我的长理" }
            },
            avatarUrl = UserInfoManager.userAvatar,
            studentId = "",
            dateText = buildDateText(term, targetWeek, targetCalendar, isShowingTomorrow),
            currentTerm = term,
            currentWeek = week,
            dataSourceLabel = "仅显示本地课表",
            todayCourses = courses,
            todayCourseMessage = if (courses.isEmpty()) "暂无本地课程，可在课表中手动添加" else "",
            isShowingTomorrow = isShowingTomorrow,
            pendingHomeworkMessage = "V1 不接入学校作业服务",
            pendingTestMessage = "V1 不接入学校测试服务",
            examMessage = "V1 不接入学校考试服务",
        )
    }

    suspend fun refreshState(): OverviewUiState = loadLocalState()

    private fun buildDateText(
        term: String,
        week: Int,
        calendar: Calendar,
        isShowingTomorrow: Boolean,
    ): String {
        val weekday = listOf("", "周日", "周一", "周二", "周三", "周四", "周五", "周六")
            .get(calendar.get(Calendar.DAY_OF_WEEK))
        val prefix = if (isShowingTomorrow) "明日 " else ""
        return "$prefix${calendar.get(Calendar.MONTH) + 1}月${calendar.get(Calendar.DAY_OF_MONTH)}日 $weekday · $term 第${week}周"
    }

    private fun List<TimeTableMySubject>.toDayCourses(
        targetWeekday: Int,
        currentWeek: Int,
    ): List<OverviewCourseUiModel> {
        val colors = listOf(
            Color(0xFF5E87F6),
            Color(0xFF45B979),
            Color(0xFFF08D3C),
            Color(0xFFB974F0),
        )
        return filter {
            it.weekday == targetWeekday &&
                (it.weeks.isNullOrEmpty() || currentWeek in it.weeks.orEmpty())
        }.sortedBy { it.start }
            .mapIndexed { index, course ->
                OverviewCourseUiModel(
                    id = "${course.id}_${course.start}_${course.weekday}_$index",
                    courseName = course.courseName,
                    classroom = course.classroom.orEmpty().ifBlank { "地点待定" },
                    teacher = course.teacher.ifBlank { "备注为空" },
                    timeText = buildCourseTimeText(course.start, course.step),
                    accentLabel = "本地课程",
                    accentColor = colors[index % colors.size],
                )
            }
    }

    private fun buildCourseTimeText(startSection: Int, sectionSpan: Int): String {
        val sectionTimes = listOf(
            "08:00" to "08:45", "08:55" to "09:40", "10:10" to "10:55",
            "11:05" to "11:50", "14:00" to "14:45", "14:55" to "15:40",
            "16:10" to "16:55", "17:05" to "17:50", "19:30" to "20:15",
            "20:25" to "21:10",
        )
        val safeStart = startSection.coerceIn(1, sectionTimes.size)
        val safeEnd = (startSection + sectionSpan - 1).coerceIn(safeStart, sectionTimes.size)
        return "${sectionTimes[safeStart - 1].first}-${sectionTimes[safeEnd - 1].second} · ${safeStart}-${safeEnd}节"
    }
}
