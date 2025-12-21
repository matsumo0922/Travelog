import UIKit
import SwiftUI
import ComposeApp

struct ComposeViewController: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        initTools()
        return ApplicationKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // no-op
    }

    private func initTools() {
        InitHelperKt.doInitNapier()
        InitHelperKt.doInitKoin()
    }
}
