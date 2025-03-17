package dev.upcraft.rtuuy.util.ext

import java.util.*

fun ByteArray.toHexString(): String {
	return HexFormat.of().formatHex(this)
}
