package dev.upcraft.rtuuy.util.ntfy

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.net.URI
import java.net.URL

class NtfyClient(client: HttpClient, serverUrl: URL, accessToken: String?) {
	companion object {
		val DEFAULT_URL: URL = URI.create("https://ntfy.sh").toURL()
	}

	private val client: HttpClient = client

	/**
	 * For security reasons we only support [access tokens](https://docs.ntfy.sh/publish/#access-tokens)
	 */
	private val accessToken: String? = accessToken

	/**
	 * the custom NTFY server URL
	 */
	private val serverUrl: URL = serverUrl

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

public fun NtfyClient(builder: NtfyClientBuilder.() -> Unit): NtfyClient {
	return NtfyClientBuilder().apply(builder).build()
}

class NtfyClientBuilder {

	lateinit var client: HttpClient
	var server: URL? = null
	var accessToken: String? = null

	fun build(): NtfyClient = NtfyClient(
		client = client,
		accessToken = accessToken,
		serverUrl = server ?: NtfyClient.DEFAULT_URL
	)
}
