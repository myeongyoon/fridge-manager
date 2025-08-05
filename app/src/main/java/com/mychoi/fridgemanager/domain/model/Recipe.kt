package com.mychoi.fridgemanager.domain.model

data class Recipe(
    val id: String,
    val name: String,                           // 레시피 이름
    val description: String? = null,            // 간단한 설명
    val cookingTimeMinutes: Int? = null,        // 조리 시간 (분)
    val difficulty: Int? = null,                // 난이도 1-5
    val servings: Int = 1,                      // 몇 인분
    val category: String? = null,               // "한식", "양식", "중식", "일식", "간식", "음료"
    val mealType: String? = null,               // "아침", "점심", "저녁", "간식", "야식"
    val instructions: List<String> = emptyList(), // 조리 단계별 설명
    val imageUrl: String? = null,               // 요리 사진 URL
    val videoUrl: String? = null,               // 요리 영상 URL
    val source: String? = null,                 // 출처 (API명, 웹사이트 등)
    val externalId: String? = null,             // 원본 API의 레시피 ID
    val tags: List<String> = emptyList(),       // ["다이어트", "간단", "매운맛", "비건"]
    val viewCount: Int = 0,                     // 조회수
    val ingredients: List<RecipeIngredient> = emptyList(), // 레시피 재료들
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * 사용자 냉장고 재료와의 매칭 점수 계산
     *
     * @param userIngredients 사용자 냉장고에 있는 재료들
     * @return 매칭 점수 (0.0 ~ 105.0, 백엔드 추천 알고리즘과 동일)
     */
    fun calculateMatchingScore(userIngredients: List<UserIngredient>): Double {
        if (ingredients.isEmpty()) return 0.0

        val userIngredientNames = userIngredients.map { it.name.lowercase().trim() }.toSet()
        var totalScore = 0.0
        var matchedCount = 0

        ingredients.forEach { recipeIngredient ->
            val ingredientName = recipeIngredient.ingredientName.lowercase().trim()
            if (userIngredientNames.contains(ingredientName)) {
                matchedCount++
                // 기본 매칭 점수
                totalScore += 15.0

                // 필수/선택 재료에 따른 보너스 점수
                totalScore += if (recipeIngredient.isEssential) {
                    10.0  // 필수 재료 보너스
                } else {
                    5.0   // 선택 재료 보너스
                }
            }
        }

        // 매칭률에 따른 추가 보너스 (백엔드 로직과 동일)
        val matchingPercentage = (matchedCount.toDouble() / ingredients.size) * 100
        val bonusScore = when {
            matchingPercentage >= 80 -> 15.0  // 80% 이상 매칭 시 보너스
            matchingPercentage >= 60 -> 10.0  // 60% 이상 매칭 시 보너스
            matchingPercentage >= 40 -> 5.0   // 40% 이상 매칭 시 보너스
            else -> 0.0
        }

        return totalScore + bonusScore
    }

    /**
     * 레시피의 매칭률 계산 (백분율)
     */
    fun getMatchingPercentage(userIngredients: List<UserIngredient>): Double {
        if (ingredients.isEmpty()) return 0.0

        val userIngredientNames = userIngredients.map { it.name.lowercase().trim() }.toSet()
        val matchedCount = ingredients.count { recipeIngredient ->
            userIngredientNames.contains(recipeIngredient.ingredientName.lowercase().trim())
        }

        return (matchedCount.toDouble() / ingredients.size) * 100
    }

    /**
     * 레시피에서 부족한 재료들 반환
     */
    fun getMissingIngredients(userIngredients: List<UserIngredient>): List<RecipeIngredient> {
        val userIngredientNames = userIngredients.map { it.name.lowercase().trim() }.toSet()
        return ingredients.filter { recipeIngredient ->
            !userIngredientNames.contains(recipeIngredient.ingredientName.lowercase().trim())
        }
    }

    /**
     * 필수 재료만 부족한지 확인 (선택 재료는 무시)
     */
    fun getMissingEssentialIngredients(userIngredients: List<UserIngredient>): List<RecipeIngredient> {
        return getMissingIngredients(userIngredients).filter { it.isEssential }
    }

    /**
     * 레시피 실행 가능 여부 확인 (필수 재료 모두 있는지)
     */
    fun isExecutable(userIngredients: List<UserIngredient>): Boolean {
        return getMissingEssentialIngredients(userIngredients).isEmpty()
    }

    /**
     * 레시피의 예상 총 조리 시간 (준비시간 포함)
     */
    fun getEstimatedTotalTime(): Int? {
        return cookingTimeMinutes?.let { cookingTime ->
            // 조리 시간 + 예상 준비 시간 (조리 시간의 30%)
            cookingTime + (cookingTime * 0.3).toInt()
        }
    }

    /**
     * 난이도 레벨을 텍스트로 반환
     */
    fun getDifficultyText(): String {
        return when (difficulty) {
            1 -> "매우 쉬움"
            2 -> "쉬움"
            3 -> "보통"
            4 -> "어려움"
            5 -> "매우 어려움"
            else -> "정보 없음"
        }
    }

    /**
     * 사용자 선호도와의 호환성 점수 계산
     */
    fun calculatePreferenceScore(userPreference: UserPreference): Double {
        var score = 0.0

        // 조리 시간 선호도
        cookingTimeMinutes?.let { cookingTime ->
            val timeDifference = kotlin.math.abs(cookingTime - userPreference.preferredCookingTimeMinutes)
            score += when {
                timeDifference <= 10 -> 10.0
                timeDifference <= 20 -> 5.0
                timeDifference <= 30 -> 2.0
                else -> 0.0
            }
        }

        // 난이도 선호도 (cooking_skill_level과 비교)
        difficulty?.let { recipeDifficulty ->
            val skillDifference = kotlin.math.abs(recipeDifficulty - userPreference.cookingSkillLevel)
            score += when {
                skillDifference == 0 -> 8.0  // 정확히 맞는 난이도
                skillDifference == 1 -> 5.0  // ±1 난이도
                skillDifference == 2 -> 2.0  // ±2 난이도
                else -> 0.0
            }
        }

        // 카테고리 선호도
        category?.let { recipeCategory ->
            if (userPreference.preferredCategories.contains(recipeCategory)) {
                score += 5.0
            }
        }

        return score
    }

    /**
     * 레시피의 총 재료 개수
     */
    fun getTotalIngredientCount(): Int = ingredients.size

    /**
     * 필수 재료 개수
     */
    fun getEssentialIngredientCount(): Int = ingredients.count { it.isEssential }

    /**
     * 선택 재료 개수
     */
    fun getOptionalIngredientCount(): Int = ingredients.count { !it.isEssential }

    /**
     * 조리 단계 수
     */
    fun getInstructionStepCount(): Int = instructions.size

    /**
     * 레시피에 특정 태그가 있는지 확인
     */
    fun hasTag(tag: String): Boolean {
        return tags.any { it.equals(tag, ignoreCase = true) }
    }

    /**
     * 레시피가 특정 카테고리인지 확인
     */
    fun isCategory(categoryName: String): Boolean {
        return category?.equals(categoryName, ignoreCase = true) == true
    }

    /**
     * 레시피가 특정 식사 시간에 적합한지 확인
     */
    fun isMealType(mealTypeName: String): Boolean {
        return mealType?.equals(mealTypeName, ignoreCase = true) == true
    }
}

data class RecipeIngredient(
    val id: String? = null,
    val recipeId: String,
    val ingredientName: String,                 // 재료명
    val amount: String? = null,                 // "2개", "100g", "1/2컵"
    val unit: String? = null,                   // "개", "g", "ml", "컵", "큰술"
    val isEssential: Boolean = true,            // 필수 재료 여부
    val preparationNote: String? = null,        // "다진 것", "슬라이스한 것"
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * 재료의 전체 표시명 (분량 + 재료명 + 준비방법)
     */
    fun getDisplayName(): String {
        val parts = mutableListOf<String>()

        // 분량 정보
        if (!amount.isNullOrBlank()) {
            parts.add(amount)
        }

        // 재료명
        parts.add(ingredientName)

        // 준비방법
        if (!preparationNote.isNullOrBlank()) {
            parts.add("(${preparationNote})")
        }

        return parts.joinToString(" ")
    }

    /**
     * 재료가 필수인지 선택인지 텍스트로 반환
     */
    fun getImportanceText(): String {
        return if (isEssential) "필수" else "선택"
    }
}