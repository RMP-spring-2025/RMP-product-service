import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.21"
    id("io.ktor.plugin") version "2.3.4"
    application
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("io.ktor:ktor-server-core:2.3.4")
    implementation("io.ktor:ktor-server-netty:2.3.4")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.4")
    implementation("io.lettuce:lettuce-core:6.5.5.RELEASE")

    // PostgreSQL + Exposed ORM
    implementation("org.jetbrains.exposed:exposed-core:0.42.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.42.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.42.0")
    implementation("org.postgresql:postgresql:42.6.0")
    implementation("com.zaxxer:HikariCP:5.0.1")

    // Redis клиент

    // Логирование
    implementation("ch.qos.logback:logback-classic:1.4.14")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}
