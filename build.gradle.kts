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

   // implementation("org.flywaydb:flyway-core:9.22.0")
    runtimeOnly("com.mysql:mysql-connector-j:8.3.0")

    // Ensure the Flyway Gradle plugin can see the JDBC driver for flywayMigrate
  //  add("flywayRuntime", "com.mysql:mysql-connector-j:8.3.0")

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
tasks.withType<Test> {
    useJUnitPlatform()
}


