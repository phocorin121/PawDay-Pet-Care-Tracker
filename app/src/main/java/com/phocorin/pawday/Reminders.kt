package com.phocorin.pawday

import android.app.*
import android.content.*
import android.net.Uri
import java.time.*
import org.json.*

object Reminders {
    private fun prefs(c: Context) = c.getSharedPreferences("pawday", Context.MODE_PRIVATE)
    fun channel(c: Context) { c.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("care", "PawDay", NotificationManager.IMPORTANCE_DEFAULT)) }
    private fun intent(c: Context, key: String): PendingIntent = PendingIntent.getBroadcast(c, 0, Intent(c, ReminderReceiver::class.java).setData(Uri.parse("pawday://reminder/"+Uri.encode(key))).putExtra("key", key), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    @Synchronized fun sync(c: Context) {
        val alarm = c.getSystemService(AlarmManager::class.java)
        val p = prefs(c)
        p.getStringSet("alarms", emptySet())!!.forEach { alarm.cancel(intent(c, it)) }
        val keys = mutableSetOf<String>()
        try {
            val s = JSONObject(p.getString("state", "{}")!!)
            val pets = s.optJSONArray("pets") ?: JSONArray()
            val valid = (0 until pets.length()).map { pets.getJSONObject(it).getString("id") }.toSet()
            val now = ZonedDateTime.now()
            for (kind in listOf("rts", "evs")) {
                val arr = s.optJSONArray(kind) ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val r = arr.getJSONObject(i)
                    if (r.optString("pet") !in valid) continue
                    val at = if (kind == "rts") {
                        val ds = r.getJSONArray("days")
                        ReminderTime.next(r.getString("time"), (0 until ds.length()).map { ds.getInt(it) }.toSet(), now)
                    } else try { LocalDateTime.parse(r.getString("date")+"T"+r.getString("time")).atZone(now.zone).takeIf { it.isAfter(now) } } catch (_: Exception) { null }
                    if (at != null) {
                        val key = "$kind:${r.getString("id")}"
                        keys.add(key)
                        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), intent(c, key))
                    }
                }
            }
        } finally { p.edit().putStringSet("alarms", keys).apply() }
    }
    fun notify(c: Context, key: String, text: String) {
        channel(c)
        val nm = c.getSystemService(NotificationManager::class.java)
        if (!nm.areNotificationsEnabled()) return
        val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        try { nm.notify(key.hashCode(), Notification.Builder(c, "care").setSmallIcon(android.R.drawable.ic_menu_my_calendar).setContentTitle("PawDay").setContentText(text).setStyle(Notification.BigTextStyle().bigText(text)).setAutoCancel(true).setContentIntent(open).build()) } catch (_: SecurityException) { }
    }
    fun receive(c: Context, key: String) {
        try {
            val s = JSONObject(prefs(c).getString("state", "{}")!!)
            val kind = key.substringBefore(':'); val id = key.substringAfter(':')
            val a = s.getJSONArray(kind)
            val r = (0 until a.length()).map { a.getJSONObject(it) }.find { it.optString("id") == id } ?: return
            val pets = s.getJSONArray("pets")
            val pet = (0 until pets.length()).map { pets.getJSONObject(it) }.find { it.optString("id") == r.optString("pet") } ?: return
            val recs = s.optJSONArray("recs") ?: JSONArray()
            if (kind == "rts" && (0 until recs.length()).any { recs.getJSONObject(it).let { x -> x.optString("rid") == id && x.optString("date") == LocalDate.now().toString() && x.optString("pet") == r.optString("pet") } }) return
            val ja = s.optString("lang") == "ja"
            val cat = r.optString("cat")
            val labels = if (ja) mapOf("meal" to "食事", "water" to "水", "walk" to "散歩", "toilet" to "トイレ", "med" to "お薬", "other" to "その他") else emptyMap()
            val label = r.optString("title").ifBlank { r.optString("label") }.ifBlank { if (cat == "med") r.optString("sub") else "" }.ifBlank { labels[cat] ?: cat }
            notify(c, key, "${pet.getString("name")} · ${r.optString("time")} · $label")
        } finally { sync(c) }
    }
}
class ReminderReceiver : BroadcastReceiver() { override fun onReceive(c: Context, i: Intent) { try { Reminders.receive(c, i.getStringExtra("key") ?: return) } catch (_: Exception) { } } }
class RestoreReceiver : BroadcastReceiver() { override fun onReceive(c: Context, i: Intent) { try { Reminders.sync(c) } catch (_: Exception) { } } }
