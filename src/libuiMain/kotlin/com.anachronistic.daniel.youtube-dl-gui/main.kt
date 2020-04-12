import libui.ktx.*
import libui.ktx.draw.*
import platform.posix.open

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
                        platform.posix.system("youtube-dl -U -q")
                    }
                }
                textfield {
                    stretchy = true
                }.disable()
                button("Download") {
                    action {
                        var command = "${dlLocation.take(2)} && cd ${dlLocation}"
                        var links = scroll.value.split("\n") as MutableList<String>
                        links.removeAll { it == "" }
                        for (link in links) {
                            command += " && youtube-dl ${link} -x --audio-format mp3"
                        }
                        platform.posix.system(command)
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
