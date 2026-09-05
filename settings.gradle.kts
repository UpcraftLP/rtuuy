pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()

		maven("https://snapshots-repo.kordex.dev")
		maven("https://releases-repo.kordex.dev")
	}
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "rtuuy"

includeBuild("build-logic")

listOf("shared-model", "discord", "web").forEach {
	include(it)
	project(":$it").name = "${rootProject.name}-$it"
}
