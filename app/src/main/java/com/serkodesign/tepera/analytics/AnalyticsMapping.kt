package com.serkodesign.tepera.analytics

import com.serkodesign.tepera.domain.risk.QuickPuzzles

/** Тривалість «Так, роблю це» → бакет (точні хвилини назовні не йдуть, тут і локально — лише бакети). */
fun intentionBucketFor(minutes: Int): IntentionBucket = when (minutes) {
    5 -> IntentionBucket.MIN_5
    15 -> IntentionBucket.MIN_15
    30 -> IntentionBucket.MIN_30
    else -> IntentionBucket.OTHER
}

fun puzzleTypeFor(kind: QuickPuzzles.Kind): PuzzleType = when (kind) {
    QuickPuzzles.Kind.ARITHMETIC -> PuzzleType.MATH
    QuickPuzzles.Kind.SCHULTE -> PuzzleType.SCHULTE
    QuickPuzzles.Kind.DIGIT_MEMORY -> PuzzleType.DIGITS
    QuickPuzzles.Kind.SEQUENCE -> PuzzleType.SEQUENCE
    QuickPuzzles.Kind.SUDOKU -> PuzzleType.SUDOKU
    QuickPuzzles.Kind.INTENTION -> PuzzleType.INTENTION
}
