plugins {
    kotlin("jvm") version "2.3.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val grpcVersion = "1.68.2"

dependencies {
    implementation(project(":chat-api"))
    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("MainKt")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform {
        excludeTags("e2e")
    }
}

tasks.register<Test>("e2eTest") {
    group = "verification"
    description = "E2E: два процесса installDist-бинарника (тег e2e). Зависит от installDist."
    dependsOn(tasks.installDist, tasks.named("testClasses"))
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("e2e")
    }
    doFirst {
        val win = System.getProperty("os.name").lowercase().contains("windows")
        val scriptName = if (win) "ExtremeProgrammingPP.bat" else "ExtremeProgrammingPP"
        val binary = layout.buildDirectory
            .get()
            .asFile
            .resolve("install/ExtremeProgrammingPP/bin/$scriptName")
        systemProperty("e2e.binary", binary.absolutePath)
    }
}