project(":domain:billing:billingevent") {
    dependencies {
        implementation(project(":domain:billing:billingproperty"))
        implementation(project(":domain:modification"))
        implementation(project(":domain:user"))
        implementation(project(":domain:financial"))
    }
}
