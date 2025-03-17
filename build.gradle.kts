import dev.kordex.gradle.plugins.kordex.DataCollection

plugins {
	idea
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.serialization)

	//alias(libs.plugins.detekt)

	alias(libs.plugins.kordex.plugin)
}

val javaVersion = 21

group = "dev.upcraft"
version = System.getenv("VERSION") ?: "1.0.0-SNAPSHOT"

println("Building ${project.name} version ${project.version}")

dependencies {
	//detektPlugins(libs.detekt)

	implementation(libs.kotlin.stdlib)
	implementation(libs.kx.ser)
	implementation(libs.kx.ser.json)

	implementation(libs.bundles.ktor.client)

	// Logging dependencies
	implementation(libs.groovy)
	implementation(libs.jansi)
	implementation(libs.logback)
	implementation(libs.logback.groovy)
	implementation(libs.logging)
}

kordEx {
	kordExVersion = libs.versions.kordex.asProvider()
	jvmTarget = javaVersion

	bot {
		// See https://docs.kordex.dev/data-collection.html
		dataCollection(DataCollection.Minimal)
		//dataCollection(DataCollection.Standard)

		mainClass = "dev.upcraft.rtuuy.AppKt"
	}

	i18n {
		classPackage = "dev.upcraft.rtuuy.i18n"
		translationBundle = "rtuuy.strings"
	}

	module("data-mongodb")
	module("web-backend")
}

// The linter is currently disabled in order to allow building, it may be re-enabled later
/*
detekt {
	buildUponDefaultConfig = true

	config.from(rootProject.files("detekt.yml"))
}
*/

kotlin {
	jvmToolchain {
		languageVersion.set(JavaLanguageVersion.of(javaVersion))
	}
}

tasks.withType<Jar> {
	manifest {
		attributes["Implementation-Title"] = project.name
		attributes["Specification-Version"] = project.version

		System.getenv("COMMIT_SHA_SHORT")?.let { sha ->
			attributes["Implementation-Version"] = sha
		}
	}
}
tasks.assemble {
	dependsOn(tasks["installDist"])
}

// IDEA no longer automatically downloads sources/javadoc jars for dependencies, so we need to explicitly enable the behavior.
idea {
	module {
		isDownloadSources = true
		isDownloadJavadoc = true
	}
}
