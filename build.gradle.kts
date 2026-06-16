plugins {
    id("java")
    id("net.neoforged.moddev") version "1.0.21"
}

group = "com.geysermap"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

neoForge {
    version = "26.1.2.76"

    mods {
        create("geysermap") {
            sourceSet(sourceSets.main.get())
        }
    }
}

repositories {
    maven("https://repo.opencollab.dev/main/")
}

dependencies {
    compileOnly("org.geysermc.geyser:api:2.10.1-SNAPSHOT")
}
