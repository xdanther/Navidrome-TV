# Compatibility and limitations

## Servers

Tested against the Subsonic API 1.16.1 contract and Navidrome's implementation of it.
Other servers (Airsonic, Gonic, Astiga, …) work to the extent they implement the same
endpoints. OpenSubsonic extensions are used opportunistically and degrade gracefully.

| Endpoint | Used for | Fallback when unsupported |
|---|---|---|
| `ping` | connection test, session verify | — |
| `getLicense` | server/plan info in onboarding | optional, ignored on failure |
| `getMusicFolders` | folder list | optional |
| `getAlbumList2` | newest, recent, frequent, random, alphabetical, starred, by year/genre | cached albums from Room |
| `getArtists` | artist index | — |
| `getArtist` | artist albums | — |
| `getArtistInfo2` | biography, similar artists | section hidden |
| `getAlbum` | album details and track list | — |
| `getSong` | single track (Party Mode requests) | — |
| `getGenres` | genre grid | empty state |
| `getStarred2` | favorites | section hidden |
| `search3` | global search, voice search | empty results |
| `getRandomSongs` | Library "Songs" tab, Home shuffle | — |
| `getLyricsBySongId` | lyrics panel | "no lyrics" message |
| `star` / `unstar` | favorites for tracks and albums | error toast |
| `scrobble` | real server scrobbles | local history still recorded |
| `getPlaylists`, `getPlaylist` | playlist screens | — |
| `createPlaylist`, `updatePlaylist`, `deletePlaylist` | playlist management | error toast |

## Features that are deliberately absent

These are not unfinished stubs: the API cannot support them truthfully, so the UI
does not pretend otherwise.

| Feature | Why it is absent |
|---|---|
| Crossfade | ExoPlayer's `setCrossfade` is for video transitions; Subsonic exposes no crossfade. |
| Gapless playback | Requires the client to prefetch and align streams; Subsonic transcoding breaks the guarantee, so no toggle is offered. |
| Loudness normalisation | Requires ReplayGain tags the Subsonic API does not return. |
| Playlist reordering | Subsonic has no move/reorder endpoint. The UI explains remove-and-re-add. |
| Complete song list | No "all songs" endpoint; `getRandomSongs` is a random sample and is labelled as such. |
| Offline downloads | Not implemented; the app streams and caches metadata only. |
| Cross-device sync of the queue | Server has no queue resource; the queue is local to the device. |
| Party Mode over the internet | The guest server binds to the LAN and has no TLS; it is not exposed publicly. |

## Lyrics

- Synchronized lyrics are shown when the server returns them and the timestamps are
  usable; the Now Playing screen highlights the current line.
- Plain lyrics are shown as static text.
- If the server does not implement `getLyricsBySongId`, the panel says so instead of
  showing placeholder text.

## Android TV behaviour

- D-pad focus is handled explicitly: rows keep a stable size, focused items scale
  slightly and get a border, and screens restore focus when returning from a detail
  page.
- The now-playing screen hides the top bar and mini player and uses Back to return.
- Media keys and system media controls work through `MediaSession`.
- Voice search from the TV launcher goes through the media session search callback
  and resolves to real server tracks; availability depends on the launcher and is not
  guaranteed on every device.
- Cleartext HTTP is allowed (network security config) because Navidrome commonly runs
  on a LAN without TLS. Use HTTPS for anything reachable from outside the LAN.

## Party Mode

- Host and guests must be on the same network; the TV advertises a URL only when it
  has a site-local IPv4 address.
- Guest tokens expire after two hours; the host can stop the session at any time,
  which revokes all tokens immediately.
- Rate limits: 20 searches and 5 requests per minute per token, at most 20 pending
  requests. Pending requests expire after 30 minutes.
- Requested songs are resolved server-side through the host's authenticated session,
  then wait for approval; guests cannot skip approval or change the queue directly.
