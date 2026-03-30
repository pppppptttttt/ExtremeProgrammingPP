package e2e

import java.io.File
import java.nio.charset.StandardCharsets
import java.net.ServerSocket
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Tag

/**
 * Два процесса **installDist**-бинарника (без вложенного Gradle).
 * Запуск: `./gradlew e2eTest` (задача зависит от `installDist` и выставляет `e2e.binary`).
 */
@Tag("e2e")
class CliTwoProcessE2ETest {

    @Test
    fun serverAndClientExchangeOneMessage() {
        runBlocking {
            withTimeout(120_000) {
                val port = ServerSocket(0).use { it.localPort }
                val projectDir = File(System.getProperty("user.dir"))
                val binary = resolveAppBinary()

                assertTrue(
                    binary.isFile,
                    "Нет бинарника: ${binary.absolutePath}. Выполни: ./gradlew installDist или ./gradlew e2eTest",
                )

                val serverLog = StringBuilder()
                val clientLog = StringBuilder()

                val serverProcess = startApp(
                    binary = binary,
                    workingDir = projectDir,
                    args = listOf("--name", "E2ESrv", "--listen-port", "$port"),
                )
                val serverReader = drainUtf8(serverProcess, serverLog)

                try {
                    assertTrue(
                        waitForSubstring(serverLog, "listen on port $port", 45_000),
                        "server did not start; output:\n${serverLogSnapshot(serverLog)}",
                    )

                    val clientProcess = startApp(
                        binary = binary,
                        workingDir = projectDir,
                        args = listOf(
                            "--name",
                            "E2ECli",
                            "--peer-host",
                            "127.0.0.1",
                            "--peer-port",
                            "$port",
                        ),
                    )
                    val clientReader = drainUtf8(clientProcess, clientLog)

                    try {
                        assertTrue(
                            waitForSubstring(clientLog, "P2P chat started", 30_000),
                            "client did not start; output:\n${clientLogSnapshot(clientLog)}",
                        )

                        clientProcess.outputStream.writer(StandardCharsets.UTF_8).buffered().apply {
                            write("e2e-ping\n")
                            flush()
                        }

                        assertTrue(
                            waitForSubstring(serverLog, "E2ECli", 20_000) &&
                                waitForSubstring(serverLog, "e2e-ping", 20_000),
                            "server did not show client message; server:\n${serverLogSnapshot(serverLog)}",
                        )

                        clientProcess.outputStream.writer(StandardCharsets.UTF_8).buffered().apply {
                            write("/exit\n")
                            flush()
                        }

                        clientProcess.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)

                        serverProcess.outputStream.writer(StandardCharsets.UTF_8).buffered().apply {
                            write("/exit\n")
                            flush()
                        }

                        serverProcess.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)
                    } finally {
                        clientReader.interrupt()
                        destroyProcess(clientProcess)
                    }
                } finally {
                    serverReader.interrupt()
                    destroyProcess(serverProcess)
                }
            }
        }
    }

    private fun resolveAppBinary(): File {
        System.getProperty("e2e.binary")?.let { return File(it) }
        val projectDir = File(System.getProperty("user.dir"))
        val win = System.getProperty("os.name").lowercase().contains("windows")
        val name = if (win) "ExtremeProgrammingPP.bat" else "ExtremeProgrammingPP"
        return File(projectDir, "build/install/ExtremeProgrammingPP/bin/$name")
    }

    private fun startApp(binary: File, workingDir: File, args: List<String>): Process {
        val cmd = ArrayList<String>(args.size + 1)
        cmd.add(binary.absolutePath)
        cmd.addAll(args)
        val pb = ProcessBuilder(cmd)
        pb.directory(workingDir)
        pb.redirectErrorStream(true)
        System.getProperty("java.home")?.let { pb.environment()["JAVA_HOME"] = it }
        return pb.start()
    }

    private fun drainUtf8(process: Process, sink: StringBuilder): Thread =
        thread(name = "e2e-drain-${System.identityHashCode(process)}") {
            try {
                process.inputStream.bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    val buf = CharArray(1024)
                    while (true) {
                        val n = reader.read(buf)
                        if (n == -1) break
                        synchronized(sink) { sink.append(buf, 0, n) }
                    }
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

    private fun waitForSubstring(sink: StringBuilder, needle: String, timeoutMs: Long): Boolean {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            synchronized(sink) {
                if (sink.contains(needle)) return true
            }
            Thread.sleep(150)
        }
        return false
    }

    private fun serverLogSnapshot(sink: StringBuilder): String = synchronized(sink) { sink.toString() }

    private fun clientLogSnapshot(sink: StringBuilder): String = synchronized(sink) { sink.toString() }

    private fun destroyProcess(p: Process) {
        if (!p.isAlive) return
        p.destroy()
        if (!p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
            p.destroyForcibly()
            p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)
        }
    }
}
