package dev.upcraft.rtuuy.util.ext

import kotlinx.datetime.Instant

inline fun <T : Any> T?.ifNull(block: () -> Unit): T? {
	if (this == null) {
		block()
	}
	return this
}

fun Instant.asSeconds(): Instant {
	return Instant.fromEpochSeconds(this.epochSeconds)
}
