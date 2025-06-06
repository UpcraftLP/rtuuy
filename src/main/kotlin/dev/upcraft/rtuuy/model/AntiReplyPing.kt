package dev.upcraft.rtuuy.model

import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.UserBehavior
import dev.upcraft.rtuuy.model.dto.anti_reply_ping.AntiReplyPingGlobalExclusionsDto
import dev.upcraft.rtuuy.model.dto.anti_reply_ping.NonPingableUserWithExclusionsDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import org.jetbrains.exposed.dao.ULongEntity
import org.jetbrains.exposed.dao.ULongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.ULongIdTable
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestamp
import org.jetbrains.exposed.sql.kotlin.datetime.duration
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

object NonPingableUsersInGuilds : ULongIdTable("non_pingable_users_in_guilds") {
	val guildId =
		reference("guild_id", DiscordGuilds, onUpdate = ReferenceOption.CASCADE, onDelete = ReferenceOption.CASCADE)
	val userId =
		reference("user_id", DiscordUsers, onUpdate = ReferenceOption.CASCADE, onDelete = ReferenceOption.CASCADE)

	val mutePeriod = duration("mute_period").default(1.minutes).nullable()
	val deleteOriginalMessages = bool("delete_original_messages").default(false)

	val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
	val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

	init {
		transaction {
			uniqueIndex(guildId, userId)
			index(false, guildId)
			SchemaUtils.create(NonPingableUsersInGuilds)
		}
	}
}

object NonPingableUsersInGuildsUserExceptions : Table("non_pingable_users_in_guilds_user_exceptions") {
	val parentId = reference(
		"parent_id",
		NonPingableUsersInGuilds,
		onUpdate = ReferenceOption.CASCADE,
		onDelete = ReferenceOption.CASCADE
	)
	val userId =
		reference("user_id", DiscordUsers, onUpdate = ReferenceOption.CASCADE, onDelete = ReferenceOption.CASCADE)

	val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
	val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

	init {
		transaction {
			uniqueIndex(parentId, userId)
			SchemaUtils.create(NonPingableUsersInGuildsUserExceptions)
		}
	}
}

object NonPingableUsersInGuildsUserExceptionsGlobal : ULongIdTable("non_pingable_users_in_guilds_user_exceptions_global") {
	val guildId =
		reference("guild_id", DiscordGuilds, onUpdate = ReferenceOption.CASCADE, onDelete = ReferenceOption.CASCADE)
	val userId =
		reference("user_id", DiscordUsers, onUpdate = ReferenceOption.CASCADE, onDelete = ReferenceOption.CASCADE)

	val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
	val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

	init {
		transaction {
			uniqueIndex(guildId, userId)
			index(false, guildId)
			SchemaUtils.create(NonPingableUsersInGuildsUserExceptionsGlobal)
		}
	}
}

object NonPingableUsersInGuildsRoleExceptions : ULongIdTable("non_pingable_users_in_guilds_role_exceptions") {
	val parentId = reference(
		"parent_id",
		NonPingableUsersInGuilds,
		onUpdate = ReferenceOption.CASCADE,
		onDelete = ReferenceOption.CASCADE
	)
	val roleId = ulong("role_id").transform(SnowflakeTransformer())

	val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
	val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

	init {
		transaction {
			uniqueIndex(parentId, roleId)
			SchemaUtils.create(NonPingableUsersInGuildsRoleExceptions)
		}
	}
}

object NonPingableUsersInGuildsRoleExceptionsGlobal : ULongIdTable("non_pingable_users_in_guilds_role_exceptions_global") {
	val guildId =
		reference("guild_id", DiscordGuilds, onUpdate = ReferenceOption.CASCADE, onDelete = ReferenceOption.CASCADE)
	val roleId = ulong("role_id").transform(SnowflakeTransformer())

	val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
	val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

	init {
		transaction {
			uniqueIndex(guildId, roleId)
			index(false, guildId)
			SchemaUtils.create(NonPingableUsersInGuildsRoleExceptionsGlobal)
		}
	}
}

class NonPingableUserInGuild(id: EntityID<ULong>) : ULongEntity(id) {
	companion object : ULongEntityClass<NonPingableUserInGuild>(NonPingableUsersInGuilds)

	var guild by DiscordGuild referencedOn NonPingableUsersInGuilds.guildId
	var user by DiscordUser referencedOn NonPingableUsersInGuilds.userId

