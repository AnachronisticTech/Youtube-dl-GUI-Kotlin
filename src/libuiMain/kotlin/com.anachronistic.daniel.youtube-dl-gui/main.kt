import libui.ktx.*
import platform.posix.*
import kotlinx.cinterop.*

fun main() = appWindow(
    title = "Youtube-DL GUI",
    width = 480,
    height = 250
) {
    lateinit var scroll: TextArea
    var dlLocation = currentLocation()
    lateinit var dlLocationField: TextField
    lateinit var keepVideo: Checkbox

    vbox {
        scroll = textarea {
            label("Links")
            stretchy = true
        }
        separator()
        vbox {
            hbox {
                dlLocationField = textfield {
                    label("Download location:")
                    stretchy = true
                    value = dlLocation
                    action {
                        dlLocation = this.value
                    }
                }
                button("Choose location") {
                    action {
                        dlLocation = OpenFolderDialog() ?: dlLocation
                        dlLocationField.value = dlLocation
                    }
                }
            }
            hbox {
                button("Update") {
                    action {
                        run("\"${currentLocation()}\"\\youtube-dl -U -q")
                    }
                }
                label("") {
                    stretchy = true
                }
                keepVideo = checkbox("Keep video")
                button("Download") {
                    action {
                        if (scroll.value != "") {
                            if (!scroll.value.contains("\n")) { scroll.append("\n") }

                            val currentDir = currentLocation()
                            val changeDrv = dlLocation.take(2)
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

fun run(command: String) = system(command)

fun currentLocation(): String = ByteArray(1024).usePinned {
    getcwd(it.addressOf(0), 1024)
}!!.toKString()
