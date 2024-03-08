project(":domain:patient") {
    dependencies {
        implementation(project(":ext"))
        implementation(project(":domain:security"))
        implementation(project(":domain:phonenumber"))
        testImplementation(project(":test"))
    }
}
