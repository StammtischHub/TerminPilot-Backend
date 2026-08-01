import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
  kotlin("jvm") version "2.4.10"
  kotlin("plugin.spring") version "2.4.10"
  kotlin("plugin.jpa") version "2.4.10"
  id("org.springframework.boot") version "4.1.0"
  id("io.spring.dependency-management") version "1.1.7"
  id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
  id("org.openapi.generator") version "7.24.0"
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
      element.file.path.contains("generated")
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
val generatedApiDir = layout.buildDirectory.dir("generated")

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webmvc")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
  developmentOnly("org.springframework.boot:spring-boot-devtools")
  testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
  testImplementation("org.springframework.boot:spring-boot-starter-test") {
    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    exclude(group = "org.mockito", module = "mockito-core")
  }

  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("tools.jackson.module:jackson-module-kotlin")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testImplementation("com.ninja-squad:springmockk:5.0.1")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  runtimeOnly("com.h2database:h2")

  // OpenAPI Generator
  implementation("io.swagger.core.v3:swagger-annotations:2.2.52")
  implementation("io.swagger.core.v3:swagger-models:2.2.52")
  implementation("jakarta.validation:jakarta.validation-api")
  apiSpec("de.stammtischhub:terminpilot-api-spec:1.0.1@yaml")

  // Apple-Provider
  implementation("com.github.lookfirst:sardine:5.13")
  implementation("org.mnode.ical4j:ical4j:4.3.0")

  // Google-Provider
  implementation("com.google.api-client:google-api-client:2.7.0")
  implementation("com.google.oauth-client:google-oauth-client-jetty:1.36.0")
  implementation("com.google.apis:google-api-services-calendar:v3-rev20260225-2.0.0")

  // Datenbank
  runtimeOnly("com.mysql:mysql-connector-j")
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
  inputs.files(apiSpec)
  outputs.file(apiSpecFile)
  doLast {
    apiSpecFile
      .get()
      .asFile.parentFile
      .mkdirs()
    apiSpec.singleFile.copyTo(apiSpecFile.get().asFile, overwrite = true)
  }
}

tasks.register<Delete>("cleanGeneratedApi") {
  description = "Removes previously generated OpenAPI sources so stale/renamed files don't linger after a spec change."
  delete(generatedApiDir)
}

openApiGenerate {
  generatorName.set("kotlin-spring")
  inputSpec.set(apiSpecFile.get().asFile.absolutePath)
  outputDir.set(
    generatedApiDir
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

tasks.named("cleanGeneratedApi") {
  dependsOn("extractApiSpec")
}

tasks.named("openApiGenerate") {
  dependsOn("extractApiSpec", "cleanGeneratedApi")
  inputs.file(apiSpecFile)
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
