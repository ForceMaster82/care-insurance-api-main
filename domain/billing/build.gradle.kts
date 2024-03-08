project(":domain:billing") {
    dependencies {
        api(project(":domain:billing:billingproperty"))
        api(project(":domain:billing:billingevent"))
    }
}
