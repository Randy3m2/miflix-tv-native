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
    implementation("uk.co.caprica:vlcj:4.8.3")
    testImplementation(kotlin("test"))
}
compose.desktop {
    application {
        mainClass="com.brunio.desktop.MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe)
            packageName="BruniO-PC"
            packageVersion="2.0.0"
            includeAllModules=true
            appResourcesRootDir.set(project.layout.projectDirectory.dir("../runtime-resources"))
            windows { iconFile.set(file("../../brunio-pc/assets/brunio.ico")) }
        }
    }
}
tasks.test { useJUnit() }
