package com.mychoi.fridgemanager.domain.model

import kotlinx.serialization.Serializable

/**
 * 사용자 선호도 및 설정 도메인 모델
 * user_profiles 테이블을 기반으로 한 사용자 맞춤 설정
 *
 * 실제 DB 스키마:
 * - id: UUID PRIMARY KEY REFERENCES auth.users(id)
 * - nickname: TEXT
 * - preferred_categories: TEXT[] DEFAULT '{}' (선호하는 요리 카테고리)
 * - cooking_skill_level: INTEGER DEFAULT 1 (요리 실력 1-5)
 * - preferred_cooking_time: INTEGER DEFAULT 60 (선호하는 최대 조리시간)
 * - created_at, updated_at: TIMESTAMP
 */
@Serializable
data class UserPreference(
    val userId: String,                          // DB의 id (auth.users와 연결)
    val nickname: String? = null,                // DB의 nickname
    val preferredCategories: List<String> = emptyList(), // DB의 preferred_categories 배열
    val cookingSkillLevel: Int = 1,              // DB의 cooking_skill_level (1-5)
    val preferredCookingTime: Int = 60,          // DB의 preferred_cooking_time (분)
    val createdAt: String = getCurrentISODateTime(), // DB의 created_at
    val updatedAt: String = getCurrentISODateTime(), // DB의 updated_at

    // 추가 확장 필드들 (앱에서만 사용, 추후 DB 확장 시 추가)
    val displayName: String = nickname ?: "",
    val email: String = "",
    val dislikedIngredients: List<String> = emptyList(),
    val dietRestrictions: List<DietRestriction> = emptyList(),
    val allergies: List<String> = emptyList(),
    val householdSize: Int = 1,
    val budgetRange: BudgetRange = BudgetRange.MEDIUM,
    val preferredMealTimes: List<MealTime> = listOf(MealTime.DINNER),
    val notificationSettings: NotificationSettings = NotificationSettings(),
    val privacySettings: PrivacySettings = PrivacySettings()
) {

    /**
     * 레시피가 사용자 선호도에 맞는지 점수 계산
     * @param recipe 평가할 레시피
     * @return 선호도 점수 (0.0~1.0, 1.0이 완벽 일치)
     */
    fun calculatePreferenceScore(recipe: Recipe): Double {
        var score = 0.0
        var totalWeight = 0.0

        // 1. 요리 실력 수준 매칭 (가중치: 0.3)
        val skillWeight = 0.3
        val skillScore = when {
            recipe.difficulty <= cookingSkillLevel -> 1.0
            recipe.difficulty == cookingSkillLevel + 1 -> 0.7
            else -> 0.3
        }
        score += skillScore * skillWeight
        totalWeight += skillWeight

        // 2. 조리 시간 선호도 (가중치: 0.25)
        val timeWeight = 0.25
        val timeScore = when {
            recipe.cookingTimeMinutes <= preferredCookingTime -> 1.0
            recipe.cookingTimeMinutes <= preferredCookingTime + 15 -> 0.7
            else -> 0.4
        }
        score += timeScore * timeWeight
        totalWeight += timeWeight

        // 3. 카테고리 선호도 (가중치: 0.2)
        val categoryWeight = 0.2
        val categoryScore = if (preferredCategories.isEmpty()) {
            0.5 // 선호 카테고리가 없으면 중립
        } else {
            if (preferredCategories.contains(recipe.category)) 1.0 else 0.2
        }
        score += categoryScore * categoryWeight
        totalWeight += categoryWeight

        // 4. 싫어하는 재료 체크 (가중치: 0.15)
        val dislikedWeight = 0.15
        val hasDislikedIngredient = recipe.ingredients.any { recipeIngredient ->
            dislikedIngredients.any { disliked ->
                recipeIngredient.name.contains(disliked, ignoreCase = true)
            }
        }
        val dislikedScore = if (hasDislikedIngredient) 0.0 else 1.0
        score += dislikedScore * dislikedWeight
        totalWeight += dislikedWeight

        // 5. 가구 인원수 적합성 (가중치: 0.1)
        val servingWeight = 0.1
        val servingScore = when {
            recipe.servings == householdSize -> 1.0
            kotlin.math.abs(recipe.servings - householdSize) <= 1 -> 0.8
            kotlin.math.abs(recipe.servings - householdSize) <= 2 -> 0.6
            else -> 0.4
        }
        score += servingScore * servingWeight
        totalWeight += servingWeight

        return if (totalWeight > 0) score / totalWeight else 0.0
    }

    /**
     * 재료에 대한 선호도 확인
     * @param ingredientName 재료명
     * @return 선호도 상태
     */
    fun getIngredientPreference(ingredientName: String): IngredientPreferenceStatus {
        return when {
            dislikedIngredients.any { it.equals(ingredientName, ignoreCase = true) } ->
                IngredientPreferenceStatus.DISLIKED
            allergies.any { ingredientName.contains(it, ignoreCase = true) } ->
                IngredientPreferenceStatus.ALLERGIC
            else -> IngredientPreferenceStatus.NEUTRAL
        }
    }

    /**
     * 레시피가 식이 제한에 맞는지 확인
     * @param recipe 확인할 레시피
     * @return 식이 제한 준수 여부
     */
    fun checkDietRestrictions(recipe: Recipe): List<DietRestriction> {
        val violatedRestrictions = mutableListOf<DietRestriction>()

        dietRestrictions.forEach { restriction ->
            if (!restriction.isRecipeCompliant(recipe)) {
                violatedRestrictions.add(restriction)
            }
        }

        return violatedRestrictions
    }

    /**
     * 추천받을 수 있는 레시피인지 확인 (기본 필터링)
     * @param recipe 확인할 레시피
     * @return 추천 가능 여부와 사유
     */
    fun canRecommendRecipe(recipe: Recipe): RecommendationEligibility {
        // 알레르기 체크
        val hasAllergen = recipe.ingredients.any { recipeIngredient ->
            allergies.any { allergen ->
                recipeIngredient.name.contains(allergen, ignoreCase = true)
            }
        }

        if (hasAllergen) {
            val allergens = allergies.filter { allergen ->
                recipe.ingredients.any { it.name.contains(allergen, ignoreCase = true) }
            }
            return RecommendationEligibility(
                canRecommend = false,
                reason = "알레르기 성분 포함: ${allergens.joinToString(", ")}"
            )
        }

        // 식이 제한 체크
        val violatedRestrictions = checkDietRestrictions(recipe)
        if (violatedRestrictions.isNotEmpty()) {
            return RecommendationEligibility(
                canRecommend = false,
                reason = "식이 제한 위반: ${violatedRestrictions.joinToString(", ") { it.displayName }}"
            )
        }

        // 실력 수준 체크 (너무 어려운 레시피는 제외)
        if (recipe.difficulty > cookingSkillLevel + 1) {
            return RecommendationEligibility(
                canRecommend = false,
                reason = "요리 난이도가 너무 높음 (현재 실력: ${cookingSkillLevel}, 레시피 난이도: ${recipe.difficulty})"
            )
        }

        return RecommendationEligibility(canRecommend = true, reason = "")
    }

    /**
     * 사용자 설정 업데이트
     * @param updates 업데이트할 설정들
     * @return 업데이트된 사용자 선호도
     */
    fun updatePreferences(updates: PreferenceUpdates): UserPreference {
        return copy(
            displayName = updates.displayName ?: this.displayName,
            skillLevel = updates.skillLevel ?: this.skillLevel,
            preferredCookingTime = updates.preferredCookingTime ?: this.preferredCookingTime,
            preferredCategories = updates.preferredCategories ?: this.preferredCategories,
            dislikedIngredients = updates.dislikedIngredients ?: this.dislikedIngredients,
            dietRestrictions = updates.dietRestrictions ?: this.dietRestrictions,
            allergies = updates.allergies ?: this.allergies,
            householdSize = updates.householdSize ?: this.householdSize,
            budgetRange = updates.budgetRange ?: this.budgetRange,
            preferredMealTimes = updates.preferredMealTimes ?: this.preferredMealTimes,
            notificationSettings = updates.notificationSettings ?: this.notificationSettings,
            updatedAt = getCurrentISODateTime()
        )
    }

    /**
     * 사용자 프로필 요약 정보
     * @return 프로필 요약 텍스트
     */
    fun getProfileSummary(): String {
        val skillText = "레벨 $cookingSkillLevel"
        val timeText = "${preferredCookingTime}분 이내"
        val categoryText = if (preferredCategories.isNotEmpty()) {
            preferredCategories.joinToString(", ")
        } else {
            "모든 카테고리"
        }

        return "요리 실력: $skillText | 선호 시간: $timeText | 선호 카테고리: $categoryText"
    }

    companion object {
        /**
         * 현재 ISO 8601 날짜시간 문자열 반환
         */
        private fun getCurrentISODateTime(): String {
            return java.time.LocalDateTime.now().toString()
        }

        /**
         * 기본 사용자 선호도 생성
         * @param userId 사용자 ID
         * @return 기본 설정이 적용된 사용자 선호도
         */
        fun createDefault(userId: String): UserPreference {
            return UserPreference(
                userId = userId,
                cookingSkillLevel = 1,
                preferredCookingTime = 60,
                preferredCategories = listOf("한식"),
                householdSize = 1
            )
        }
    }
}

