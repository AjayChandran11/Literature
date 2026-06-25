package com.cards.game.literature.notifications

import platform.Foundation.NSDateComponents
import platform.Foundation.NSError
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

actual object PuzzleReminderScheduler {
    private const val ID = "lit_puzzle_reminder"

    /**
     * A repeating daily calendar trigger at [PuzzleReminder.HOUR_OF_DAY] local — the OS fires it even
     * while the app is closed. (Unlike Android, a repeating trigger can't be skipped for a single day,
     * so on iOS the reminder may still arrive on a day already solved before the hour; an acceptable
     * edge for a re-engagement nudge.)
     */
    actual fun schedule() {
        val content = UNMutableNotificationContent().apply {
            setTitle("Today's puzzle is ready ⭐")
            setBody("A fresh Literature puzzle is waiting — keep your streak alive.")
            setSound(UNNotificationSound.defaultSound())
        }
        val components = NSDateComponents().apply {
            hour = PuzzleReminder.HOUR_OF_DAY.toLong()
            minute = PuzzleReminder.MINUTE.toLong()
        }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = true)
        val request = UNNotificationRequest.requestWithIdentifier(ID, content, trigger)
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.removePendingNotificationRequestsWithIdentifiers(listOf(ID)) // replace any prior
        center.addNotificationRequest(request) { _: NSError? -> }
    }

    actual fun cancel() {
        UNUserNotificationCenter.currentNotificationCenter()
            .removePendingNotificationRequestsWithIdentifiers(listOf(ID))
    }
}
