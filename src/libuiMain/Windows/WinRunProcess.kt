import kotlinx.cinterop.*
import platform.windows.*

actual class RunProcess {
    actual fun run(command: String): Int {
        memScoped {
            val si: STARTUPINFOA = alloc()
            val pi: PROCESS_INFORMATION = alloc()
            si.cb = sizeOf<STARTUPINFOA>().convert()

            if (CreateProcessA(
                null,
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
                return -1
            } else {
                WaitForSingleObject(pi.hProcess, INFINITE)
                val exitCode: DWORDVar = alloc()
                val result = GetExitCodeProcess(pi.hProcess, exitCode.ptr).convert<Int>()
                CloseHandle(pi.hProcess)
                CloseHandle(pi.hThread)

                return if (result == 0) { // If result is 0, GetExitCodeProcess failed
                    -1
                } else {
                    exitCode.value.convert()
                }
            }
        }
    }
}