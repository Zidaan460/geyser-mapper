plugins {
    id("java")
    id("net.neoforged.moddev") version "2.0.+"
}

group = "com.geysermap"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

neoForge {
    version = "26.1.2.76"

    runs {
        create("server") {
            server()
            systemProperty("forge.logging.console.level", "debug")
        }
    }

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
    // Geyser API only (no full jar needed)
    compileOnly("org.geysermc.geyser:api:2.10.1-SNAPSHOT")
}
