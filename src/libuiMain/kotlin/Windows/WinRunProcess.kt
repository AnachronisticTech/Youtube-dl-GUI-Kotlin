import libui.ktx.*

class WinRunProcess: RunProcess() {
    override fun run() {
        MsgBox(
            text = "Windows Run",
            details = "Running a windows process"
        )
    }
}