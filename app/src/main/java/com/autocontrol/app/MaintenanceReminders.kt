package com.autocontrol.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class MecanicaCachiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.schedule(this)
    }
}

object ReminderScheduler {
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<MaintenanceReminderWorker>(24, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "mecanica_cachi_maintenance_reminders",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

class MaintenanceReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("autocontrol", Context.MODE_PRIVATE)
        val raw = prefs.getString("vehicles", null) ?: return Result.success()
        val vehicles = try { JSONArray(raw) } catch (_: Exception) { return Result.success() }
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "maintenance_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(channelId, "Recordatorios de mantenimiento", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Avisos de próximos servicios y mantenimientos vencidos"
            })
        }
        val today = dateOnly(Date())
        val notified = prefs.getStringSet("notified_maintenance_reminders", emptySet())?.toMutableSet() ?: mutableSetOf()
        for (i in 0 until vehicles.length()) {
            val vehicle = vehicles.optJSONObject(i) ?: continue
            val services = vehicle.optJSONArray("maintenances") ?: JSONArray()
            val plate = vehicle.optString("plate", "Vehículo")
            val model = listOf(vehicle.optString("make"), vehicle.optString("model")).filter { it.isNotBlank() }.joinToString(" ")
            for (j in 0 until services.length()) {
                val service = services.optJSONObject(j) ?: continue
                val dueRaw = service.optString("dueDate", "")
                val due = parseDate(dueRaw) ?: continue
                val days = ((due.time - today.time) / 86400000L).toInt()
                if (days > 7) continue
                val id = plate + "|" + service.optLong("id") + "|" + dueRaw
                if (id in notified) continue
                val title = when {
                    days < 0 -> "Mantenimiento vencido"
                    days == 0 -> "Mantenimiento para hoy"
                    else -> "Mantenimiento próximo"
                }
                val timing = when {
                    days < 0 -> "Vencido hace " + (-days) + " días"
                    days == 0 -> "La fecha indicada es hoy"
                    else -> "Faltan " + days + " días"
                }
                val content = listOf(service.optString("type", "Servicio"), plate, model, timing).filter { it.isNotBlank() }.joinToString(" · ")
                val notification = NotificationCompat.Builder(applicationContext, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()
                try {
                    NotificationManagerCompat.from(applicationContext).notify(id.hashCode(), notification)
                    notified.add(id)
                } catch (_: SecurityException) {
                    // Retry on the next scheduled check if permission is not granted.
                }
            }
        }
        prefs.edit().putStringSet("notified_maintenance_reminders", notified).apply()
        return Result.success()
    }

    private fun parseDate(value: String): Date? {
        for (pattern in listOf("dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd")) {
            try {
                val format = SimpleDateFormat(pattern, Locale("es", "AR")).apply {
                    isLenient = false
                    timeZone = TimeZone.getDefault()
                }
                val parsed = format.parse(value) ?: continue
                return dateOnly(parsed)
            } catch (_: Exception) { }
        }
        return null
    }

    private fun dateOnly(date: Date): Date {
        val calendar = java.util.Calendar.getInstance().apply {
            time = date
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return calendar.time
    }
}