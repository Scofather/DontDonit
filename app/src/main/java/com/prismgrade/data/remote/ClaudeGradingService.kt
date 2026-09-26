package com.prismgrade.data.remote

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.models.messages.Base64ImageSource
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.ImageBlockParam
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.TextBlockParam
import com.prismgrade.data.image.PreparedImage
import com.prismgrade.domain.model.GradeReport
import com.prismgrade.domain.model.InspectionSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Talks to the Claude Messages API and returns a parsed [GradeReport].
 *
 * The SDK's calls are blocking, so every request is moved to [ioDispatcher].
 * Clients are cached per API key — building one opens an OkHttp pool, and the
 * key only changes when the user edits it in Settings.
 */
class ClaudeGradingService(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clientFactory: (String) -> AnthropicClient = { key ->
        AnthropicOkHttpClient.builder().apiKey(key).build()
    },
) {

    private var cachedKey: String? = null
    private var cachedClient: AnthropicClient? = null

    /** Everything that can go wrong, in terms the UI can act on. */
    sealed class GradingException(message: String) : Exception(message) {
        object MissingApiKey : GradingException(
            "Add your Anthropic API key in Settings before running an inspection.",
        )

        object InvalidApiKey : GradingException(
            "That API key was rejected. Check it in Settings.",
        )

        object RateLimited : GradingException(
            "Claude is rate limiting these requests. Wait a moment and try again.",
        )

        object Offline : GradingException(
            "Couldn't reach Claude. Check your connection and try again.",
        )

        class Refused(val explanation: String) : GradingException(
            "Claude declined to grade this one. $explanation".trim(),
        )

        class Unreadable(val reply: String) : GradingException(
            "Claude's answer couldn't be read as a grade. Try again.",
        )

        class Unexpected(val detail: String) : GradingException(detail)
    }

    suspend fun gradeFromPhotos(
        apiKey: String,
        front: PreparedImage,
        back: PreparedImage?,
    ): GradeReport {
        val blocks = buildList {
            add(front.toContentBlock())
            back?.let { add(it.toContentBlock()) }
            add(
                ContentBlockParam.ofText(
                    TextBlockParam.builder()
                        .text(GradingPrompts.forPhotos(hasBack = back != null))
                        .build(),
                ),
            )
        }
        return request(apiKey, blocks, InspectionSource.PHOTOS)
    }

    suspend fun gradeFromDescription(
        apiKey: String,
        card: String,
        condition: String,
    ): GradeReport {
        val blocks = listOf(
            ContentBlockParam.ofText(
                TextBlockParam.builder()
                    .text(GradingPrompts.forDescription(card, condition))
                    .build(),
            ),
        )
        return request(apiKey, blocks, InspectionSource.DESCRIPTION)
    }

    private suspend fun request(
        apiKey: String,
        blocks: List<ContentBlockParam>,
        source: InspectionSource,
    ): GradeReport = withContext(ioDispatcher) {
        if (apiKey.isBlank()) throw GradingException.MissingApiKey

        val params = MessageCreateParams.builder()
            .model(MODEL)
            .maxTokens(MAX_TOKENS)
            .system(GradingPrompts.SYSTEM)
            // Grading is a judgement call across several categories — worth the
            // thinking. Effort HIGH is the API default; named here deliberately.
            .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.HIGH).build())
            .addUserMessageOfBlockParams(blocks)
            .build()

        val message = try {
            client(apiKey).messages().create(params)
        } catch (e: AnthropicServiceException) {
            throw e.toGradingException()
        } catch (e: IOException) {
            throw GradingException.Offline
        }

        message.stopReason()
            .filter { it.toString().equals("refusal", ignoreCase = true) }
            .ifPresent {
                val why = message.stopDetails()
                    .map { details -> details.explanation().orElse("") }
                    .orElse("")
                throw GradingException.Refused(why)
            }

        val reply = message.content()
            .mapNotNull { block -> block.text().map { it.text() }.orElse(null) }
            .joinToString("\n")
            .trim()

        if (reply.isEmpty()) throw GradingException.Unreadable("")

        try {
            GradeReportParser.parse(reply, source)
        } catch (e: GradeReportParser.MalformedReplyException) {
            throw GradingException.Unreadable(e.reply)
        }
    }

    private fun client(apiKey: String): AnthropicClient {
        cachedClient?.let { if (cachedKey == apiKey) return it }
        return clientFactory(apiKey).also {
            cachedClient = it
            cachedKey = apiKey
        }
    }

    private fun AnthropicServiceException.toGradingException(): GradingException {
        val type = errorType().map { it.toString().lowercase() }.orElse("")
        return when {
            type.contains("authentication") || type.contains("permission") ->
                GradingException.InvalidApiKey
            type.contains("rate_limit") || type.contains("overloaded") ->
                GradingException.RateLimited
            else -> GradingException.Unexpected(
                message ?: "The inspection didn't complete. Try again in a moment.",
            )
        }
    }

    private fun PreparedImage.toContentBlock(): ContentBlockParam =
        ContentBlockParam.ofImage(
            ImageBlockParam.builder()
                .source(
                    Base64ImageSource.builder()
                        .data(base64)
                        .mediaType(Base64ImageSource.MediaType.IMAGE_JPEG)
                        .build(),
                )
                .build(),
        )

    private companion object {
        const val MODEL = "claude-opus-5"

        /** Non-streaming request: keep the response inside the SDK's HTTP timeout. */
        const val MAX_TOKENS = 16_000L
    }
}
