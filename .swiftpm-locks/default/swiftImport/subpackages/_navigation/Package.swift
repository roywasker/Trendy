// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_navigation",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_navigation",
      type: .none,
      targets: ["_navigation"]
    )
  ],
  dependencies: [
  ],
  targets: [
    .target(
      name: "_navigation",
      dependencies: [
      ]
    )
  ]
)
