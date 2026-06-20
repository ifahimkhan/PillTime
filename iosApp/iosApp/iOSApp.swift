import SwiftUI
import UserNotifications
import Shared

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
        let center = UNUserNotificationCenter.current()
        center.delegate = self
        
        // Request notification permissions at startup
        center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            if let error = error {
                print("Notification permission error: \(error)")
            }
        }
        return true
    }

    // Force notifications to show as banners and play sounds even when app is in foreground
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound, .badge])
    }

    // Handle user interaction with notification action buttons in the background
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let actionIdentifier = response.actionIdentifier
        let reminderIdString = response.notification.request.identifier

        if actionIdentifier == "TAKEN" {
            // Dismisses notification and stops sound natively
            print("Reminder \(reminderIdString) marked as Taken.")
        } else if actionIdentifier == "SNOOZE" {
            // Dismisses notification, stops sound, and schedules snooze alert 10 min later
            if let reminderId = Int64(reminderIdString) {
                snoozeNotification(reminderId: reminderId, originalContent: response.notification.request.content)
            }
        }
        completionHandler()
    }

    private func snoozeNotification(reminderId: Int64, originalContent: UNNotificationContent) {
        let content = UNMutableNotificationContent()
        content.title = "Snoozed: \(originalContent.title)"
        content.body = originalContent.body
        content.sound = UNNotificationSound(named: UNNotificationSoundName("Alarmed.wav"))
        content.categoryIdentifier = originalContent.categoryIdentifier

        // Fire again in 10 minutes (600 seconds)
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 600, repeats: false)
        let request = UNNotificationRequest(
            identifier: "\(reminderId)_snooze",
            content: content,
            trigger: trigger
        )

        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                print("Error scheduling iOS snooze: \(error)")
            }
        }
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}