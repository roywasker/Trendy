// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_di",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "_di",
      type: .none,
      targets: ["_di"]
    )
  ],
  dependencies: [
  ],
  targets: [
    .target(
      name: "_di",
      dependencies: [
      ]
    )
  ]
)
