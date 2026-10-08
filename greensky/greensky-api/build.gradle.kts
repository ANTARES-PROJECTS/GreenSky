plugins {
    `java-library`
}

dependencies {
    // Paper 26.1.2 build 74 (STABLE). Build exato; nunca usar build.+
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.74-stable")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.release.set(25)
}
