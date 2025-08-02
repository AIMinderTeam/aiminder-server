plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.google.osdetector") version "1.7.3"
    id("org.springdoc.openapi-gradle-plugin") version "1.9.0"
    id("pl.allegro.tech.build.axion-release") version "1.18.17"
}

scmVersion {
    tag { prefix.set("") }
    versionCreator { tag, _ -> tag }
    snapshotCreator { _, _ -> "" }
}

group = "ai.aiminder"
version = scmVersion.version

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

extra["springAiVersion"] = "1.0.0"

dependencies {
    val jwtVersion = "0.12.3"
    val flywayVersion = "11.10.4"

    // env
    implementation("io.github.cdimascio:dotenv-java:3.0.0")

    // spring
    implementation("org.springframework.ai:spring-ai-starter-model-openai")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    if (osdetector.arch.equals("aarch_64")) {
        implementation("io.netty:netty-resolver-dns-native-macos:4.2.2.Final:osx-aarch_64")
    }
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // postgres
    implementation("org.postgresql:r2dbc-postgresql:1.0.7.RELEASE")
    implementation("org.postgresql:postgresql:42.7.7")

    // kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // jwt
    implementation("io.jsonwebtoken:jjwt-api:$jwtVersion")
    implementation("io.jsonwebtoken:jjwt-impl:$jwtVersion")
    implementation("io.jsonwebtoken:jjwt-jackson:$jwtVersion")

    // flyway
    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")

    // swagger
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.8.9")

    // test
    run {
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("io.projectreactor:reactor-test")
        testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
        testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register("copyJar", Copy::class) {
    dependsOn("bootJar")
    val jarFile = "aiminder-server-$version.jar"
    from("build/libs")
    into(file("docker"))
    include(jarFile)
}

tasks.named("build") {
    dependsOn("copyJar")
}

openApi {
    apiDocsUrl.set("http://localhost/v3/api-docs")
}

tasks.register<Exec>("publishTypeNpm") {
    dependsOn("test")
    val npmrcPassword =
        project.findProperty("password")?.toString()
            ?: throw GradleException("Please provide a password using -Ppassword=<value>")
    commandLine("./openapi-generate.sh", "-version", scmVersion.version, "-password", npmrcPassword)
}
