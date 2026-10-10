// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_feature_admin_panel",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_feature_admin_panel",
      type: .none,
      targets: ["_feature_admin_panel"]
    )
  ],
  dependencies: [
  ],
  targets: [
    .target(
      name: "_feature_admin_panel",
      dependencies: [
      ]
    )
  ]
)
