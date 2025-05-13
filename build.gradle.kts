plugins {
    kotlin("jvm") version "2.1.21"
    id("maven-publish")
}

group = "ruckustboom"
version = "0.1.0"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("ruckustboom:serial:0.1.0")
    testImplementation(kotlin("test"))
}

kotlin {
    explicitApi()
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
