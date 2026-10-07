plugins {
    java
    id("com.gradleup.shadow") version "9.6.1"
}

group = "com.greencodes"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Build exato e STABLE (Fill API: 26.2 build 132). Nunca usar build.+
    compileOnly("io.papermc.paper:paper-api:26.2.build.132-stable")

    // Embutidas no jar (shade) para o boot nao depender de internet.
    implementation("com.zaxxer:HikariCP:7.1.0")
    implementation("org.postgresql:postgresql:42.7.13")
    implementation("org.flywaydb:flyway-core:13.9.0")
    implementation("org.flywaydb:flyway-database-postgresql:13.9.0")

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(25)
    }
    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching("plugin.yml") { expand(props) }
    }
    test {
        useJUnitPlatform()
    }
    jar {
        archiveClassifier.set("plain")
    }
    shadowJar {
        archiveClassifier.set("")
        // Service files (ex.: plugins do Flyway) existem em varios jars e precisam ser mesclados, nao descartados.
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles()
        // slf4j ja vem com o Paper; nao embutir.
        dependencies { exclude(dependency("org.slf4j:.*")) }
        val libs = "com.greencodes.greensky.libs"
        relocate("com.zaxxer.hikari", "$libs.hikari")
        relocate("org.postgresql", "$libs.postgresql")
        relocate("org.flywaydb", "$libs.flyway")
    }
    build {
        dependsOn(shadowJar)
    }
}
