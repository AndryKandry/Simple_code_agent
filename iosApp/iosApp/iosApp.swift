import SwiftUI
import ComposeApp

@main
struct ComposeApp: App {
    
    init() {
        // initKoin
        KoinSetupKt.doInitKoin(
            setupContext: { _ in },
            appModule: KoinSetupKt.createEmptyModule()
        )
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView().ignoresSafeArea(.all)
        }
    }
}

struct ContentView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return MainKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // Updates will be handled by Compose
    }
}
