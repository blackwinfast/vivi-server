val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("io.ktor.plugin") version "3.3.1"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.0"
}

// Configure test task to be deterministic in CI/local runs
tasks.test {
    // run tests sequentially to avoid intermittent flakiness from shared state
    maxParallelForks = 1
    testLogging {
        showStandardStreams = true
    }
}

// ktlint and detekt configuration
ktlint {
    debug.set(false)
    version.set("0.50.0")
    outputToConsole.set(true)
    // Enforce ktlint in CI: fail the build on style violations.
    ignoreFailures.set(false)
    // Exclude generated or very large template files that break the parser temporarily.
    // Use the extension filter block to exclude files (task-level exclusion via the plugin API).
    // Exclude the Routing.kt path under the main source set so ktlint won't try to parse it.
    // Use a file-glob the plugin reliably recognizes for source-files.
    filter {
        // exclude by filename anywhere in the tree
        exclude("**/Routing.kt")
    }
}

detekt {
    toolVersion = "1.23.0"
    config = files("detekt.yml")
    buildUponDefaultConfig = true
    parallel = true
    // Enforce detekt in CI: fail the build on issues not present in the baseline.
    ignoreFailures = false
    // Use a baseline file to ignore existing findings; we'll generate it and commit it.
    baseline = file("detekt-baseline.xml")
}
// Exclude problematic files at the task level to avoid analyzer crashes (temporary).
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    // exclude the single Routing.kt file that contains large embedded templates
    exclude("**/Routing.kt")
}

// Additionally configure ktlint tasks to exclude the Routing.kt file explicitly
// (use path under src so the check/format tasks don't attempt to parse the large template).
// Note: task-level ktlint configuration attempted earlier caused script compilation errors
// in this project's Gradle Kotlin DSL. The extension-level filter above excludes the file
// from checks and formats, which is sufficient for the temporary exclusion.

// NOTE: earlier attempts to configure ktlint tasks caused Kotlin DSL resolution issues;
// the extension-level ktlint.filter above should be sufficient to exclude Routing.kt.

group = "com.example"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

// 添加测试任务：运行 Main.kt
tasks.register<JavaExec>("runTest") {
    group = "application"
    description = "运行情绪系统测试程序"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.example.MainKt")
}

dependencies {
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.ktor:ktor-server-netty")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-config-yaml")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-cio")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-client-logging")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    // Exposed + SQLite for DB-backed DAOs (Kotlin 2 compatible)
    implementation("org.jetbrains.exposed:exposed-core:0.61.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.61.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.61.0")
    implementation("org.xerial:sqlite-jdbc:3.42.0.0")
    implementation("com.zaxxer:HikariCP:5.0.1")
    // detekt formatting rules require the detekt-formatting plugin
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.0")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}
