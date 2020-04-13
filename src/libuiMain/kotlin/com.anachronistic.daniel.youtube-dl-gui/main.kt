import libui.ktx.*
import libui.ktx.draw.*
import platform.posix.*
import kotlinx.cinterop.*

fun main() = appWindow(
    title = "Youtube-DL GUI",
    width = 480,
    height = 250
) {
    lateinit var scroll: TextArea
    var dlLocation: String = "C:\\Users\\myUsername\\Videos"
    lateinit var dlLocationLabel: Label

    vbox {
        scroll = textarea {
            label("Links")
            stretchy = true
        }
        separator()
        vbox {
            hbox {
                label("Download location:")
                dlLocationLabel = label(dlLocation)
            }
            hbox {
                button("Set download location") {
                    action {
                        dlLocation = OpenFolderDialog() ?: dlLocation
                        dlLocationLabel.text = dlLocation
                    }
                }
                button("Update") {
                    action {
                        run("\"%cd%\"\\youtube-dl -U -q")
                    }
                }
                textfield {
                    stretchy = true
                }.disable()
                button("Download") {
                    action {
                        if (scroll.value != "") {
                            if (!scroll.value.contains("\n")) { scroll.append("\n") }

                            val copyYDL = "copy \"%cd%\"\\youtube-dl.exe ${dlLocation}\\youtube-dl.exe"
                            val copyFFM = " && copy \"%cd%\"\\ffmpeg.exe ${dlLocation}\\ffmpeg.exe"
                            val changeDrv = " && ${dlLocation.take(2)}"
                            val changeDir = " && cd ${dlLocation}"
                            val delYDL = " && del ${dlLocation}\\youtube-dl.exe"
                            val delFFM = " && del ${dlLocation}\\ffmpeg.exe"

                            var command = copyYDL + copyFFM + changeDrv + changeDir
                            var links = scroll.value.split("\n") as MutableList<String>
                            links.removeAll { it == "" }
                            for (link in links) {
                                command += " && \"%cd%\"\\youtube-dl.exe ${link} ${if (true) "-x --audio-format mp3" else ""} --ffmpeg-location \"%cd%\"\\ffmpeg.exe"
                            }
                            command += delYDL + delFFM
                            run(command)
                        }
                    }
                }
            }
        }
        stretchy = true
    }
}

fun notYetImplemented() = MsgBoxError(
    text = "This feature does not yet exist.",
    details = "Please be patient."
)

fun run(command: String) = platform.posix.system(command)
