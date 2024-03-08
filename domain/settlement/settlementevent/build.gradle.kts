project(":domain:settlement:settlementevent") {
    dependencies {
        implementation(project(":domain:settlement:settlementproperty"))
        implementation(project(":domain:modification"))
        implementation(project(":domain:user"))
    }
}
