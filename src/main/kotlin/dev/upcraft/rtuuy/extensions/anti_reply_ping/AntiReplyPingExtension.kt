package dev.upcraft.rtuuy.extensions.anti_reply_ping

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import dev.kordex.core.checks.failed
import dev.kordex.core.checks.isNotBot
import dev.kordex.core.checks.passed
import dev.kordex.core.checks.types.CheckContext
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.commands.converters.impl.optionalDuration
import dev.kordex.core.commands.converters.impl.role
import dev.kordex.core.commands.converters.impl.user
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.extensions.event
import dev.kordex.core.i18n.withContext
import dev.kordex.core.storage.StorageType
import dev.kordex.core.storage.StorageUnit
import dev.kordex.core.utils.format
import dev.kordex.core.utils.timeoutUntil
import dev.kordex.core.utils.toDuration
import dev.upcraft.rtuuy.i18n.Translations
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toDateTimePeriod
import kotlin.time.Duration.Companion.minutes

class AntiReplyPingExtension : Extension() {
	override val name = "anti_reply_ping"

	private val MENTION_REGEX = Regex("<@(\\d*)>")

	private val storageUnit = StorageUnit<AntiReplyPingConfig>(
        StorageType.Config,
        "rtuuy",
        "anti_reply_ping"
    )

	suspend fun CheckContext<MessageCreateEvent>.hasBadReplyPing() {
		if (!passed) {
			return
		}

		val logger = KotlinLogging.logger("dev.upcraft.rtuuy.AntiReplyPingExtension.hasBadReplyPing")
		val config = event.guildId?.let { getConfig(it) }

		val goodRoles = config?.allowedRoles
		val everyoneRoleId = event.getGuildOrNull()?.everyoneRole?.id
		goodRoles?.let {
			for (goodRole in goodRoles) {
				if (event.member?.roleIds?.contains(goodRole) == true || everyoneRoleId == goodRole) {
					logger.failed("User has a role in the excluded list")
					fail("User has a role in the excluded list")
					return
				}
			}
		}

		val badIds = config?.forbiddenUsers
		val candidates = MENTION_REGEX.findAll(event.message.content).map {
            Snowflake(it.groupValues.last())
		}

		badIds?.let {
			for (badId in badIds) {
				if (badId in event.message.mentionedUserIds) {
					if (badId !in candidates) {
						logger.passed()
						pass()
						return
					}
				}
			}
		}

		logger.failed("No bad reply pings were detected")
		fail("No bad reply pings were detected")
	}

