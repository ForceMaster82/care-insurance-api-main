project(":domain") {
    allOpen {
        annotation("jakarta.persistence.Entity")
        annotation("jakarta.persistence.Embeddable")
    }

    dependencies {
        api(project(":domain:base"))
        api(project(":domain:message"))
        api(project(":domain:patch"))
        api(project(":domain:security"))
        api(project(":domain:user"))
        api(project(":domain:modification"))
        api(project(":domain:settlement"))
        api(project(":domain:billing"))
        api(project(":domain:caregiving"))
        api(project(":domain:patient"))
        api(project(":domain:phonenumber"))
        api(project(":domain:insurance"))
        api(project(":domain:file"))
        api(project(":domain:financial"))
        api(project(":domain:reconciliation"))
        api(project(":domain:agency"))
        implementation(project(":ext"))
        testImplementation(project(":test"))
        testImplementation(project(mapOf("path" to ":api")))
    }
}
