package dev.upcraft.rtuuy.extensions.ban_sync

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.ban
import dev.kord.core.event.Event
import dev.kord.core.event.guild.BanAddEvent
import dev.kord.gateway.Intent
import dev.kordex.core.annotations.NotTranslated
import dev.kordex.core.checks.failed
import dev.kordex.core.checks.guildFor
import dev.kordex.core.checks.passed
import dev.kordex.core.checks.types.CheckContext
import dev.kordex.core.checks.userFor
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ExtensionState
import dev.kordex.core.extensions.event
import dev.kordex.core.healthcheck.utils.addHealthCheck
import dev.kordex.core.storage.StorageType
import dev.kordex.core.storage.StorageUnit
import dev.kordex.core.utils.env
import dev.kordex.core.utils.envOf
import dev.kordex.core.utils.scheduling.Scheduler
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.filter
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class BanSyncExtension : Extension() {
	override val name = "ban_sync"
	override val intents: MutableSet<Intent> = mutableSetOf(Intent.GuildModeration)

	private val syncedServerIds = env("BAN_SYNC_SERVER_IDS")
	private val isDryRun = envOf<Boolean>("BAN_SYNC_DRY_RUN")
	private var syncedServers = mutableListOf<Snowflake>()
	private var syncingBans = mutableMapOf<Snowflake, BanEntry>()
	private var hasStartedInitialSync = false
	private var isSyncing = false
	private val scheduler = Scheduler()

	private val storageUnit = StorageUnit<BanSyncConfig>(
		StorageType.Config,
		"rtuuy",
		"ban_sync"
	)

	@OptIn(NotTranslated::class)
	private suspend fun <T : Event> CheckContext<T>.isSyncedGuild() {
		if (!passed) {
			return
		}

		val logger = KotlinLogging.logger("dev.upcraft.rtuuy.BanSyncExtension.isSyncedGuild")

		if (guildFor(event)?.id in syncedServers) {
			logger.passed()
			pass()
			return
		}

		logger.failed("Guild is not in the list of guilds with synced bans!")
		fail("Guild is not in the list of guilds with synced bans!")
	}

	@OptIn(NotTranslated::class)
	private suspend fun <T : Event> CheckContext<T>.isNotSyncingUser() {
		if (!passed) {
			return
		}

		val logger = KotlinLogging.logger("dev.upcraft.rtuuy.BanSyncExtension.isNotSyncing")

		if (userFor(event)?.id in syncingBans) {
			logger.failed("This user's ban is currently being synced! Avoiding redundancy.")
			fail("This user's ban is currently being synced! Avoiding redundancy.")
			return
		}

		logger.passed()
		pass()
	}

	override suspend fun setup() {
		addHealthCheck("ban_sync") {
			healthyIf {
				this@BanSyncExtension.state == ExtensionState.LOADED
			}

			unhealthyIf("missed last sync interval") {
				val config = getConfig()

				syncedServers.isNotEmpty() && hasStartedInitialSync && !isSyncing
					&& config.lastSynced + config.syncInterval < Clock.System.now() - 2.seconds
			}
		}

		if (syncedServerIds.isNotEmpty()) {
			val logger = KotlinLogging.logger("dev.upcraft.rtuuy.BanSyncExtension.initialSync")
			syncedServers = syncedServerIds.split(',').map { Snowflake(it) }.toMutableList()
			logger.passed("Successfully loaded servers from SYNCED_BAN_SERVERS!")
		}

		if (isDryRun) {
			val logger = KotlinLogging.logger("dev.upcraft.rtuuy.BanSyncExtension.dryRunEnabled")
			logger.passed("Dry run has been enabled. All bans to be synced will not be applied!")
		}

		val syncInterval = getConfig().syncInterval

		scheduler.schedule(syncInterval, true, "Ban Sync", 10, true) {
			syncBans()
		}
		scheduler.schedule(5.seconds, true, "Initial Ban Sync", 1, false) {
			syncBans()
		}

		event<BanAddEvent> {
			check {
				isSyncedGuild()
				isNotSyncingUser()
			}

			action {
				val ban = BanEntry(event.user.id, event.guildId, event.getBan().reason ?: "unknown", Clock.System.now())
				val originalGuild = event.guild.asGuild()

				val logger = KotlinLogging.logger("dev.upcraft.rtuuy.BanSyncExtension.banSyncOnBanAdd")
				logger.info { "User ${event.user.username} (${event.user.id}) has been banned on ${originalGuild.name} (${originalGuild.id})! Syncing that ban between synced servers." }

				syncingBans.put(event.user.id, ban)
				val syncedServers = syncedServers
				syncedServers.filter { it != event.guildId }.forEach { guildId ->
					kord.getGuildOrNull(guildId)?.also { guild ->
						if (guild.getBanOrNull(ban.userId) == null) {
							guild.ban(ban.userId) {
								reason = ban.toReasonString()
							}
							logger.info { "Successfully synced the ban to ${guild.name} (${guild.id})" }
						} else {
							logger.debug { "User has already been banned on ${guild.name} (${guild.id})" }
						}
					} ?: logger.failed("Unable to sync ban to $guildId")
				}
				syncingBans.remove(event.user.id)
			}
		}
	}

	private suspend fun syncBans() {
		if (isSyncing) {
			return
		}
		hasStartedInitialSync = true
		isSyncing = true

		val config = getConfig()
		val now = Clock.System.now()
		val logger = KotlinLogging.logger("dev.upcraft.rtuuy.BanSyncExtension.syncBans")

		// Just in case
		if (config.lastSynced < now) {
			val toBan = mutableMapOf<Snowflake, BanEntry>()
			syncedServers.forEach { guildId ->
				kord.getGuildOrNull(guildId)?.also { guild ->
					guild.bans
						.filter {
							// ignore bans that were created due to sync
							// TODO track this in a DB maybe?
							it.reason?.startsWith(BanEntry.SYNCED_PREFIX) != true
						}
						.collect { ban ->
							val banEntry = BanEntry.from(ban, now)
							syncingBans.put(ban.userId, banEntry)
							toBan.putIfAbsent(ban.userId, banEntry)
						}
				} ?: {
					// TODO track error
					logger.error { "Ban sync: unable to find server with ID $guildId" }
				}
			}

			syncedServers.forEach { guildId ->
				kord.getGuildOrNull(guildId)?.also { guild ->
					toBan.values.filter { it.originGuildId != guildId }.forEach { ban ->
						val existingBan = guild.getBanOrNull(ban.userId)
						if(existingBan == null) {
							logger.info { "Syncing ban of ${ban.userId} to ${guild.name} (${guild.id})" }
							if(!isDryRun) {
								guild.ban(ban.userId) {
									reason = ban.toReasonString()
								}
							}
						}
					}
				} ?: {
					// TODO track error
					logger.error { "Ban sync: unable to find server with ID $guildId" }
				}
			}
			toBan.keys.forEach { syncingBans.remove(it) }

			config.lastSynced = now
			saveConfig()
		}
		isSyncing = false
	}

	private suspend fun getConfig(): BanSyncConfig {
		var config = storageUnit.get()

		if (config == null) {
			config = BanSyncConfig(2L.hours, Instant.DISTANT_PAST)

			storageUnit.save(config)
		}

		return config
	}

	private suspend fun saveConfig() {
		storageUnit.save()
	}
}
