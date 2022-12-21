package sample

class HelloLinking(
    var s: String,
    var i: Int
)

fun cobolEntry(options: HelloLinking) {
    action(options)
}

internal fun action(options: HelloLinking) {
    options.s = options.s + options.s
    options.i = options.i + options.i
    println("${options.s} ${options.i}")
}
