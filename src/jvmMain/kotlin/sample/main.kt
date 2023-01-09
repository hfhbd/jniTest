package sample

import kotlin.system.*

class Linking(
    var s: String,
    var i: Int
)

fun cobolEntry(options: Linking) {
    action(options)
}

private fun action(linking: Linking) {
    linking.s = linking.s + linking.s
    linking.i = linking.i + linking.i
    println("${linking.s} ${linking.i}")
    exitProcess(4)
}

fun main(vararg arg: String) {
    val linking = Linking(
        s = arg[0],
        i = arg[1].toInt(),
    )
    action(linking)
    
    val helloLinking = HelloLinking(
        s = linking.s,
        i = linking.i
    )
    action(helloLinking)
    linking.s = helloLinking.s
    linking.i = helloLinking.i
}
