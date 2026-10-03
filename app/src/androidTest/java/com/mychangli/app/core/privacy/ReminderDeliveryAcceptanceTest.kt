package com.mychangli.app.core.privacy

import android.app.NotificationManager
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import com.mychangli.app.BuildConfig
import com.mychangli.app.common.cache.CommonInfo
import com.mychangli.app.feature.common.data.local.entity.TimeTableMySubject
import com.mychangli.app.feature.common.data.local.room.database.CoursesDataBase
import com.mychangli.app.feature.schedule.*
import com.mychangli.app.feature.schedule.reminder.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import java.time.ZonedDateTime
import java.io.File

/** Real Android notification + five-minute AlarmManager delivery, synthetic local course only. */
class ReminderDeliveryAcceptanceTest {
    @Test fun normalThenSilentSnoozeUsesSameOccurrence() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue(BuildConfig.DEBUG && BuildConfig.CAMPUS_TOOLS_API_BASE_URL.startsWith("http://127.0.0.1:"))
        val manager=context.getSystemService(NotificationManager::class.java)
        assumeTrue(manager.areNotificationsEnabled()) // Permission is granted through the actual system UI before this test.
        val oldPeriods=CourseTimePreferences.periods
        val oldEnabled=CourseReminderPreferences.enabled
        val oldSilent=CourseReminderPreferences.silent
        val oldSnooze=CourseReminderPreferences.snoozeEnabled
        val now=ZonedDateTime.now().withSecond(0).withNano(0)
        val start=now.plusMinutes(1)
        val end=now.plusMinutes(30)
        assumeTrue(start.toLocalDate()==end.toLocalDate())
        val term=CommonInfo.getCurrentTerm()
        val dao=CoursesDataBase.getDatabase(context).courseDao()
        var id=0
        try {
            CourseTimePreferences.save(oldPeriods.toMutableList().apply { this[0]=CoursePeriod(start.toLocalTime(),end.toLocalTime()) })
            id=dao.insertCourse(TimeTableMySubject(courseName="隔离提醒验收",classroom="测试教室",teacher="测试教师",weeks=(1..20).toList(),start=1,step=1,weekday=start.dayOfWeek.value,isCustom=true,term=term)).toInt()
            assertTrue(id>0)
            CourseReminderPreferences.enabled=true;CourseReminderPreferences.silent=false;CourseReminderPreferences.snoozeEnabled=true
            val starts=start.toInstant().toEpochMilli()
            val notificationId=(31*id+(starts xor (starts ushr 32)).toInt()).and(0x7fffffff)
            val intent=Intent(context,CourseReminderReceiver::class.java).setAction(CourseReminderScheduler.ACTION_COURSE_REMINDER)
                .putExtra(CourseReminderScheduler.EXTRA_COURSE_ID,id)
                .putExtra(CourseReminderScheduler.EXTRA_COURSE_NAME,"隔离提醒验收")
                .putExtra(CourseReminderScheduler.EXTRA_START_MILLIS,starts)
                .putExtra(CourseReminderScheduler.EXTRA_END_MILLIS,end.toInstant().toEpochMilli())
            context.sendBroadcast(intent)
            fun waitNotification(timeout:Long):android.service.notification.StatusBarNotification? {
                val until=System.currentTimeMillis()+timeout
                while(System.currentTimeMillis()<until) {
                    manager.activeNotifications.firstOrNull { it.id==notificationId }?.let { return it }
                    Thread.sleep(500)
                }; return null
            }
            val initial=waitNotification(10000) ?: error("Initial reminder was not posted")
            assertEquals(ReminderChannels.NORMAL,initial.notification.channelId)
            val snooze=initial.notification.actions.first { it.title.toString()=="稍后5分钟" }.actionIntent
            snooze.send();Thread.sleep(1000)
            val prefs=context.getSharedPreferences("reminder_snooze",0)
            val saved=prefs.getString(notificationId.toString(),null)
            assertNotNull(saved);assertFalse(manager.activeNotifications.any { it.id==notificationId })
            snooze.send();Thread.sleep(500)
            assertEquals(saved,prefs.getString(notificationId.toString(),null))
            CourseReminderPreferences.silent=true;ReminderChannels.ensure(context)
            ReminderSnooze.restore(context)
            val delivered=waitNotification(360000) ?: error("Five-minute snooze was not delivered within six minutes")
            assertEquals(ReminderChannels.SILENT,delivered.notification.channelId)
            val channel=manager.getNotificationChannel(ReminderChannels.SILENT)
            assertNull(channel.sound);assertFalse(channel.shouldVibrate())
            assertEquals("课程稍后提醒",delivered.notification.extras.getString("android.title"))
            assertFalse(prefs.contains(notificationId.toString()))
            manager.cancel(notificationId)
            dao.deleteCustomCourseById(id)
            context.sendBroadcast(Intent(intent).setAction(ReminderSnooze.ACTION_DELIVER))
            Thread.sleep(1500)
            assertFalse(manager.activeNotifications.any { it.id==notificationId })
            File(context.getExternalFilesDir(null),"reminder-delivery-result.txt").writeText("PASS: normal channel -> actual 5-minute alarm -> silent channel; duplicate action preserved deadline; same notification identity; deleted course suppressed. Physical sound/vibration not measured.")
        } finally {
            if(id>0) dao.deleteCustomCourseById(id)
            CourseTimePreferences.save(oldPeriods)
            CourseReminderPreferences.enabled=oldEnabled;CourseReminderPreferences.silent=oldSilent;CourseReminderPreferences.snoozeEnabled=oldSnooze
            CourseReminderScheduler.reschedule(context)
        }
    }
}
