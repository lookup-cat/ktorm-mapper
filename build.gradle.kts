plugins {
    kotlin("jvm") version "1.4.21"
    maven
}

group = "com.htt"
version = "1.2"

repositories {
    mavenCentral()
}


dependencies {
    val ktormVersion = "3.3.0"
    implementation(kotlin("stdlib"))
    implementation("org.ktorm:ktorm-core:${ktormVersion}")

    testImplementation("org.ktorm:ktorm-support-mysql:${ktormVersion}")
    testImplementation("org.mariadb.jdbc:mariadb-java-client:2.7.2")
    testImplementation("junit:junit:4.13.1")
    testImplementation("org.ktorm:ktorm-jackson:${ktormVersion}")
}
