package com.mychangli.app.feature.mooc.ui

import android.os.Bundle
import com.mychangli.app.R
import com.mychangli.app.base.ComposeActivity
import com.mychangli.app.common.data.local.mmkv.StudentInfoManager.studentId
import com.mychangli.app.common.data.local.mmkv.StudentInfoManager.studentPassword
import com.mychangli.app.core.PlanetApplication
import com.mychangli.app.core.Route
import com.mychangli.app.widget.view.CustomToast

class MoocActivity : ComposeActivity() {
    private val moocViewModel by lazy { (application as PlanetApplication).moocViewModel }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (studentId.isEmpty() || studentPassword.isEmpty()) {
            CustomToast.showMessage(this, getString(R.string.bind_notification))
            Route.goBindingUser(this)
            finish()
            return
        }

        setComposeContent {
            MoocScreen(
                moocViewModel = moocViewModel,
                onBack = ::finish,
                onOpenCoursePage = { courseId ->
                    Route.goMoocCoursePage(this, courseId)
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (studentId.isNotEmpty() && studentPassword.isNotEmpty() && moocViewModel.shouldAutoRefreshOnEnter()) {
            moocViewModel.refreshCourses()
        }
    }
}
