dependencies {
    implementation(project(":common"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    // JPA support (optional, activated with 'jpa' profile)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")

    testImplementation("io.mockk:mockk-jvm:1.13.13")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("au.com.dius.pact.provider:junit5:4.6.14")
    testImplementation("au.com.dius.pact.provider:spring:4.6.14")
    testImplementation("io.rest-assured:rest-assured:5.4.0")
    testImplementation("io.rest-assured:kotlin-extensions:5.4.0")

    // Testcontainers
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testImplementation("org.testcontainers:postgresql:1.19.3")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
}

tasks.register<Copy>("collectPacts") {
    from("../attendee-service/build/pacts") {
        include("*-SessionService.json")
    }
    from("../cfp-service/build/pacts") {
        include("*-SessionService.json")
    }
    into("build/pacts")
}

tasks.test {
    dependsOn("collectPacts")
}
