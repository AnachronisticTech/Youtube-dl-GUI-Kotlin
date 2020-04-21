import libui.ktx.*

class UnixRunProcess: RunProcess() {
    override fun run() {
        MsgBox(
            text = "Unix Run",
            details = "Running a unix process"
        )
    }
}