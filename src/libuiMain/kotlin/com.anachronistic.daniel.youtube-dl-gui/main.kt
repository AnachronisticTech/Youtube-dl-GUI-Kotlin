import libui.ktx.*
import platform.posix.*
import kotlinx.cinterop.*

var ignoreErrors = false
var noPlaylist = false
var noPartFiles = false
var audioFormat = "none"
var keepVideo = false
var filenameTemplate = "%(title)s-%(id)s.%(ext)s"
var username = ""
var password = ""

fun main() = appWindow(
    title = "Youtube-DL GUI",
    width = 550,
    height = 350
) {
    tabpane {
        page("Links") {
            links()
        }
        page("Advanced settings") {
            settings()
        }
    }

}

fun TabPane.Page.links() = vbox {
    var dlLocation = currentLocation()
    lateinit var dlLocationField: TextField

    val scroll: TextArea = textarea {
        label("Insert links here:")
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
            button("Download") {
                action {
                    if (scroll.value != "") {
                        if (!scroll.value.contains("\n")) { scroll.append("\n") }
                        val path = ydlLocation()

                        val links = scroll.value.lines() as MutableList<String>
                        links.removeAll { it == "" || it == "\n" }
                        for (link in links) {
                            var command = "$path $link -o \"$dlLocation\"\\$filenameTemplate"
                            if (ignoreErrors) command += " -i"
                            if (noPlaylist) command += " --no-playlist"
                            if (noPartFiles) command += " --no-part"
                            if (audioFormat != "none") {
                                command += " -x --audio-format $audioFormat"
                                if (keepVideo) command += " -k"
                            }
                            if (username != "" && password != "") {
                                command += " -u $username -p $password"
                            }
                            system(command)
                        }
                    }
                }
            }
        }
    }
    stretchy = true
}

fun TabPane.Page.settings() = vbox {
    group("Options").hbox {
        checkbox("Ignore errors") {
            action {
                ignoreErrors = this.value
            }
        }
        checkbox("No playlist") {
            action {
                noPlaylist = this.value
            }
        }
        checkbox("No part files") {
            action {
                noPartFiles = this.value
            }
        }
    }
    group("Extract Audio").hbox {
        slider(0, 8) {
            val format = label("None")
            action {
                format.text = when (this.value) {
                    1 -> "Best"
                    2 -> "AAC"
                    3 -> "FLAC"
                    4 -> "MP3"
                    5 -> "M4A"
                    6 -> "Opus"
                    7 -> "Vorbis"
                    8 -> "wav"
                    else -> "None"
                }
                audioFormat = format.text.toLowerCase()
            }
        }
        checkbox("Keep video") {
            action {
                keepVideo = this.value
            }
        }
    }
    group("Filename Template").hbox {
        textfield {
            stretchy = true
            value = "%(title)s-%(id)s.%(ext)s"
            action {
                filenameTemplate = this.value
            }
        }
    }
    group("Authentication (ignored if empty)").vbox {
        textfield {
            label("Username")
            action {
                username = this.value
            }
        }
        passwordfield {
            label("Password")
            action {
                password = this.value
            }
        }
    }
    hbox {
        label("") {
            stretchy = true
        }
        button("Set default settings") {
            action {
                notYetImplemented()
            }
        }
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
