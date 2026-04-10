//import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.5.5"
    id("io.spring.dependency-management") version "1.1.7"
    java
    jacoco
    //id("org.flywaydb.flyway") version "9.22.0"    // <-- add the Gradle Flyway plugin
}



group = "com.sudheer"
version = "0.1.0"
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.16")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("net.lingala.zip4j:zip4j:2.11.5")
    implementation("org.apache.pdfbox:pdfbox:2.0.31")

    // JWT Token Support
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // Flyway for database migrations (with MySQL 8.0 support)
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")
    runtimeOnly("com.mysql:mysql-connector-j")

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

val jacocoExclusions = listOf(
    "**/PortfolioTrackerApplication.class",
    "**/api/controller/**",
    "**/api/dto/**",
    "**/application/mapper/**",
    "**/application/usecase/**",
    "**/application/usecase/impl/**",
    "**/config/**",
    "**/domain/service/**",
    "**/domain/service/impl/**",
    "**/infrastructure/amfi/client/**",
    "**/infrastructure/amfi/parser/AmfiNavAllTxtParser.class",
    "**/infrastructure/config/**",
    "**/infrastructure/persistence/entity/**",
    "**/infrastructure/persistence/audit/**",
    "**/infrastructure/amfi/parser/Parsed*.class",
    "**/jobs/**",
    "**/repository/**",
    "**/enums/GoalStatus.class",
    "**/enums/UploadStatus.class",
    "**/service/AmfiIngestService.class",
    "**/service/AmfiParser.class",
    "**/service/CamsPdfStatementParser*.class",
    "**/service/FundService.class",
    "**/service/GoalService.class",
    "**/service/PortfolioService*.class",
    "**/service/ResetService.class",
    "**/service/SchemeRegistry*.class",
    "**/service/TransactionIngestService.class",
    "**/service/UploadHistoryService.class",
    "**/service/impl/**",
    "**/infrastructure/scheduler/**",
    "**/util/PdfTextDebugger.class"
)

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(jacocoExclusions)
            }
        })
    )
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(jacocoExclusions)
            }
        })
    )
    violationRules {
        rule {
            limit {
                minimum = "0.75".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}



