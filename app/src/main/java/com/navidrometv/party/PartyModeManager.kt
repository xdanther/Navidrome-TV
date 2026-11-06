package com.navidrometv.party

import com.navidrometv.core.common.AppLog
import com.navidrometv.core.common.DataResult
import com.navidrometv.core.model.Track
import com.navidrometv.domain.repository.MusicRepository
import com.navidrometv.playback.PlaybackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.SecureRandom
import java.util.Collections
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class PartyGuest(
    val id: String,
    val name: String,
    val joinedAt: Long,
)

data class PartyRequest(
    val id: String,
    val guestName: String,
    val track: Track,
    val requestedAt: Long,
)

data class PartyState(
    val running: Boolean = false,
    val port: Int = 0,
    val code: String = "",
    val url: String? = null,
    val guests: List<PartyGuest> = emptyList(),
    val pending: List<PartyRequest> = emptyList(),
    val notice: String? = null,
    val error: String? = null,
)

data class PartyJoin(val token: String, val expiresAt: Long)

data class GuestTrack(
    val id: String,
    val title: String,
    val artist: String?,
    val album: String?,
)

data class GuestQueueState(
    val nowPlaying: GuestTrack?,
    val upcoming: List<GuestTrack>,
    val pending: Int,
)

data class GuestRequestResult(val ok: Boolean, val message: String)

/** Contract the embedded HTTP server uses; keeps HTTP concerns out of the manager. */
interface PartyApi {
    fun join(code: String, guestName: String): PartyJoin?
    fun search(token: String, query: String): List<GuestTrack>?
    fun queueState(token: String): GuestQueueState?
    fun requestTrack(token: String, songId: String): GuestRequestResult
}

/**
 * Host side of Party Mode (a standalone Guest Queue feature, unrelated to any
 * Apple feature).
 *
 * Security model:
 *  - a short numerical pairing code is required to obtain a guest token;
 *  - tokens expire after two hours and are revoked when the session stops;
 *  - guests can only search and *request*; nothing enters the queue until the host
 *    approves it on the TV;
 *  - no server URL or Subsonic credential is ever sent to a guest device.
 */