/**
 * 식이 제한 사항 열거형 (추후 확장용)
 */
enum class DietRestriction(val displayName: String, val description: String) {
    VEGETARIAN("채식주의", "육류, 생선 제외"),
    VEGAN("비건", "모든 동물성 제품 제외"),
    GLUTEN_FREE("글루텐 프리", "글루텐 함유 식품 제외"),
    LACTOSE_FREE("유당 불내증", "유제품 제외"),
    LOW_SODIUM("저염식", "나트륨 제한"),
    LOW_SUGAR("저당식", "설탕 제한"),
    HALAL("할랄", "이슬람 식이법 준수"),
    KOSHER("코셔", "유대교 식이법 준수");

    /**
     * 레시피가 이 식이 제한을 준수하는지 확인
     * @param recipe 확인할 레시피
     * @return 준수 여부
     */
    fun isRecipeCompliant(recipe: Recipe): Boolean {
        // 간단한 구현 (실제로는 더 정교한 검사 필요)
        return when (this) {
            VEGETARIAN -> !recipe.ingredients.any {
                it.name.contains("고기") || it.name.contains("생선")
            }
            VEGAN -> !recipe.ingredients.any {
                it.name.contains("고기") || it.name.contains("생선") ||
                        it.name.contains("우유") || it.name.contains("달걀") || it.name.contains("치즈")
            }
            GLUTEN_FREE -> !recipe.ingredients.any {
                it.name.contains("밀") || it.name.contains("글루텐")
            }
            LACTOSE_FREE -> !recipe.ingredients.any {
                it.name.contains("우유") || it.name.contains("치즈") || it.name.contains("크림")
            }
            else -> true // 다른 제한사항은 추후 구현
        }
    }
}

