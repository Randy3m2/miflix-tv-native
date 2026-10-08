package com.miflix.native2.data

import com.miflix.native2.model.PairingInfo
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.Collections
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class PairingServer(
    private val onLogin: (String, String) -> Unit,
    private val onAddon: (String) -> Unit
) {
    private var server: ServerSocket? = null
    private val running = AtomicBoolean(false)
    private val pool = Executors.newCachedThreadPool()
    private val token = UUID.randomUUID().toString().replace("-", "")
    private val shortCode = token.take(6).uppercase(Locale.US)

    fun start(): PairingInfo {
        if (running.get()) return info()
        server = ServerSocket(0)
        running.set(true)
        pool.execute {
            while (running.get()) {
                try {
                    val socket = server?.accept() ?: break
                    pool.execute { handle(socket) }
                } catch (_: Throwable) {
                    if (!running.get()) break
                }
            }
        }
        return info()
    }

    fun stop() {
        running.set(false)
        runCatching { server?.close() }
        server = null
    }

    private fun info(): PairingInfo {
        val ip = localIpv4() ?: "127.0.0.1"
        val port = server?.localPort ?: 0
        return PairingInfo("http://$ip:$port/?token=$token", shortCode)
    }

    private fun handle(socket: Socket) {
        socket.use { s ->
            s.soTimeout = 8000
            val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
            val request = reader.readLine() ?: return
            val parts = request.split(" ")
            val method = parts.getOrNull(0).orEmpty()
            val target = parts.getOrNull(1).orEmpty()
            var contentLength = 0
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                if (line.startsWith("Content-Length:", true)) contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
            }
            val body = if (contentLength > 0) CharArray(contentLength).also { reader.read(it) }.concatToString() else ""
            val params = parseParams(body)
            val queryToken = target.substringAfter("token=", "").substringBefore("&")
            val suppliedToken = params["token"] ?: queryToken
            if (suppliedToken != token) {
                respond(s, 403, "<h2>Pairing session expired or invalid.</h2>")
                return
            }

            when {
                method == "POST" && target.startsWith("/login") -> {
                    val email = params["email"].orEmpty().trim()
                    val password = params["password"].orEmpty()
                    if (email.isBlank() || password.isBlank()) respond(s, 400, "<h2>Email and password are required.</h2>")
                    else {
                        onLogin(email, password)
                        respond(s, 200, successPage("Sign-in sent to MiFlix. Check your TV."))
                    }
                }
                method == "POST" && target.startsWith("/addon") -> {
                    val manifest = params["manifest"].orEmpty().trim()
                    if (!manifest.startsWith("http")) respond(s, 400, "<h2>Please paste a valid http(s) Stremio manifest URL.</h2>")
                    else {
                        onAddon(manifest)
                        respond(s, 200, successPage("Add-on sent to MiFlix. Check your TV."))
                    }
                }
                else -> respond(s, 200, pairingPage())
            }
        }
    }

    private fun pairingPage() = """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
        <title>MiFlix Pair</title><style>
        body{font-family:system-ui;background:#050505;color:#fff;margin:0;padding:28px}main{max-width:620px;margin:auto}
        .card{background:#151515;border:1px solid #303030;border-radius:20px;padding:22px;margin:18px 0}h1{font-size:30px}h2{font-size:20px}
        input,textarea{box-sizing:border-box;width:100%;padding:14px;margin:7px 0;border-radius:12px;border:1px solid #444;background:#0b0b0b;color:white;font-size:16px}
        button{width:100%;padding:14px;border:0;border-radius:999px;background:#fff;color:#000;font-weight:800;font-size:16px;margin-top:10px}
        small{color:#aaa;line-height:1.4}.code{font-size:22px;font-weight:800;letter-spacing:4px}</style></head><body><main>
        <h1>MiFlix TV Pairing</h1><p class="code">$shortCode</p><small>Keep this page open only while pairing. Your phone and TV must be on the same Wi‑Fi.</small>
        <div class="card"><h2>Sign in to MiFlix</h2><form method="post" action="/login">
        <input type="hidden" name="token" value="$token"><input name="email" type="email" placeholder="Email" required>
        <input name="password" type="password" placeholder="Password" required><button>Sign in on TV</button></form></div>
        <div class="card"><h2>Add Torrentio / Comet / Stremio add-on</h2><form method="post" action="/addon">
        <input type="hidden" name="token" value="$token"><textarea name="manifest" rows="4" placeholder="Paste the configured manifest.json URL" required></textarea>
        <button>Add to MiFlix</button></form><small>The add-on is stored as private account setup and shared across your MiFlix profiles.</small></div>
        </main></body></html>
    """.trimIndent()

    private fun successPage(message: String) = """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><style>
        body{font-family:system-ui;background:#050505;color:#fff;display:grid;place-items:center;min-height:90vh;text-align:center}div{max-width:500px;padding:30px}.ok{font-size:54px}</style></head>
        <body><div><div class="ok">✓</div><h2>$message</h2><p>You can return to the TV.</p></div></body></html>
    """.trimIndent()

    private fun parseParams(body: String): Map<String, String> = body.split("&").mapNotNull { part ->
        if (!part.contains("=")) return@mapNotNull null
        val k = URLDecoder.decode(part.substringBefore("="), "UTF-8")
        val v = URLDecoder.decode(part.substringAfter("="), "UTF-8")
        k to v
    }.toMap()

    private fun respond(socket: Socket, status: Int, html: String) {
        val bytes = html.toByteArray(Charsets.UTF_8)
        val reason = if (status == 200) "OK" else if (status == 403) "Forbidden" else "Bad Request"
        val header = "HTTP/1.1 $status $reason\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n"
        socket.getOutputStream().use { out -> out.write(header.toByteArray()); out.write(bytes); out.flush() }
    }

    private fun localIpv4(): String? {
        return Collections.list(NetworkInterface.getNetworkInterfaces()).asSequence()
            .filter { runCatching { it.isUp && !it.isLoopback }.getOrDefault(false) }
            .flatMap { Collections.list(it.inetAddresses).asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress }?.hostAddress
    }
}
