// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "io_github_mirzemehdi_kmpauth_firebase_google_3_0_6",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "io_github_mirzemehdi_kmpauth_firebase_google_3_0_6",
      type: .none,
      targets: ["io_github_mirzemehdi_kmpauth_firebase_google_3_0_6"]
    )
  ],
  dependencies: [
    .package(
      url: "https://github.com/firebase/firebase-ios-sdk.git",
      "12.17.0"..."12.999.999"
    ),
    .package(
      url: "https://github.com/google/GoogleSignIn-iOS.git",
      from: "9.1.0"
    )
  ],
  targets: [
    .target(
      name: "io_github_mirzemehdi_kmpauth_firebase_google_3_0_6",
      dependencies: [
        .product(
          name: "FirebaseAuth",
          package: "firebase-ios-sdk"
        ),
        .product(
          name: "FirebaseCore",
          package: "firebase-ios-sdk"
        ),
        .product(
          name: "GoogleSignIn",
          package: "GoogleSignIn-iOS"
        )
      ]
    )
  ]
)
