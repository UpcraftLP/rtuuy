package dev.upcraft.rtuuy.extensions.ban_sync

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.ban
import dev.kord.core.entity.Ban
import dev.kord.core.entity.Guild
import dev.kord.core.event.Event
import dev.kord.core.event.guild.BanAddEvent
import dev.kord.gateway.Intent
import dev.kord.rest.request.RestRequestException
import dev.kordex.core.annotations.NotTranslated
import dev.kordex.core.checks.types.CheckContext
import dev.kordex.core.checks.userFor
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import dev.kordex.core.utils.envOf
import dev.kordex.core.utils.envOrNull
import dev.kordex.core.utils.scheduling.Scheduler
import dev.upcraft.rtuuy.model.BanSyncRepository
import dev.upcraft.rtuuy.model.DiscordUserRepository
import dev.upcraft.rtuuy.util.analytics.posthog
import dev.upcraft.rtuuy.util.ext.asSeconds
import dev.upcraft.rtuuy.util.ext.ifNull
import dev.upcraft.rtuuy.util.ext.isNotSyncedBan
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Clock
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

// TODO facilitate ban syncing via token system
//::
// - create ban group with name and token
// - create a way to share tokens to add new guilds to existing sync groups
@OptIn(ExperimentalAtomicApi::class)
class BanSyncExtension : Extension() {
	override val name = "ban_sync"
	override val intents: MutableSet<Intent> = mutableSetOf(Intent.GuildModeration)

	private val isDryRun = envOf<Boolean>("BAN_SYNC_DRY_RUN")
	private val syncInterval = loadSyncIntervalFromEnv()
	private val scheduler = Scheduler()

	private val discordUsers by inject<DiscordUserRepository>()
	private val syncedBans by inject<BanSyncRepository>()

	private var syncingBans: MutableMap<Snowflake, BanEntry> = ConcurrentHashMap()
	private var hasStartedInitialSync = false
	private val isSyncing = AtomicBoolean(false)

	@OptIn(NotTranslated::class)
	private suspend fun <T : Event> CheckContext<T>.isNotSyncingUser() {
		if (!passed) {
			return
		}

		failIf("already syncing user") {
			userFor(event)?.id in syncingBans
		}
	}

	override suspend fun setup() {
//		addHealthCheck("ban_sync") {
//			startingIfNot { hasStartedInitialSync }
//
//			unhealthyIf("missed last sync interval") {
//				val config = getConfig()
//
//				syncedServers.isNotEmpty() && hasStartedInitialSync && !isSyncing
//					&& config.lastSynced + config.syncInterval < Clock.System.now() - 2.seconds
//			}
//		}

		if (isDryRun) {
			logger.warn { "Dry run has been enabled. All bans to be synced will not be applied!" }
		}

		syncInterval?.let {
			scheduler.schedule(it, true, "Ban Sync", 10, true) {
				syncBans()
			}
			scheduler.schedule(5.seconds, true, "Initial Ban Sync", 1, false) {
				syncBans()
			}
		}

		event<BanAddEvent> {
			check {
				isNotSyncingUser()
				isNotSyncedBan()
			}

			action {
				val ban = BanEntry(event.user.id, event.guildId, event.getBan().reason ?: "unknown", Clock.System.now())
				val originalGuild = event.guild.asGuild()

				syncingBans.put(event.user.id, ban)
				try {
					val syncedGuilds = syncedBans.getSyncedGuildsWith(event.guild)
						.map { it.id.value }

					syncedGuilds.ifEmpty {
						return@action
					}

					logger.info { "User ${event.user.username} (${event.user.id}) has been banned on ${originalGuild.name} (${originalGuild.id})! Syncing that ban between synced servers." }
					// TODO this should all be scheduled onto a work queue, and pending actions stored in DB
					syncedGuilds.mapNotNull { guildId ->
						kord.getGuildOrNull(guildId).ifNull {
							logger.warn { "Guild $guildId not found while trying to sync ban for user ${event.user.id} (from ${originalGuild.id})" }
						}
					}.forEach { guild ->
						guild.syncBan(ban)
					}
				} finally {
					syncingBans.remove(event.user.id)
				}
			}
		}

//		ephemeralSlashCommand {
//			name = "create_sync_group".toKey()
//			description = "Create a new sync group".toKey()
//			defaultMemberPermissions = Permissions(Permission.ManageGuild)
//
//			check {
//				anyGuild()
//			}
//
//			action {
//				val guild = guildFor(event)!!
//				val syncGroup = syncedBans.createSyncGroup(guild)
//
//				respond {
//					content = "Created sync group with ID ${syncGroup.id}"
//				}
//			}
//		}
//
//		ephemeralSlashCommand {
//			name = "add_to_sync_group".toKey()
//			description = "Add this guild to an existing sync group".toKey()
//			arguments
//
//			check {
//				anyGuild()
//			}
//
//			action {
//				val guild = guildFor(event)!!
//				val syncGroup = syncedBans.createSyncGroup(guild)
//
//				respond {
//					content = "Created sync group with ID ${syncGroup.id}"
//				}
//			}
//		}
	}

