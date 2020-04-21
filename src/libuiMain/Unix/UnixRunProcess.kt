import libui.ktx.*

actual class RunProcess() {
    actual fun run() {
        MsgBox(
            text = "Unix Run",
            details = "Running a unix process"
        )
    }
}