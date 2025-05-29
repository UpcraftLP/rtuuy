package dev.upcraft.rtuuy.extensions.anti_reply_ping

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.message.allowedMentions
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.isNotBot
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.commands.converters.impl.optionalBoolean
import dev.kordex.core.commands.converters.impl.optionalDuration
import dev.kordex.core.commands.converters.impl.role
import dev.kordex.core.commands.converters.impl.user
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.extensions.event
import dev.kordex.core.i18n.withContext
import dev.kordex.core.utils.any
import dev.kordex.core.utils.repliedMessageOrNull
import dev.kordex.core.utils.timeoutUntil
import dev.kordex.core.utils.toDuration
import dev.upcraft.rtuuy.i18n.Translations
import dev.upcraft.rtuuy.model.AntiReplyPingRepository
import dev.upcraft.rtuuy.util.analytics.posthog
import dev.upcraft.rtuuy.util.ext.asRoleMention
import dev.upcraft.rtuuy.util.ext.forAnalytics
import dev.upcraft.rtuuy.util.ext.hasReplyPing
import dev.upcraft.rtuuy.util.ext.ifNull
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import org.koin.core.component.inject

val logger = KotlinLogging.logger {}

class AntiReplyPingExtension : Extension() {
	override val name = "anti_reply_ping"

	@OptIn(PrivilegedIntent::class)
	override val intents: MutableSet<Intent> = mutableSetOf(Intent.GuildMessages, Intent.MessageContent)

	private val antiReplyPingRepository by inject<AntiReplyPingRepository>()

