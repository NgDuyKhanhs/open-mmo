plugins {
	kotlin("jvm") version "2.0.21"
	kotlin("plugin.spring") version "2.0.21"
	id("org.springframework.boot") version "3.3.4"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.openmmo"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb:3.3.4")
	implementation("org.springframework.boot:spring-boot-starter-web:3.3.4")
	implementation("org.springframework.boot:spring-boot-starter-webflux:3.3.4") // For WebClient
	implementation("org.springframework.boot:spring-boot-starter-security:3.3.4")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server:3.3.4")
	implementation("org.springframework.boot:spring-boot-starter-validation:3.3.4") // Jakarta validation
	implementation("org.springframework.boot:spring-boot-starter-mail:3.3.4") // For email/MIME support
	implementation("org.springframework.boot:spring-boot-starter-cache:3.3.4") // Spring Cache abstraction
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310") // For Java Time support
	implementation("org.springaicommunity:spring-ai-agent-utils:0.4.2")
	
	// JWT Token Support
	implementation("io.jsonwebtoken:jjwt-api:0.12.1")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.1")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.1")
	
	// Google OAuth2 Libraries
	implementation("com.google.api-client:google-api-client:2.0.0")
	implementation("com.google.auth:google-auth-library-oauth2-http:1.11.0")

	// Note: Gmail API accessed via RestTemplate + JSON mapping (not needed explicit dependency)

	// Ed25519 cryptography
	implementation("org.bouncycastle:bcprov-jdk18on:1.78")

	// Language Detection Library
	implementation("com.github.pemistahl:lingua:1.2.2")

	testImplementation("org.springframework.boot:spring-boot-starter-test:3.3.4")
	testImplementation("org.springframework.boot:spring-boot-starter-data-mongodb:3.3.4")
	testImplementation("org.springframework.security:spring-security-test:6.3.1")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// Load .env.local file for local development
tasks.named("bootRun") {
	dependsOn("loadEnvLocal")
}

tasks.register("loadEnvLocal") {
	doFirst {
		val envFile = file(".env.local")
		if (envFile.exists()) {
			println("📝 Loading environment variables from .env.local...")
			envFile.readLines().forEach { line ->
				val trimmed = line.trim()
				if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
					val (key, value) = trimmed.split("=", limit = 2).let {
						if (it.size == 2) it[0] to it[1] else return@forEach
					}
					System.setProperty(key, value)
					// Also set as environment variable
					@Suppress("DEPRECATION")
					ProcessBuilder().environment()[key] = value
				}
			}
			println("✅ Environment variables loaded!")
		}
	}
}

