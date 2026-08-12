package com.mychangli.app.core

import android.content.Context
import android.content.Intent
import com.mychangli.app.common.ui.WebViewActivity
import com.mychangli.app.core.account.ui.AccountCenterActivity
import com.mychangli.app.core.account.AccountAccess
import com.mychangli.app.feature.common.ui.CetActivity
import com.mychangli.app.feature.common.ui.ContractActivity
import com.mychangli.app.feature.common.ui.MandeActivity
import com.mychangli.app.feature.ledger.ui.AccountBookActivity
import com.mychangli.app.feature.lostfound.ui.LostFoundActivity
import com.mychangli.app.feature.feedback.ui.FeedbackActivity
import com.mychangli.app.feature.ledger.ui.AddSomethingAccountActivity
import com.mychangli.app.feature.ledger.ui.FixSomethingAccountActivity
import com.mychangli.app.feature.resources.ResourceLibraryActivity
import com.mychangli.app.feature.schedule.reminder.CourseReminderSettingsActivity
import com.mychangli.app.feature.timetable.ui.TimeTableActivity
import com.mychangli.app.profileSettings.ui.AboutActivity
import com.mychangli.app.core.privacy.ReleaseDocumentType
import com.mychangli.app.core.privacy.ReleaseInformationActivity

/**
 * 所有页面跳转逻辑都应卸载Route中，方便统一管理
 * 使用方法：Route.goxx()
 *
 * 学校绑定、学校接口、校园地图、体育预约和支付在 V1 均没有路由目标。
 */
object Route {

    fun goAccountCenter(context: Context) {
        AccountAccess.openOrRequestLogin(context, Intent(context, AccountCenterActivity::class.java))
    }

    fun goFeedback(context: Context) {
        AccountAccess.openOrRequestLogin(context, Intent(context, FeedbackActivity::class.java))
    }

    fun goCourseReminders(context: Context) {
        context.startActivity(Intent(context, CourseReminderSettingsActivity::class.java))
    }

    fun goLostFound(context: Context) {
        AccountAccess.openOrRequestLogin(context, LostFoundActivity.listIntent(context))
    }

    /** Compatibility route retained for old callers; the campus channel is no longer opened. */
    fun goLostFoundChannel(context: Context) {
        AccountAccess.openOrRequestLogin(context, LostFoundActivity.listIntent(context))
    }

    fun goHome(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
        context.startActivity(intent)
    }

    fun goHomeForcibly(context: Context) {
        val intent = Intent(
            context,
            MainActivity::class.java
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
    }

    fun goTimetable(context: Context) {
        val intent = Intent(context, TimeTableActivity::class.java)
        context.startActivity(intent)
    }

    fun goCet(context: Context) {
        val intent = Intent(context, CetActivity::class.java)
        context.startActivity(intent)
    }

    fun goMande(context: Context) {
        val intent = Intent(context, MandeActivity::class.java)
        context.startActivity(intent)
    }

    fun goLocalCampusPostDetail(context: Context, postId: String) {
        if (postId.isBlank()) return
        context.startActivity(LostFoundActivity.detailIntent(context, postId))
    }

    fun goContract(context: Context) {
        val intent = Intent(context, ContractActivity::class.java)
        context.startActivity(intent)
    }

    fun goResourceLibrary(context: Context) {
        AccountAccess.openOrRequestLogin(
            context,
            Intent(context, ResourceLibraryActivity::class.java),
        )
    }

    fun goAccountBook(context: Context) {
        val intent = Intent(context, AccountBookActivity::class.java)
        context.startActivity(intent)
    }

    fun goAddSomethingAccount(context: Context) {
        val intent = Intent(context, AddSomethingAccountActivity::class.java)
        context.startActivity(intent)
    }

    fun goFixSomethingAccount(context: Context, itemId: Int) {
        val intent = Intent(context, FixSomethingAccountActivity::class.java).apply {
            putExtra("ITEM_ID", itemId)
        }
        context.startActivity(intent)
    }

    fun goWebView(context: Context, url: String) {
        val intent = Intent(context, WebViewActivity::class.java).apply {
            putExtra("url_tag", url)
        }
        context.startActivity(intent)
    }

    fun goAbout(context: Context) {
        val intent = Intent(context, AboutActivity::class.java)
        context.startActivity(intent)
    }

    fun goReleaseInformation(context: Context, type: ReleaseDocumentType) {
        context.startActivity(ReleaseInformationActivity.intent(context, type))
    }

}
