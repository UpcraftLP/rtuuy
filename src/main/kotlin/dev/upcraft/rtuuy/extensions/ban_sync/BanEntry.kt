package dev.upcraft.rtuuy.extensions.ban_sync

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Ban
import kotlinx.datetime.Instant

data class BanEntry(
	val userId: Snowflake,
	val originGuildId: Snowflake,
	val reason: String,
	val timestamp: Instant,
) {
	fun toReasonString(): String? {
		return "${SYNCED_PREFIX}[${this.originGuildId} on ${this.timestamp}] ${this.reason}"
	}

	companion object {
		fun from(ban: Ban, timestamp: Instant): BanEntry {
			return BanEntry(ban.user.id, ban.data.guildId, ban.reason ?: "[unknown reason]", timestamp)
		}

		const val SYNCED_PREFIX = "[synced]"
	}
}
