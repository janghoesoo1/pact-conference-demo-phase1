dependencies {
    implementation(project(":common"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    testImplementation("io.mockk:mockk-jvm:1.13.13")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("au.com.dius.pact.provider:junit5:4.6.14")
    testImplementation("au.com.dius.pact.provider:spring:4.6.14")
}

tasks.test {
    dependsOn(":attendee-service:test")
}
