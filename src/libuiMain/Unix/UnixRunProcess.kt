import platform.posix.*

actual class RunProcess {
    actual fun run(command: String): Int {
        return system(command)
    }
}