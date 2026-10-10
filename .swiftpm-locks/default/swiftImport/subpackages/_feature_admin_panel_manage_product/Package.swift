// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_feature_admin_panel_manage_product",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_feature_admin_panel_manage_product",
      type: .none,
      targets: ["_feature_admin_panel_manage_product"]
    )
  ],
  dependencies: [
  ],
  targets: [
    .target(
      name: "_feature_admin_panel_manage_product",
      dependencies: [
      ]
    )
  ]
)
