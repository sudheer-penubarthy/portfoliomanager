//import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.0"
    java
    //id("org.flywaydb.flyway") version "9.22.0"    // <-- add the Gradle Flyway plugin
}



group = "com.example"
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

    implementation("org.flywaydb:flyway-core:9.22.0")
    runtimeOnly("com.mysql:mysql-connector-j:8.3.0")

    runtimeOnly("com.mysql:mysql-connector-j:8.3.0")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
/*
flyway {
    url: ${DB_URL:jdbc:mysql://localhost:3306/portfolio_tracker?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC}
    username: ${DB_USER:youruser}
    password: ${DB_PASS:yourpassword}
    locations = arrayOf("classpath:db/migration")
    driver = "com.mysql.cj.jdbc.Driver"
}
*/
tasks.withType<Test> {
    useJUnitPlatform()
}

