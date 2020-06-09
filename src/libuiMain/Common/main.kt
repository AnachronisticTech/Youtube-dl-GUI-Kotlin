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
    var password: String = "",
    var ffmpegLocation: String = ""
)

enum class AudioFormat {
    None, Best, AAC, FLAC, MP3, M4A, Opus, Vorbis, Wav;
}

enum class Location {
    PATH, DIR, SET, NONE
}

var settings = Settings()
var ffmpegLocation = Location.NONE
val delimiter = when (Platform.osFamily) {
    OsFamily.WINDOWS -> "\\"
    else -> "/"
}

val dispatcher = Dispatcher()

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
            fclose(file)
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
    lateinit var dlLocationButton: Button
    lateinit var updateButton: Button

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
            dlLocationButton = button("Choose location") {
                action {
                    dlLocation = OpenFolderDialog() ?: dlLocation
                    dlLocationField.value = dlLocation
                }
            }
        }
        hbox {
            updateButton = button("Update") {
                action {
                    val path = ydlLocation()
                    val code = RunProcess().run("\"$path\" -U -q")
                    if (code == 0) {
                        print("Update complete")
                    } else {
                        print("Update failed with code $code")
                    }
                }
            }
            val bar = progressbar { stretchy = true }
            button("Download") {
                action {
                    if (scroll.value != "") {
                        dispatcher.queueFunction {
                            this@button.disable()
                            updateButton.disable()
                            dlLocationButton.disable()
                            dlLocationField.disable()
                            bar.value = -1
                        }

                        if (!scroll.value.contains("\n")) { scroll.append("\n") }
                        val path = ydlLocation()

                        val links = scroll.value.lines() as MutableList<String>
                        links.removeAll { it == "" || it == "\n" }

                        var commandExtras = ""
                        if (settings.ignoreErrors) commandExtras += " -i"
                        if (settings.noPlaylist) commandExtras += " --no-playlist"
                        if (settings.noPartFiles) commandExtras += " --no-part"
                        if (settings.audioFormat != 0) {
                            if (ffmpegLocation()) {
                                commandExtras += " -x --audio-format ${AudioFormat.values()[settings.audioFormat].name.toLowerCase()}"
                                if (settings.keepVideo) commandExtras += " -k"
                                if (ffmpegLocation == Location.SET) commandExtras += " --ffmpeg-location \"${settings.ffmpegLocation}\""
                                if (ffmpegLocation == Location.DIR) commandExtras += " --ffmpeg-location \"${currentLocation()}${delimiter}ffmpeg.exe\""
                            } else {
                                print("FFmpeg was not found on the path, in the current directory, or in the Advanced Settings location.")
                                return@action
                            }
                        }
                        if (settings.username != "" && settings.password != "") {
                            commandExtras += " -u ${settings.username} -p ${settings.password}"
                        }

                        for (link in links) {
                            val command = "$path $link -o \"$dlLocation$delimiter${settings.filenameTemplate}\"$commandExtras"
                            dispatcher.queueFunction {
                                RunProcess().run(command)
                            }
                        }

                        dispatcher.queueFunction {
                            this@button.enable()
                            updateButton.enable()
                            dlLocationButton.enable()
                            dlLocationField.enable()
                            bar.value = 0
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
    lateinit var ffmpegLocation: TextField
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
    group("FFmpeg location").hbox {
        ffmpegLocation = textfield {
            stretchy = true
            value = settings.ffmpegLocation
            action {
                settings.ffmpegLocation = value
            }
        }
        button("Locate") {
            action {
                ffmpegLocation.value = OpenFileDialog() ?: ""
                settings.ffmpegLocation = ffmpegLocation.value
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
                    val jsonData = Json.stringify(Settings.serializer(), settings)
                    val file = fopen("config.txt", "w")
                    try {
                        fputs(jsonData, file)
                    } finally {
                        fclose(file)
                    }
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
    return if (RunProcess().run("youtube-dl --version") != 0) {
        val localCopy = "\"${currentLocation()}${delimiter}youtube-dl\""
        if (RunProcess().run("$localCopy --version") != 0) {
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

fun ffmpegLocation(): Boolean {
    fun ffmpegExists(): Boolean = when {
        RunProcess().run("\"${currentLocation()}${delimiter}ffmpeg\" -version") == 0 -> {
            ffmpegLocation = Location.DIR
            true
        }
        RunProcess().run("ffmpeg -version") == 0 -> {
            ffmpegLocation = Location.PATH
            true
        }
        else -> {
            ffmpegLocation = Location.NONE
            false
        }
    }

    return if (settings.ffmpegLocation == "") {
        ffmpegExists()
    } else {
        if (RunProcess().run("\"${settings.ffmpegLocation}\" -version") == 0) {
            ffmpegLocation = Location.SET
            true
        } else {
            ffmpegExists()
        }
    }
}

fun currentLocation(): String = ByteArray(1024).usePinned {
    getcwd(it.addressOf(0), 1024) // use 1024u on macOS
}!!.toKString()
