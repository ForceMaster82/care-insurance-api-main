project(":domain:settlement") {
    dependencies {
        api(project(":domain:settlement:settlementproperty"))
        api(project(":domain:settlement:settlementevent"))
    }
}
