import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    kotlin("plugin.jpa") version "2.3.21"
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("org.owasp.dependencycheck") version "9.2.0"
}

ktlint {
    reporters {
        reporter(ReporterType.CHECKSTYLE)
    }
}

tasks.named("check") {
    setDependsOn(
        dependsOn.filterNot { dep ->
            dep is TaskProvider<*> && dep.name.startsWith("ktlint")
        },
    )
}

dependencyCheck {
    failBuildOnCVSS = 4.0f
    formats = listOf("HTML", "SARIF")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.jar {
    enabled = false
}

tasks.bootJar {
    archiveBaseName.set("terminpilot-backend")
    archiveVersion.set("")
}

group = "de.stammtischHub"
version = "0.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Provider
    implementation("com.github.lookfirst:sardine:5.13")
    implementation("org.mnode.ical4j:ical4j:4.2.5")

    // Datenbank
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.mysql:mysql-connector-j")
    implementation("org.jetbrains.kotlin:kotlin-noarg")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
