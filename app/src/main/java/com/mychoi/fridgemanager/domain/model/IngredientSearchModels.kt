package com.mychoi.fridgemanager.domain.model

import kotlinx.serialization.Serializable

/**
 * 재료 검색 관련 데이터 모델들
 * IngredientRepository의 검색 기능에서 사용되는 모델 정의
 */

/**
 * 카테고리 정보 (재료 개수 포함)
 * 카테고리 필터 UI 구성 및 통계 표시용
 */
@Serializable
data class IngredientCategoryInfo(
    val categoryName: String,                // 내부 식별용 카테고리명 (영어)
    val displayName: String,                 // 사용자 표시용 이름 (한글)
    val ingredientCount: Int,                // 해당 카테고리의 재료 개수
    val subcategories: List<String> = emptyList(), // 서브카테고리 목록
    val isPopular: Boolean = false,          // 인기 카테고리 여부
    val description: String? = null,         // 카테고리 설명
    val iconName: String? = null            // 아이콘 이름 (추후 UI용)
) {
    /**
     * UI 표시용 텍스트 (재료 개수 포함)
     */
    fun getDisplayText(): String {
        return "$displayName ($ingredientCount 개)"
    }

    /**
     * 서브카테고리 포함 여부 확인
     */
    fun hasSubcategories(): Boolean {
        return subcategories.isNotEmpty()
    }

    /**
     * 특정 서브카테고리 포함 여부 확인
     */
    fun hasSubcategory(subcategoryName: String): Boolean {
        return subcategories.contains(subcategoryName)
    }

    /**
     * 카테고리 우선순위 계산 (인기도 + 재료 개수)
     */
    fun getPriorityScore(): Int {
        var score = ingredientCount
        if (isPopular) score += 100 // 인기 카테고리 보너스
        return score
    }

    companion object {
        /**
         * 기본 카테고리 목록 생성
         */
        fun getDefaultCategories(): List<IngredientCategoryInfo> {
            return listOf(
                IngredientCategoryInfo(
                    categoryName = "vegetables",
                    displayName = "채소",
                    ingredientCount = 0,
                    subcategories = listOf("잎채소", "뿌리채소", "과채소", "버섯류"),
                    isPopular = true,
                    description = "신선한 채소류",
                    iconName = "vegetable"
                ),
                IngredientCategoryInfo(
                    categoryName = "meat",
                    displayName = "육류",
                    ingredientCount = 0,
                    subcategories = listOf("소고기", "돼지고기", "닭고기", "가공육"),
                    isPopular = true,
                    description = "신선한 육류 및 가공육",
                    iconName = "meat"
                ),
                IngredientCategoryInfo(
                    categoryName = "seafood",
                    displayName = "해산물",
                    ingredientCount = 0,
                    subcategories = listOf("생선", "조개류", "갑각류", "건어물"),
                    isPopular = true,
                    description = "신선한 해산물",
                    iconName = "seafood"
                ),
                IngredientCategoryInfo(
                    categoryName = "dairy",
                    displayName = "유제품",
                    ingredientCount = 0,
                    subcategories = listOf("우유", "치즈", "요거트", "버터"),
                    isPopular = true,
                    description = "우유 및 유제품",
                    iconName = "dairy"
                ),
                IngredientCategoryInfo(
                    categoryName = "grains",
                    displayName = "곡류",
                    ingredientCount = 0,
                    subcategories = listOf("쌀", "밀가루", "면류", "잡곡"),
                    isPopular = true,
                    description = "곡물 및 곡물 가공품",
                    iconName = "grains"
                ),
                IngredientCategoryInfo(
                    categoryName = "seasonings",
                    displayName = "조미료",
                    ingredientCount = 0,
                    subcategories = listOf("기본조미료", "양념류", "향신료", "소스"),
                    isPopular = true,
                    description = "각종 조미료 및 양념",
                    iconName = "seasonings"
                ),
                IngredientCategoryInfo(
                    categoryName = "fruits",
                    displayName = "과일",
                    ingredientCount = 0,
                    subcategories = listOf("신선과일", "건과일", "견과류"),
                    isPopular = false,
                    description = "신선한 과일류",
                    iconName = "fruits"
                )
            )
        }

        /**
         * 카테고리명으로 기본 카테고리 정보 찾기
         */
        fun findDefaultCategory(categoryName: String): IngredientCategoryInfo? {
            return getDefaultCategories().find {
                it.categoryName == categoryName || it.displayName == categoryName
            }
        }
    }
}

/**
 * 재료 제안 컨텍스트
 * AI 기반 제안 시 필요한 정보들
 */
