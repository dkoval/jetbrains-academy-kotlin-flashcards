package flashcards

import java.io.File
import java.io.PrintWriter
import java.util.*
import kotlin.random.Random
import kotlin.system.exitProcess

private const val CLI_USAGE = "Usage: java Flashcards [-import filename] [-export filename]"
private const val SUPPORTED_ACTIONS = "(add, remove, import, export, ask, exit, log, hardest card, reset stats)"

fun main(args: Array<String>) {
    val flashcards = Flashcards(Args.parse(args))
    while (true) {
        flashcards.println("Input the action $SUPPORTED_ACTIONS:")
        val action = flashcards.readLine()!!
        when (action.toLowerCase()) {
            "add" -> flashcards.add()
            "remove" -> flashcards.remove()
            "import" -> flashcards.import()
            "export" -> flashcards.export()
            "ask" -> flashcards.ask()
            "exit" -> flashcards.exit()
            "log" -> flashcards.log()
            "hardest card" -> flashcards.hardestCard()
            "reset stats" -> flashcards.resetStats()
            else -> flashcards.help(action)
        }
        flashcards.println()
    }
}

data class Args(val importFilename: String? = null, val exportFilename: String? = null) {

    companion object {
        val EMPTY = Args()

        fun parse(args: Array<String>): Args {
            // valid number of arguments: 0, 2, 4
            if (args.size % 2 != 0 || args.size > 4) {
                println(CLI_USAGE)
                exitProcess(1)
            }

            if (args.isEmpty()) {
                return EMPTY
            }

            var importFilename: String? = null
            var exportFilename: String? = null

            for (i in 0 until args.size.coerceAtMost(4) step 2) {
                val action = args[i]
                val filename = args[i + 1]
                when (action) {
                    "-import" -> importFilename = filename
                    "-export" -> exportFilename = filename
                    else -> {
                        println("Invalid action: $action.\n$CLI_USAGE")
                        exitProcess(1)
                    }
                }
            }
            return Args(importFilename, exportFilename)
        }
    }
}

class Flashcards(private val args: Args) {
    private val cardToDefinitionMap = mutableMapOf<String, Definition>()
    private val definitionToCardMap = mutableMapOf<String, String>()
    private val consoleLog = mutableListOf<String>()

    init {
        args.importFilename?.also { filename -> doImport(filename) }
    }

    private data class Definition(val value: String, var numErrorsAnswering: Int = 0)

    fun readLine(): String? = kotlin.io.readLine()?.also { consoleLog.add(it) }

    fun println(message: String? = null) {
        if (message != null) {
            kotlin.io.println(message)
        } else {
            kotlin.io.println()
        }
        consoleLog.add(message ?: "\n")
    }

    fun add() {

        fun readUniqueKey(existingKeys: Set<String>, onNonUniqueKey: (key: String) -> Unit): String? {
            val key = readLine()!!
            val exists = existingKeys.contains(key)
            if (exists) {
                onNonUniqueKey(key)
                return null
            }
            return key
        }

        println("The card:")
        val card = readUniqueKey(cardToDefinitionMap.keys) {
            println("The card \"$it\" already exists.")
        } ?: return

        println("The definition of the card:")
        val definition = readUniqueKey(definitionToCardMap.keys) {
            println("The definition \"$it\" already exists.")
        } ?: return

        cardToDefinitionMap[card] = Definition(definition)
        definitionToCardMap[definition] = card
        println("The pair (\"$card\":\"$definition\") has been added.")
    }

    fun remove() {
        println("The card:")
        val cardToRemove = readLine()!!
        val definitionToRemove = cardToDefinitionMap.remove(cardToRemove)
        if (definitionToRemove != null) {
            definitionToCardMap.remove(definitionToRemove.value)
            println("The card has been removed.")
        } else {
            println("Can't remove \"$cardToRemove\": there is no such card.")
        }
    }

    fun import() {
        println("File name:")
        val filename = readLine()!!
        doImport(filename)
    }