	private suspend fun Guild.syncBan(ban: BanEntry, skipCheck: Boolean = false) {
		if (skipCheck || this.getBanOrNull(ban.userId) == null) {
			if (isDryRun) {
				logger.info { "[DRY-RUN] Not syncing ban for user ${ban.userId} to guild ${this.id}" }
				return
			}

			try {
				this.ban(ban.userId) {
					reason = ban.toReasonString()
				}
				logger.info { "Successfully synced ban for user ${ban.userId} to guild ${this.id}" }
			} catch (e: RestRequestException) {
				logger.error(e) { "Failed to sync ban for user ${ban.userId} to guild ${this.id}" }
				posthog {
					captureException(
						e, null, mapOf(
							"user_id" to ban.userId,
							"guild_id" to this@syncBan.id,
							"ban_reason" to ban.reason,
							"ban_timestamp" to ban.timestamp
						)
					)
				}
			}
		} else {
			logger.debug { "User ${ban.userId} has already been banned in guild ${this.id}" }
		}
	}

	private suspend fun syncBans() {
		if (isSyncing.load()) {
			logger.warn { "Ban sync already in progress" }
			return
		}
		hasStartedInitialSync = true
		try {
			isSyncing.store(true)
			val startTime = Clock.System.now().asSeconds()
			logger.info { "Running scheduled ban sync at $startTime" }

			val syncGroups = syncedBans.getAllSyncGroups()

			// step 1: consolidate all users to ban, sorted by origin guild
			val bannedUsers = ConcurrentHashMap<Snowflake, MutableMap<Snowflake, Ban>>()
			syncGroups.flatMap { it.guilds }.distinct().forEach { dbGuild ->
				val guild = kord.getGuildOrNull(dbGuild.id.value)
				if (guild == null) {
					logger.warn { "Unable to find server with ID ${dbGuild.id.value}" }
					return@forEach
				}

				guild.bans.collect {
					bannedUsers.computeIfAbsent(guild.id) { mutableMapOf() }.put(it.userId, it)
				}
			}

			// step 2: calculate which users need to be banned in which guilds, filtering out synced bans
			var toBan: MutableMap<Snowflake, MutableList<BanEntry>> = ConcurrentHashMap()
			bannedUsers.forEach { (guildId, guildBans) ->
				val bansFromGuild = guildBans
					.filter { it.value.reason?.startsWith(BanEntry.SYNCED_PREFIX) != true } // DO NOT sync bans that we created ourselves
					.map { BanEntry.from(ban = it.value, timestamp = startTime) }

				// figure out which guilds to sync this guild with
				val targetGuildIds = syncGroups
					.filter { it.guilds.any { g -> g.id.value == guildId } }
					.flatMap { it.guilds.map { g -> g.id.value } }
					.distinct()
					.filter { it != guildId }

				targetGuildIds.forEach { targetGuildId ->
					toBan.computeIfAbsent(targetGuildId) { mutableListOf() }.addAll(bansFromGuild)
				}
			}

			// step 3: deduplicate work and filter out existing bans
			toBan = toBan.mapValues { e ->
				e.value
					.distinctBy { it.userId } // skip duplicates
					.filter { bannedUsers[e.key]?.keys?.contains(it.userId) != true } // skip users that have already been banned
					.toMutableList()
			}.toMutableMap()

			// step 4: finally ban the users in each guild
			toBan.forEach { (guildId, bans) ->
				val guild = kord.getGuildOrNull(guildId)
				if (guild == null) {
					logger.error { "Unable to find server with ID ${guildId.value}, unable to apply bans" }
					return@forEach
				}

				bans.forEach { ban -> guild.syncBan(ban, skipCheck = true) }

				syncedBans.setLastSynced(guild, startTime)
			}
		} finally {
			isSyncing.store(false)
		}
	}

	private fun loadSyncIntervalFromEnv(): Duration? {
		return envOrNull("BAN_SYNC_INTERVAL")?.let {
			Duration.parseOrNull(it).ifNull {
				logger.error { "Unable to parse BAN_SYNC_INTERVAL: '${it}' is not a valid duration" }
			}
		}.ifNull {
			logger.warn { "BAN_SYNC_INTERVAL not set or invalid, disabling periodic ban sync" }
		}
	}
}
