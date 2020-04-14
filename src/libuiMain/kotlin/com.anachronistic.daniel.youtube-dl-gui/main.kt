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
    lateinit var keepVideo: Checkbox

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
                        val currentDir = ByteArray(1024).usePinned {
                            platform.posix.getcwd(it.addressOf(0), 1024)
                        }!!.toKString()
                        run("\"$currentDir\"\\youtube-dl -U -q")
                    }
                }
                textfield {
                    stretchy = true
                }.disable()
                keepVideo = checkbox("Keep video")
                button("Download") {
                    action {
                        if (scroll.value != "") {
                            if (!scroll.value.contains("\n")) { scroll.append("\n") }

                            val currentDir = ByteArray(1024).usePinned {
                                platform.posix.getcwd(it.addressOf(0), 1024)
                            }!!.toKString()
                            val changeDrv = "${dlLocation.take(2)}"
                            val changeDir = " && cd $dlLocation"

                            var command = changeDrv + changeDir
                            var links = scroll.value.split("\n") as MutableList<String>
                            links.removeAll { it == "" }
                            for (link in links) {
                                command += " && \"$currentDir\"\\youtube-dl.exe $link ${if (!keepVideo.value) "-x --audio-format mp3" else ""} --ffmpeg-location \"$currentDir\"\\ffmpeg.exe"
                            }
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

fun print(information: String) = MsgBox(
    text = "Information",
    details = "Info: $information"
)

fun run(command: String) = platform.posix.system(command)
