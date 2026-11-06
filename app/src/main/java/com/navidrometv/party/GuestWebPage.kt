package com.navidrometv.party

/**
 * The guest page is deliberately dependency-free: plain HTML/CSS/JS, small enough
 * to be served from a television, and usable in any phone browser on the LAN.
 * It never receives the Navidrome URL or credentials — only track metadata that
 * the host's own authenticated session looked up.
 */
object GuestWebPage {

    val HTML: String = """
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Navidrome TV · Guest Queue</title>
<style>
  :root { color-scheme: dark; }
  * { box-sizing: border-box; }
  body { margin:0; font-family: -apple-system, system-ui, "Segoe UI", Roboto, sans-serif;
         background: radial-gradient(circle at 30% 0%, #2a1633 0%, #0a0a0f 55%); color:#f2f2f6; min-height:100vh; }
  header { padding:20px 18px 8px; }
  h1 { font-size:19px; margin:0; letter-spacing:-0.3px; }
  .sub { color:#9a9aaa; font-size:13px; margin-top:4px; }
  main { padding:0 18px 28px; max-width:640px; margin:0 auto; }
  .card { background:rgba(255,255,255,.06); border:1px solid rgba(255,255,255,.08);
          border-radius:16px; padding:14px; margin-top:14px; }
  input { width:100%; padding:12px 14px; border-radius:12px; border:1px solid rgba(255,255,255,.14);
          background:rgba(0,0,0,.35); color:#fff; font-size:16px; }
  button { border:0; border-radius:12px; padding:12px 16px; font-size:15px; font-weight:600;
           background:#f5334f; color:#fff; margin-top:10px; width:100%; }
  button.ghost { background:rgba(255,255,255,.1); color:#fff; }
  .row { display:flex; gap:10px; align-items:center; justify-content:space-between; padding:10px 0;
         border-bottom:1px solid rgba(255,255,255,.06); }
  .row:last-child { border-bottom:0; }
  .meta { min-width:0; }
  .title { font-size:15px; font-weight:600; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
  .artist { font-size:12px; color:#9a9aaa; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
  .add { width:auto; margin:0; padding:8px 14px; font-size:13px; flex:0 0 auto; }
  .muted { color:#9a9aaa; font-size:13px; }
  .ok { color:#6be38b; font-size:13px; margin-top:8px; }
  .err { color:#ff8a8a; font-size:13px; margin-top:8px; }
  section { display:none; }
  section.active { display:block; }
</style>
</head>
<body>
<header>
  <h1>Guest Queue</h1>
  <div class="sub">Add music to the TV. The host approves every song.</div>
</header>
<main>
  <section id="join" class="active">
    <div class="card">
      <div class="muted">Pairing code shown on the TV</div>
      <input id="code" inputmode="numeric" maxlength="4" placeholder="1234" autocomplete="off">
      <input id="name" style="margin-top:10px" maxlength="24" placeholder="Your name">
      <button id="joinBtn">Join the queue</button>
      <div id="joinMsg"></div>
    </div>
  </section>

  <section id="app">
    <div class="card">
      <div class="muted">Now playing</div>
      <div id="nowPlaying" class="title" style="margin-top:6px">—</div>
    </div>
    <div class="card">
      <input id="query" placeholder="Search songs, artists, albums" autocomplete="off">
      <button id="searchBtn">Search</button>
      <div id="results"></div>
    </div>
    <div class="card">
      <div class="muted">Up next</div>
      <div id="upNext" class="muted" style="margin-top:6px">Nothing queued yet</div>
    </div>
    <button class="ghost" id="leaveBtn" style="margin-top:14px">Leave</button>
  </section>
</main>
<script>
(function () {
  var token = null;
  var $ = function (id) { return document.getElementById(id); };

  function post(path, body) {
    return fetch(path, { method: 'POST', headers: { 'Content-Type': 'text/plain' }, body: body })
      .then(function (r) { return r.json(); });
  }

  function show(id) {
    document.querySelectorAll('section').forEach(function (s) { s.classList.remove('active'); });
    $(id).classList.add('active');
  }

  function renderResults(list) {
    var container = $('results');
    container.innerHTML = '';
    if (!list.length) {
      container.innerHTML = '<div class="muted" style="margin-top:10px">No matches.</div>';
      return;
    }
    list.forEach(function (track) {
      var row = document.createElement('div');
      row.className = 'row';
      var meta = document.createElement('div');
      meta.className = 'meta';
      var title = document.createElement('div');
      title.className = 'title';
      title.textContent = track.title;
      var artist = document.createElement('div');
      artist.className = 'artist';
      artist.textContent = (track.artist || '') + (track.album ? ' · ' + track.album : '');
      meta.appendChild(title);
      meta.appendChild(artist);
      var btn = document.createElement('button');
      btn.className = 'add';
      btn.textContent = 'Request';
      btn.onclick = function () { request(track.id, btn); };
      row.appendChild(meta);
      row.appendChild(btn);
      container.appendChild(row);
    });
  }

  function request(songId, btn) {
    btn.disabled = true;
    btn.textContent = 'Sent';
    post('api/request', 'token=' + encodeURIComponent(token) + '&songId=' + encodeURIComponent(songId))
      .then(function (data) {
        if (!data.ok) { btn.textContent = 'Request'; btn.disabled = false; }
        loadQueue();
      });
  }

  function loadQueue() {
    if (!token) return;
    fetch('api/queue?token=' + encodeURIComponent(token))
      .then(function (r) { return r.json(); })
      .then(function (data) {
        if (!data || !data.ok) return;
        $('nowPlaying').textContent = data.nowPlaying
          ? data.nowPlaying.title + ' — ' + (data.nowPlaying.artist || '')
          : 'Nothing playing';
        var up = $('upNext');
        up.innerHTML = '';
        if (!data.upcoming || !data.upcoming.length) {
          up.textContent = 'Nothing queued yet';
        } else {
          data.upcoming.forEach(function (t) {
            var line = document.createElement('div');
            line.className = 'artist';
            line.textContent = t.title + ' — ' + (t.artist || '');
            up.appendChild(line);
          });
        }
      });
  }

  $('joinBtn').onclick = function () {
    var code = $('code').value.trim();
    var name = $('name').value.trim();
    post('api/join', 'code=' + encodeURIComponent(code) + '&name=' + encodeURIComponent(name))
      .then(function (data) {
        if (data.ok) {
          token = data.token;
          show('app');
          loadQueue();
        } else {
          $('joinMsg').className = 'err';
          $('joinMsg').textContent = data.message || 'Could not join.';
        }
      });
  };

  $('searchBtn').onclick = function () {
    var q = $('query').value.trim();
    if (q.length < 2) return;
    fetch('api/search?token=' + encodeURIComponent(token) + '&q=' + encodeURIComponent(q))
      .then(function (r) { return r.json(); })
      .then(function (data) { renderResults((data && data.tracks) || []); });
  };

  $('leaveBtn').onclick = function () {
    token = null;
    $('results').innerHTML = '';
    $('query').value = '';
    show('join');
  };

  setInterval(loadQueue, 15000);
})();
</script>
</body>
</html>
""".trimIndent()
}
