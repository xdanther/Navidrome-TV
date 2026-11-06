package com.navidrometv.party

import fi.iki.elonen.NanoHTTPD
import java.net.URLDecoder

/**
 * Embedded HTTP server for Party Mode.
 *
 * It serves exactly one static page and four JSON endpoints. It has no access to
 * Subsonic credentials, only to [PartyApi], which itself only exposes search and
 * request operations against the host's own session.
 */
class PartyGuestServer(
    port: Int,
    private val api: PartyApi,
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        return try {
            when (session.uri) {
                "/", "/index.html" -> newFixedLengthResponse(
                    Response.Status.OK,
                    "text/html; charset=utf-8",
                    GuestWebPage.HTML,
                )
                "/api/join" -> handleJoin(session)
                "/api/search" -> handleSearch(session)
                "/api/queue" -> handleQueue(session)
                "/api/request" -> handleRequest(session)
                else -> json(Response.Status.NOT_FOUND, """{"ok":false,"message":"Not found"}""")
            }
        } catch (error: Exception) {
            json(Response.Status.INTERNAL_ERROR, """{"ok":false,"message":"Server error"}""")
        }
    }

    private fun handleJoin(session: IHTTPSession): Response {
        val params = params(session)
        val code = params["code"].orEmpty()
        val name = params["name"].orEmpty()
        val join = api.join(code, name)
            ?: return json(Response.Status.OK, """{"ok":false,"message":"Wrong or expired pairing code"}""")
        return json(
            Response.Status.OK,
            """{"ok":true,"token":"${escape(join.token)}","expiresAt":${join.expiresAt}}""",
        )
    }

    private fun handleSearch(session: IHTTPSession): Response {
        val params = params(session)
        val token = params["token"].orEmpty()
        val query = params["q"].orEmpty()
        val tracks = api.search(token, query)
            ?: return json(Response.Status.OK, """{"ok":false,"message":"Session expired"}""")
        val payload = tracks.joinToString(",") { track ->
            """{"id":"${escape(track.id)}","title":"${escape(track.title)}","artist":"${escape(track.artist.orEmpty())}","album":"${escape(track.album.orEmpty())}"}"""
        }
        return json(Response.Status.OK, """{"ok":true,"tracks":[$payload]}""")
    }

    private fun handleQueue(session: IHTTPSession): Response {
        val token = params(session)["token"].orEmpty()
        val state = api.queueState(token)
            ?: return json(Response.Status.OK, """{"ok":false,"message":"Session expired"}""")
        val now = state.nowPlaying?.let { track ->
            """{"id":"${escape(track.id)}","title":"${escape(track.title)}","artist":"${escape(track.artist.orEmpty())}"}"""
        } ?: "null"
        val upcoming = state.upcoming.joinToString(",") { track ->
            """{"id":"${escape(track.id)}","title":"${escape(track.title)}","artist":"${escape(track.artist.orEmpty())}"}"""
        }
        return json(
            Response.Status.OK,
            """{"ok":true,"nowPlaying":$now,"upcoming":[$upcoming],"pending":${state.pending}}""",
        )
    }

    private fun handleRequest(session: IHTTPSession): Response {
        val params = params(session)
        val token = params["token"].orEmpty()
        val songId = params["songId"].orEmpty()
        if (songId.isBlank()) {
            return json(Response.Status.OK, """{"ok":false,"message":"Missing song"}""")
        }
        val result = api.requestTrack(token, songId)
        return json(
            Response.Status.OK,
            """{"ok":${result.ok},"message":"${escape(result.message)}"}""",
        )
    }

    private fun params(session: IHTTPSession): Map<String, String> {
        val result = mutableMapOf<String, String>()
        session.parameters.forEach { (key, values) ->
            values.firstOrNull()?.let { result[key] = it }
        }
        if (session.method == Method.POST) {
            val body = mutableMapOf<String, String>()
            runCatching { session.parseBody(body) }
            body["postData"]?.let { data ->
                data.split('&').forEach { pair ->
                    val separator = pair.indexOf('=')
                    if (separator > 0) {
                        val key = URLDecoder.decode(pair.substring(0, separator), "UTF-8")
                        val value = URLDecoder.decode(pair.substring(separator + 1), "UTF-8")
                        result[key] = value
                    }
                }
            }
        }
        return result
    }

    private fun json(status: Response.Status, body: String): Response =
        newFixedLengthResponse(status, "application/json; charset=utf-8", body)

    private fun escape(value: String): String = buildString(value.length + 8) {
        value.forEach { character ->
            when (character) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (character < ' ') append(' ') else append(character)
            }
        }
    }
}
