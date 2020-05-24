import kotlinx.cinterop.*
import libui.ktx.*
import platform.windows.*

actual class RunProcess {
    actual fun run(command: String): Boolean {
        memScoped {
            val si: STARTUPINFOA = alloc()
            val pi: PROCESS_INFORMATION = alloc()
            si.cb = sizeOf<STARTUPINFOA>().convert()

            if (CreateProcessA(
                "C:\\utils\\youtube-dl.exe",
                command.cstr.getPointer(this),
                null,
                null,
                0,
                (NORMAL_PRIORITY_CLASS or CREATE_NO_WINDOW).convert(),
                null,
                null,
                si.ptr,
                pi.ptr
            ).convert<Int>() == 0) { // If result is 0, CreateProcessA failed
                MsgBox(
                    text = "Windows Run Failed",
                    details = "Failed to execute command $command"
                )
                return false
            } else {
                WaitForSingleObject(pi.hProcess, INFINITE)
                val exitCode: DWORDVar = alloc()
                val result = GetExitCodeProcess(pi.hProcess, exitCode.ptr)
                CloseHandle(pi.hProcess)
                CloseHandle(pi.hThread)

                if (result.convert<Int>() == 0) { // If result is 0, GetExitCodeProcess failed
                    MsgBox(
                        text = "Windows Run Failed",
                        details = "Executed command but couldn't get exit code."
                    )
                }

                return true
            }
        }

    }
}