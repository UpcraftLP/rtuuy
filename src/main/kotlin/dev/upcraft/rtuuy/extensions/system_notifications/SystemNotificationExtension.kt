package dev.upcraft.rtuuy.extensions.system_notifications

import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Webhook
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.rest.builder.message.embed
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import dev.kordex.core.utils.envOrNull
import dev.kordex.core.utils.executeStored
import dev.upcraft.rtuuy.App
import dev.upcraft.rtuuy.i18n.Translations
import dev.upcraft.rtuuy.util.ntfy.NtfyClient
import dev.upcraft.rtuuy.util.ntfy.NtfyTags
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import java.net.URI

class SystemNotificationExtension : Extension() {
	override val name = "system_notifications"

	private val discordWebhookUrls = envOrNull("SYSTEM_NOTIFICATION_WEBHOOK_URLS")
	private var discordWebhooks: MutableSet<Webhook> = mutableSetOf()

	private val ntfyTopic = envOrNull("SYSTEM_NOTIFICATION_NTFY_TOPIC")
	private var ntfyClient: NtfyClient? = ntfyTopic?.let {
		NtfyClient {
			client = HttpClient {
				install(ContentNegotiation) {
					json()
				}
			}
			accessToken = envOrNull("SYSTEM_NOTIFICATION_NTFY_TOKEN")
			// custom URL if provided, public service otherwise
			server = envOrNull("SYSTEM_NOTIFICATION_NTFY_SERVER")
		}
	}

	override suspend fun setup() {
		discordWebhookUrls?.let { urls ->
			urls.split(",").forEach { url ->
				val split = url.split("/")

				if (split.size != 7) {
					throw IllegalArgumentException("Invalid SYSTEM_NOTIFICATION_WEBHOOK_URL: $url")
				}

				val webhookId = Snowflake(split[5])
				val token = split[6]

				val webhook = kord.getWebhookWithToken(webhookId, token)
				discordWebhooks.add(webhook)
			}
		}

		event<ReadyEvent> {
			var seen = false
			action {
				if (seen) {
					return@action
				}
				seen = true

				val applicationId = kord.getApplicationInfo().id
				val botUsername = kord.getSelf().username

				val replacements = mapOf(
					"version" to App.VERSION,
					"author" to App.AUTHOR,
					"repository_url" to App.REPOSITORY_URL,
					"commit_url" to App.COMMIT_URL,
					"commit_sha" to App.COMMIT_SHA,
					"bot_username" to botUsername,
					"application_id" to applicationId,
				)

				discordWebhooks.forEach { webhook ->
					webhook.executeStored {
						embed {
							title = Translations.SystemNotifications.StartupWebhook.title
								.translateNamed(replacements)
							description = Translations.SystemNotifications.StartupWebhook.message
								.translateNamed(replacements)

							footer {
								text = Translations.Notifications.Footer.poweredBy
									.translateNamed(replacements)
							}
						}
					}
				}

				ntfyClient?.let { ntfy ->
					ntfy.publish(ntfyTopic!!) {
						tags.add(NtfyTags.Robot)
						message = Translations.SystemNotifications.StartupNtfy.message
							.translateNamed(
								"version" to App.VERSION,
								"bot_username" to botUsername,
								"application_id" to applicationId
							)
						click = URI.create(App.COMMIT_URL)
					}
				}
			}
		}
	}
}
