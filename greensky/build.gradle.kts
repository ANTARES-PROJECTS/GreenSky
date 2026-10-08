// Configuração comum aos módulos. Cada módulo tem o próprio build.gradle.kts.
subprojects {
    group = "com.greencodes"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}
