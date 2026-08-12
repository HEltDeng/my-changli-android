package com.mychangli.app.freshNews.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.mychangli.app.feature.lostfound.ui.LostFoundActivity

/** Compatibility activity for older in-app navigation; the community channel is no longer shown. */
class LostFoundChannelActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(LostFoundActivity.listIntent(this))
        finish()
    }
}
