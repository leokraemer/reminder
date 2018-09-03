package de.leo.smartTrigger.datacollector.datacollection.sensors

/**
 * Created by Leo on 12.03.2018.
 */
data class Person(val name: String,
                  val age: Int = 0,
                  val showMsg : (msg: String) -> Unit)


fun main(args: Array<String>) {
    val p = Person("Bob", 29,  {msg -> println(msg)})
    p.showMsg("Hello, world!")
}