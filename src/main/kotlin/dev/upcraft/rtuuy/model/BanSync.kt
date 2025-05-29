package dev.upcraft.rtuuy.model

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior
import dev.upcraft.rtuuy.model.dto.ban_sync.BanSyncGroupDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object BanSyncGroups : UUIDTable("ban_sync_groups") {
	val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
	val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

	init {
		transaction {
			SchemaUtils.create(BanSyncGroups)
		}
	}
}

object GuildsInBanSyncGroups : Table() {
	val groupId =
		reference("group_id", BanSyncGroups, onUpdate = ReferenceOption.CASCADE, onDelete = ReferenceOption.CASCADE)
	val guildId =
		reference("guild_id", DiscordGuilds, onUpdate = ReferenceOption.CASCADE, onDelete = ReferenceOption.CASCADE)

	val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
	val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

	override val primaryKey = PrimaryKey(groupId, guildId)

	init {
		transaction {
			SchemaUtils.create(GuildsInBanSyncGroups)

			index(false, guildId)
			index(false, groupId)
		}
	}
}

/**
 * tracks when bans TO a guild were last synced, from all other guilds that it is synced with
 */
object BanSyncTimes : SnowflakeIdTable("ban_sync_times", columnName = "guild_id") {
	val lastSynced = timestamp("last_synced").default(Instant.DISTANT_PAST)

	init {
		transaction {
			SchemaUtils.create(BanSyncTimes)
		}
	}
}

class BanSyncGroup(id: EntityID<UUID>) : UUIDEntity(id) {
	companion object : UUIDEntityClass<BanSyncGroup>(BanSyncGroups)

	var guilds by DiscordGuild via GuildsInBanSyncGroups

	var createdAt by BanSyncGroups.createdAt
	var updatedAt by BanSyncGroups.updatedAt

	fun toDto(): BanSyncGroupDto = BanSyncGroupDto(guilds.toList(), createdAt, updatedAt)
}

class BanSyncTime(id: EntityID<Snowflake>) : SnowflakeEntity(id) {
	companion object : SnowflakeEntityClass<BanSyncTime>(BanSyncTimes)

	var lastSynced by BanSyncTimes.lastSynced
}

class BanSyncRepository(private val database: Database, private val discordUsers: DiscordUserRepository) {

	suspend fun createSyncGroup(guild: GuildBehavior): BanSyncGroup = withContext(Dispatchers.IO) {
		newSuspendedTransaction(db = database) {
			val dbGuild = discordUsers.getOrCreateGuild(guild)

			BanSyncGroup.new {
				guilds = SizedCollection(dbGuild)
			}
		}
	}

	suspend fun getSyncGroups(guild: GuildBehavior): List<BanSyncGroupDto> = withContext(Dispatchers.IO) {
		newSuspendedTransaction(db = database) {
			val query = BanSyncGroups.innerJoin(GuildsInBanSyncGroups)
				.select(BanSyncGroups.columns)
				.where { GuildsInBanSyncGroups.guildId eq guild.id }
				.orderBy(BanSyncGroups.createdAt)

			BanSyncGroup.wrapRows(query).with(BanSyncGroup::guilds).map { it.toDto() }.toList()
		}
	}

	suspend fun addToSyncGroup(guild: GuildBehavior, groupId: UUID): Boolean = withContext(Dispatchers.IO) {
		newSuspendedTransaction(db = database) {
			val syncGroup = BanSyncGroup.findById(groupId)?.load(BanSyncGroup::guilds)
			if (syncGroup == null) {
				return@newSuspendedTransaction false
			}

			syncGroup.guilds = SizedCollection(syncGroup.guilds.plus(discordUsers.getOrCreateGuild(guild)))

			return@newSuspendedTransaction true
		}
	}

	suspend fun removeFromSyncGroup(guild: GuildBehavior, groupId: UUID): Boolean = withContext(Dispatchers.IO) {
		newSuspendedTransaction(db = database) {
			val syncGroup = BanSyncGroup.findById(groupId)?.load(BanSyncGroup::guilds)
			if (syncGroup == null) {
				return@newSuspendedTransaction false
			}

			syncGroup.guilds = SizedCollection(syncGroup.guilds.minus(discordUsers.getOrCreateGuild(guild)))

			return@newSuspendedTransaction true
		}
	}

	suspend fun getSyncedGuildsWith(guild: GuildBehavior): List<DiscordGuild> {
		return getSyncGroups(guild)
			.flatMap { it.guilds }
			.distinct()
			.filter { it.id.value != guild.id }
	}

	suspend fun getAllSyncGroups(): List<BanSyncGroupDto> = withContext(Dispatchers.IO) {
		newSuspendedTransaction(db = database) {
			BanSyncGroup.all().with(BanSyncGroup::guilds).map { it.toDto() }.toList()
		}
	}

	suspend fun getLastSynced(guild: GuildBehavior): Instant = withContext(Dispatchers.IO) {
		newSuspendedTransaction(db = database) {
			BanSyncTime.findById(guild.id)?.lastSynced ?: Instant.DISTANT_PAST
		}
	}

	suspend fun setLastSynced(guild: GuildBehavior, time: Instant = Clock.System.now()) = withContext(Dispatchers.IO) {
		newSuspendedTransaction(db = database) {
			BanSyncTime.findByIdAndUpdate(guild.id) {
				it.lastSynced = time
			} ?: BanSyncTime.new(guild.id) {
				lastSynced = time
			}
		}
	}
}