	override suspend fun setup() {
		event<MessageCreateEvent> {
			check {
				anyGuild()
				isNotBot()
				hasReplyPing()
			}

			action {
				val user = event.member!!
				val pingedUser = event.message.repliedMessageOrNull()?.author ?: return@action
				val guild = event.getGuildOrNull()!!

				val replyPingSettings = antiReplyPingRepository.getNonPingableUser(guild, pingedUser)
					.ifNull { return@action }!! // user not excluded

				if (guild.everyoneRole.id in replyPingSettings.roleExceptions) {
					logger.warn { "everyone role is allowed to ping user ${pingedUser.id} in guild ${guild.id}" }
					return@action
				}

				if (user.id in replyPingSettings.userExceptions || user.roles.map { it.id }
						.any { it in replyPingSettings.roleExceptions }) {
					// allowed to ping
					return@action
				}

				posthog {
					capture(
						user.id.forAnalytics(), "triggered_anti_reply_ping", mapOf(
							"guild_id" to guild.id.toString(),
							"guild_name" to guild.name,
							"channel_id" to event.message.channelId.toString(),
						)
					)
				}

				if (replyPingSettings.deleteOriginalMessage) {
					logger.debug { "deleting message ${event.message.id} by ${user.id} in guild ${guild.id} because it is a reply ping" }
					event.message.delete("reply ping")
				}

				replyPingSettings.mutePeriod?.let { duration ->
					user.edit {
						reason = "[Anti-Reply-Ping] Muted for reply-pinging ${pingedUser.username}"
						timeoutUntil = Clock.System.now().plus(duration)
					}
				}

				event.message.channel.createMessage {
					content = Translations.Moderation.AntiReplyPing.timeOut
						.withContext(this@action)
						.translateNamed(
							"user" to user.mention,
							"pinged_user" to pingedUser.mention,
						)

					allowedMentions {
						users.add(user.id)
					}
				}
			}
		}

		ephemeralSlashCommand {
			name = Translations.Commands.AntiReplyPing.name
			description = Translations.Commands.AntiReplyPing.description
			defaultMemberPermissions = Permissions(Permission.ManageGuild)

			check {
				anyGuild()
			}

			// anti_reply_ping add <user>
			class AntiReplyPingAddArgs : Arguments() {
				val user by user {
					name = Translations.Arguments.AntiReplyPing.User.name
					description = Translations.Arguments.AntiReplyPing.User.description
				}
				val deleteOriginalMessages by optionalBoolean {
					name = Translations.Arguments.AntiReplyPing.DeleteOriginalMessage.name
					description = Translations.Arguments.AntiReplyPing.DeleteOriginalMessage.description
				}
				val timeoutDuration by optionalDuration {
					name = Translations.Arguments.AntiReplyPing.TimeoutDuration.name
					description = Translations.Arguments.AntiReplyPing.TimeoutDuration.description
				}
			}
			ephemeralSubCommand(::AntiReplyPingAddArgs) {
				name = Translations.Commands.AntiReplyPing.Add.name
				description = Translations.Commands.AntiReplyPing.Add.description

				action {
					antiReplyPingRepository.createOrUpdateNonPingableUser(
						guild!!,
						arguments.user,
						arguments.deleteOriginalMessages ?: true,
						arguments.timeoutDuration?.toDuration(TimeZone.UTC)
					)

					respond {
						content = Translations.Commands.AntiReplyPing.Add.Response.success
							.withContext(this@action)
							.translateNamed(
								"user" to arguments.user.mention
							)
					}
				}
			}

			// anti_reply_ping remove <user>
			class AntiReplyPingRemoveArgs : Arguments() {
				val user by user {
					name = Translations.Arguments.AntiReplyPing.User.name
					description = Translations.Arguments.AntiReplyPing.User.description
				}
			}
			ephemeralSubCommand(::AntiReplyPingRemoveArgs) {
				name = Translations.Commands.AntiReplyPing.Remove.name
				description = Translations.Commands.AntiReplyPing.Remove.description

				action {

					if (antiReplyPingRepository.deleteNonPingableUser(guild!!, arguments.user)) {
						respond {
							content = Translations.Commands.AntiReplyPing.Remove.Response.success
								.withContext(this@action)
								.translateNamed(
									"user" to arguments.user.mention
								)
						}
					} else {
						respond {
							content = Translations.Commands.AntiReplyPing.Remove.Response.notFound
								.withContext(this@action)
								.translateNamed(
									"user" to arguments.user.mention
								)
						}
					}
				}
			}

			// anti_reply_ping list
			ephemeralSubCommand {
				name = Translations.Commands.AntiReplyPing.List.name
				description = Translations.Commands.AntiReplyPing.List.description

				action {
					val nonReplyPings = antiReplyPingRepository.getNonPingableUsers(guild!!)

					// TODO list individual exclusions where applicable
					val list = buildList {
						if (nonReplyPings.isNotEmpty()) {
							nonReplyPings.map { it.user.mention() }.chunked(15).forEach { add(it.joinToString("\n")) }
						} else {
							add(
								Translations.Commands.AntiReplyPing.List.placeholder
									.withContext(this@action)
									.translate()
							)
						}
					}

					editingPaginator {
						list.forEach { users ->
							page {
								title = Translations.Commands.AntiReplyPing.List.Embed.title
									.withContext(this@action)
									.translate()
								description = users
							}
						}
					}.send()
				}
			}

			// anti_reply_ping add_excluded_role
			class AntiReplyPingAddGuildRoleExclusionArgs : Arguments() {
				val role by role {
					name = Translations.Arguments.AntiReplyPing.User.name
					description = Translations.Arguments.AntiReplyPing.User.description
				}
			}
			ephemeralSubCommand(::AntiReplyPingAddGuildRoleExclusionArgs) {
				name = Translations.Commands.AntiReplyPing.AddExcludedRole.name
				description = Translations.Commands.AntiReplyPing.AddExcludedRole.description

				action {
					if (antiReplyPingRepository.createGuildRoleException(guild!!, arguments.role.id)) {
						respond {
							content = Translations.Commands.AntiReplyPing.AddExcludedRole.Response.success
								.withContext(this@action)
								.translateNamed(
									"role" to arguments.role.mention
								)
						}
					} else {
						respond {
							content = Translations.Commands.AntiReplyPing.AddExcludedRole.Response.alreadyAdded
								.withContext(this@action)
								.translateNamed(
									"role" to arguments.role.mention
								)
						}
					}
				}
			}

			// anti_reply_ping remove_excluded_role
			class AntiReplyPingRemoveGuildRomeExclusionArgs : Arguments() {
				val role by role {
					name = Translations.Arguments.AntiReplyPing.User.name
					description = Translations.Arguments.AntiReplyPing.User.description
				}
			}
			ephemeralSubCommand(::AntiReplyPingRemoveGuildRomeExclusionArgs) {
				name = Translations.Commands.AntiReplyPing.RemoveExcludedRole.name
				description = Translations.Commands.AntiReplyPing.RemoveExcludedRole.description

				action {
					if (antiReplyPingRepository.deleteGuildRoleException(guild!!, arguments.role.id)) {
						respond {
							content = Translations.Commands.AntiReplyPing.RemoveExcludedRole.Response.success
								.withContext(this@action)
								.translateNamed(
									"role" to arguments.role.mention
								)
						}
					} else {
						respond {
							content = Translations.Commands.AntiReplyPing.RemoveExcludedRole.Response.notFound
								.withContext(this@action)
								.translateNamed(
									"role" to arguments.role.mention
								)
						}
					}
				}
			}

			// anti_reply_ping list_global_exclusions
			ephemeralSubCommand {
				name = Translations.Commands.AntiReplyPing.ListGlobalExclusions.name
				description = Translations.Commands.AntiReplyPing.ListGlobalExclusions.description

				action {
					val config = antiReplyPingRepository.listGuildExclusions(guild!!)
					val allIds = config.excludedRoleIds.map { it.asRoleMention() }
						.plus(config.excludedUsers.map { it.mention() })
					val list = buildList {
						if (allIds.isNotEmpty()) {
							allIds.chunked(15).forEach { add(it.joinToString("\n")) }
						} else {
							add(
								Translations.Commands.AntiReplyPing.ListGlobalExclusions.placeholder
									.withContext(this@action)
									.translate()
							)
						}
					}

					editingPaginator {
						list.forEach { roles ->
							page {
								title = Translations.Commands.AntiReplyPing.ListGlobalExclusions.Embed.title
									.withContext(this@action)
									.translate()
								description = roles
							}
						}
					}.send()
				}
			}
		}
	}
}
