package com.mychangli.app.feature.common.compose_ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.mychangli.app.R
import com.mychangli.app.core.Route
import com.mychangli.app.widget.view.CustomToast

@Immutable
data class FunctionShortcut(
    val id: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector,
    val tintColor: Color,
    val destination: FunctionDestination,
    @StringRes val statusRes: Int? = null,
)

enum class FunctionDestination {
    Timetable,
    ScoreInquiry,
    Classroom,
    Homework,
    Electronic,
    ExamArrangement,
    Calendar,
    Cet,
    LostFound,
    AccountBook,
    ResourceLibrary,
    Mandarin,
}

/**
 * 首页正在按这些稳定 ID 查找快捷入口，因此这里保留原有顺序、ID 与跳转。
 */
fun primaryFunctionShortcuts(): List<FunctionShortcut> =
    v1VisibleFunctionCatalog().selectById(
        "schedule",
        "grade",
        "classroom",
        "homework",
        "electric",
        "exam",
        "calendar",
    )

/**
 * 保留旧接口，避免已有调用方因服务页重新分区而失效。
 */
fun secondaryFunctionShortcuts(): List<FunctionShortcut> =
    v1VisibleFunctionCatalog().selectById(
        "cet",
        "lost_found",
        "document",
        "mandarin",
    )

fun learningFunctionShortcuts(): List<FunctionShortcut> =
    v1VisibleFunctionCatalog().selectById(
        "schedule",
        "grade",
        "classroom",
        "homework",
        "exam",
        "document",
        "cet",
        "mandarin",
    )

fun sportsFunctionShortcuts(): List<FunctionShortcut> =
    emptyList()

fun lifeFunctionShortcuts(): List<FunctionShortcut> =
    v1VisibleFunctionCatalog().selectById(
        "electric",
        "calendar",
        "lost_found",
    )

fun FunctionShortcut.toFunctionItemData(context: Context): FunctionItemData =
    FunctionItemData(
        id = id,
        title = context.getString(titleRes),
        icon = icon,
        tintColor = tintColor,
        statusLabel = statusRes?.let(context::getString),
        onClick = { openFunctionShortcut(context, destination) },
    )

fun openFunctionShortcut(context: Context, destination: FunctionDestination) {
    when (destination) {
        FunctionDestination.Timetable -> Route.goTimetable(context)
        FunctionDestination.ScoreInquiry -> Route.goScoreInquiry(context)
        FunctionDestination.Classroom -> Route.goClassInfo(context)
        FunctionDestination.Homework -> Route.goMooc(context)
        FunctionDestination.Electronic -> Route.goElectronic(context)
        FunctionDestination.ExamArrangement -> Route.goExamArrangement(context)
        FunctionDestination.Calendar -> Route.goCalendar(context)
        FunctionDestination.Cet -> Route.goCet(context)
        FunctionDestination.LostFound -> Route.goLostFound(context)
        FunctionDestination.AccountBook -> Route.goAccountBook(context)
        FunctionDestination.ResourceLibrary -> Route.goResourceLibrary(context)
        FunctionDestination.Mandarin -> Route.goMande(context)
    }
}

private fun functionCatalog(): List<FunctionShortcut> = listOf(
    FunctionShortcut(
        id = "schedule",
        titleRes = R.string.feature_function_course_schedule,
        icon = Icons.Outlined.CalendarMonth,
        tintColor = FunctionColors.Schedule,
        destination = FunctionDestination.Timetable,
    ),
    FunctionShortcut(
        id = "grade",
        titleRes = R.string.feature_function_grade_query,
        icon = Icons.Outlined.Assessment,
        tintColor = FunctionColors.Grade,
        destination = FunctionDestination.ScoreInquiry,
    ),
    FunctionShortcut(
        id = "classroom",
        titleRes = R.string.feature_function_classroom_availability,
        icon = Icons.Outlined.MeetingRoom,
        tintColor = FunctionColors.Classroom,
        destination = FunctionDestination.Classroom,
    ),
    FunctionShortcut(
        id = "homework",
        titleRes = R.string.inquiry_homework,
        icon = Icons.Outlined.Assignment,
        tintColor = FunctionColors.Homework,
        destination = FunctionDestination.Homework,
    ),
    FunctionShortcut(
        id = "electric",
        titleRes = R.string.feature_function_electricity_query,
        icon = Icons.Outlined.ElectricBolt,
        tintColor = FunctionColors.Electric,
        destination = FunctionDestination.Electronic,
    ),
    FunctionShortcut(
        id = "exam",
        titleRes = R.string.feature_function_exam_schedule,
        icon = Icons.Outlined.EventNote,
        tintColor = FunctionColors.Exam,
        destination = FunctionDestination.ExamArrangement,
    ),
    FunctionShortcut(
        id = "calendar",
        titleRes = R.string.feature_function_school_calendar,
        icon = Icons.Outlined.MenuBook,
        tintColor = FunctionColors.Calendar,
        destination = FunctionDestination.Calendar,
    ),
    FunctionShortcut(
        id = "cet",
        titleRes = R.string.feature_function_cet_exam,
        icon = Icons.Outlined.Language,
        tintColor = FunctionColors.CET,
        destination = FunctionDestination.Cet,
    ),
    FunctionShortcut(
        id = "lost_found",
        titleRes = R.string.feature_function_lost_found,
        icon = Icons.Outlined.FindInPage,
        tintColor = FunctionColors.LostFound,
        destination = FunctionDestination.LostFound,
    ),
    FunctionShortcut(
        id = "account",
        titleRes = R.string.feature_function_account_book,
        icon = Icons.Outlined.AccountBalanceWallet,
        tintColor = FunctionColors.Account,
        destination = FunctionDestination.AccountBook,
    ),
    FunctionShortcut(
        id = "document",
        titleRes = R.string.feature_function_document_library,
        icon = Icons.Outlined.FolderOpen,
        tintColor = FunctionColors.Document,
        destination = FunctionDestination.ResourceLibrary,
    ),
    FunctionShortcut(
        id = "mandarin",
        titleRes = R.string.feature_function_mandarin_test,
        icon = Icons.Outlined.RecordVoiceOver,
        tintColor = FunctionColors.Mandarin,
        destination = FunctionDestination.Mandarin,
    ),
)

/**
 * 校历仍保留路由和实现，V1 仅隐藏已失效旧数据源对应的用户入口。
 */
private fun v1VisibleFunctionCatalog(): List<FunctionShortcut> =
    functionCatalog().filterNot { it.id == "calendar" }

private fun List<FunctionShortcut>.selectById(vararg ids: String): List<FunctionShortcut> {
    val shortcutsById = associateBy(FunctionShortcut::id)
    return ids.mapNotNull(shortcutsById::get)
}
