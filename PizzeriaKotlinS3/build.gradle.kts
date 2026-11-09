import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.21"
    application
}

repositories {
    mavenCentral()
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    sourceSets {
        main { kotlin.setSrcDirs(listOf("src")) }
        test { kotlin.setSrcDirs(listOf("tests")) }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(kotlin("reflect"))
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("MainKt")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Permite ejecutar la demostración con Main.kt sin interacción y guardar solo su salida.
tasks.register<JavaExec>("demostracion") {
    group = "application"
    description = "Ejecuta el resumen integrado de la evaluación de Semana 3."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("MainKt")
    args("--demo")
}
