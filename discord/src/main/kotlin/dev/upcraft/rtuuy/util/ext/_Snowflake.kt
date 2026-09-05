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

enum class MentionType {
	CHANNEL,
	COMMAND,
	ROLE,
	USER;

	fun toMention(id: Snowflake, vararg args: String): String {
		return when (this) {
			USER -> "<@${id}>"
			COMMAND -> "</${args.first()}:${id}>"
			ROLE -> "<@&${id}>"
			CHANNEL -> "<#${id}>"
		}
	}
}

fun Snowflake.asUserMention(): String {
	return MentionType.USER.toMention(this)
}

fun Snowflake.asRoleMention(): String {
	return MentionType.ROLE.toMention(this)
}

fun Snowflake.asChannelMention(): String {
	return MentionType.CHANNEL.toMention(this)
}

fun Snowflake.asCommandMention(command: String): String {
	return MentionType.COMMAND.toMention(this, command)
}
