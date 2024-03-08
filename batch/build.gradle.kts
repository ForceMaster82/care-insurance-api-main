import org.springframework.boot.gradle.tasks.bundling.BootJar

project(":batch") {
    tasks.getByName<BootJar>("bootJar") {
        archiveFileName.set("care-insurance-batch.jar")
    }

    dependencies {
        implementation(project(":domain"))
        implementation(project(":sql"))
        testImplementation(project(":test"))

        implementation("org.springframework.boot:spring-boot-starter-batch")
        testImplementation("org.springframework.batch:spring-batch-test")
    }
}
