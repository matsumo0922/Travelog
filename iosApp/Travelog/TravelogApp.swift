//
//  TravelogApp.swift
//  Travelog
//
//  Created by 松本 大智 on 2025/12/22.
//

import SwiftUI
import ComposeApp

@main
struct TravelogApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeViewController()
                .ignoresSafeArea(.all)
                .onOpenURL { url in
                    SupabaseAuthHandler.shared.handleDeeplink(url: url)
                }
        }
    }
}
