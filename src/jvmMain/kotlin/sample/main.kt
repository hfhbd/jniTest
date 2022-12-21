package sample

class Options(
    var s: String,
    var i: Int
)

fun jclEntry(options: Options) {
    action(options)
    println(options)
}

private fun action(options: Options) {
    options.s = options.s + options.s
    options.i = options.i + options.i
    println(options)
}

fun main(vararg arg: String) {
    val options = Options(
        s = arg[0],
        i = arg[1].toInt(),
    )
    action(options)
}
