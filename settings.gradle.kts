rootProject.name = "Slash"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            val coroutines = version("coroutines", "1.7.1")
            val jda = version("jda", "5.0.0-beta.21")
            val prometheus = version("prometheus", "0.16.0")
            val reflections = version("reflections", "0.10.2")
            val sentry = version("sentry", "7.6.0")
            val slf4j = version("slf4j", "2.0.7")

            library("coroutines.core", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").versionRef(coroutines)

            library("jda", "net.dv8tion", "JDA").versionRef(jda)
            library("prometheus", "io.prometheus", "simpleclient").versionRef(prometheus)
            library("reflections", "org.reflections", "reflections").versionRef(reflections)
            library("sentry", "io.sentry", "sentry").versionRef(sentry)
            library("slf4j", "org.slf4j", "slf4j-api").versionRef(slf4j)
        }
    }
}