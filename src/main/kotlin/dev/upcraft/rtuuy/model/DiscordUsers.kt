package dev.upcraft.rtuuy.model

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.entity.User
import dev.kord.core.entity.effectiveName
import dev.upcraft.rtuuy.util.ext.asUserMention
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DiscordGuilds : SnowflakeIdTable("discord_guilds") {
	val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
	val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

	init {
		transaction {
			SchemaUtils.create(DiscordGuilds)
		}
	}
}

object DiscordUsers : SnowflakeIdTable("discord_users") {

	val handle = varchar("handle", 32)
	val displayName = varchar("display_name", 32).nullable()

	val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
	val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

	init {
		transaction {
			index(false, handle)
			index(false, displayName)
			SchemaUtils.create(DiscordUsers)
		}
	}
}

object DiscordUsersInGuilds : Table("discord_users_in_guilds") {
	val discordUser = reference(
		"discord_user_id",
		DiscordUsers,
		onUpdate = ReferenceOption.CASCADE,
		onDelete = ReferenceOption.CASCADE
	)
	val guildId = reference(
		"discord_guild_id",
		DiscordGuilds,
		onUpdate = ReferenceOption.CASCADE,
		onDelete = ReferenceOption.CASCADE
	)

	val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
	val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

	override val primaryKey = PrimaryKey(discordUser, guildId)

	init {
		SchemaUtils.create(DiscordUsersInGuilds)
	}
}

class DiscordGuild(id: EntityID<Snowflake>) : SnowflakeEntity(id) {
	companion object : SnowflakeEntityClass<DiscordGuild>(DiscordGuilds)

	var users by DiscordUser via DiscordUsersInGuilds

	var createdAt by DiscordGuilds.createdAt
	var updatedAt by DiscordGuilds.updatedAt

	suspend fun forEachUser(block: suspend (DiscordUser) -> Unit) {
		newSuspendedTransaction {
			users.forEach { block(it) }
		}
	}

	suspend fun update(block: suspend DiscordGuild.() -> Unit) {
		newSuspendedTransaction {
			val old = updatedAt
			block()
			if (updatedAt == old) {
				updatedAt = Clock.System.now()
			}
		}
	}
}

class DiscordUser(id: EntityID<Snowflake>) : SnowflakeEntity(id) {
	companion object : SnowflakeEntityClass<DiscordUser>(DiscordUsers)

	var guilds by DiscordGuild via DiscordUsersInGuilds

	var handle by DiscordUsers.handle
	var displayName by DiscordUsers.displayName

	var createdAt by DiscordUsers.createdAt
	var updatedAt by DiscordUsers.updatedAt

	fun mention(): String {
		return id.value.asUserMention()
	}

	suspend fun update(block: suspend DiscordUser.() -> Unit) {
		newSuspendedTransaction {
			val old = updatedAt
			block()
			if (updatedAt == old) {
				updatedAt = Clock.System.now()
			}
		}
	}

	suspend fun leaveGuild(guildId: Snowflake) {
		DiscordGuild.findById(guildId)?.let { guild ->
			leaveGuild(guild)
		}
	}

	suspend fun leaveGuild(guild: DiscordGuild) {
		update {
			guilds = SizedCollection(guilds.minus(guild))
		}
	}
}

class DiscordUserRepository(private val database: Database) {

	suspend fun getOrCreateUser(user: User, guild: GuildBehavior): DiscordUser = withContext(Dispatchers.IO) {
		newSuspendedTransaction(db = database) {
			val guildEntity = getOrCreateGuild(guild)

			DiscordUser.findById(user.id)?.apply {
				val needsUpdate = (handle != user.username)
					|| (displayName != user.effectiveName)

				if (needsUpdate) {
					update {
						handle = user.username
						displayName = user.effectiveName
					}
				}

				if (!guilds.contains(guildEntity)) {
					guilds = SizedCollection(guilds.plus(guildEntity))
				}
			} ?: DiscordUser.new(user.id) {
				handle = user.username
				displayName = user.effectiveName
				guilds = SizedCollection(guildEntity)
			}
		}
	}

	suspend fun getUser(snowflake: Snowflake): DiscordUser? = withContext(Dispatchers.IO) {
		newSuspendedTransaction(db = database) {
			DiscordUser.findById(snowflake)
		}
	}

	suspend fun deleteUser(snowflake: Snowflake) = withContext(Dispatchers.IO) {
		newSuspendedTransaction(db = database) {
			DiscordUser.findById(snowflake)?.delete()
		}
	}

	suspend fun inGuild(guild: Snowflake): Long = withContext(Dispatchers.IO) {
		newSuspendedTransaction(db = database) {
			DiscordGuild.findById(guild)?.users?.count() ?: 0
		}
	}

	suspend fun allUsers(): List<DiscordUser> = withContext(Dispatchers.IO) {
		newSuspendedTransaction(db = database) {
			DiscordUser.all().toList()
		}
	}

	suspend fun getOrCreateGuild(guild: GuildBehavior, loadUsers: Boolean = false): DiscordGuild =
		withContext(Dispatchers.IO) {
			newSuspendedTransaction(db = database) {
				(DiscordGuild.findById(guild.id) ?: DiscordGuild.new(guild.id) {}).apply {
					if (loadUsers) {
						load(DiscordGuild::users)
					}
				}
			}
		}

	suspend fun getGuild(guild: GuildBehavior, loadUsers: Boolean = false): DiscordGuild? =
		withContext(Dispatchers.IO) {
			newSuspendedTransaction(db = database) {
				DiscordGuild.findById(guild.id)?.apply {
					if (loadUsers) {
						load(DiscordGuild::users)
					}
				}
			}
		}

	suspend fun deleteGuild(dbGuild: DiscordGuild) = withContext(Dispatchers.IO) {
		newSuspendedTransaction(db = database) {
			dbGuild.delete()
		}
	}

	suspend fun allGuilds(loadUsers: Boolean = false): List<DiscordGuild> = withContext(Dispatchers.IO) {
		newSuspendedTransaction(db = database) {
			var tmp = DiscordGuild.all()
			if (loadUsers) {
				tmp = tmp.with(DiscordGuild::users)
			}
			tmp.toList()
		}
	}
}
