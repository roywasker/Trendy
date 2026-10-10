import SwiftUI
import GoogleSignIn
import Firebase
import shared
import FirebaseCore
import FirebaseMessaging
import ComposeApp

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea()
                .onOpenURL { url in

                    if GIDSignIn.sharedInstance.handle(url) { return }

                    guard let components = URLComponents(url: url, resolvingAgainstBaseURL: true),
                          let queryItems = components.queryItems else { return }

                    guard url.scheme == "com.example.trendy", url.host == "paypalpay" else { return }
                    let payment = PayPalCallback.shared.parse(
                        success: queryItems.first(where: { $0.name == "success" })?.value,
                        cancel: queryItems.first(where: { $0.name == "cancel" })?.value,
                        token: queryItems.first(where: { $0.name == "token" })?.value
                    )
                    PreferencesRepository().savePayPalData(isSuccess: payment.isSuccess, error: payment.error, token: payment.token)
                }
        }
    }
}

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        NotifierManager.shared.initialize(configuration: NotificationPlatformConfigurationIos(
                    showPushNotification: true,
                    askNotificationPermissionOnStart: true,
                    notificationSoundName: nil
                  )
              )
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
           Messaging.messaging().apnsToken = deviceToken
     }
}