plugins {
    kotlin("jvm") version "2.1.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20"
    id("org.jetbrains.compose") version "1.8.2"
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("org.json:json:20240303")
    testImplementation(kotlin("test"))
}
compose.desktop { application { mainClass = "com.brunio.desktop.MainKt" } }
tasks.test { useJUnit() }
