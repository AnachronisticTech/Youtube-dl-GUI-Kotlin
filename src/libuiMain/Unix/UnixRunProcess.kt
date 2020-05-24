import libui.ktx.*

actual class RunProcess {
    actual fun run(command: String): Boolean {
        MsgBox(
            text = "Unix Run",
            details = "Running a unix process"
        )
    }
}