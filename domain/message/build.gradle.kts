project(":domain:message") {
    allOpen {
        annotation("jakarta.persistence.Entity")
        annotation("jakarta.persistence.Embeddable")
    }

    dependencies {
        implementation(project(":domain:base"))
    }
}
