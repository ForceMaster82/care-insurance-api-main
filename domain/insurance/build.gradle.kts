project(":domain:insurance") {
    allOpen {
        annotation("jakarta.persistence.Entity")
        annotation("jakarta.persistence.Embeddable")
    }

    dependencies {
        implementation(project(":domain:base"))
        implementation(project(":domain:security"))
        implementation(project(":domain:modification"))
        implementation(project(":domain:user"))
        implementation(project(":ext"))
        testImplementation(project(":test"))
        testImplementation(project(mapOf("path" to ":api")))
    }
}
