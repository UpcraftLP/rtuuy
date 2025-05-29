package dev.upcraft.rtuuy.model.dto

import dev.kord.common.entity.Snowflake
import dev.upcraft.rtuuy.model.DiscordUser

data class AntiReplyPingGlobalExclusions(
	val guildId: Snowflake,
	val excludedUsers: List<DiscordUser>,
	val excludedRoleIds: List<Snowflake>
)
