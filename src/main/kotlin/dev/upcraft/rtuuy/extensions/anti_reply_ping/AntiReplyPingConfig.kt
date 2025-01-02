package dev.upcraft.rtuuy.extensions.anti_reply_ping

import dev.kord.common.entity.Snowflake
import dev.kord.common.serialization.DurationInMilliseconds
import dev.kordex.core.storage.Data
import kotlinx.serialization.Serializable

@Serializable
data class AntiReplyPingConfig(
    var forbiddenUsers: MutableSet<@Serializable Snowflake>,
    var allowedRoles: MutableSet<@Serializable Snowflake>,
	// This is stored in milliseconds
    var mutePeriod: DurationInMilliseconds
) : Data
