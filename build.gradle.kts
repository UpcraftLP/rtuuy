import dev.kordex.gradle.plugins.docker.file.*
import dev.kordex.gradle.plugins.kordex.DataCollection

plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.serialization)

	alias(libs.plugins.shadow)
	//alias(libs.plugins.detekt)

	alias(libs.plugins.kordex.docker)
	alias(libs.plugins.kordex.plugin)
}

group = "dev.upcraft"
version = "1.0.0-SNAPSHOT"

dependencies {
	detektPlugins(libs.detekt)

	implementation(libs.kotlin.stdlib)
	implementation(libs.kx.ser)

	// Logging dependencies
	implementation(libs.groovy)
	implementation(libs.jansi)
	implementation(libs.logback)
	implementation(libs.logback.groovy)
	implementation(libs.logging)
}

kordEx {
	kordExVersion = libs.versions.kordex.asProvider()

	bot {
		// See https://docs.kordex.dev/data-collection.html
		dataCollection(DataCollection.None)
		//dataCollection(DataCollection.Standard)

		mainClass = "dev.upcraft.rtuuy.AppKt"
	}

	i18n {
		classPackage = "dev.upcraft.rtuuy.i18n"
		translationBundle = "rtuuy.strings"
	}
}

// The linter is currently disabled in order to allow building, it may be re-enabled later
/*
detekt {
	buildUponDefaultConfig = true

	config.from(rootProject.files("detekt.yml"))
}
*/

// Automatically generate a Dockerfile. Set `generateOnBuild` to `false` if you'd prefer to manually run the
// `createDockerfile` task instead of having it run whenever you build.
docker {
	// Create the Dockerfile in the root folder.
	file(rootProject.file("Dockerfile"))

	commands {
		// Each function (aside from comment/emptyLine) corresponds to a Dockerfile instruction.
		// See: https://docs.docker.com/reference/dockerfile/

		from("eclipse-temurin:21-jdk-alpine")

		emptyLine()

		runShell("mkdir -p /bot/plugins")
		runShell("mkdir -p /bot/data")

		emptyLine()

		copy("build/libs/$name-*-all.jar", "/bot/bot.jar")

		emptyLine()

		// Add volumes for locations that you need to persist. This is important!
		volume("/bot/data")  // Storage for data files
		volume("/bot/plugins")  // Plugin ZIP/JAR location

		emptyLine()

		workdir("/bot")

		emptyLine()

		entryPointExec(
			"java", "-Xms2G", "-Xmx2G",
			"-jar", "/bot/bot.jar"
		)
	}
}