@Serializable
data class IngredientSuggestionContext(
    val userId: String,
    val currentRecipeId: String? = null,        // Recipe 객체 대신 ID 참조로 변경
    val userIngredientIds: List<String> = emptyList(), // UserIngredient 리스트 대신 ID 리스트로 변경
    val userPreferenceId: String? = null,       // UserPreference 객체 대신 ID 참조로 변경
    val mealType: String? = null,           // "아침", "점심", "저녁"
    val cuisineType: String? = null,        // "한식", "양식", "중식"
    val difficulty: Int? = null,            // 요리 난이도 1-5
    val cookingTime: Int? = null,           // 희망 조리시간 (분)
    val season: Season? = null,             // 현재 계절
    val budget: BudgetRange? = null,        // 예산 범위
    val dietaryRestrictions: List<DietRestriction> = emptyList(), // 식이 제한
    val allergies: List<String> = emptyList(), // 알레르기 성분
    val searchHistory: List<String> = emptyList(), // 최근 검색 기록
    val contextType: SuggestionContextType = SuggestionContextType.GENERAL
) {
    /**
     * 컨텍스트 유효성 검증
     */
    fun isValid(): Boolean {
        return userId.isNotBlank()
    }

    /**
     * 제안 우선순위 계산 (높을수록 더 맞춤형 제안 가능)
     */
    fun getPriority(): Int {
        var priority = 0
        if (currentRecipeId != null) priority += 10
        if (userPreferenceId != null) priority += 5
        if (mealType != null) priority += 3
        if (season != null) priority += 2
        if (cuisineType != null) priority += 2
        if (difficulty != null) priority += 1
        if (cookingTime != null) priority += 1
        if (dietaryRestrictions.isNotEmpty()) priority += 3
        if (searchHistory.isNotEmpty()) priority += 2
        return priority
    }

    /**
     * 컨텍스트 완성도 평가 (0.0 ~ 1.0)
     */
    fun getCompletenessScore(): Double {
        val maxPossibleScore = 30.0 // 모든 정보가 있을 때 최대 점수
        return (getPriority().toDouble() / maxPossibleScore).coerceAtMost(1.0)
    }

    /**
     * 특정 카테고리에 대한 선호도 점수
     * 실제 UserPreference 객체가 필요한 경우 Repository에서 조회 후 계산
     */
    fun getCategoryPreference(categoryName: String, userPreference: UserPreference?): Double {
        return when {
            userPreference == null -> 0.5 // 중립
            userPreference.preferredCategories.contains(categoryName) -> 1.0
            userPreference.dislikedIngredients.any { disliked ->
                // 카테고리 내 싫어하는 재료가 있으면 선호도 낮음
                disliked.contains(categoryName, ignoreCase = true)
            } -> 0.2
            else -> 0.5
        }
    }

    /**
     * 제안 컨텍스트 요약 텍스트
     */
    fun getSummary(): String {
        val parts = mutableListOf<String>()

        mealType?.let { parts.add("${it} 요리") }
        cuisineType?.let { parts.add(it) }
        difficulty?.let { parts.add("난이도 ${it}") }
        cookingTime?.let { parts.add("${it}분 이내") }
        season?.let { parts.add("${it.displayName} 제철") }

        if (dietaryRestrictions.isNotEmpty()) {
            parts.add(dietaryRestrictions.joinToString(",") { it.displayName })
        }

        return if (parts.isNotEmpty()) {
            parts.joinToString(" • ")
        } else {
            "일반 재료 제안"
        }
    }

    companion object {
        /**
         * 기본 컨텍스트 생성
         */
        fun createDefault(userId: String): IngredientSuggestionContext {
            return IngredientSuggestionContext(
                userId = userId,
                season = Season.getCurrentSeason(),
                contextType = SuggestionContextType.GENERAL
            )
        }

        /**
         * 레시피 기반 컨텍스트 생성
         */
        fun createForRecipe(
            userId: String,
            recipeId: String,
            recipe: Recipe,
            userIngredientIds: List<String>
        ): IngredientSuggestionContext {
            return IngredientSuggestionContext(
                userId = userId,
                currentRecipeId = recipeId,
                userIngredientIds = userIngredientIds,
                mealType = recipe.mealType,
                cuisineType = recipe.category,
                difficulty = recipe.difficulty,
                cookingTime = recipe.cookingTimeMinutes,
                season = Season.getCurrentSeason(),
                contextType = SuggestionContextType.RECIPE_BASED
            )
        }
    }
}

/**
 * 제안 컨텍스트 유형
 */
enum class SuggestionContextType(val displayName: String) {
    GENERAL("일반 제안"),
    RECIPE_BASED("레시피 기반"),
    SHOPPING_LIST("장보기 목록"),
    SEASONAL("계절 맞춤"),
    DIETARY("식이 맞춤"),
    QUICK_MEAL("간편 요리"),
    HEALTHY("건강식"),
    BUDGET("경제적")
}