    private fun doImport(filename: String) {
        val file = File(filename)
        if (!file.exists()) {
            println("File not found.")
        } else {
            Scanner(file).use { scanner ->
                val size = scanner.nextLine().toInt()
                repeat(size) {
                    val card = scanner.nextLine()
                    val definition = scanner.nextLine()
                    val numErrorsAnswering = scanner.nextLine().toInt()
                    cardToDefinitionMap[card]?.also { oldDefinition -> definitionToCardMap.remove(oldDefinition.value) }
                    cardToDefinitionMap[card] = Definition(definition, numErrorsAnswering)
                    definitionToCardMap[definition] = card
                }
                println("$size cards have been loaded.")
            }
        }
    }

    fun export() {
        println("File name:")
        val filename = readLine()!!
        doExport(filename)
    }

    private fun doExport(filename: String) {
        dumpToFile(filename) { writer ->
            writer.println(cardToDefinitionMap.size)
            cardToDefinitionMap.forEach { (card, definition) ->
                writer.println(card)
                writer.println(definition.value)
                writer.println(definition.numErrorsAnswering)
            }
        }
        println("${cardToDefinitionMap.size} cards have been saved.")
    }

    private fun dumpToFile(filename: String, block: (PrintWriter) -> Unit) {
        val file = File(filename)
        file.printWriter().use(block)
    }

    fun ask() {
        println("How many times to ask?")
        val numQuestions = readLine()!!.toInt()
        val cards = cardToDefinitionMap.keys.toList()
        repeat(numQuestions) {
            val randomCardIndex = Random.nextInt(0, cards.size)
            val card = cards[randomCardIndex]
            println("Print the definition of \"$card\":")

            val enteredDefinition = readLine()!!
            val correctDefinition = cardToDefinitionMap[card]!!

            if (enteredDefinition == correctDefinition.value) {
                println("Correct answer")
            } else {
                val cardWithEnteredDefinition = definitionToCardMap[enteredDefinition]
                if (cardWithEnteredDefinition != null) {
                    println("Wrong answer. The correct one is \"${correctDefinition.value}\", " +
                            "you've just written the definition of \"$cardWithEnteredDefinition\".")
                } else {
                    println("Wrong answer. The correct one is \"${correctDefinition.value}\".")
                }
                correctDefinition.numErrorsAnswering++
            }
        }
    }

    fun exit() {
        println("Bye bye!")
        args.exportFilename?.also { filename -> doExport(filename) }
        exitProcess(0)
    }

    fun log() {
        println("File name:")
        val filename = readLine()!!
        dumpToFile(filename) { writer -> consoleLog.forEach(writer::println) }
        println("The log has been saved.")
    }

    fun hardestCard() {
        val answer = cardToDefinitionMap.asSequence()
                .filter { (_, definition) -> definition.numErrorsAnswering != 0 }
                .map { (card, definition) -> card to definition.numErrorsAnswering }
                .groupBy { (_, numErrorsAnswering) -> numErrorsAnswering }
                .maxBy { (numErrorsAnswering, _) -> numErrorsAnswering }
                ?.let { (numErrorsAnswering, cardToNumErrorsAnsweringList) ->
                    numErrorsAnswering to cardToNumErrorsAnsweringList.map { (card, _) -> card }
                }

        if (answer == null || answer.first == 0) {
            println("There are no cards with errors.")
        } else {
            val (numErrorsAnswering, cards) = answer
            val (prefix, postfix) =
                    if (cards.size == 1) "The hardest card is" to "it"
                    else "The hardest cards are" to "them"

            val cardsToPrint = cards.joinToString(transform = { card -> "\"$card\"" })
            println("$prefix $cardsToPrint. You have $numErrorsAnswering errors answering $postfix.")
        }
    }

    fun resetStats() {
        cardToDefinitionMap.forEach { (_, definition) -> definition.numErrorsAnswering = 0 }
        println("Card statistics has been reset.")
    }

    fun help(action: String) {
        print("Oops! Can't perform: $action. What about giving one of the supported actions $SUPPORTED_ACTIONS a try?")
    }
}