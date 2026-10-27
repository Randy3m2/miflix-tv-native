package com.brunio.desktop

import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Private child-process IPC. No listening port, shell command interpolation, or token logging. */
private class BridgeFailure(message:String):IllegalStateException(message)

class BridgeClient : Closeable {
    private val executor = Executors.newSingleThreadExecutor { task -> Thread(task, "brunio-ipc").apply { isDaemon = true } }
    private var process: Process? = null
    private var input: java.io.BufferedReader? = null
    private var output: java.io.BufferedWriter? = null
    private var sequence = 0L
    @Volatile private var closed = false

    private fun start() {
        check(!closed) { "La conexión se cerró" }
        if (process != null) return
        val packaged=System.getProperty("compose.application.resources.dir")?.let{Path.of(it,"bridge","BruniO-service.exe")}
        if(packaged!=null && Files.isRegularFile(packaged)) {
            process=ProcessBuilder(packaged.toAbsolutePath().toString()).redirectError(ProcessBuilder.Redirect.DISCARD).start()
            input=process!!.inputStream.bufferedReader(Charsets.UTF_8);output=process!!.outputStream.bufferedWriter(Charsets.UTF_8)
            return
        }
        val configured = System.getenv("BRUNIO_BRIDGE")?.let { Path.of(it) }
        val path = configured ?: sequenceOf(
            Path.of("..", "backend", "bridge.py"),
            Path.of("desktop", "brunio-pc-next", "backend", "bridge.py")
        ).firstOrNull { Files.isRegularFile(it) }
        require(path != null && Files.isRegularFile(path)) { "No se encontró el servicio local de BruniO" }
        val python = System.getenv("BRUNIO_PYTHON") ?: "python"
        process = ProcessBuilder(python, "-u", path.toAbsolutePath().toString())
            .redirectError(ProcessBuilder.Redirect.DISCARD).start()
        input = process!!.inputStream.bufferedReader(Charsets.UTF_8)
        output = process!!.outputStream.bufferedWriter(Charsets.UTF_8)
    }

    suspend fun call(method: String, params: JSONObject = JSONObject()): Any = withContext(Dispatchers.IO) {
        check(!closed) { "La conexión se cerró" }
        val future = executor.submit<Any> {
            start()
            val id = ++sequence
            output!!.write(JSONObject().put("id", id).put("method", method).put("params", params).toString())
            output!!.newLine(); output!!.flush()
            val line = StringBuilder()
            while (true) {
                val c = input!!.read()
                check(c >= 0) { "El servicio local se detuvo" }
                if (c == 10) break
                check(line.length < 16 * 1024 * 1024) { "La respuesta supera el límite" }
                line.append(c.toChar())
            }
            val response = JSONObject(line.toString())
            check(response.optLong("id") == id) { "Respuesta fuera de secuencia" }
            if(response.has("error"))throw BridgeFailure(when(response.optString("code")) {
                "auth.invalid"->"Revisa el correo, la contraseña y la confirmación de tu cuenta."
                "setup.tmdb"->"Configura el token de TMDB en Ajustes para cargar el catálogo."
                "setup.addons"->"Agrega tus complementos en Ajustes para buscar fuentes."
                "scope.changed"->"El perfil cambió. Vuelve a abrir esta pantalla."
                "network.unavailable"->"No hay conexión con el servicio. Comprueba tu conexión e inténtalo de nuevo."
                else->"No se pudo completar la operación. Comprueba la conexión y la configuración de tu cuenta."
            })
            response.opt("result") ?: JSONObject.NULL
        }
        try { future.get(120, TimeUnit.SECONDS) }
        catch (e: Exception) {
            if(e is java.util.concurrent.ExecutionException && e.cause is BridgeFailure)throw e.cause as BridgeFailure
            if (e is java.util.concurrent.TimeoutException) close()
            throw IllegalStateException(if (e is java.util.concurrent.TimeoutException) "El servicio local no respondió a tiempo" else "No se pudo completar la operación local", e)
        }
    }

    override fun close() {
        closed = true
        process?.destroyForcibly()
        executor.shutdownNow()
    }
}
