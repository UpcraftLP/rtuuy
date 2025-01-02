package dev.upcraft.rtuuy.extensions.ban_sync

import dev.kord.common.serialization.DurationInMilliseconds
import dev.kord.common.serialization.InstantInEpochMilliseconds
import dev.kordex.core.storage.Data
import kotlinx.serialization.Serializable

@Serializable
data class BanSyncConfig(
	var syncInterval: DurationInMilliseconds,
	var lastSynced: InstantInEpochMilliseconds,
) : Data
