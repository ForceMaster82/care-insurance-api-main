project(":domain:file") {
    allOpen {
        annotation("jakarta.persistence.Entity")
        annotation("jakarta.persistence.Embeddable")
    }

    dependencies {
        implementation(project(":domain:base"))
    }
}
