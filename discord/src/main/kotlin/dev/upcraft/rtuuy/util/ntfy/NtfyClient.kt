package dev.upcraft.rtuuy.util.ntfy

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

class NtfyClient(
	private val client: HttpClient,
	/**
	 * the custom NTFY server URL
	 */
	private val serverUrl: String,
	/**
	 * For security reasons we only support [access tokens](https://docs.ntfy.sh/publish/#access-tokens)
	 */
	private val accessToken: String?
) {
	companion object {
		const val DEFAULT_URL = "https://ntfy.sh"
	}

	suspend fun publish(topic: String, message: NtfyMessageBuilder.() -> Unit) {
		val messageObj = NtfyMessageBuilder().apply(message).toMessage(topic)
		val response = client.post(serverUrl) {
			contentType(ContentType.Application.Json)
			accessToken?.let { bearerAuth(it) }
			setBody(messageObj)
		}
		// TODO handle HTTP response
	}
}

fun NtfyClient(builder: NtfyClientBuilder.() -> Unit): NtfyClient {
	return NtfyClientBuilder().apply(builder).build()
}

class NtfyClientBuilder {

	lateinit var client: HttpClient
	var server: String? = null
	var accessToken: String? = null

	fun build(): NtfyClient = NtfyClient(
		client = client,
		accessToken = accessToken,
		serverUrl = server ?: NtfyClient.DEFAULT_URL
	)
}