	var mutePeriod by NonPingableUsersInGuilds.mutePeriod
	var deleteOriginalMessage by NonPingableUsersInGuilds.deleteOriginalMessages

	var userExceptions by DiscordUser via NonPingableUsersInGuildsUserExceptions
	val roleExceptions by NonPingableUsersInGuildsRoleException referrersOn NonPingableUsersInGuildsRoleExceptions.parentId

	var createdAt by NonPingableUsersInGuilds.createdAt
	var updatedAt by NonPingableUsersInGuilds.updatedAt
}

class NonPingableUsersInGuildsRoleException(id: EntityID<ULong>) : ULongEntity(id) {
	companion object : ULongEntityClass<NonPingableUsersInGuildsRoleException>(NonPingableUsersInGuildsRoleExceptions)

	var parent by NonPingableUserInGuild referencedOn NonPingableUsersInGuildsRoleExceptions.parentId
	var roleId by NonPingableUsersInGuildsRoleExceptions.roleId

	var createdAt by NonPingableUsersInGuildsRoleExceptions.createdAt
	var updatedAt by NonPingableUsersInGuildsRoleExceptions.updatedAt
}

class NonPingableUsersInGuildsUserExceptionGlobal(id: EntityID<ULong>) : ULongEntity(id) {
	companion object :
		ULongEntityClass<NonPingableUsersInGuildsUserExceptionGlobal>(NonPingableUsersInGuildsUserExceptionsGlobal)

	var guild by DiscordGuild referencedOn NonPingableUsersInGuildsUserExceptionsGlobal.guildId
	var user by DiscordUser referencedOn NonPingableUsersInGuildsUserExceptionsGlobal.userId

	var createdAt by NonPingableUsersInGuildsUserExceptionsGlobal.createdAt
	var updatedAt by NonPingableUsersInGuildsUserExceptionsGlobal.updatedAt
}

class NonPingableUsersInGuildsRoleExceptionGlobal(id: EntityID<ULong>) : ULongEntity(id) {
	companion object :
		ULongEntityClass<NonPingableUsersInGuildsRoleExceptionGlobal>(NonPingableUsersInGuildsRoleExceptionsGlobal)

	var guild by DiscordGuild referencedOn NonPingableUsersInGuildsRoleExceptionsGlobal.guildId
	var roleId by NonPingableUsersInGuildsRoleExceptionsGlobal.roleId

	var createdAt by NonPingableUsersInGuildsRoleExceptionsGlobal.createdAt
	var updatedAt by NonPingableUsersInGuildsRoleExceptionsGlobal.updatedAt
}

class AntiReplyPingRepository(private val database: Database, private val discordUsers: DiscordUserRepository) {

	suspend fun getNonPingableUsers(guild: GuildBehavior): List<NonPingableUserInGuild> = withContext(Dispatchers.IO) {
		newSuspendedTransaction(db = database) {
			NonPingableUserInGuild.find { NonPingableUsersInGuilds.guildId eq guild.id }
				.with(NonPingableUserInGuild::user).toList()
		}
	}

	suspend fun getNonPingableUser(guild: GuildBehavior, user: UserBehavior): NonPingableUserWithExclusionsDto? =
		withContext(Dispatchers.IO) {
			newSuspendedTransaction(db = database) {
				val found =
					NonPingableUserInGuild.find { (NonPingableUsersInGuilds.guildId eq guild.id) and (NonPingableUsersInGuilds.userId eq user.id) }
						.with(NonPingableUserInGuild::userExceptions, NonPingableUserInGuild::roleExceptions)
						.firstOrNull()
				if (found == null) {
					return@newSuspendedTransaction null
				}

				val excludedUsers = found.userExceptions.map { it.id.value }.toMutableSet()
				NonPingableUsersInGuildsUserExceptionsGlobal.select(NonPingableUsersInGuildsUserExceptionsGlobal.userId)
					.where { NonPingableUsersInGuildsUserExceptionsGlobal.guildId eq guild.id }
					.map { it[NonPingableUsersInGuildsUserExceptionsGlobal.userId].value }
					.toCollection(excludedUsers)

				val excludedRoles = found.roleExceptions.map { it.roleId }.toMutableSet()
				NonPingableUsersInGuildsRoleExceptionsGlobal.select(NonPingableUsersInGuildsRoleExceptionsGlobal.roleId)
					.where { NonPingableUsersInGuildsRoleExceptionsGlobal.guildId eq guild.id }
					.map { it[NonPingableUsersInGuildsRoleExceptionsGlobal.roleId] }
					.toCollection(excludedRoles)

				return@newSuspendedTransaction NonPingableUserWithExclusionsDto(
					found.user,
					found.guild,
					found.mutePeriod,
					found.deleteOriginalMessage,
					excludedUsers,
					excludedRoles
				)
			}
		}

