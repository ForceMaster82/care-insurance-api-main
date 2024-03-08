project(":domain:agency") {
    dependencies {
        implementation(project(":domain:security"))
        implementation(project(":domain:financial"))
        implementation(project(":domain:base"))
        implementation(project(":domain:user"))
    }
}