/**
 * 재료 검색 통계
 * 인기 검색어, 검색 패턴 분석 결과
 */
@Serializable
data class IngredientSearchStats(
    val totalSearches: Int,                                    // 총 검색 횟수
    val uniqueSearches: Int,                                   // 고유 검색어 수
    val topSearchedIngredients: List<Pair<String, Int>>,       // 재료명과 검색 횟수
    val searchTrends: Map<String, List<Int>>,                  // 일별 검색 트렌드
    val noResultQueries: List<String>,                         // 검색 결과가 없었던 쿼리들
    val averageResultsPerSearch: Double,                       // 검색당 평균 결과 수
    val period: Int,                                          // 분석 기간 (일)
    val categorySearchDistribution: Map<String, Int> = emptyMap(), // 카테고리별 검색 분포
    val searchTimeDistribution: Map<Int, Int> = emptyMap(),    // 시간대별 검색 분포 (0-23시)
    val averageQueryLength: Double = 0.0,                     // 평균 검색어 길이
    val mostCommonSearchPatterns: List<String> = emptyList(), // 자주 사용되는 검색 패턴
    val seasonalSearchTrends: Map<String, Map<String, Int>> = emptyMap() // 계절별 검색 트렌드
) {
    /**
     * 가장 인기 있는 재료 Top N
     */
    fun getTopIngredients(limit: Int = 5): List<String> {
        return topSearchedIngredients.take(limit).map { it.first }
    }

    /**
     * 검색 효율성 점수 (0.0 ~ 1.0)
     * 검색 결과가 없는 비율이 낮을수록 높은 점수
     */
    fun getSearchEfficiency(): Double {
        return if (totalSearches > 0) {
            val noResultRate = noResultQueries.size.toDouble() / totalSearches
            (1.0 - noResultRate).coerceAtLeast(0.0)
        } else {
            1.0
        }
    }

    /**
     * 검색 다양성 점수 (0.0 ~ 1.0)
     * 고유 검색어 비율이 높을수록 높은 점수
     */
    fun getSearchDiversity(): Double {
        return if (totalSearches > 0) {
            (uniqueSearches.toDouble() / totalSearches).coerceAtMost(1.0)
        } else {
            0.0
        }
    }

    /**
     * 가장 활발한 검색 시간대
     */
    fun getPeakSearchHour(): Int? {
        return searchTimeDistribution.maxByOrNull { it.value }?.key
    }

    /**
     * 가장 인기 있는 카테고리
     */
    fun getTopSearchedCategory(): String? {
        return categorySearchDistribution.maxByOrNull { it.value }?.key
    }

    /**
     * 검색 통계 요약 텍스트
     */
    fun getSummary(): String {
        val efficiency = (getSearchEfficiency() * 100).toInt()
        val diversity = (getSearchDiversity() * 100).toInt()
        val topIngredient = getTopIngredients(1).firstOrNull() ?: "없음"
        val topCategory = getTopSearchedCategory() ?: "없음"

        return """
            검색 통계 (${period}일간)
            • 총 검색: ${totalSearches}회 (고유: ${uniqueSearches}개)
            • 검색 효율성: ${efficiency}%
            • 검색 다양성: ${diversity}%
            • 인기 재료: $topIngredient
            • 인기 카테고리: $topCategory
            • 평균 검색어 길이: ${"%.1f".format(averageQueryLength)}자
        """.trimIndent()
    }

    /**
     * 개선 제안 생성
     */
    fun getImprovementSuggestions(): List<String> {
        val suggestions = mutableListOf<String>()

        if (getSearchEfficiency() < 0.8) {
            suggestions.add("검색어 자동완성 기능을 개선하여 검색 성공률을 높여보세요")
        }

        if (getSearchDiversity() < 0.3) {
            suggestions.add("다양한 재료를 검색해보세요. 새로운 요리에 도전해보는 것은 어떨까요?")
        }

        if (averageQueryLength < 2.0) {
            suggestions.add("더 구체적인 검색어를 사용하면 더 정확한 결과를 얻을 수 있습니다")
        }

        if (noResultQueries.size > totalSearches * 0.2) {
            suggestions.add("검색되지 않는 재료가 많습니다. 새로운 재료 제안 기능을 사용해보세요")
        }

        return suggestions
    }

    companion object {
        /**
         * 빈 통계 객체 생성
         */
        fun empty(period: Int = 30): IngredientSearchStats {
            return IngredientSearchStats(
                totalSearches = 0,
                uniqueSearches = 0,
                topSearchedIngredients = emptyList(),
                searchTrends = emptyMap(),
                noResultQueries = emptyList(),
                averageResultsPerSearch = 0.0,
                period = period
            )
        }

        /**
         * 검색 기록으로부터 통계 생성
         */
        fun fromSearchHistory(
            searchHistory: List<SearchRecord>,
            period: Int = 30
        ): IngredientSearchStats {
            if (searchHistory.isEmpty()) return empty(period)

            val totalSearches = searchHistory.size
            val uniqueSearches = searchHistory.map { it.query }.distinct().size
            val noResultQueries = searchHistory.filter { it.resultCount == 0 }.map { it.query }
            val averageResults = searchHistory.map { it.resultCount }.average()

            // 인기 검색어 계산
            val searchCounts = searchHistory.groupBy { it.query }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }

            return IngredientSearchStats(
                totalSearches = totalSearches,
                uniqueSearches = uniqueSearches,
                topSearchedIngredients = searchCounts,
                searchTrends = emptyMap(), // 복잡한 계산으로 별도 구현 필요
                noResultQueries = noResultQueries,
                averageResultsPerSearch = averageResults,
                period = period,
                averageQueryLength = searchHistory.map { it.query.length }.average()
            )
        }
    }
}

