val gitTag: String? by lazy {
    providers.exec {
        commandLine("git", "describe", "--tags", "--abbrev=0")
        workingDir(rootProject.projectDir)
        isIgnoreExitValue = true
    }.standardOutput.asText.map { it.trim() }.orNull
}

val gitHash: String? by lazy {
    providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        workingDir(rootProject.projectDir)
        isIgnoreExitValue = true
    }.standardOutput.asText.map { it.trim() }.orNull
}

rootProject.version = gitTag ?: gitHash ?: "dev"

subprojects {
    this.version = rootProject.version
}