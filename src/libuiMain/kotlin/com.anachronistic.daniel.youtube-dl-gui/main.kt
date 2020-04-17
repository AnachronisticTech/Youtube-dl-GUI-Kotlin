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
                        val path = ydlLocation()
                        system("\"$path\" -U -q")
                    }
                }
                label("") {
                    stretchy = true
                }
                keepVideo = checkbox("Keep video")
                button("Download") {
                    action {
                        if (scroll.value != "") {
                            val path = ydlLocation()

                            val links = scroll.value.lines() as MutableList<String>
                            links.removeAll { it == "" || it == "\n" }
                            for (link in links) {
                                system("$path $link ${if (!keepVideo.value) "-x --audio-format mp3" else ""} -o \"$dlLocation\"\\%(title)s-%(id)s.%(ext)s")
                            }
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

fun ydlLocation(): String {
    return if (system("youtube-dl --help") != 0) {
        val localCopy = "\"${currentLocation()}\"\\youtube-dl.exe"
        if (system("$localCopy --help") != 0) {
            MsgBoxError(
                text = "Youtube-dl not found",
                details = "You can download it from https://github.com/ytdl-org/youtube-dl/releases."
            )
            exit(1)
        }
        localCopy
    } else {
        "youtube-dl"
    }
}

fun currentLocation(): String = ByteArray(1024).usePinned {
    getcwd(it.addressOf(0), 1024)
}!!.toKString()
