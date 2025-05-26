package dev.upcraft.rtuuy.model

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.entity.User
import dev.kord.core.entity.effectiveName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import org.jetbrains.exposed.dao.ULongEntity
import org.jetbrains.exposed.dao.ULongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.ULongIdTable
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DiscordGuilds : ULongIdTable("discord_guilds") {
	val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
	val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

	init {
		transaction {
			SchemaUtils.create(DiscordGuilds)
		}
	}
}

object DiscordUsers : ULongIdTable("discord_users") {

	val handle = varchar("handle", 32)
	val displayName = varchar("display_name", 32).nullable()

	val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
	val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

	init {
		transaction {
			SchemaUtils.create(DiscordUsers)
		}
	}
}

object DiscordUsersInGuilds : Table() {
	val discordUser = reference("discord_user_id", DiscordUsers, onUpdate = ReferenceOption.CASCADE, onDelete = ReferenceOption.CASCADE)
	val guildId = reference("discord_guild_id", DiscordGuilds, onUpdate = ReferenceOption.CASCADE, onDelete = ReferenceOption.CASCADE)

	val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
	val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

	override val primaryKey = PrimaryKey(discordUser, guildId)

	init {
		SchemaUtils.create(DiscordUsersInGuilds)
	}
}

class DiscordGuild(id: EntityID<ULong>) : ULongEntity(id) {
	companion object : ULongEntityClass<DiscordGuild>(DiscordGuilds)

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

class DiscordUser(id: EntityID<ULong>) : ULongEntity(id) {
	companion object : ULongEntityClass<DiscordUser>(DiscordUsers)

	var guilds by DiscordGuild via DiscordUsersInGuilds

	var handle by DiscordUsers.handle
	var displayName by DiscordUsers.displayName

	var createdAt by DiscordUsers.createdAt
	var updatedAt by DiscordUsers.updatedAt

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
		DiscordGuild.findById(guildId.value)?.let { guild ->
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
			val guildEntity = DiscordGuild.findById(guild.id.value) ?: DiscordGuild.new(guild.id.value) {}

			DiscordUser.findById(user.id.value)?.apply {
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
			} ?: DiscordUser.new(user.id.value) {
				handle = user.username
				displayName = user.effectiveName
				guilds = SizedCollection(guildEntity)
			}
		}
	}

	suspend fun getUser(snowflake: Snowflake): DiscordUser? = withContext(Dispatchers.IO) {
		DiscordUser.findById(snowflake.value)
	}

	suspend fun deleteUser(snowflake: Snowflake) = withContext(Dispatchers.IO) {
		newSuspendedTransaction(db = database) {
			DiscordUser.findById(snowflake.value)?.delete()
		}
	}

	suspend fun inGuild(guild: Snowflake): Long = withContext(Dispatchers.IO) {
		DiscordGuild.findById(guild.value)?.users?.count() ?: 0
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

	suspend fun allUsers(): List<DiscordUser> = withContext(Dispatchers.IO) {
		DiscordUser.all().toList()
	}

	suspend fun getGuild(guild: GuildBehavior, users: Boolean = false): DiscordGuild? = withContext(Dispatchers.IO) {
		newSuspendedTransaction(db = database) {
			DiscordGuild.findById(guild.id.value)?.apply {
				if (users) {
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
}
