package dev.upcraft.rtuuy.util.analytics

import com.posthog.java.PostHog
import com.posthog.java.PostHogLogger
import dev.kordex.core.utils.envOrNull
import dev.kordex.core.utils.getKoin
import dev.kordex.core.utils.loadModule
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.dsl.onClose

object PostHogAnalytics {

	private const val DEFAULT_INSTANCE_URL = "https://us.i.posthog.com"

	fun init(): PostHog? {
		envOrNull("POSTHOG_API_KEY")?.let { posthogApiKey ->
			val posthogInstanceUrl = envOrNull("POSTHOG_INSTANCE_URL") ?: DEFAULT_INSTANCE_URL

			loadModule {
				single {
					PostHog.Builder(posthogApiKey).host(posthogInstanceUrl).logger(Logger()).build()
				}.onClose { it?.shutdown() }
			}
		}

		return getKoin().getOrNull<PostHog>()
	}
}

suspend fun posthog(posthog: suspend PostHog.() -> Unit) {
	getKoin().getOrNull<PostHog>()?.let {
		posthog(it)
	}
}

class Logger: PostHogLogger {

	private val logger = KotlinLogging.logger(PostHogAnalytics.javaClass.name)

	override fun debug(message: String?) {
		logger.debug { message }
	}

	override fun info(message: String?) {
		logger.info { message }
	}

	override fun warn(message: String?) {
		logger.warn { message }
	}

	override fun error(message: String?) {
		logger.error { message }
	}

	override fun error(message: String?, throwable: Throwable?) {
		logger.error(throwable) { message }
	}

}
