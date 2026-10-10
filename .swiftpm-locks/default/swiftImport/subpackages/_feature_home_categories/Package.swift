// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_feature_home_categories",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_feature_home_categories",
      type: .none,
      targets: ["_feature_home_categories"]
    )
  ],
  dependencies: [
  ],
  targets: [
    .target(
      name: "_feature_home_categories",
      dependencies: [
      ]
    )
  ]
)
