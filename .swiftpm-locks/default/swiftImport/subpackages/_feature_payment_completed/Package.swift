// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_feature_payment_completed",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_feature_payment_completed",
      type: .none,
      targets: ["_feature_payment_completed"]
    )
  ],
  dependencies: [
  ],
  targets: [
    .target(
      name: "_feature_payment_completed",
      dependencies: [
      ]
    )
  ]
)
