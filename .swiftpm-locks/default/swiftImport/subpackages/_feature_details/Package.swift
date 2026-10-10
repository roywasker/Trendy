// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_feature_details",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_feature_details",
      type: .none,
      targets: ["_feature_details"]
    )
  ],
  dependencies: [
  ],
  targets: [
    .target(
      name: "_feature_details",
      dependencies: [
      ]
    )
  ]
)
