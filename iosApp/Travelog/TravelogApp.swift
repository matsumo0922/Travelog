//
//  TravelogApp.swift
//  Travelog
//
//  Created by 松本 大智 on 2025/12/22.
//

import SwiftUI

@main
struct TravelogApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeViewController()
                .ignoresSafeArea(.all)
        }
    }
}
