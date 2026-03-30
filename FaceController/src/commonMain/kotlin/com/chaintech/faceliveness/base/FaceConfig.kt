package com.chaintech.faceliveness.base

data class FaceConfig(
    val autoBrightnessIncreased: Boolean = true,
    val isBrightnessControllerRequired: Boolean = true,
    val totalEyeCount: Int = 4,
    val captureSelfieHoldDuration: Long = 1000L,
    val requiredFaceFramesForConfirmation: Int = 5,
    val requiredCloseFramesForConfirmation: Int = 2,
    val requiredFaceFrames: Int = 5,
    val brightnessTooDarkValue: FaceBrightnessState = FaceBrightnessState.TooDark,
    val brightnessDarkValue: FaceBrightnessState = FaceBrightnessState.Dark,
    val brightnessConfigValue: FaceBrightnessState = FaceBrightnessState.Medium,
    val brightnessTooBrightValue: FaceBrightnessState = FaceBrightnessState.TooBright,
    val brightnessBrightValue: FaceBrightnessState = FaceBrightnessState.Bright,
    val listOfFaceDetection: List<FaceMoveDetection> = listOf(),
    val listOfFaceDiagonalDetection: List<FaceMoveDiagonalDetection> = listOf(
        FaceMoveDiagonalDetection.MOVE_TOP_LEFT,
        FaceMoveDiagonalDetection.MOVE_BOTTOM_LEFT,
        FaceMoveDiagonalDetection.MOVE_BOTTOM_RIGHT,
        FaceMoveDiagonalDetection.MOVE_TOP_RIGHT,
    ),
    val listOfFaceTask: List<FaceTask> = listOf(
        FaceTask.FACE_ROTATION_DIAGONAL,
        FaceTask.EYE_BLINK,
        FaceTask.SELFIE_CAPTURE
    ),
) {
    init {
        // ✅ Validation 1: Check for duplicate moves in regular detection
        val regularDuplicates = listOfFaceDetection.groupingBy { it }.eachCount().filter { it.value > 1 }
        if (regularDuplicates.isNotEmpty()) {
            throw IllegalArgumentException(
                """
                ❌ ERROR: Duplicate moves in listOfFaceDetection!
                Duplicates found: $regularDuplicates
                Each move can only appear once in the list.
                """.trimIndent()
            )
        }

        // ✅ Validation 2: Check for duplicate moves in diagonal detection
        val diagonalDuplicates = listOfFaceDiagonalDetection.groupingBy { it }.eachCount().filter { it.value > 1 }
        if (diagonalDuplicates.isNotEmpty()) {
            throw IllegalArgumentException(
                """
                ❌ ERROR: Duplicate moves in listOfFaceDiagonalDetection!
                Duplicates found: $diagonalDuplicates
                Each move can only appear once in the list.
                """.trimIndent()
            )
        }

        // ✅ Validation 3: Ensure only ONE rotation type based on task list
        val hasFaceRotation = listOfFaceTask.contains(FaceTask.FACE_ROTATION)
        val hasFaceRotationDiagonal = listOfFaceTask.contains(FaceTask.FACE_ROTATION_DIAGONAL)

        val hasRegularMoves = listOfFaceDetection.isNotEmpty()
        val hasDiagonalMoves = listOfFaceDiagonalDetection.isNotEmpty()

        // ✅ Both rotation tasks in list
        if (hasFaceRotation && hasFaceRotationDiagonal) {
            throw IllegalArgumentException(
                """
                ❌ ERROR: Cannot use both FACE_ROTATION and FACE_ROTATION_DIAGONAL!
                Current tasks: $listOfFaceTask
                
                Choose ONE:
                - FaceTask.FACE_ROTATION (for LEFT, RIGHT, TOP, BOTTOM)
                - FaceTask.FACE_ROTATION_DIAGONAL (for TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT)
                """.trimIndent()
            )
        }

        // ✅ FACE_ROTATION task with wrong moves
        if (hasFaceRotation) {
            if (hasDiagonalMoves && !hasRegularMoves) {
                throw IllegalArgumentException(
                    """
                    ❌ ERROR: FaceTask.FACE_ROTATION requires listOfFaceDetection!
                    Current: listOfFaceDetection is empty, but listOfFaceDiagonalDetection has data.
                    
                    Solution: Add moves to listOfFaceDetection (MOVE_LEFT, MOVE_RIGHT, MOVE_TOP, MOVE_BOTTOM)
                    """.trimIndent()
                )
            }
            if (!hasRegularMoves) {
                throw IllegalArgumentException(
                    """
                    ❌ ERROR: FaceTask.FACE_ROTATION is in task list but listOfFaceDetection is empty!
                    
                    Solution: Add moves to listOfFaceDetection or remove FaceTask.FACE_ROTATION from tasks.
                    """.trimIndent()
                )
            }
        }

        // ✅ FACE_ROTATION_DIAGONAL task with wrong moves
        if (hasFaceRotationDiagonal) {
            if (hasRegularMoves && !hasDiagonalMoves) {
                throw IllegalArgumentException(
                    """
                    ❌ ERROR: FaceTask.FACE_ROTATION_DIAGONAL requires listOfFaceDiagonalDetection!
                    Current: listOfFaceDiagonalDetection is empty, but listOfFaceDetection has data.
                    
                    Solution: Add moves to listOfFaceDiagonalDetection (MOVE_TOP_LEFT, MOVE_TOP_RIGHT, etc.)
                    """.trimIndent()
                )
            }
            if (!hasDiagonalMoves) {
                throw IllegalArgumentException(
                    """
                    ❌ ERROR: FaceTask.FACE_ROTATION_DIAGONAL is in task list but listOfFaceDiagonalDetection is empty!
                    
                    Solution: Add moves to listOfFaceDiagonalDetection or remove FaceTask.FACE_ROTATION_DIAGONAL from tasks.
                    """.trimIndent()
                )
            }
        }

        // ✅ Both lists have data but no rotation task
        if (hasRegularMoves && hasDiagonalMoves && !hasFaceRotation && !hasFaceRotationDiagonal) {
            throw IllegalArgumentException(
                """
                ❌ ERROR: Both listOfFaceDetection and listOfFaceDiagonalDetection have data!
                But no rotation task found in listOfFaceTask.
                
                Solution: Add either FaceTask.FACE_ROTATION or FaceTask.FACE_ROTATION_DIAGONAL to tasks.
                """.trimIndent()
            )
        }

        // ✅ Validation 4: Check eye blink configuration
        if (listOfFaceTask.contains(FaceTask.EYE_BLINK)) {
            if (totalEyeCount < 1) {
                throw IllegalArgumentException(
                    """
                    ❌ ERROR: FaceTask.EYE_BLINK requires totalEyeCount >= 1!
                    Current: totalEyeCount = $totalEyeCount
                    """.trimIndent()
                )
            }
        }

        // ✅ Validation 5: Ensure at least one task
        if (listOfFaceTask.isEmpty()) {
            throw IllegalArgumentException(
                """
                ❌ ERROR: listOfFaceTask is empty!
                At least one task is required: FACE_ROTATION, FACE_ROTATION_DIAGONAL, EYE_BLINK, or SELFIE_CAPTURE
                """.trimIndent()
            )
        }

        // ✅ Success message
        println("✅ FaceConfig validated successfully!")
        println("   Tasks: $listOfFaceTask")
        if (hasRegularMoves) {
            println("   Regular moves: $listOfFaceDetection")
        }
        if (hasDiagonalMoves) {
            println("   Diagonal moves: $listOfFaceDiagonalDetection")
        }
    }
}
enum class FaceMoveDetection {
    MOVE_LEFT,
    MOVE_RIGHT,
    MOVE_TOP,
    MOVE_BOTTOM,

}
enum class FaceMoveDiagonalDetection {
    MOVE_TOP_LEFT,
    MOVE_TOP_RIGHT,
    MOVE_BOTTOM_LEFT,
    MOVE_BOTTOM_RIGHT
}

enum class FaceTask{
    EYE_BLINK,
    FACE_ROTATION,

    FACE_ROTATION_DIAGONAL,
    SELFIE_CAPTURE
}

sealed class FaceBrightnessState (val value : Float){
    object TooDark : FaceBrightnessState(0.3f)
    object Dark : FaceBrightnessState(0.5f)
    object TooBright : FaceBrightnessState(0.9f)
    object Bright : FaceBrightnessState(0.7f)
    object Medium : FaceBrightnessState(0.4f)

}

