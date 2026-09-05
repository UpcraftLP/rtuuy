package dev.upcraft.rtuuy.extensions.`fun`.grok

import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.message.allowedMentions
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

private const val commandPrefix = "@grok"

@OptIn(PrivilegedIntent::class)
class GrokExtension: Extension() {
	override val name = "fun:grok"

	override val intents: MutableSet<Intent> = mutableSetOf(Intent.MessageContent)

	val triggers: Set<String> = setOf("is this true", "explain")
	val responses: List<String> = listOf("no.", "The claim of white genocide is highly controversial")

	override suspend fun setup() {

		event<MessageCreateEvent> {
			check {
				anyGuild()
				message?.let {
					passIf {
						it.startsWith(commandPrefix) && it.substring(commandPrefix.length).trim().let { rest ->
							triggers.any { trigger -> rest.startsWith(trigger) }
						}
					}
				} ?: run {
					fail()
				}
			}

			action {
				event.message.reply {
					content = responses.randomOrNull() ?: "I don't know"
					allowedMentions {
						repliedUser = false
					}
				}
			}

		}
	}
}
