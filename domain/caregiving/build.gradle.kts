project(":domain:caregiving") {
    allOpen {
        annotation("jakarta.persistence.Entity")
        annotation("jakarta.persistence.Embeddable")
    }

    dependencies {
        implementation(project(":domain:base"))
        implementation(project(":domain:modification"))
        implementation(project(":domain:billing:billingevent"))
        implementation(project(":domain:billing:billingproperty"))
        implementation(project(":domain:settlement:settlementevent"))
        implementation(project(":domain:settlement:settlementproperty"))
        implementation(project(":domain:security"))
        implementation(project(":domain:patch"))
        implementation(project(":domain:patient"))
        implementation(project(":domain:phonenumber"))
        implementation(project(":domain:insurance"))
        implementation(project(":domain:user"))
        implementation(project(":domain:file"))
        implementation(project(":domain:financial"))
        implementation(project(":domain:message"))
        implementation(project(":domain:reconciliation:reconciliationproperty"))
        implementation(project(":domain:reconciliation:reconciliationevent"))
        implementation(project(":domain:agency"))
        implementation(project(":ext"))
        testImplementation(project(":test"))
        testImplementation(project(mapOf("path" to ":api")))
    }
}