	suspend fun createOrUpdateNonPingableUser(
		guild: GuildBehavior,
		user: UserBehavior,
		deleteOriginalMessages: Boolean,
		mutePeriod: Duration? = null
	) = withContext(Dispatchers.IO) {
		newSuspendedTransaction(db = database) {
			val dbGuild = discordUsers.getOrCreateGuild(guild)
			val dbUser = discordUsers.getOrCreateUser(user.fetchUser(), guild)
			NonPingableUsersInGuilds.upsert(
				keys = arrayOf(NonPingableUsersInGuilds.guildId, NonPingableUsersInGuilds.userId),
				where = { (NonPingableUsersInGuilds.guildId eq dbGuild.id) and (NonPingableUsersInGuilds.userId eq dbUser.id) },
				onUpdateExclude = listOf(NonPingableUsersInGuilds.updatedAt)
			) {
				it[NonPingableUsersInGuilds.guildId] = dbGuild.id
				it[NonPingableUsersInGuilds.userId] = dbUser.id
				it[NonPingableUsersInGuilds.deleteOriginalMessages] = deleteOriginalMessages
				it[NonPingableUsersInGuilds.mutePeriod] = mutePeriod
				it[NonPingableUsersInGuilds.updatedAt] = Clock.System.now()
			}
		}
	}

	suspend fun deleteNonPingableUser(guild: GuildBehavior, user: UserBehavior): Boolean = withContext(Dispatchers.IO) {
		newSuspendedTransaction(db = database) {
			val deletedCount = NonPingableUsersInGuilds.deleteWhere {
				(NonPingableUsersInGuilds.guildId eq guild.id) and (NonPingableUsersInGuilds.userId eq user.id)
			}

			deletedCount > 0
		}
	}

	suspend fun createGuildRoleException(guild: GuildBehavior, roleId: Snowflake): Boolean =
		withContext(Dispatchers.IO) {
			newSuspendedTransaction(db = database) {
				val existing =
					NonPingableUsersInGuildsRoleExceptionGlobal.count((NonPingableUsersInGuildsRoleExceptionsGlobal.guildId eq guild.id) and (NonPingableUsersInGuildsRoleExceptionsGlobal.roleId eq roleId)) > 0
				if (existing) {
					return@newSuspendedTransaction false
				}

				val dbGuild = discordUsers.getOrCreateGuild(guild)
				NonPingableUsersInGuildsRoleExceptionGlobal.new {
					this.guild = dbGuild
					this.roleId = roleId
				}

				return@newSuspendedTransaction true
			}
		}

	suspend fun deleteGuildRoleException(guild: GuildBehavior, roleId: Snowflake): Boolean =
		withContext(Dispatchers.IO) {
			newSuspendedTransaction(db = database) {
				val deleted = NonPingableUsersInGuildsRoleExceptionsGlobal.deleteWhere(limit = 1) {
					(NonPingableUsersInGuildsRoleExceptionsGlobal.guildId eq guild.id) and (NonPingableUsersInGuildsRoleExceptionsGlobal.roleId eq roleId)
				}

				deleted > 0
			}
		}

	suspend fun listGuildExclusions(guild: GuildBehavior): AntiReplyPingGlobalExclusionsDto =
		withContext(Dispatchers.IO) {
			newSuspendedTransaction(db = database) {
				val excludedUsers =
					NonPingableUsersInGuildsUserExceptionGlobal.find { NonPingableUsersInGuildsUserExceptionsGlobal.guildId eq guild.id }
						.with(NonPingableUsersInGuildsUserExceptionGlobal::user)
						.map { it.user }
						.toList()

				val excludedRoles =
					NonPingableUsersInGuildsRoleExceptionGlobal.find { NonPingableUsersInGuildsRoleExceptionsGlobal.guildId eq guild.id }
						.map { it.roleId }
						.toList()

				AntiReplyPingGlobalExclusionsDto(
					guild.id,
					excludedUsers,
					excludedRoles
				)
			}
		}
}
