// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "KotlinMultiplatformLinkedPackage",
  platforms: [
    .iOS("16.0")
  ],
  products: [
    .library(
      name: "KotlinMultiplatformLinkedPackage",
      type: .none,
      targets: ["KotlinMultiplatformLinkedPackage"]
    )
  ],
  dependencies: [
    .package(path: "subpackages/_composeApp"),
    .package(path: "subpackages/_data"),
    .package(path: "subpackages/_di"),
    .package(path: "subpackages/_feature_admin_panel"),
    .package(path: "subpackages/_feature_admin_panel_manage_product"),
    .package(path: "subpackages/_feature_auth"),
    .package(path: "subpackages/_feature_details"),
    .package(path: "subpackages/_feature_home"),
    .package(path: "subpackages/_feature_home_cart"),
    .package(path: "subpackages/_feature_home_cart_checkout"),
    .package(path: "subpackages/_feature_home_categories"),
    .package(path: "subpackages/_feature_home_categories_category_search"),
    .package(path: "subpackages/_feature_home_products_overview"),
    .package(path: "subpackages/_feature_payment_completed"),
    .package(path: "subpackages/_feature_profile"),
    .package(path: "subpackages/_navigation"),
    .package(path: "subpackages/dev_gitlive_firebase_app_3_0_0_alpha01"),
    .package(path: "subpackages/dev_gitlive_firebase_auth_3_0_0_alpha01"),
    .package(path: "subpackages/io_github_mirzemehdi_kmpauth_firebase_3_0_6"),
    .package(path: "subpackages/io_github_mirzemehdi_kmpauth_firebase_google_3_0_6"),
    .package(path: "subpackages/io_github_mirzemehdi_kmpauth_google_3_0_6"),
    .package(path: "subpackages/io_github_mirzemehdi_kmpnotifier_push_firebase_2_0_0")
  ],
  targets: [
    .target(
      name: "KotlinMultiplatformLinkedPackage",
      dependencies: [
        .product(name: "_composeApp", package: "_composeApp"),
        .product(name: "_data", package: "_data"),
        .product(name: "_di", package: "_di"),
        .product(name: "_feature_admin_panel", package: "_feature_admin_panel"),
        .product(name: "_feature_admin_panel_manage_product", package: "_feature_admin_panel_manage_product"),
        .product(name: "_feature_auth", package: "_feature_auth"),
        .product(name: "_feature_details", package: "_feature_details"),
        .product(name: "_feature_home", package: "_feature_home"),
        .product(name: "_feature_home_cart", package: "_feature_home_cart"),
        .product(name: "_feature_home_cart_checkout", package: "_feature_home_cart_checkout"),
        .product(name: "_feature_home_categories", package: "_feature_home_categories"),
        .product(name: "_feature_home_categories_category_search", package: "_feature_home_categories_category_search"),
        .product(name: "_feature_home_products_overview", package: "_feature_home_products_overview"),
        .product(name: "_feature_payment_completed", package: "_feature_payment_completed"),
        .product(name: "_feature_profile", package: "_feature_profile"),
        .product(name: "_navigation", package: "_navigation"),
        .product(name: "dev_gitlive_firebase_app_3_0_0_alpha01", package: "dev_gitlive_firebase_app_3_0_0_alpha01"),
        .product(name: "dev_gitlive_firebase_auth_3_0_0_alpha01", package: "dev_gitlive_firebase_auth_3_0_0_alpha01"),
        .product(name: "io_github_mirzemehdi_kmpauth_firebase_3_0_6", package: "io_github_mirzemehdi_kmpauth_firebase_3_0_6"),
        .product(name: "io_github_mirzemehdi_kmpauth_firebase_google_3_0_6", package: "io_github_mirzemehdi_kmpauth_firebase_google_3_0_6"),
        .product(name: "io_github_mirzemehdi_kmpauth_google_3_0_6", package: "io_github_mirzemehdi_kmpauth_google_3_0_6"),
        .product(name: "io_github_mirzemehdi_kmpnotifier_push_firebase_2_0_0", package: "io_github_mirzemehdi_kmpnotifier_push_firebase_2_0_0")
      ]
    )
  ]
)
