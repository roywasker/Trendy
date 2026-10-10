// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_feature_home_products_overview",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_feature_home_products_overview",
      type: .none,
      targets: ["_feature_home_products_overview"]
    )
  ],
  dependencies: [
  ],
  targets: [
    .target(
      name: "_feature_home_products_overview",
      dependencies: [
      ]
    )
  ]
)
