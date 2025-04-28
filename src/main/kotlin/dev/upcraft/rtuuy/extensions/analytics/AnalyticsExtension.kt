package dev.upcraft.rtuuy.extensions.analytics

import dev.kord.core.event.guild.BanAddEvent
import dev.kord.core.event.guild.BanRemoveEvent
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kordex.core.checks.isNotBot
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import dev.upcraft.rtuuy.util.analytics.PostHogAnalytics
import dev.upcraft.rtuuy.util.analytics.posthog
import dev.upcraft.rtuuy.util.ext.forAnalytics
import dev.upcraft.rtuuy.util.ext.hasUser
import dev.upcraft.rtuuy.util.ext.isNotJoinMessage

class AnalyticsExtension : Extension() {
	override val name = "analytics"

	@OptIn(PrivilegedIntent::class)
	override val intents: MutableSet<Intent> =
		mutableSetOf(Intent.GuildMessages, Intent.MessageContent, Intent.GuildMembers, Intent.GuildModeration)

	override suspend fun setup() {
		if (!PostHogAnalytics.init()) {
			return
		}

		event<MessageCreateEvent> {
			check {
				isNotJoinMessage()
				hasUser()
				isNotBot()
			}

			action {
				event.member?.id?.forAnalytics()?.let { analyticsId ->
					posthog {
						val guild = event.message.getGuild()
						val hasAttachments = event.message.attachments.isNotEmpty()
						capture(
							analyticsId, "message_created", listOfNotNull(
								"guild_id" to guild.id.toString(),
								"guild_name" to guild.name,
								"channel_id" to event.message.channelId.toString(),
								"length" to event.message.content.length,
								("has_attachments" to hasAttachments).takeIf { hasAttachments },
							).toMap()
						)
					}
				}
			}
		}

		event<MemberJoinEvent> {
			check {
				isNotBot()
			}

			action {
				posthog {
					event.member.id
					val guild = event.getGuild()
					capture(
						event.member.id.forAnalytics(), "member_joined", mapOf(
							"guild_id" to guild.id.toString(),
							"guild_name" to guild.name,
						)
					)
				}
			}
		}

		event<MemberLeaveEvent> {
			check {
				isNotBot()
			}

			action {
				posthog {
					val guild = event.getGuild()
					capture(
						event.user.id.forAnalytics(), "member_left", mapOf(
							"guild_id" to guild.id.toString(),
							"guild_name" to guild.name,
						)
					)
				}
			}
		}

		event<BanAddEvent> {
			check {
				isNotBot()
			}

			action {
				posthog {
					val guild = event.getGuild()
					capture(
						event.user.id.forAnalytics(), "member_banned", mapOf(
							"guild_id" to guild.id.toString(),
							"guild_name" to guild.name,
						)
					)
				}
			}
		}

		event<BanRemoveEvent> {
			check {
				isNotBot()
			}

			action {
				posthog {
					val guild = event.getGuild()
					capture(
						event.user.id.forAnalytics(), "member_unbanned", mapOf(
							"guild_id" to guild.id.toString(),
							"guild_name" to guild.name,
						)
					)
				}
			}
		}
	}
}
