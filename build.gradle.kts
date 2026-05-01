import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    kotlin("plugin.jpa") version "2.2.21"
    id("org.springframework.boot") version "4.0.5"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
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

tasks.jar {
    enabled = false
}

tasks.bootJar {
    archiveBaseName.set("terminpilot-backend")
    archiveVersion.set("")
}

group = "de.stammtischHub"
version = "0.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.lookfirst:sardine:5.13")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    implementation("org.mnode.ical4j:ical4j:4.2.4")

    // Datenbank
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.mysql:mysql-connector-j")
    implementation("org.jetbrains.kotlin:kotlin-noarg")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	implementation("com.google.apis:google-api-services-calendar:v3-rev20260225-2.0.0")
	implementation("com.google.auth:google-auth-library-oauth2-http:1.46.0")
	implementation("com.google.http-client:google-http-client-jackson2:2.1.0")
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

tasks.withType<Test> {
    useJUnitPlatform()
}
