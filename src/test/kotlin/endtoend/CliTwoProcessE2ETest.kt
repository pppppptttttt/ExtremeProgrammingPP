package endtoend

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Tag
import java.io.File
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertTrue

private const val E2E_TIMEOUT_OVERALL_MS = 120_000L
private const val E2E_WAIT_LISTEN_MS = 45_000L
private const val E2E_WAIT_CLIENT_START_MS = 30_000L
private const val E2E_WAIT_MESSAGE_MS = 20_000L
private const val E2E_PROCESS_WAIT_SEC = 30L
private const val E2E_DESTROY_WAIT_SEC = 10L
private const val E2E_DESTROY_KILL_SEC = 5L
private const val E2E_POLL_DRAIN_MS = 150L
private const val E2E_READ_BUFFER_SIZE = 1024

/**
 * Два процесса **installDist**-бинарника (без вложенного Gradle).
 * Запуск: `./gradlew e2eTest` (задача зависит от `installDist` и выставляет `e2e.binary`).
 */
@Tag("e2e")
class CliTwoProcessE2ETest {
    @Test
    fun serverAndClientExchangeOneMessage() {
        runBlocking {
            withTimeout(E2E_TIMEOUT_OVERALL_MS) {
                val port = ServerSocket(0).use { it.localPort }
                val projectDir = File(System.getProperty("user.dir"))
                val binary = resolveAppBinary()

                assertTrue(
                    binary.isFile,
                    "Нет бинарника: ${binary.absolutePath}. Выполни: ./gradlew installDist или ./gradlew e2eTest",
                )

                val serverLog = StringBuilder()
                val clientLog = StringBuilder()

                val serverProcess =
                    startApp(
                        binary = binary,
                        workingDir = projectDir,
                        args = listOf("--name", "E2ESrv", "--listen-port", "$port"),
                    )
                val serverReader = drainUtf8(serverProcess, serverLog)

                try {
                    assertTrue(
                        waitForSubstring(serverLog, "listen on port $port", E2E_WAIT_LISTEN_MS),
                        "server did not start; output:\n${serverLogSnapshot(serverLog)}",
                    )

                    val clientProcess =
                        startApp(
                            binary = binary,
                            workingDir = projectDir,
                            args =
                                listOf(
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
                            waitForSubstring(clientLog, "P2P chat started", E2E_WAIT_CLIENT_START_MS),
                            "client did not start; output:\n${clientLogSnapshot(clientLog)}",
                        )
                        exchangePingAndExit(clientProcess, serverProcess, serverLog)
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

    private fun exchangePingAndExit(
        clientProcess: Process,
        serverProcess: Process,
        serverLog: StringBuilder,
    ) {
        clientProcess.outputStream.writer(StandardCharsets.UTF_8).buffered().apply {
            write("e2e-ping\n")
            flush()
        }

        assertTrue(
            waitForSubstring(serverLog, "E2ECli", E2E_WAIT_MESSAGE_MS) &&
                waitForSubstring(serverLog, "e2e-ping", E2E_WAIT_MESSAGE_MS),
            "server did not show client message; server:\n${serverLogSnapshot(serverLog)}",
        )

        clientProcess.outputStream.writer(StandardCharsets.UTF_8).buffered().apply {
            write("/exit\n")
            flush()
        }
        clientProcess.waitFor(E2E_PROCESS_WAIT_SEC, TimeUnit.SECONDS)

        serverProcess.outputStream.writer(StandardCharsets.UTF_8).buffered().apply {
            write("/exit\n")
            flush()
        }
        serverProcess.waitFor(E2E_PROCESS_WAIT_SEC, TimeUnit.SECONDS)
    }

    private fun resolveAppBinary(): File {
        System.getProperty("e2e.binary")?.let { return File(it) }
        val projectDir = File(System.getProperty("user.dir"))
        val win = System.getProperty("os.name").lowercase().contains("windows")
        val name = if (win) "ExtremeProgrammingPP.bat" else "ExtremeProgrammingPP"
        return File(projectDir, "build/install/ExtremeProgrammingPP/bin/$name")
    }

    private fun startApp(
        binary: File,
        workingDir: File,
        args: List<String>,
    ): Process {
        val cmd = ArrayList<String>(args.size + 1)
        cmd.add(binary.absolutePath)
        cmd.addAll(args)
        val pb = ProcessBuilder(cmd)
        pb.directory(workingDir)
        pb.redirectErrorStream(true)
        System.getProperty("java.home")?.let { pb.environment()["JAVA_HOME"] = it }
        return pb.start()
    }

    private fun drainUtf8(
        process: Process,
        sink: StringBuilder,
    ): Thread =
        thread(name = "endtoend-drain-${System.identityHashCode(process)}") {
            try {
                process.inputStream.bufferedReader(StandardCharsets.UTF_8).use { reader ->
                    val buf = CharArray(E2E_READ_BUFFER_SIZE)
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

    private fun waitForSubstring(
        sink: StringBuilder,
        needle: String,
        timeoutMs: Long,
    ): Boolean {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            synchronized(sink) {
                if (sink.contains(needle)) return true
            }
            Thread.sleep(E2E_POLL_DRAIN_MS)
        }
        return false
    }

    private fun serverLogSnapshot(sink: StringBuilder): String = synchronized(sink) { sink.toString() }

    private fun clientLogSnapshot(sink: StringBuilder): String = synchronized(sink) { sink.toString() }

    private fun destroyProcess(p: Process) {
        if (!p.isAlive) return
        p.destroy()
        if (!p.waitFor(E2E_DESTROY_WAIT_SEC, TimeUnit.SECONDS)) {
            p.destroyForcibly()
            p.waitFor(E2E_DESTROY_KILL_SEC, TimeUnit.SECONDS)
        }
    }
}
