import facebook.FacebookWrapper
import facebook.FacebookWrapperImpl
import info.debatty.java.stringsimilarity.Jaccard
import info.debatty.java.stringsimilarity.Levenshtein
import org.intellij.lang.annotations.Language
import java.io.File
import java.time.LocalDateTime

/*
 * Written by Christopher Stelzmüller <tuesd4y@protonmail.ch>, October 2017
 */

object Main {
    val fb = FacebookWrapperImpl("")

    @JvmStatic
    fun main(args: Array<String>) {

        // example object: "10155004153142688
        val comments = fb.getComments("10155004153142688")
                .toList()
                .blockingGet()
                .groupBy { it.message.count(Character::isWhitespace) < 3 }

        val jaccard = Jaccard()
        val levenshtein = Levenshtein()

        @Language("RegExp")
        val onlyUpperCasePattern = "[^A-Z]".toRegex()
        val onlyRegularCharactersPattern = "[^A-Za-z0-9 .]".toRegex()
        val originalStops = mutableListOf<FacebookWrapper.Comment>()
                originalStops.addAll(comments[true]!!)
                originalStops.addAll(comments[false]!!)

        val stops = originalStops.map { it.message.toUpperCase().replace(onlyUpperCasePattern, "") }

        val stopsList = mutableListOf<MutableList<Int>>()

        for(i in 0 until stops.size) {
            val length = stops[i].length / 3
            val currentList = mutableListOf(i)
            for( j in 0 until stops.size) {
                if(i == j)
                    continue

                if(jaccard.similarity(stops[i], stops[j]) > 0.2
                        || levenshtein.distance(stops[i], stops[j]) < length
                        || stops[i] in stops[j])
                    currentList.add(j)
            }
            stopsList.add(currentList)
        }

        val date = LocalDateTime.now()

        val facebookStopFile = File("stops on ${date.dayOfMonth}-${date.month}_${date.hour}:${date.minute}_(${stops.size}).csv")
        val pw = facebookStopFile.printWriter()
        pw.write("Place;# Mentions;Mentions\n")

        (0 until stops.size)
                .map { stops[it] to it }
                .distinctBy { it.first }
                .map { (_, it) -> Triple(
                        originalStops[it].message.replace(onlyRegularCharactersPattern, ""),
                        stopsList[it].size,
                        stopsList[it].map { originalStops[it].message.replace(onlyRegularCharactersPattern, "") }
                ) }
                .sortedByDescending { it.second }
                .filter { it.second > 1 }
                .drop(1)
                .forEach { (stop, times, all) ->
                    println(String.format("%-2d\t %-50s\t -----> %s", times, stop, all.joinToString()))
                    pw.write("$stop;$times;$all\n")
        }
        pw.close()
        println()
        println("saved to csv file at: ${facebookStopFile.absolutePath}")

    }
}