fun String.printlnIt() = println(this)

fun main(vararg args: String) {
    Vcs.executeCommand(args.getOrNull(0), args.drop(1))
}
