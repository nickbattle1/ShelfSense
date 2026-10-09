package com.example.shelfsense.worker

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.shelfsense.MainActivity
import com.example.shelfsense.R
import com.example.shelfsense.data.local.PantryItem
import com.example.shelfsense.data.local.actionInfo
import com.example.shelfsense.domain.ActionDates
import com.example.shelfsense.domain.PantryFilter
import java.time.LocalDate

// builds the grouped reminder: one notification per item up to a cap, plus a summary
// that opens the pantry already filtered to the items that need using
object ReminderNotifier {

    const val CHANNEL_ID = "expiry_reminders"
    private const val GROUP_KEY = "com.example.shelfsense.EXPIRY"
    private const val SUMMARY_ID = 4200
    private const val MAX_CHILDREN = 5
    private const val MAX_LINES = 6

    fun createChannel(context: Context) {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(context.getString(R.string.channel_reminders_name))
            .setDescription(context.getString(R.string.channel_reminders_description))
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    // returns false when notifications are blocked, so the caller knows nothing was shown
    @SuppressLint("MissingPermission") // canPost checks it first
    fun show(context: Context, items: List<PantryItem>, today: LocalDate): Boolean {
        if (!canPost(context)) return false
        val manager = NotificationManagerCompat.from(context)
        // yesterday's reminders are replaced rather than piling up
        manager.cancelAll()
        if (items.isEmpty()) return true

        val sorted = items.sortedBy { it.actionDate }
        val colour = ContextCompat.getColor(context, R.color.shelf_green)

        sorted.take(MAX_CHILDREN).forEach { item ->
            val child = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_shelfsense)
                .setColor(colour)
                .setContentTitle(item.name)
                .setContentText(lineFor(item, today))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setGroup(GROUP_KEY)
                .setAutoCancel(true)
                .setContentIntent(deepLink(context, "shelfsense://item/${item.id}", item.id.hashCode()))
                .build()
            manager.notify(item.id.hashCode(), child)
        }

        val title = if (sorted.size == 1) "1 item to use soon" else "${sorted.size} items to use soon"
        val style = NotificationCompat.InboxStyle().setBigContentTitle(title)
        sorted.take(MAX_LINES).forEach { style.addLine("${it.name}: ${lineFor(it, today)}") }
        if (sorted.size > MAX_LINES) style.setSummaryText("+${sorted.size - MAX_LINES} more")

        val summary = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_shelfsense)
            .setColor(colour)
            .setContentTitle(title)
            .setContentText(sorted.take(3).joinToString(", ") { it.name })
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setContentIntent(
                deepLink(context, "shelfsense://pantry?filter=${PantryFilter.SOON.name}", SUMMARY_ID)
            )
            .build()
        manager.notify(SUMMARY_ID, summary)
        return true
    }

    private fun lineFor(item: PantryItem, today: LocalDate): String =
        ActionDates.daysLeftLabel(item.actionInfo(today).daysLeft)

    // TaskStackBuilder gives the deep link a proper back stack, so Back from the item lands on Home
    private fun deepLink(context: Context, uri: String, requestCode: Int): PendingIntent? {
        val intent = Intent(Intent.ACTION_VIEW, uri.toUri(), context, MainActivity::class.java)
        return TaskStackBuilder.create(context)
            .addNextIntentWithParentStack(intent)
            .getPendingIntent(requestCode, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