/**
 * 예산 범위 열거형
 */
enum class BudgetRange(val displayName: String, val maxCostPer100g: Int) {
    LOW("저예산 (경제적)", 1000),
    MEDIUM("적정 예산", 3000),
    HIGH("고예산 (프리미엄)", 10000),
    UNLIMITED("예산 제한 없음", Int.MAX_VALUE)
}

/**
 * 식사 시간대 열거형
 */
enum class MealTime(val displayName: String, val hourRange: IntRange) {
    BREAKFAST("아침", 6..10),
    BRUNCH("브런치", 10..12),
    LUNCH("점심", 12..14),
    SNACK("간식", 14..17),
    DINNER("저녁", 18..21),
    LATE_NIGHT("야식", 21..24)
}

/**
 * 재료 선호도 상태 열거형
 */
enum class IngredientPreferenceStatus(val displayName: String) {
    LIKED("좋아함"),
    NEUTRAL("보통"),
    DISLIKED("싫어함"),
    ALLERGIC("알레르기")
}

/**
 * 알림 설정
 */
@Serializable
data class NotificationSettings(
    val expiryReminder: Boolean = true,           // 유통기한 알림
    val recipeRecommendation: Boolean = true,     // 레시피 추천 알림
    val shoppingListReminder: Boolean = false,    // 장보기 알림 (추후 기능)
    val mealPlanningReminder: Boolean = false     // 식단 계획 알림 (추후 기능)
)

/**
 * 개인정보 설정
 */
@Serializable
data class PrivacySettings(
    val shareRecipes: Boolean = false,            // 레시피 공유 허용
    val allowDataCollection: Boolean = true,      // 데이터 수집 허용
    val personalizedAds: Boolean = false          // 맞춤형 광고 허용
)

/**
 * 선호도 업데이트 데이터 클래스
 */
data class PreferenceUpdates(
    val nickname: String? = null,
    val preferredCategories: List<String>? = null,
    val cookingSkillLevel: Int? = null,
    val preferredCookingTime: Int? = null,
    val dislikedIngredients: List<String>? = null,
    val dietRestrictions: List<DietRestriction>? = null,
    val allergies: List<String>? = null,
    val householdSize: Int? = null,
    val budgetRange: BudgetRange? = null,
    val preferredMealTimes: List<MealTime>? = null,
    val notificationSettings: NotificationSettings? = null
)

/**
 * 추천 자격 결과
 */
data class RecommendationEligibility(
    val canRecommend: Boolean,
    val reason: String
)