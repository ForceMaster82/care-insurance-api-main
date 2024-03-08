import org.springframework.boot.gradle.tasks.bundling.BootJar

project(":api") {
    tasks.getByName<BootJar>("bootJar") {
        archiveFileName.set("care-insurance-api.jar")
    }

    dependencies {
        implementation(project(":domain"))
        implementation(project(":sql"))
        implementation(project(":ext"))
        testImplementation(project(":test"))
    }
}
