plugins {
    kotlin("jvm") version "1.9.22"
}

group = "com.bimurto"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.springframework.boot:spring-boot-starter-web:3.2.2")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.codehaus.groovy:groovy-all:3.0.19")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
