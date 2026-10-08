plugins {
    java
    id("com.gradleup.shadow") version "9.6.1"
}

// O jar continua se chamando greensky-<versão>.jar (e não greensky-core-...).
base.archivesName.set("greensky")

dependencies {
    implementation(project(":greensky-api"))

    // Plugins externos: só compilar contra o jar exato em libs/ (regra do CLAUDE.md). Nunca embutir.
    compileOnly(files(rootDir.resolve("../libs/nLogin-2.0.24.jar")))

    // Paper 26.1.2 build 74 (STABLE). Build exato; nunca usar build.+
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.74-stable")

    // Embutidas no jar (shade) para o boot nao depender de internet.
    implementation("com.zaxxer:HikariCP:7.1.0")
    implementation("org.postgresql:postgresql:42.7.13")
    implementation("org.flywaydb:flyway-core:13.9.0")
    implementation("org.flywaydb:flyway-database-postgresql:13.9.0")

    testImplementation("io.papermc.paper:paper-api:26.1.2.build.74-stable")
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
        options.release.set(25)  // Paper 26.1+ exige Java 25
        options.compilerArgs.add("-Xlint:deprecation")
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

    /** Copia o jar do plugin para server/plugins/ (servidor de teste), trocando a versão anterior. */
    register<Copy>("deploy") {
        group = "greensky"
        description = "Copia o jar do plugin para ../server/plugins"
        val pluginsDir = rootDir.resolve("../server/plugins")
        dependsOn(shadowJar)
        from(shadowJar.flatMap { it.archiveFile })
        into(pluginsDir)
        doFirst {
            pluginsDir.listFiles { f -> f.name.startsWith("greensky") && f.name.endsWith(".jar") }
                ?.forEach { it.delete() }
        }
    }
}