/**
 * 검색 기록 단일 항목
 * 통계 생성을 위한 헬퍼 클래스
 */
@Serializable
data class SearchRecord(
    val query: String,
    val resultCount: Int,
    val timestamp: Long,
    val userId: String,
    val category: String? = null,
    val searchDurationMs: Long = 0
) {
    /**
     * 검색 시간대 (0-23시)
     */
    fun getHourOfDay(): Int {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(java.util.Calendar.HOUR_OF_DAY)
    }

    /**
     * 검색 성공 여부
     */
    fun isSuccessful(): Boolean {
        return resultCount > 0
    }

    /**
     * 검색 품질 점수 (0.0 ~ 1.0)
     */
    fun getQualityScore(): Double {
        return when {
            resultCount == 0 -> 0.0
            resultCount in 1..5 -> 1.0  // 최적의 결과 수
            resultCount in 6..20 -> 0.8 // 양호한 결과 수
            resultCount in 21..50 -> 0.6 // 보통 결과 수
            else -> 0.4 // 너무 많은 결과
        }
    }
}

/**
 * 계절 정보
 * 제철 재료 추천 및 계절별 분석용
 */
enum class Season(val displayName: String, val months: IntRange) {
    SPRING("봄", 3..5),
    SUMMER("여름", 6..8),
    AUTUMN("가을", 9..11),
    WINTER("겨울", listOf(12, 1, 2));

    constructor(displayName: String, months: List<Int>) : this(displayName, months.first()..months.last()) {
        // WINTER의 경우 특별 처리 필요
    }

    /**
     * 계절 아이콘 이름
     */
    fun getIconName(): String {
        return when (this) {
            SPRING -> "spring"
            SUMMER -> "summer"
            AUTUMN -> "autumn"
            WINTER -> "winter"
        }
    }

    /**
     * 계절 색상 (UI용)
     */
    fun getColor(): String {
        return when (this) {
            SPRING -> "#4CAF50" // 연두색
            SUMMER -> "#FF9800" // 주황색
            AUTUMN -> "#FF5722" // 빨간색
            WINTER -> "#2196F3" // 파란색
        }
    }

    /**
     * 계절 설명
     */
    fun getDescription(): String {
        return when (this) {
            SPRING -> "새싹이 돋는 봄, 신선한 나물과 채소의 계절"
            SUMMER -> "무더운 여름, 시원한 음식과 과일의 계절"
            AUTUMN -> "수확의 계절, 풍성한 곡물과 과일의 계절"
            WINTER -> "추운 겨울, 따뜻한 국물 요리의 계절"
        }
    }

    companion object {
        /**
         * 현재 월에 해당하는 계절 반환
         */
        fun getCurrentSeason(): Season {
            val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
            return values().find { season ->
                when (season) {
                    WINTER -> currentMonth == 12 || currentMonth in 1..2
                    else -> currentMonth in season.months
                }
            } ?: SPRING
        }

        /**
         * 월에 해당하는 계절 반환
         */
        fun getSeasonByMonth(month: Int): Season {
            return when (month) {
                in 3..5 -> SPRING
                in 6..8 -> SUMMER
                in 9..11 -> AUTUMN
                12, 1, 2 -> WINTER
                else -> getCurrentSeason() // 잘못된 월이면 현재 계절
            }
        }

        /**
         * 다음 계절 반환
         */
        fun Season.getNext(): Season {
            return when (this) {
                SPRING -> SUMMER
                SUMMER -> AUTUMN
                AUTUMN -> WINTER
                WINTER -> SPRING
            }
        }

        /**
         * 이전 계절 반환
         */
        fun Season.getPrevious(): Season {
            return when (this) {
                SPRING -> WINTER
                SUMMER -> SPRING
                AUTUMN -> SUMMER
                WINTER -> AUTUMN
            }
        }
    }
}