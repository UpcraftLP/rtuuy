package dev.upcraft.rtuuy.util.ntfy

import java.net.URL
import kotlin.time.Duration

/**
 * See the [NTFY documentation](https://docs.ntfy.sh/publish/#publish-as-json)
 *
 * Note that [Action Buttons](https://docs.ntfy.sh/publish/#action-buttons) are currently not supported
 */
data class NtfyMessage(
	val topic: String,

	val tags: MutableSet<String>?,
	val priority: Int?,
	val delay: String?,

	val title: String?,

	val message: String?,
	val markdown: Boolean?,

	val icon: URL?,

	val attach: URL?,
	val filename: String?,

	val click: URL?,
	val email: String?,
	val call: String?
)

class NtfyMessageBuilder {

	public var tags: MutableSet<String> = mutableSetOf()
	public var priority: Int? = null
	public var delay: Duration? = null
	public var title: String? = null
	public var message: String? = null
	public var markdown: Boolean? = null
	public var icon: URL? = null
	public var attach: URL? = null
	public var filename: String? = null
	public var click: URL? = null
	public var email: String? = null
	public var call: String? = null

	fun toMessage(topic: String): NtfyMessage = NtfyMessage(
		topic,
		tags = tags.ifEmpty { null },
		priority = priority,
		delay = delay?.toString(),
		title = title,
		message = message,
		markdown = markdown,
		icon = icon,
		attach = attach,
		filename = filename,
		click = click,
		email = email,
		call = call
	)
}
