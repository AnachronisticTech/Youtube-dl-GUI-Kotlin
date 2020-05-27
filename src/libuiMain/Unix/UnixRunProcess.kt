import platform.posix.*

actual class RunProcess {
    actual fun run(command: String): Int {
//        MsgBox(
//            text = "Unix Run",
//            details = "Running a unix process"
//        )
        return system(command)
    }
}