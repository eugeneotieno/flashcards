package flashcards

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*

private val termsList = mutableListOf<String>()
private val definitionList = mutableListOf<String>()
private val mistakeList = mutableListOf<Int>()
private val logList = mutableListOf<String>()
private var exportFile = ""

fun main(args: Array<String>) {
    val strCommands = "Input the action (add, remove, import, export, ask, exit, log, hardest card, reset stats):"
    val errorAsk = { output("No questions stored to ask.\n") }
    var exit = false

    if (args.isNotEmpty()) handleCommandLine(args)
    do {
        when (getString(strCommands).lowercase(Locale.getDefault())) {
            "add" -> getCard()
            "remove" -> removeCard()
            "import" -> import()
            "export" -> export()
            "ask" -> if (termsList.isEmpty()) errorAsk() else ask()
            "exit" -> exit = true
            "log" -> export(true)
            "hardest card" -> hardestCard()
            "reset stats" -> resetStats()
        }
    } while (!exit)
    println("Bye Bye!")
    if (exportFile.isNotEmpty()) export(file = exportFile)
}

private fun handleCommandLine(commands: Array<String>) {
    try {
        val commandsMap = mutableMapOf(commands[0] to commands[1])
        if (commands.size == 4) commandsMap[commands[2]] = commands[3]
        for ((command, file) in commandsMap) {
            when (command) {
                "-import" -> import(file)
                "-export" -> exportFile = file
            }
        }
    } catch (e: Exception) {
        output("There was an error involved in your command line import/export of a file, please try again.\n")
    }
}

private fun getCard() {
    val strTermExists = { term: String -> "The card \"$term\" already exists.\n" }
    val strDefExists = { def: String -> "The definition \"$def\" already exists.\n" }
    val term = getString("The card:")
    if (termsList.contains(term)) {
        output(strTermExists(term))
        return
    }
    val definition = getString("The definition of the card:")
    if (definitionList.contains(definition)) {
        output(strDefExists(definition))
        return
    }

    addCard(term, definition, 0)
    output("The pair (\"$term\":\"$definition\") has been added.\n")
}

private fun removeCard() {
    val strRemoved = "The card has been removed.\n"
    val term = getString("Which card:")
    val strNotFound = "Can't remove \"$term\": there is no such card.\n"

    output(
        if (termsList.contains(term)) {
            val index = termsList.indexOf(term)
            termsList.removeAt(index)
            definitionList.removeAt(index)
            mistakeList.removeAt(index)
            strRemoved
        } else strNotFound
    )
}

private fun import(file: String? = null) {
    try {
        val myFile = File(file ?: getFileName())
        var count = 0
        var list: List<String>

        myFile.forEachLine {
            count++
            list = it.split("@:#:%")
            if (termsList.contains(list[0])) {
                overWriteCard(list[0], list[1], list[2].toInt())
            } else addCard(list[0], list[1], list[2].toInt())
        }
        output("$count cards have been loaded\n")
    } catch (e: FileNotFoundException) {
        output("File not found.\n")
    } catch (e: Exception) {
        output("There was an error in loading your file. Please ensure it is not open in another program.\n")
    }
}

private fun export(log: Boolean = false, file: String? = null) {
    try {
        val myFile = File(file ?: getFileName())
        val strExport = { num: Int ->
            if (log) logList[num] else "${termsList[num]}@:#:%${definitionList[num]}@:#:%${mistakeList[num]}\n"
        }
        val strOutput = if (log) "The log has been saved.\n" else "${termsList.size} cards have been saved.\n"

        myFile.writeText("")
        for (num in (if (log) logList.indices else termsList.indices)) myFile.appendText(strExport(num))
        output(strOutput)
    } catch (e: IOException) {
        output(
            "Access was denied in writing to the file. Please ensure it is not set to read only, or open in " +
                    "another program.\n"
        )
    } catch (e: Exception) {
        output("There was an error in writing your file, please try again.\n")
    }
}

private fun ask() {
    val tries = getNum("How many times to ask?")
    val strGetDef = { term: String -> "Print the definition of \"$term\":" }
    val strCorrect = "Correct!"
    val strWrong = { definition: String -> "Wrong. The right answer is \"$definition\"" }
    val strWrongTerm = { term: String -> ", but your definition is correct for \"$term\"." }
    var lastIndex = -1
    val random = {
        var num = lastIndex
        if (termsList.size > 1) while (num == lastIndex) num = (0..termsList.lastIndex).random() else num = 0
        lastIndex = num
        num
    }

    repeat(tries) {
        var index = random()
        val answer = getString(strGetDef(termsList[index]))
        val definition = definitionList[index]

        output(
            when {
                answer == definition -> strCorrect
                definitionList.contains(answer) -> {
                    mistakeList[index] += 1
                    index = definitionList.indexOf(answer)
                    strWrong(definition) + strWrongTerm(termsList[index])
                }
                else -> {
                    mistakeList[index] += 1
                    "${strWrong(definition)}."
                }
            }
        )
    }
    output("")
}

private fun hardestCard() {
    if (mistakeList.all { (it == 0) }) output("There are no cards with errors.\n") else {
        val lrgNum = mistakeList.maxOrNull()!!
        val indexes = mutableListOf<Int>()

        for (num in mistakeList.indices) if (mistakeList[num] == lrgNum) indexes.add(num)
        if (indexes.size == 1) {
            output("The hardest card is \"${termsList[indexes[0]]}\". You have $lrgNum errors answering it.\n")
        } else {
            var combo = ""

            for (i in indexes.indices) combo += "\"${termsList[indexes[i]]}\"" + if (i != indexes.lastIndex) ", " else ""
            output("The hardest cards are $combo. You have $lrgNum errors answering them.\n")
        }
    }
}

private fun resetStats() {
    for (i in mistakeList.indices) mistakeList[i] = 0
    output("Card statistics have been reset.\n")
}

private fun addCard(term: String, definition: String, mistakes: Int) {
    termsList.add(term)
    definitionList.add(definition)
    mistakeList.add(mistakes)
}

private fun overWriteCard(term: String, definition: String, mistakes: Int) {
    val index = termsList.indexOf(term)
    definitionList[index] = definition
    mistakeList[index] = mistakes
}

private fun getFileName() = getString("File name:")

private fun output(string: String) {
    logList.add("$string\n")
    println(string)
}

private fun IntRange.random() = Random().nextInt(endInclusive + 1 - start) + start

private fun getNum(text: String, defaultMessage: Boolean = false): Int {
    val strErrorNum = " was not a number, please try again: "
    var num = text
    var default = defaultMessage

    do {
        num = getString(if (default) num + strErrorNum else num)
        if (!default) default = true
    } while (!isNumber(num))

    return num.toInt()
}

private fun getString(text: String): String {
    output(text)
    val hold = readln()
    logList.add("$hold\n")
    return hold
}

private fun isNumber(number: String) = number.toIntOrNull() != null
