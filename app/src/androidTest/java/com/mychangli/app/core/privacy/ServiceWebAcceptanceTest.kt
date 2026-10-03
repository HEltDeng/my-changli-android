package com.mychangli.app.core.privacy

import android.graphics.Bitmap
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.mychangli.app.core.web.ServiceWebActivity
import com.mychangli.app.feature.common.ui.MandeActivity
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Reads layout geometry only; never enters or submits identity data or CAPTCHA. */
class ServiceWebAcceptanceTest {
    @Test fun mandarinMobileLayout() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        ActivityScenario.launch(MandeActivity::class.java).use { scenario ->
            lateinit var web: WebView
            scenario.onActivity { activity ->
                web=ServiceWebActivity::class.java.getDeclaredField("web").apply { isAccessible=true }.get(activity) as WebView
            }
            fun js(script:String):String {
                val latch=CountDownLatch(1);var value=""
                instrumentation.runOnMainSync { web.evaluateJavascript(script) { value=it;latch.countDown() } }
                assertTrue(latch.await(5,TimeUnit.SECONDS));return value
            }
            var ready=false
            repeat(30) { if(!ready) { android.os.SystemClock.sleep(1000);ready=js("document.querySelectorAll('.input-dom').length >= 4") == "true" } }
            fun shot(name:String) { instrumentation.uiAutomation.takeScreenshot().let { bitmap -> File(context.getExternalFilesDir(null),name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) } } }
            shot("mandarin-after.png")
            val geometry=js("JSON.stringify({url:location.origin+location.pathname,width:innerWidth,scroll:document.documentElement.scrollWidth,fields:Array.from(document.querySelectorAll('.input-dom,.btn-list .btn,#canvas,.forget-examId')).map(e=>({tag:e.tagName,id:e.id,x:e.getBoundingClientRect().x,right:e.getBoundingClientRect().right,width:e.getBoundingClientRect().width,height:e.getBoundingClientRect().height})),style:!!document.getElementById('mychangli-mobile-layout')})")
            File(context.getExternalFilesDir(null),"mandarin-layout.json").writeText(geometry)
            assertTrue("Official form unavailable; real query remains unverified",ready)
            assertEquals("true",js("Array.from(document.querySelectorAll('.input-dom,.btn-list .btn,#canvas')).every(e=>e.getBoundingClientRect().left>=0 && e.getBoundingClientRect().right<=innerWidth+1)"))
            js("document.querySelector('.input-dom').focus();document.querySelector('.input-dom').scrollIntoView({block:'center'});true")
            val encoded=js("JSON.stringify((function(){var r=document.querySelector('.input-dom').getBoundingClientRect();return [(r.x+r.width/2)/innerWidth,(r.y+r.height/2)/innerWidth];})())")
            val point=com.google.gson.JsonParser.parseString(com.google.gson.JsonParser.parseString(encoded).asString).asJsonArray
            val location=IntArray(2);var width=0
            instrumentation.runOnMainSync { web.getLocationOnScreen(location);width=web.width }
            val x=location[0]+point[0].asFloat*width;val y=location[1]+point[1].asFloat*width
            val now=android.os.SystemClock.uptimeMillis()
            instrumentation.sendPointerSync(android.view.MotionEvent.obtain(now,now,android.view.MotionEvent.ACTION_DOWN,x,y,0))
            instrumentation.sendPointerSync(android.view.MotionEvent.obtain(now,now+80,android.view.MotionEvent.ACTION_UP,x,y,0))
            android.os.SystemClock.sleep(1200);shot("mandarin-keyboard-after.png")
            js("document.querySelector('.btn-list').scrollIntoView({block:'center'});true")
            android.os.SystemClock.sleep(500);shot("mandarin-actions-after.png")
        }
    }
}
