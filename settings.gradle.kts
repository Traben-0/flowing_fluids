import org.gradle.internal.impldep.com.google.common.collect.ImmutableList

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.essential.gg/repository/maven-public")
        maven("https://maven.architectury.dev")
        maven("https://maven.fabricmc.net")
        maven("https://maven.minecraftforge.net")
        maven("https://maven.neoforged.net/releases/")
    }
    // We also recommend specifying your desired version here if you're using more than one of the plugins,
    // so you do not have to change the version in multilpe places when updating.
    plugins {
        val egtVersion = "0.7.0-alpha.2"
        id("gg.essential.multi-version.root") version egtVersion
        id("gg.essential.multi-version.api-validation") version egtVersion
    }
}

fun Int.formatVersionNumber(): String {
    val str = this.toString()
    val l = str.length
    val major = str.substring((l - 6).coerceAtLeast(0), l - 4)
    val minor = str.substring(l - 4, l - 2).trimStart('0')
    val patch = str.substring(l - 2, l).trimStart('0')
    return "$major.$minor${if (patch.isNotEmpty()) ".$patch" else ""}"
}

fun MutableList<String>.version(mcVersion: Int, forge: Boolean = true, neoforge: Boolean = true): MutableList<String> {
    val verString = mcVersion.formatVersionNumber()

    this.add("$verString-fabric")
    if (forge) this.add("$verString-forge")
    if (neoforge) this.add("$verString-neoforge")

    return this
}

mutableListOf<String>()
    .version(26_01_00, forge = false, neoforge = false)
    .version(1_21_11, forge = false)
    .version(1_21_09, forge = false)
    .version(1_21_06)
    .version(1_21_05)
    .version(1_21_04)
    .version(1_21_03)
    .version(1_21_00)
    .version(1_20_01, neoforge = false)
.forEach { version ->
    include(":$version")
    project(":$version").apply {
        // This is where the `build` folder and per-version overwrites will reside
        projectDir = file("versions/$version")
        // All sub-projects get configured by the same `build.gradle.kts` file, the string is relative to projectDir
        // You could use separate build files for each project, but usually that would just be duplicating lots of code
        buildFileName = "../../build.gradle.kts"
    }
}

// We use the `build.gradle.kts` file for all the sub-projects (cause that's where most the interesting stuff lives),
// so we need to use a different build file for the original root project.
rootProject.buildFileName = "root.gradle.kts"