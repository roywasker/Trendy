// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_data",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_data",
      type: .none,
      targets: ["_data"]
    )
  ],
  dependencies: [
  ],
  targets: [
    .target(
      name: "_data",
      dependencies: [
      ]
    )
  ]
)
