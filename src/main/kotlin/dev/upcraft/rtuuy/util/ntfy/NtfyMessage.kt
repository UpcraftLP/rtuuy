package dev.upcraft.rtuuy.util.ntfy

import kotlinx.serialization.Serializable
import java.net.URL
import kotlin.time.Duration

/**
 * See the [NTFY documentation](https://docs.ntfy.sh/publish/#publish-as-json)
 *
 * Note that [Action Buttons](https://docs.ntfy.sh/publish/#action-buttons) are currently not supported
 */
@Serializable
data class NtfyMessage(
	val topic: String,

	val tags: MutableSet<String>?,
	val priority: Int?,
	val delay: String?,

	val title: String?,

	val message: String?,
	val markdown: Boolean?,

	val icon: String?,

	val attach: String?,
	val filename: String?,

	val click: String?,
	val email: String?,
	val call: String?
)

@Suppress("MemberVisibilityCanBePrivate")
class NtfyMessageBuilder {

	var tags: MutableSet<String> = mutableSetOf()
	var priority: Int? = null
	var delay: Duration? = null
	var title: String? = null
	var message: String? = null
	var markdown: Boolean? = null
	var icon: URL? = null
	var attach: URL? = null
	var filename: String? = null
	var click: URL? = null
	var email: String? = null
	var call: String? = null

	fun toMessage(topic: String): NtfyMessage = NtfyMessage(
		topic,
		tags = tags.ifEmpty { null },
		priority = priority,
		delay = delay?.toString(),
		title = title,
		message = message,
		markdown = markdown,
		icon = icon?.toString(),
		attach = attach?.toString(),
		filename = filename,
		click = click?.toString(),
		email = email,
		call = call
	)
}
