import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.0.1"
    id("io.spring.dependency-management") version "1.1.0"
    kotlin("jvm") version "1.7.22"
    kotlin("plugin.spring") version "1.7.22"
    kotlin("plugin.jpa") version "1.7.22"
    kotlin("plugin.allopen") version "1.7.22"
    id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
    id("org.jlleitschuh.gradle.ktlint-idea") version "10.3.0"
}

java.sourceCompatibility = JavaVersion.VERSION_17

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

allprojects {
    group = "kr.caredoc"
    version = "1.0.14"

    apply(plugin = "java")
    apply(plugin = "kotlin")
    apply(plugin = "kotlin-spring")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
    apply(plugin = "org.jetbrains.kotlin.plugin.allopen")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "org.jlleitschuh.gradle.ktlint-idea")

    repositories {
        mavenCentral()
        maven(url = "https://jitpack.io")
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "17"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    dependencies {
        val kotestVersion = "5.5.4"

        implementation("org.springframework.boot:spring-boot-starter-data-jpa")
        implementation("org.springframework.boot:spring-boot-starter-jdbc")
        implementation("org.springframework.boot:spring-boot-starter-security")
        implementation("org.springframework.boot:spring-boot-starter-web")
        implementation("org.springframework.boot:spring-boot-starter-webflux")
        implementation("org.springframework.boot:spring-boot-starter-validation")
        implementation("org.springframework.boot:spring-boot-starter-actuator")
        implementation("org.springframework.boot:spring-boot-starter-logging")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.6.4")

        implementation("jakarta.validation:jakarta.validation-api:3.0.2")

        implementation("com.github.guepardoapps:kulid:2.0.0.0")

        implementation("io.jsonwebtoken:jjwt-api:0.11.5")
        implementation("io.jsonwebtoken:jjwt-impl:0.11.5")
        implementation("io.jsonwebtoken:jjwt-jackson:0.11.5")

        implementation("org.liquibase:liquibase-core:4.17.2")

        implementation("software.amazon.awssdk:s3:2.19.19")
        implementation("software.amazon.awssdk:sts:2.19.19")
        implementation("com.amazonaws:aws-encryption-sdk-java:2.4.0")
        implementation("com.amazonaws:aws-java-sdk-core:1.12.408")
        implementation("com.amazonaws:aws-java-sdk-kms:1.12.408")
        implementation("com.amazonaws:aws-java-sdk-ses:1.12.420")
        implementation("com.amazonaws:aws-java-sdk-sts:1.12.420")
        implementation("com.amazonaws:aws-java-sdk-secretsmanager:1.11.355")
        implementation("com.amazonaws.secretsmanager:aws-secretsmanager-jdbc:2.0.2")

        implementation("io.sentry:sentry-spring-boot-starter-jakarta:6.21.0")
        implementation("io.sentry:sentry-jdbc:6.21.0")

        implementation("com.github.librepdf:openpdf:1.3.30")
        implementation("org.apache.pdfbox:pdfbox:2.0.29")

        implementation("ch.qos.logback.contrib:logback-jackson:0.1.5")
        implementation("ch.qos.logback.contrib:logback-json-classic:0.1.5")

        implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:2.3.1")
        implementation("org.apache.poi:poi:4.1.2")
        implementation("org.apache.poi:poi-ooxml:4.1.2")

        runtimeOnly("com.h2database:h2")
        runtimeOnly("com.mysql:mysql-connector-j")
        annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("org.springframework.security:spring-security-test")

        testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
        testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
        testImplementation("io.kotest:kotest-property:$kotestVersion")
        testImplementation("io.kotest.extensions:kotest-extensions-spring:1.1.2")

        testImplementation("io.mockk:mockk:1.13.2")
        testImplementation("com.ninja-squad:springmockk:4.0.0")
    }
}
