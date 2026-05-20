package io.trading.infra.persistence

import io.trading.domain.common.Cursor
import kotlinx.datetime.Instant
import java.util.Base64
import java.util.UUID

/**
 * Курсор: base64(timestamp_ns:uuid). Используется для (executed_at DESC, id) пагинации.
 */
object CursorCodec {
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun encode(ts: Instant, id: UUID): Cursor {
        val raw = "${ts.toEpochMilliseconds()}|$id"
        return Cursor(encoder.encodeToString(raw.toByteArray(Charsets.UTF_8)))
    }

    data class Decoded(val ts: Instant, val id: UUID)

    fun decode(cursor: Cursor): Decoded {
        val raw = String(decoder.decode(cursor.value), Charsets.UTF_8)
        val parts = raw.split('|')
        require(parts.size == 2) { "Bad cursor" }
        return Decoded(
            ts = Instant.fromEpochMilliseconds(parts[0].toLong()),
            id = UUID.fromString(parts[1]),
        )
    }
}
