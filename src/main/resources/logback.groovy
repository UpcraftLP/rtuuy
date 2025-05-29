import ch.qos.logback.core.joran.spi.ConsoleTarget

def environment = System.getenv("ENVIRONMENT") ?: "production"

def defaultLevel = INFO
def defaultTarget = ConsoleTarget.SystemErr

if (environment == "dev") {
	defaultLevel = DEBUG
	defaultTarget = ConsoleTarget.SystemOut

	// log DB queries
	logger("Exposed", DEBUG)

	// Silence warning about missing native PRNG
	logger("io.ktor.util.random", ERROR)
}

// using FQCN here because IDEA is stupid and keeps removing the import
//noinspection UnnecessaryQualifiedReference
appender("CONSOLE", ch.qos.logback.core.ConsoleAppender) {
	encoder(PatternLayoutEncoder) {
		pattern = "%boldGreen(%d{yyyy-MM-dd}) %boldYellow(%d{HH:mm:ss}) %gray(|) %highlight(%5level) %gray(|) %boldMagenta(%40.40logger{40}) %gray(|) %msg%n"

		withJansi = true
	}

	target = defaultTarget
}

root(defaultLevel, ["CONSOLE"])
