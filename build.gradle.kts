//import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.0"
    java
    jacoco
    //id("org.flywaydb.flyway") version "9.22.0"    // <-- add the Gradle Flyway plugin
}



group = "com.sudheer"
version = "0.1.0"
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.1.0")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // JWT Token Support
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // Flyway for database migrations (with MySQL 8.0 support)
    implementation("org.flywaydb:flyway-core:9.22.3")
    implementation("org.flywaydb:flyway-mysql:9.22.3")
    implementation("com.mysql:mysql-connector-j:8.3.0")

    // MapStruct for DTO <-> Entity mapping
    implementation("org.mapstruct:mapstruct:1.6.3")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")


    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

// Force resolution so no older transitive flyway-core sneaks in
/*configurations.all {
    resolutionStrategy {
        force("org.flywaydb:flyway-core:9.22.0")
    }
}*/
/*
flyway {
    url = "jdbc:mysql://localhost:3306/portfolio_tracker?useSSL=false&serverTimezone=UTC"
    user = "devuser"
    password = "devpass"
    locations = arrayOf("classpath:db/migration")
    driver = "com.mysql.cj.jdbc.Driver"
}
*/
tasks.withType<JavaCompile> {
    options.annotationProcessorPath = configurations["annotationProcessor"]
}
tasks.withType<Test> {
    useJUnitPlatform()
}

// JaCoCo Code Coverage Configuration
jacoco {
    toolVersion = "0.8.10"
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
}



