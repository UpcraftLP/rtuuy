package dev.upcraft.rtuuy.model.dto.anti_reply_ping

import dev.kord.common.entity.Snowflake
import dev.upcraft.rtuuy.model.DiscordUser

data class AntiReplyPingGlobalExclusionsDto(
    val guildId: Snowflake,
    val excludedUsers: List<DiscordUser>,
    val excludedRoleIds: List<Snowflake>
)
