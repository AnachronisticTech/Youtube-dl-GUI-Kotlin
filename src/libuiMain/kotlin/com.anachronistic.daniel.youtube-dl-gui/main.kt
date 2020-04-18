import libui.ktx.*
import platform.posix.*
import kotlinx.cinterop.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json

@Serializable
data class Settings(
    var ignoreErrors: Boolean = false,
    var noPlaylist: Boolean = false,
    var noPartFiles: Boolean = false,
    var audioFormat: Int = 0,
    var keepVideo: Boolean = false,
    var filenameTemplate: String = "%(title)s-%(id)s.%(ext)s",
    var username: String = "",
    var password: String = ""
)

enum class AudioFormat {
    None, Best, AAC, FLAC, MP3, M4A, Opus, Vorbis, Wav;
}

var settings = Settings()
val delimiter = when (Platform.osFamily) {
    OsFamily.WINDOWS -> "\\"
    else -> "/"
}

@UnstableDefault
fun main() = appWindow(
    title = "Youtube-DL GUI",
    width = 550,
    height = 350
) {
    val file = fopen("config.txt", "r")
    if (file != null) {
        memScoped {
            val bufferLength = 64 * 1024
            val buffer = allocArray<ByteVar>(bufferLength)
            val line = fgets(buffer, bufferLength, file)!!.toKString()
            settings = Json.parse(Settings.serializer(), line)
        }
    }
    tabpane {
        page("Links") {
            linksPage()
        }
        page("Advanced settings") {
            settingsPage()
        }
    }
}

fun TabPane.Page.linksPage() = vbox {
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
                            var command = "$path $link -o \"$dlLocation$delimiter${settings.filenameTemplate}\""
                            if (settings.ignoreErrors) command += " -i"
                            if (settings.noPlaylist) command += " --no-playlist"
                            if (settings.noPartFiles) command += " --no-part"
                            if (settings.audioFormat != 0) {
                                command += " -x --audio-format ${AudioFormat.values()[settings.audioFormat].name.toLowerCase()}"
                                if (settings.keepVideo) command += " -k"
                            }
                            if (settings.username != "" && settings.password != "") {
                                command += " -u ${settings.username} -p ${settings.password}"
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

@UnstableDefault
fun TabPane.Page.settingsPage() = vbox {
    group("Options").hbox {
        checkbox("Ignore errors") {
            value = settings.ignoreErrors
            action {
                settings.ignoreErrors = this.value
            }
        }
        checkbox("No playlist") {
            value = settings.noPlaylist
            action {
                settings.noPlaylist = this.value
            }
        }
        checkbox("No part files") {
            settings.noPartFiles
            action {
                settings.noPartFiles = this.value
            }
        }
    }
    group("Extract Audio").hbox {
        slider(0, 8) {
            val format = label("None")
            value = settings.audioFormat
            format.text = AudioFormat.values()[value].name
            action {
                format.text = AudioFormat.values()[this.value].name
                settings.audioFormat = this.value
            }
        }
        checkbox("Keep video") {
            value = settings.keepVideo
            action {
                settings.keepVideo = this.value
            }
        }
    }
    group("Filename Template").hbox {
        textfield {
            stretchy = true
            value = settings.filenameTemplate
            action {
                settings.filenameTemplate = this.value
            }
        }
    }
    group("Authentication (ignored if empty)").vbox {
        textfield {
            label("Username")
            value = settings.username
            action {
                settings.username = this.value
            }
        }
        passwordfield {
            label("Password")
            value = settings.password
            action {
                settings.password = this.value
            }
        }
    }
    hbox {
        label("") {
            stretchy = true
        }
        button("Save settings as defaults") {
            action {
                memScoped {
                    val jsonData = Json.stringify(Settings.serializer(), settings).cstr
                    val file = fopen("config.txt", "w")
                    fwrite(jsonData, 1u, jsonData.size.toUInt(), file) // use .toULong() on macOS
                    fclose(file)
                }
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
    return if (system("youtube-dl --version") != 0) {
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
    getcwd(it.addressOf(0), 1024) // use 1024u on macOS
}!!.toKString()
