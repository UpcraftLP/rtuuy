package dev.upcraft.rtuuy.util.analytics

import dev.kordex.core.utils.envOrNull
import dev.kordex.core.utils.getKoin
import dev.kordex.core.utils.loadModule
import net.hollowcube.posthog.PostHog
import net.hollowcube.posthog.PostHogClient
import org.koin.dsl.onClose
import java.time.Duration

object PostHogAnalytics {

	private const val DEFAULT_INSTANCE_URL = "https://us.i.posthog.com"

	fun init(): Boolean {
		envOrNull("POSTHOG_PROJECT_API_KEY")?.let { projectApiKey ->
			PostHog.init(projectApiKey) { builder ->
				val posthogInstanceUrl = envOrNull("POSTHOG_INSTANCE_URL") ?: DEFAULT_INSTANCE_URL
				builder.endpoint(posthogInstanceUrl)

				// required for ex. feature flags, local evaluation
				envOrNull("POSTHOG_PERSONAL_API_KEY")?.let { personalApiKey ->
					builder.personalApiKey(personalApiKey)
				}

				builder
			}

			loadModule {
				factory { PostHog.getClient() }.onClose { it?.shutdown(Duration.ofSeconds(5)) }
			}

			return true
		}

		return false
	}
}

suspend fun posthog(posthog: suspend PostHogClient.() -> Unit) {
	getKoin().getOrNull<PostHogClient>()?.let {
		posthog(it)
	}
}
