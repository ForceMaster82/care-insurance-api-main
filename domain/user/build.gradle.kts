project(":domain:user") {
    allOpen {
        annotation("jakarta.persistence.Entity")
        annotation("jakarta.persistence.Embeddable")
    }

    dependencies {
        implementation(project(":domain:base"))
        implementation(project(":domain:message"))
        implementation(project(":domain:patch"))
        implementation(project(":domain:security"))
        implementation(project(":domain:modification"))
        implementation(project(":ext"))
        testImplementation(project(":test"))
    }
}
