import java.io.File

/**
 * Version Control System
 *
 * @author Matteo Zattera
 * @since 26/12/2022
 */
object Vcs {

    private const val VCS_DIR_NAME = "vcs"
    private const val COMMITS_DIR_NAME = "commits"
    private const val CONFIG_FILE_NAME = "config.txt"
    private const val INDEX_FILE_NAME = "index.txt"
    private const val LOG_FILE_NAME = "log.txt"

    init {
        File(VCS_DIR_NAME).also { if (!it.exists() || !it.isDirectory) it.mkdir() }
        File("$VCS_DIR_NAME${File.separator}$COMMITS_DIR_NAME").also { if (!it.exists() || !it.isDirectory) it.mkdir() }
    }

    private val configFile = File("$VCS_DIR_NAME${File.separator}$CONFIG_FILE_NAME").also { it.createNewFile() }
    private val indexFile = File("$VCS_DIR_NAME${File.separator}$INDEX_FILE_NAME").also { it.createNewFile() }
    private val logFile = File("$VCS_DIR_NAME${File.separator}$LOG_FILE_NAME").also { it.createNewFile() }

    /** Returns a hash value of the [files] using the SHA-256 hash function */
    private fun createHash(files: List<File>): String {
        val string = files.joinToString("") { it.name + it.readText() }
        val md = java.security.MessageDigest.getInstance("SHA-256")
        md.update(string.toByteArray())
        val digest = md.digest()
        return digest.joinToString("") { Integer.toHexString(0xFF and it.toInt()) }
    }

    /** Executes the [command] with the given [arguments], if [command] is `null` the HELP command is executed. */
    fun executeCommand(command: String?, arguments: List<String> = listOf()) {
        if (command == null)
            Command.HELP.execute(arguments)
        else
            Command.values().find { it.value == command }?.execute(arguments) ?: "'$command' is not a SVCS command.".printlnIt()
    }

    private class TooManyArgumentsException : Exception("Too many arguments for the inputted command")

    private enum class Command(val value: String, val description: String) {

        HELP("--help", "Show commands") {
            override fun execute(args: List<String>) = Command.values()
                .filter { it != HELP }
                .joinToString("\n", "These are SVCS commands:\n") { it.value.padEnd(11, ' ') + it.description }
                .printlnIt()
        },

        CONFIG("config", "Get and set a username.") {
            override fun execute(args: List<String>) = when (args.size) {
                0 -> (if (configFile.readText() == "") "Please, tell me who you are." else "The username is ${configFile.readText()}.").printlnIt()
                1 -> configFile.writeText(args[0]).also { "The username is ${args[0]}.".printlnIt() }
                else -> throw TooManyArgumentsException()
            }
        },

        ADD("add", "Add a file to the index.") {
            override fun execute(args: List<String>) {

                val trackedFiles = indexFile.readLines().mapNotNull { fileName -> File(fileName).takeIf { it.exists() } }.toMutableList()

                when (args.size) {
                    0 -> (if (trackedFiles.isEmpty()) this.description else indexFile.readLines().joinToString("\n", "Tracked files:\n")).printlnIt()
                    1 -> {
                        val file = File(args[0])
                        if (file.exists() && file.isFile) {
                            if (args[0] !in indexFile.readLines()) trackedFiles.add(File(args[0]))
                            "The file '${args[0]}' is tracked.".printlnIt()
                        } else "Can't find '${args[0]}'.".printlnIt()
                    }
                    else -> throw TooManyArgumentsException()
                }

                indexFile.writeText(trackedFiles.joinToString("\n") { it.name })
            }
        },

        LOG("log", "Show commit logs.") {
            override fun execute(args: List<String>) = when (args.size) {
                0 -> logFile.readText().ifEmpty { "No commits yet." }.printlnIt()
                else -> throw TooManyArgumentsException()
            }
        },

        COMMIT("commit", "Save changes.") {
            override fun execute(args: List<String>) {

                when (args.size) {
                    0 -> "Message was not passed.".printlnIt()
                    1 -> {
                        val trackedFiles = indexFile.readLines().mapNotNull { fileName -> File(fileName).takeIf { it.exists() } }
                        val commitHash = createHash(trackedFiles)

                        if (logFile.readText().startsWith("commit $commitHash") || indexFile.readText().isEmpty()) {
                            "Nothing to commit.".printlnIt()
                        } else {
                            val newCommitDir = File("$VCS_DIR_NAME${File.separator}$COMMITS_DIR_NAME${File.separator}$commitHash")
                            if (!newCommitDir.exists()) {
                                newCommitDir.mkdir()
                                for (file in trackedFiles) file.copyTo(File("${newCommitDir.path}${File.separator}${file.name}"))
                            }

                            val oldLogContent = logFile.readText()
                            logFile.writeText("commit $commitHash\nAuthor: ${configFile.readText()}\n${args[0]}\n\n$oldLogContent".trimEnd())

                            "Changes are committed.".printlnIt()
                        }
                    }
                    else -> throw TooManyArgumentsException()
                }
            }
        },

        CHECKOUT("checkout", "Restore a file.") {
            override fun execute(args: List<String>) = when(args.size) {
                0 -> "Commit id was not passed.".printlnIt()
                1 -> {
                    val commitDir = File("$VCS_DIR_NAME${File.separator}$COMMITS_DIR_NAME${File.separator}${args[0]}")
                    if (!commitDir.exists() || commitDir.isFile) {
                        "Commit does not exist.".printlnIt()
                    } else {
                        for (file in commitDir.listFiles()!!) file.copyTo(File(file.name), overwrite = true)
                        "Switched to commit ${args[0]}.".printlnIt()
                    }
                }
                else -> throw TooManyArgumentsException()
            }
        };

        abstract fun execute(args: List<String>)
    }
}
