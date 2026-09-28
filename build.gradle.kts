plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.firebaseCrashlytics) apply false
    // Applied (not `apply false`) so the root project can aggregate every module's coverage.
    alias(libs.plugins.kover)
}

// Real coverage, not the test-lines-over-production-lines proxy we had been reasoning from.
// `./gradlew koverHtmlReport` writes build/reports/kover/html/index.html.
dependencies {
    kover(project(":shared"))
    kover(project(":server"))
    kover(project(":composeApp"))
}

kover {
    reports {
        // `total` governs the aggregated report; filters set only on `reports` apply to the
        // per-variant ones and left the generated accessors in the merged total.
        total {
            filters {
                excludes {
                    // Generated, or drawn rather than decided: neither says anything about
                    // whether the logic is exercised.
                    packages("literature.composeapp.generated.resources")
                    classes(
                        "*.ComposableSingletons*",
                        "*_*Preview*",
                        "*.BuildConfig",
                    )
                    annotatedBy("androidx.compose.runtime.Composable")
                }
            }
        }
    }
}
