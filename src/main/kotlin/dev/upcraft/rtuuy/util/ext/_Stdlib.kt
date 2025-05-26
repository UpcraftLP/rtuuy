package dev.upcraft.rtuuy.util.ext

inline fun <T : Any> T?.ifNull(block: () -> Unit): T? {
	if(this == null) {
		block()
	}
	return this
}
