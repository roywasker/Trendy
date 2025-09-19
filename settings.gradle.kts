rootProject.name = "EcommerceProject"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
                includeGroupAndSubgroups("com.google.firebase")
            }
        }
        mavenCentral()
    }
}
include(":data")
include(":di")
include(":feature:admin_panel")
include(":feature:admin_panel:manage_product")
include(":feature:details")
include(":feature:home:cart")
include(":feature:home:cart:checkout")
include(":feature:home:cart:checkout")
include(":feature:home:categories")
include(":feature:home:categories:category_search")
include(":feature:home:products_overview")
include(":feature:home")
include(":feature:payment_completed")
include(":feature:profile")
include(":freature:details")
include(":feature:auth")
include(":navigation")
include(":composeApp")
include(":shared")
