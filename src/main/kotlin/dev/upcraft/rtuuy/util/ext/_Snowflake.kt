package dev.upcraft.rtuuy.util.ext

import dev.kord.common.entity.Snowflake
import java.security.MessageDigest

private const val SALT = "x3c6V3EQxPDHtNsaSnJ0WD1aKnxvuaDK"

fun Snowflake.forAnalytics(): String {
	val input = this.toString() + SALT
	val instance = MessageDigest.getInstance("SHA3-256")
	val digest = instance.digest(input.toByteArray(Charsets.UTF_8))
	return digest.toHexString()
}
