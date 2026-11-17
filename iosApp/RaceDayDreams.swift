import RaceDayShared
import SwiftUI

@main
struct RaceDayDreamsApp: App {
    var body: some Scene {
        WindowGroup { RaceDayRoot() }
    }
}

struct RaceDayRoot: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        do {
            let directory = try FileManager.default.url(
                for: .applicationSupportDirectory, in: .userDomainMask,
                appropriateFor: nil, create: true
            )
            return AppleAppHostKt.raceDayViewController(
                databasePath: directory.appendingPathComponent("racedaydreams.sqlite").path,
                bundleIdentifier: Bundle.main.bundleIdentifier ?? "org.racedaydreams.ios"
            )
        } catch {
            let controller = UIHostingController(rootView: Text("Could not open local training data."))
            return controller
        }
    }

    func updateUIViewController(_ controller: UIViewController, context: Context) {}
}
