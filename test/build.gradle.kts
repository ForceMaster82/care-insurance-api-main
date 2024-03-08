project(":test") {
    dependencies {
        implementation(project(":domain"))
        implementation(project(":domain:base"))
        implementation(project(":domain:message"))
        implementation(project(":domain:security"))
        implementation(project(":domain:user"))
        implementation(project(":ext"))
        implementation("io.mockk:mockk:1.13.2")
        implementation("org.springframework.boot:spring-boot-starter-test")
    }
}
