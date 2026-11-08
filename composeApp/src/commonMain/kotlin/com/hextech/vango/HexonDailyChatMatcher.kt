package com.hextech.vango

import kotlin.random.Random

/**
 * Offline daily-chat intent matching for Hexon.
 * Confident local hit → pick a non-repeating answer from the intent.
 * Weak hit → null (caller routes to technical Q&A / remote LLM / rephrase).
 */
object HexonDailyChatMatcher {
    /** Last answer text used per intent id — avoid consecutive repeats. */
    private val lastAnswerByIntent = mutableMapOf<Int, String>()

    data class Match(
        val intentId: Int,
        val answer: String,
        val score: Float,
    )

    fun match(rawQuery: String): Match? {
        val q = normalizeDaily(rawQuery)
        if (q.isBlank() || q.length < 2) return null

        var bestId = -1
        var bestScore = 0f
        for (intent in HexonDailyChatCorpus) {
            val score = scoreIntent(q, intent)
            if (score > bestScore) {
                bestScore = score
                bestId = intent.id
            }
        }
        if (bestId < 0 || bestScore < CONFIDENT_SCORE) return null
        val intent = HexonDailyChatCorpus.first { it.id == bestId }
        val answer = pickAnswer(intent) ?: return null
        lastAnswerByIntent[intent.id] = answer
        return Match(intentId = intent.id, answer = answer, score = bestScore)
    }

    /** Test helper — does not mutate anti-repeat state when dryRun=true. */
    fun scoreOnly(rawQuery: String): Pair<Int, Float>? {
        val q = normalizeDaily(rawQuery)
        if (q.isBlank()) return null
        var bestId = -1
        var bestScore = 0f
        for (intent in HexonDailyChatCorpus) {
            val score = scoreIntent(q, intent)
            if (score > bestScore) {
                bestScore = score
                bestId = intent.id
            }
        }
        return if (bestId < 0) null else bestId to bestScore
    }

    fun resetAntiRepeatForTests() {
        lastAnswerByIntent.clear()
    }

    private fun pickAnswer(intent: HexonDailyIntent): String? {
        val answers = intent.answers
        if (answers.isEmpty()) return null
        if (answers.size == 1) return answers[0]
        val last = lastAnswerByIntent[intent.id]
        val pool = if (last != null) answers.filter { it != last } else answers
        val choices = pool.ifEmpty { answers }
        return choices[Random.nextInt(choices.size)]
    }

    private fun scoreIntent(q: String, intent: HexonDailyIntent): Float {
        var best = 0f
        val titleN = normalizeDaily(intent.title)
        if (titleN.isNotEmpty()) {
            best = maxOf(best, phraseScore(q, titleN))
        }
        for (question in intent.questions) {
            val n = normalizeDaily(question)
            if (n.isEmpty()) continue
            best = maxOf(best, phraseScore(q, n))
        }
        return best
    }

    private fun phraseScore(query: String, phrase: String): Float {
        if (query == phrase) return 1f
        // Short command-like phrases: require near-equality / containment both ways carefully.
        if (phrase.length <= 12) {
            if (query == phrase) return 1f
            if (query.startsWith(phrase) && query.length <= phrase.length + 4) return 0.92f
            if (phrase.startsWith(query) && query.length >= 3 && query.length + 2 >= phrase.length) return 0.88f
        }
        if (query.contains(phrase) && phrase.length >= 4) {
            val ratio = phrase.length.toFloat() / query.length.toFloat()
            return (0.72f + 0.25f * ratio).coerceAtMost(0.95f)
        }
        if (phrase.contains(query) && query.length >= 4) {
            val ratio = query.length.toFloat() / phrase.length.toFloat()
            return (0.70f + 0.22f * ratio).coerceAtMost(0.93f)
        }
        val qt = tokens(query)
        val pt = tokens(phrase)
        if (qt.isEmpty() || pt.isEmpty()) return 0f
        val inter = qt.intersect(pt).size
        if (inter == 0) return 0f
        val precision = inter.toFloat() / qt.size.toFloat()
        val recall = inter.toFloat() / pt.size.toFloat()
        val f1 = 2f * precision * recall / (precision + recall)
        // Require meaningful overlap for multi-token queries.
        return if (f1 >= 0.72f && inter >= minOf(2, pt.size)) f1 * 0.9f else 0f
    }
}

/** Confidence floor — below this we refuse the daily corpus (no wrong smalltalk). */
internal const val CONFIDENT_SCORE = 0.78f

internal fun normalizeDaily(s: String): String =
    s.lowercase()
        .replace('ı', 'i')
        .replace('İ', 'i')
        .replace('ğ', 'g')
        .replace('ü', 'u')
        .replace('ş', 's')
        .replace('ö', 'o')
        .replace('ç', 'c')
        .replace(Regex("[?!.,;:\"'’“”()\\[\\]{}…]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

private fun tokens(s: String): Set<String> =
    s.split(' ').map { it.trim() }.filter { it.length >= 2 }.toSet()
