project(":domain:reconciliation:reconciliationevent") {
    dependencies {
        implementation(project(":domain:reconciliation:reconciliationproperty"))
        implementation(project(":domain:security"))
    }
}
