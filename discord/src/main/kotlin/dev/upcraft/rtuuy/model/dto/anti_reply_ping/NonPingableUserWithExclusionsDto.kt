package dev.upcraft.rtuuy.model.dto.anti_reply_ping

import dev.kord.common.entity.Snowflake
import dev.upcraft.rtuuy.model.DiscordGuild
import dev.upcraft.rtuuy.model.DiscordUser
import kotlin.time.Duration

data class NonPingableUserWithExclusionsDto(
    val user: DiscordUser,
    val guild: DiscordGuild,
    val mutePeriod: Duration?,
    val deleteOriginalMessage: Boolean,
    val userExceptions: Set<Snowflake>,
    val roleExceptions: Set<Snowflake>
)