@Singleton
class PartyModeManager @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playbackRepository: PlaybackRepository,
) : PartyApi {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val random = SecureRandom()

    private val _state = MutableStateFlow(PartyState())
    val state: StateFlow<PartyState> = _state.asStateFlow()

    private var server: PartyGuestServer? = null
    private var activeCode: String = ""

    private data class GuestSession(
        val guest: PartyGuest,
        val expiresAt: Long,
        val requestTimes: MutableList<Long> = Collections.synchronizedList(mutableListOf()),
        val searchTimes: MutableList<Long> = Collections.synchronizedList(mutableListOf()),
        var lastResults: List<Track> = emptyList(),
    )

    private val sessions = Collections.synchronizedMap(mutableMapOf<String, GuestSession>())
    private val pending = Collections.synchronizedList(mutableListOf<PartyRequest>())

    init {
        scope.launch {
            while (isActive) {
                delay(60_000)
                pruneExpired()
            }
        }
    }

    fun start(): PartyState {
        if (_state.value.running) return _state.value
        activeCode = "%04d".format(random.nextInt(10_000))
        var bound: PartyGuestServer? = null
        var usedPort = 0
        for (candidate in DEFAULT_PORT until DEFAULT_PORT + 12) {
            val attempt = PartyGuestServer(candidate, this)
            try {
                attempt.start()
                bound = attempt
                usedPort = candidate
                break
            } catch (error: Exception) {
                AppLog.warn("Party port $candidate unavailable")
                runCatching { attempt.stop() }
            }
        }
        if (bound == null) {
            _state.update { it.copy(error = "Could not start the guest server on this network") }
            return _state.value
        }
        server = bound
        _state.value = PartyState(
            running = true,
            port = usedPort,
            code = activeCode,
            url = guestUrl(usedPort),
        )
        AppLog.info("Party mode started on port $usedPort")
        return _state.value
    }

    fun stop() {
        runCatching { server?.stop() }
        server = null
        sessions.clear()
        pending.clear()
        activeCode = ""
        _state.value = PartyState()
    }

    fun approve(requestId: String) {
        val request = synchronized(pending) { pending.firstOrNull { it.id == requestId } } ?: return
        playbackRepository.addToQueue(request.track)
        synchronized(pending) { pending.removeAll { it.id == requestId } }
        publishPending()
        _state.update { it.copy(notice = "Added ${request.track.title} to the queue") }
    }

    fun reject(requestId: String) {
        synchronized(pending) { pending.removeAll { it.id == requestId } }
        publishPending()
        _state.update { it.copy(notice = "Request dismissed") }
    }

    fun consumeNotice() {
        _state.update { it.copy(notice = null) }
    }

    private fun publishPending() {
        val snapshot = synchronized(pending) { pending.toList() }
        _state.update { it.copy(pending = snapshot) }
    }

    private fun publishGuests() {
        val guests = sessions.values.map { it.guest }
        _state.update { it.copy(guests = guests) }
    }

    private fun pruneExpired() {
        val now = System.currentTimeMillis()
        sessions.entries.removeAll { (_, session) -> session.expiresAt <= now }
        val removed = synchronized(pending) {
            pending.removeAll { now - it.requestedAt > PENDING_TTL_MS }
        }
        if (removed) publishPending()
        publishGuests()
    }

    // PartyApi ------------------------------------------------------------

    override fun join(code: String, guestName: String): PartyJoin? {
        if (!_state.value.running || code != activeCode) return null
        val name = guestName.trim().ifBlank { "Guest" }.take(24)
        val guest = PartyGuest(id = UUID.randomUUID().toString(), name = name, joinedAt = System.currentTimeMillis())
        val token = UUID.randomUUID().toString().replace("-", "")
        val expiresAt = System.currentTimeMillis() + TOKEN_TTL_MS
        sessions[token] = GuestSession(guest = guest, expiresAt = expiresAt)
        publishGuests()
        return PartyJoin(token = token, expiresAt = expiresAt)
    }

    override fun search(token: String, query: String): List<GuestTrack>? {
        val session = validSession(token) ?: return null
        val now = System.currentTimeMillis()
        synchronized(session.searchTimes) {
            session.searchTimes.removeAll { now - it > 60_000 }
            if (session.searchTimes.size >= MAX_SEARCHES_PER_MINUTE) return emptyList()
            session.searchTimes.add(now)
        }
        if (query.trim().length < 2) return emptyList()
        val result = runBlocking { musicRepository.search(query.trim()) }
        return when (result) {
            is DataResult.Success -> {
                val tracks = result.data.tracks.take(25)
                session.lastResults = tracks
                tracks.map { it.toGuestTrack() }
            }
            is DataResult.Failure -> emptyList()
        }
    }

    override fun queueState(token: String): GuestQueueState? {
        validSession(token) ?: return null
        val playback = playbackRepository.state.value
        return GuestQueueState(
            nowPlaying = playback.currentTrack?.toGuestTrack(),
            upcoming = playback.queue.drop(playback.currentIndex + 1).take(10).map { it.toGuestTrack() },
            pending = synchronized(pending) { pending.size },
        )
    }

    override fun requestTrack(token: String, songId: String): GuestRequestResult {
        val session = validSession(token)
            ?: return GuestRequestResult(false, "Session expired. Rejoin with the code on the TV.")
        val now = System.currentTimeMillis()
        synchronized(session.requestTimes) {
            session.requestTimes.removeAll { now - it > 60_000 }
            if (session.requestTimes.size >= MAX_REQUESTS_PER_MINUTE) {
                return GuestRequestResult(false, "Slow down: 5 requests per minute.")
            }
            session.requestTimes.add(now)
        }
        if (synchronized(pending) { pending.size } >= MAX_PENDING) {
            return GuestRequestResult(false, "Too many pending requests.")
        }

        val known = session.lastResults.firstOrNull { it.id == songId }
        val track = known ?: when (val result = runBlocking { musicRepository.getSong(songId) }) {
            is DataResult.Success -> result.data
            is DataResult.Failure -> return GuestRequestResult(false, "That song is not available")
        }

        val request = PartyRequest(
            id = UUID.randomUUID().toString(),
            guestName = session.guest.name,
            track = track,
            requestedAt = now,
        )
        synchronized(pending) { pending.add(request) }
        publishPending()
        return GuestRequestResult(true, "Request sent. The host will approve it on the TV.")
    }

    private fun validSession(token: String): GuestSession? {
        val session = sessions[token] ?: return null
        if (session.expiresAt <= System.currentTimeMillis()) {
            sessions.remove(token)
            publishGuests()
            return null
        }
        return session
    }

    private fun Track.toGuestTrack() = GuestTrack(
        id = id,
        title = title,
        artist = artistName,
        album = albumName,
    )

    private fun guestUrl(port: Int): String? {
        val address = localIpv4Address() ?: return null
        return "http://$address:$port/"
    }

    private fun localIpv4Address(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces()
            .toList()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress }
            ?.hostAddress
    }.getOrNull()

    private companion object {
        const val DEFAULT_PORT = 8765
        const val TOKEN_TTL_MS = 2 * 60 * 60 * 1000L
        const val PENDING_TTL_MS = 30 * 60 * 1000L
        const val MAX_REQUESTS_PER_MINUTE = 5
        const val MAX_SEARCHES_PER_MINUTE = 20
        const val MAX_PENDING = 20
    }
}
