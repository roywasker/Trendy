// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_feature_home_cart_checkout",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_feature_home_cart_checkout",
      type: .none,
      targets: ["_feature_home_cart_checkout"]
    )
  ],
  dependencies: [
  ],
  targets: [
    .target(
      name: "_feature_home_cart_checkout",
      dependencies: [
      ]
    )
  ]
)
