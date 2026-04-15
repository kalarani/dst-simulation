plugins {
	java
	id("org.springframework.boot") version "4.0.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation("com.squareup.okhttp3:okhttp:5.1.0")
    testImplementation("com.squareup.okhttp3:okhttp-jvm:5.1.0")
    testImplementation("org.testcontainers:toxiproxy:1.21.3")
    testImplementation("eu.rekawek.toxiproxy:toxiproxy-java:2.1.11")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
