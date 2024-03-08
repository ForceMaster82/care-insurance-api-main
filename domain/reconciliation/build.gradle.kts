project(":domain:reconciliation") {
    dependencies {
        api(project(":domain:reconciliation:reconciliationevent"))
        api(project(":domain:reconciliation:reconciliationproperty"))
    }
}
