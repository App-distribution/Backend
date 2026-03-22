pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AppDistribution"

include(":app")
include(":core:common")
include(":core:ui")
include(":core:datastore")
include(":core:network")
include(":core:database")
include(":feature:auth")
include(":feature:home")
include(":feature:browse")
include(":feature:build-detail")
include(":feature:upload")
include(":feature:mine")
include(":feature:settings")
