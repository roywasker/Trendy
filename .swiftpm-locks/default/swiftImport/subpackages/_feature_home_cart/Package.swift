// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_feature_home_cart",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_feature_home_cart",
      type: .none,
      targets: ["_feature_home_cart"]
    )
  ],
  dependencies: [
  ],
  targets: [
    .target(
      name: "_feature_home_cart",
      dependencies: [
      ]
    )
  ]
)
