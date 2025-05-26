package dev.upcraft.rtuuy.model.dto

import dev.upcraft.rtuuy.model.DiscordGuild
import kotlinx.datetime.Instant
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
data class BanSyncGroupDto(
	val guilds: List<DiscordGuild>,
	val createdAt: Instant,
	val updatedAt: Instant
)