	override suspend fun setup() {
		event<MessageCreateEvent> {
			check {
				isNotBot()
				hasBadReplyPing()
			}

			action {
				val config = getConfig(event.message.getGuild().id)
				event.message.reply {
					content = Translations.Moderation.AntiReplyPing.timeOut
						.withContext(this@action)
						.translateNamed(
							"user" to event.member?.mention
						)
				}

				event.member?.edit {
					reason = "Reply-pinging an user covered by the anti-reply ping system."
					timeoutUntil = Clock.System.now().plus(config.mutePeriod)
				}
			}
		}

		ephemeralSlashCommand {
			name = Translations.Commands.AntiReplyPing.name
			description = Translations.Commands.AntiReplyPing.description
			defaultMemberPermissions = Permissions(Permission.ManageGuild)

			ephemeralSubCommand(::AntiReplyPingAddArgs) {
				name = Translations.Commands.AntiReplyPing.Add.name
				description = Translations.Commands.AntiReplyPing.Add.description

				action {
					guild?.let {
						var config = getConfig(guild!!.id)

						if (arguments.user.id !in config.forbiddenUsers) {
							config.forbiddenUsers.add(arguments.user.id)

							saveConfig(guild!!.id)

							respond {
								content = Translations.Commands.AntiReplyPing.Add.Response.success
									.withContext(this@action)
									.translateNamed(
										"user" to arguments.user.mention
									)
							}
						} else {
							respond {
								content = Translations.Commands.AntiReplyPing.Add.Response.alreadyAdded
									.withContext(this@action)
									.translateNamed(
										"user" to arguments.user.mention
									)
							}
						}
					}
				}
			}

			ephemeralSubCommand(::AntiReplyPingRemoveArgs) {
				name = Translations.Commands.AntiReplyPing.Remove.name
				description = Translations.Commands.AntiReplyPing.Remove.description

				action {
					guild?.let {
						var config = getConfig(guild!!.id)

						if (arguments.user.id in config.forbiddenUsers) {
							config.forbiddenUsers.remove(arguments.user.id)

							saveConfig(guild!!.id)

							respond {
								content = Translations.Commands.AntiReplyPing.Remove.Response.success
									.withContext(this@action)
									.translateNamed(
										"user" to arguments.user.mention
									)
							}
						} else {
							respond {
								content = Translations.Commands.AntiReplyPing.Remove.Response.alreadyRemoved
									.withContext(this@action)
									.translateNamed(
										"user" to arguments.user.mention
									)
							}
						}
					}
				}
			}

			ephemeralSubCommand {
				name = Translations.Commands.AntiReplyPing.List.name
				description = Translations.Commands.AntiReplyPing.List.description

				action {
					val config = getConfig(guild!!.id)
					val pings = config.forbiddenUsers.map { "<@$it>" }
					val list = buildList<String> {
						if (pings.isNotEmpty()) {
							var i = 0
							while (i < pings.count()) {
								add(pings.take(15).joinToString("\n"))
								i++
							}
						} else {
							add(
                                Translations.Commands.AntiReplyPing.List.placeholder
								.withContext(this@action)
								.translate()
							)
						}
					}

					editingPaginator {
						for (pings in list) {
							page {
								title = "Non-reply-pingable users"
								description = pings
							}
						}
					}.send()
				}
			}

			ephemeralSubCommand(::AntiReplyPingExcludeArgs) {
				name = Translations.Commands.AntiReplyPing.Exclude.name
				description = Translations.Commands.AntiReplyPing.Exclude.description

				action {
					guild?.let {
						var config = getConfig(guild!!.id)

						if (arguments.role.id !in config.allowedRoles) {
							config.allowedRoles.add(arguments.role.id)
							saveConfig(guild!!.id)

							respond {
								content = Translations.Commands.AntiReplyPing.Exclude.Response.success
									.withContext(this@action)
									.translateNamed(
										"role" to arguments.role.mention
									)
							}
						} else {
							respond {
								content = Translations.Commands.AntiReplyPing.Exclude.Response.alreadyAdded
									.withContext(this@action)
									.translateNamed(
										"role" to arguments.role.mention
									)
							}
						}
					}
				}
			}

			ephemeralSubCommand(::AntiReplyPingIncludeArgs) {
				name = Translations.Commands.AntiReplyPing.Include.name
				description = Translations.Commands.AntiReplyPing.Include.description

				action {
					guild?.let {
						var config = getConfig(guild!!.id)

						if (arguments.role.id in config.allowedRoles) {
							config.allowedRoles.remove(arguments.role.id)
							saveConfig(guild!!.id)

							respond {
								content = Translations.Commands.AntiReplyPing.Include.Response.success
									.withContext(this@action)
									.translateNamed(
										"role" to arguments.role.mention
									)
							}
						} else {
							respond {
								content = Translations.Commands.AntiReplyPing.Include.Response.alreadyRemoved
									.withContext(this@action)
									.translateNamed(
										"role" to arguments.role.mention
									)
							}
						}
					}
				}
			}

			ephemeralSubCommand {
				name = Translations.Commands.AntiReplyPing.RoleList.name
				description = Translations.Commands.AntiReplyPing.RoleList.description

				action {
					val config = getConfig(guild!!.id)
					val pings = config.allowedRoles.map { "<@$it>" }
					val list = buildList<String> {
						if (pings.isNotEmpty()) {
							var i = 0
							while (i < pings.count()) {
								add(pings.take(15).joinToString("\n"))
								i++
							}
						} else {
							add(
                                Translations.Commands.AntiReplyPing.RoleList.placeholder
								.withContext(this@action)
								.translate()
							)
						}
					}

					editingPaginator {
						for (pings in list) {
							page {
								title = "Excluded roles"
								description = pings
							}
						}
					}.send()
				}
			}

			ephemeralSubCommand(::AntiReplyPingSetTimeoutDurationArgs) {
				name = Translations.Commands.AntiReplyPing.SetTimeoutDuration.name
				description = Translations.Commands.AntiReplyPing.SetTimeoutDuration.description

				action {
					arguments.duration?.let { duration ->
						var config = getConfig(guild!!.id)
						config.mutePeriod = duration.toDuration(TimeZone.Companion.UTC)
						saveConfig(guild!!.id)

						respond {
							content = Translations.Commands.AntiReplyPing.SetTimeoutDuration.Response.success
								.withContext(this@action)
								.translateNamed(
									"duration" to duration.format(getLocale())
								)
						}
					}

					if (arguments.duration == null) {
						respond {
							var config = getConfig(guild!!.id)
							content = Translations.Commands.AntiReplyPing.SetTimeoutDuration.Response.noDuration
								.withContext(this@action)
								.translateNamed(
									// This could be simplified to duration.format(getLocale), but let's not risk it yet
									"duration" to config.mutePeriod.toDateTimePeriod().format(getLocale())
								)
						}
					}
				}
			}
		}
	}

	inner class AntiReplyPingAddArgs : Arguments() {
		val user by user {
			name = Translations.Arguments.AntiReplyPing.User.name
			description = Translations.Arguments.AntiReplyPing.User.description
		}
	}

	inner class AntiReplyPingRemoveArgs : Arguments() {
		val user by user {
			name = Translations.Arguments.AntiReplyPing.User.name
			description = Translations.Arguments.AntiReplyPing.User.description
		}
	}

	inner class AntiReplyPingExcludeArgs : Arguments() {
		val role by role {
			name = Translations.Arguments.AntiReplyPing.User.name
			description = Translations.Arguments.AntiReplyPing.User.description
		}
	}

	inner class AntiReplyPingIncludeArgs : Arguments() {
		val role by role {
			name = Translations.Arguments.AntiReplyPing.User.name
			description = Translations.Arguments.AntiReplyPing.User.description
		}
	}

	inner class AntiReplyPingSetTimeoutDurationArgs : Arguments() {
		val duration by optionalDuration {
			name = Translations.Arguments.AntiReplyPing.Duration.name
			description = Translations.Arguments.AntiReplyPing.Duration.description
		}
	}

	private suspend fun getConfig(guildId: Snowflake): AntiReplyPingConfig {
		var config = storageUnit
			.withGuild(guildId)
			.get()

		if (config == null) {
			config = AntiReplyPingConfig(
                mutableSetOf(),
                mutableSetOf(),
                5L.minutes
            )

			storageUnit
				.withGuild(guildId)
				.save(config)
		}

		return config
	}

	private suspend fun saveConfig(guildId: Snowflake) {
		storageUnit
			.withGuild(guildId)
			.save()
	}
}
