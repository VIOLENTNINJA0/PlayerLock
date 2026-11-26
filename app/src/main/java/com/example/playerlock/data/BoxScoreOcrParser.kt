package com.example.playerlock.data

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.Result
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class ScannedBoxScore(
    val min: Int = 0,
    val pts: Int,
    val reb: Int,
    val ast: Int,
    val stl: Int,
    val blk: Int,
    val tov: Int,
    val fgm: Int,
    val fga: Int,
    val ftm: Int,
    val fta: Int
)

object BoxScoreOcrParser {

    private val splitRegex = Regex("""(\d+)\s*[-/]\s*(\d+)""")
    private val intRegex = Regex("""\d+""")
    private val timeRegex = Regex("""^\d{1,2}:\d{2}$""")

    suspend fun parseCroppedRow(bitmap: Bitmap): Result<ScannedBoxScore> =
        suspendCancellableCoroutine { cont ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    cont.resume(parseVisionText(visionText))
                }
                .addOnFailureListener { exception ->
                    cont.resumeWithException(exception)
                }
        }

    private fun parseVisionText(visionText: Text): Result<ScannedBoxScore> {
        return try {
            // Collect all Text.Element tokens and sort strictly left-to-right by bounding box left edge
            val elements = mutableListOf<Pair<String, Int>>()
            for (block in visionText.textBlocks) {
                for (line in block.lines) {
                    for (element in line.elements) {
                        val text = element.text.trim()
                        if (text.isNotEmpty()) {
                            elements.add(Pair(text, element.boundingBox?.left ?: 0))
                        }
                    }
                }
            }
            elements.sortBy { it.second }

            val splitMatches = mutableListOf<Pair<Int, Int>>() // (g1, g2) per shooting split
            val standaloneNums = mutableListOf<Int>()

            var splitStarted = false
            for ((text, _) in elements) {
                val splitMatch = splitRegex.find(text)
                if (splitMatch != null) {
                    splitStarted = true
                    splitMatches.add(
                        Pair(
                            splitMatch.groupValues[1].toInt(),
                            splitMatch.groupValues[2].toInt()
                        )
                    )
                } else if (!splitStarted) {
                    // Standalone integer appearing before any shooting split.
                    // Ignore player-name words and MM:SS formatted minute strings.
                    if (timeRegex.matches(text)) continue
                    val intMatch = intRegex.find(text)
                    if (intMatch != null && intMatch.value == text) {
                        standaloneNums.add(intMatch.value.toInt())
                    }
                }
            }

            // ---- Shooting splits ----
            var fgm = 0
            var fga = 0
            var ftm = 0
            var fta = 0

            if (splitMatches.size >= 1) {
                fgm = splitMatches[0].first
                fga = splitMatches[0].second
                if (fgm > fga) fgm = fga // clamp fgm <= fga
            }

            if (splitMatches.size >= 3) {
                // 3 pairs: FGM-FGA, 3PM-3PA, FTM-FTA -> 3rd pair is FT
                ftm = splitMatches[2].first
                fta = splitMatches[2].second
            } else if (splitMatches.size >= 2) {
                // 2 pairs: FGM-FGA, FTM-FTA -> 2nd pair is FT
                ftm = splitMatches[1].first
                fta = splitMatches[1].second
            }
            if (ftm > fta) ftm = fta // clamp ftm <= fta

            // ---- Standalone counting stats ----
            var min = 0
            var pts = 0
            var reb = 0
            var ast = 0
            var stl = 0
            var blk = 0
            var tov = 0

            if (standaloneNums.size >= 7) {
                // index 0 = MIN, indices 1..6 = PTS, REB, AST, STL, BLK, TOV
                min = standaloneNums.getOrElse(0) { 0 }
                pts = standaloneNums.getOrElse(1) { 0 }
                reb = standaloneNums.getOrElse(2) { 0 }
                ast = standaloneNums.getOrElse(3) { 0 }
                stl = standaloneNums.getOrElse(4) { 0 }
                blk = standaloneNums.getOrElse(5) { 0 }
                tov = standaloneNums.getOrElse(6) { 0 }
            } else {
                // Map available standalone numbers in order to PTS, REB, AST, STL, BLK, TOV
                pts = standaloneNums.getOrElse(0) { 0 }
                reb = standaloneNums.getOrElse(1) { 0 }
                ast = standaloneNums.getOrElse(2) { 0 }
                stl = standaloneNums.getOrElse(3) { 0 }
                blk = standaloneNums.getOrElse(4) { 0 }
                tov = standaloneNums.getOrElse(5) { 0 }
            }

            Result.success(
                ScannedBoxScore(min, pts, reb, ast, stl, blk, tov, fgm, fga, ftm, fta)
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}