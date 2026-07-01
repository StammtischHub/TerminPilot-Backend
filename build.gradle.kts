import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
  kotlin("jvm") version "2.4.0"
  kotlin("plugin.spring") version "2.4.0"
  kotlin("plugin.jpa") version "2.4.0"
  id("org.springframework.boot") version "4.1.0"
  id("io.spring.dependency-management") version "1.1.7"
  id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
  id("org.openapi.generator") version "7.23.0"
  jacoco
}

val envFile = file(".env")
if (envFile.exists()) {
  envFile.forEachLine { line ->
    if (line.isNotBlank() && !line.startsWith("#")) {
      val (key, value) = line.split("=", limit = 2)
      System.setProperty(key.trim(), value.trim())
    }
  }
}

group = "de.stammtischHub"
version = "0.0.0-SNAPSHOT"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(24)
  }
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll("-Xjsr305=strict")
  }
}

ktlint {
  reporters {
    reporter(ReporterType.CHECKSTYLE)
  }
  filter {
    exclude { element ->
      element.file.path.contains("/generated/")
    }
  }
}

allOpen {
  annotation("jakarta.persistence.Entity")
  annotation("jakarta.persistence.MappedSuperclass")
  annotation("jakarta.persistence.Embeddable")
}

repositories {
  mavenCentral()
  maven {
    url = uri("https://maven.pkg.github.com/StammtischHub/TerminPilot-API-Spec")
    credentials {
      username = System.getenv("GITHUB_ACTOR")
        ?: System.getProperty("GITHUB_ACTOR")
        ?: "x-access-token"
      password = System.getenv("GITHUB_TOKEN")
        ?: System.getProperty("GITHUB_TOKEN")
        ?: error("GITHUB_TOKEN ist nicht gesetzt")
    }
  }
}

val apiSpec: Configuration = configurations.create("apiSpec")
val apiSpecFile = layout.buildDirectory.file("api-spec/openapi.yaml")

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webmvc")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("tools.jackson.module:jackson-module-kotlin")

  // OpenAPI Generator
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")
  implementation("io.swagger.core.v3:swagger-annotations:2.2.52")
  implementation("io.swagger.core.v3:swagger-models:2.2.52")
  implementation("jakarta.validation:jakarta.validation-api")
  apiSpec("de.stammtischhub:terminpilot-api-spec:0.0.0@yaml")

  // Provider
  implementation("com.github.lookfirst:sardine:5.13")
  implementation("org.mnode.ical4j:ical4j:4.3.0")

  // Datenbank
  runtimeOnly("com.mysql:mysql-connector-j")

  developmentOnly("org.springframework.boot:spring-boot-devtools")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

sourceSets {
  main {
    kotlin {
      srcDir(layout.buildDirectory.dir("generated/src/main/kotlin"))
    }
  }
}

dependencyLocking {
  lockAllConfigurations()
}

tasks.register("extractApiSpec") {
  description = "Extracts the API spec from the GitHub repository into the build directory for the OpenAPI Generator."
  dependsOn(apiSpec)
  outputs.file(apiSpecFile)
  doLast {
    apiSpecFile
      .get()
      .asFile.parentFile
      .mkdirs()
    apiSpec.singleFile.copyTo(apiSpecFile.get().asFile, overwrite = true)
  }
}

openApiGenerate {
  generatorName.set("kotlin-spring")
  inputSpec.set(apiSpecFile.get().asFile.absolutePath)
  outputDir.set(
    layout.buildDirectory
      .dir("generated")
      .get()
      .asFile.absolutePath,
  )
  apiPackage.set("de.stammtischHub.terminPilot.api.generated")
  modelPackage.set("de.stammtischHub.terminPilot.model.generated")
  configOptions.set(
    mapOf(
      "interfaceOnly" to "true",
      "useSpringBoot3" to "true",
      "useTags" to "true",
      "kotlinSupportNullable" to "true",
    ),
  )
}

tasks.named("openApiGenerate") {
  dependsOn("extractApiSpec")
}

tasks.named("compileKotlin") {
  dependsOn("openApiGenerate")
}

tasks.named("runKtlintCheckOverMainSourceSet") {
  dependsOn("openApiGenerate")
}

tasks.named("runKtlintFormatOverMainSourceSet") {
  dependsOn("openApiGenerate")
}

tasks.withType<Test> {
  useJUnitPlatform()
}

tasks.test {
  finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
  reports {
    xml.required = true
    html.required = false
  }
}

tasks.jar {
  enabled = false
}

tasks.bootJar {
  archiveBaseName.set("terminpilot-backend")
  archiveVersion.set("")
}
