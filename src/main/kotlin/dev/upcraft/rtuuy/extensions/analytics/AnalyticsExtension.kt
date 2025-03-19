package dev.upcraft.rtuuy.extensions.analytics

import dev.kord.core.event.guild.BanAddEvent
import dev.kord.core.event.guild.BanRemoveEvent
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kordex.core.checks.isNotBot
import dev.kordex.core.checks.memberFor
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import dev.upcraft.rtuuy.util.analytics.PostHogAnalytics
import dev.upcraft.rtuuy.util.analytics.posthog
import dev.upcraft.rtuuy.util.ext.forAnalytics

class AnalyticsExtension : Extension() {
	override val name = "analytics"

	override suspend fun setup() {
		if (!PostHogAnalytics.init()) {
			return
		}

		event<MessageCreateEvent> {
			check {
				isNotBot()
			}

			action {
				memberFor(event)?.id?.forAnalytics()?.let { analyticsId ->
					posthog {
						val guild = event.message.getGuild()
						capture(
							analyticsId, "message_created", mapOf(
								"guild_id" to guild.id.toString(),
								"guild_name" to guild.name,
								"channel_id" to event.message.channelId.toString(),
							)
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
