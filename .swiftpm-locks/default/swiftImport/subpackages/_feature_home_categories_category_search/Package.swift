// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_feature_home_categories_category_search",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_feature_home_categories_category_search",
      type: .none,
      targets: ["_feature_home_categories_category_search"]
    )
  ],
  dependencies: [
  ],
  targets: [
    .target(
      name: "_feature_home_categories_category_search",
      dependencies: [
      ]
    )
  ]
)
