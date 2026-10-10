// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_feature_auth",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_feature_auth",
      type: .none,
      targets: ["_feature_auth"]
    )
  ],
  dependencies: [
  ],
  targets: [
    .target(
      name: "_feature_auth",
      dependencies: [
      ]
    )
  ]
)
