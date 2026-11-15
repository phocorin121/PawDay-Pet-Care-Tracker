package com.phocorin.pawday
import android.content.Intent
import android.app.NotificationManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class DeviceSmokeTest {
    private fun activity(): MainActivity {
        val ins=InstrumentationRegistry.getInstrumentation()
        val a=ins.startActivitySync(Intent(ins.targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        ins.waitForIdleSync()
        return a
    }
    private fun js(a: MainActivity, code: String): String {
        val latch=CountDownLatch(1);var value=""
        a.runOnUiThread { a.web.evaluateJavascript(code) { value=it; latch.countDown() } }
        assertTrue(latch.await(10,TimeUnit.SECONDS));return value
    }
    private fun ready(a: MainActivity) { for(i in 0..39){if(js(a,"typeof S")!="\"undefined\"")return;Thread.sleep(250)};fail("Web app did not load") }
    @Test fun offlineLoadAndNativePersistence() {
        val a=activity();ready(a)
        assertEquals("true",js(a,"typeof Android.store==='function'"))
        assertEquals("true",js(a,"S.pets=[{id:'device-test',name:'Device Pet',type:'dog'}];S.cur='device-test';quick('water');save()"))
        val ins=InstrumentationRegistry.getInstrumentation();ins.runOnMainSync{a.finish()};val b=activity();ready(b)
        assertEquals("\"Device Pet\"",js(b,"P().name"));assertEquals("1",js(b,"S.recs.filter(r=>r.pet==='device-test').length"))
        ins.runOnMainSync{b.finish()}
    }
    @Test fun notificationChannelAndScheduledRoutine() {
        val a=activity();ready(a)
        assertEquals("true",js(a,"S.pets=[{id:'alarm-pet',name:'Alarm Pet',type:'cat'}];S.rts=[{id:'alarm-routine',pet:'alarm-pet',cat:'meal',time:'23:59',days:[0,1,2,3,4,5,6]}];save()"))
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val c=InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull(c.getSystemService(NotificationManager::class.java).getNotificationChannel("care"))
        assertTrue(c.getSharedPreferences("pawday",0).getStringSet("alarms",emptySet())!!.contains("rts:alarm-routine"))
        InstrumentationRegistry.getInstrumentation().runOnMainSync{a.finish()}
    }
}
