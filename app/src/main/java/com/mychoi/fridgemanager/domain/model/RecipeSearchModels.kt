package com.mychoi.fridgemanager.domain.model

import kotlinx.serialization.Serializable

/**
 * 레시피 검색 관련 데이터 모델들
 * 검색 요청, 필터링, 정렬 기능을 위한 클래스들
 */

/**
 * 레시피 검색 요청 데이터 클래스
 * 고급 검색 기능을 위한 필터 조건들
 */
@Serializable
data class RecipeSearchRequest(
    val query: String = "",                          // 검색어
    val categories: List<String> = emptyList(),      // 포함할 카테고리
    val excludedCategories: List<String> = emptyList(), // 제외할 카테고리
    val minDifficulty: Int? = null,                  // 최소 난이도
    val maxDifficulty: Int? = null,                  // 최대 난이도
    val maxCookingTime: Int? = null,                 // 최대 조리시간 (분)
    val requiredIngredients: List<String> = emptyList(), // 필수 재료
    val excludedIngredients: List<String> = emptyList(), // 제외할 재료
    val tags: List<String> = emptyList(),            // 포함할 태그
    val mealTypes: List<String> = emptyList(),       // 식사 타입
    val sortBy: RecipeSortBy = RecipeSortBy.RELEVANCE, // 정렬 기준
    val limit: Int = 20                              // 결과 제한 수
) {
    init {
        // 검색 조건 검증
        require(limit > 0 && limit <= 100) { "검색 결과 수는 1-100 사이여야 합니다" }
        minDifficulty?.let { require(it in 1..5) { "최소 난이도는 1-5 사이여야 합니다" } }
        maxDifficulty?.let { require(it in 1..5) { "최대 난이도는 1-5 사이여야 합니다" } }
        maxCookingTime?.let { require(it > 0) { "최대 조리시간은 0보다 커야 합니다" } }

        // 논리적 검증
        if (minDifficulty != null && maxDifficulty != null) {
            require(minDifficulty <= maxDifficulty) { "최소 난이도는 최대 난이도보다 작거나 같아야 합니다" }
        }
    }

    /**
     * 필터 조건이 설정되어 있는지 확인
     */
    fun hasFilters(): Boolean {
        return query.isNotBlank() || categories.isNotEmpty() ||
                excludedCategories.isNotEmpty() || minDifficulty != null ||
                maxDifficulty != null || maxCookingTime != null ||
                requiredIngredients.isNotEmpty() || excludedIngredients.isNotEmpty() ||
                tags.isNotEmpty() || mealTypes.isNotEmpty()
    }

    /**
     * 검색 조건 요약 텍스트
     */
    fun getSummary(): String {
        val conditions = mutableListOf<String>()

        if (query.isNotBlank()) conditions.add("검색어: '$query'")
        if (categories.isNotEmpty()) conditions.add("카테고리: ${categories.joinToString(", ")}")
        if (maxCookingTime != null) conditions.add("조리시간: ${maxCookingTime}분 이내")
        if (minDifficulty != null || maxDifficulty != null) {
            val difficultyRange = when {
                minDifficulty != null && maxDifficulty != null -> "$minDifficulty-$maxDifficulty"
                minDifficulty != null -> "${minDifficulty}+"
                else -> "~$maxDifficulty"
            }
            conditions.add("난이도: $difficultyRange")
        }
        if (requiredIngredients.isNotEmpty()) conditions.add("필수 재료: ${requiredIngredients.joinToString(", ")}")

        return if (conditions.isNotEmpty()) {
            conditions.joinToString(" | ")
        } else {
            "전체 레시피"
        }
    }

    /**
     * 빠른 검색용 프리셋 생성
     */
    companion object {
        /**
         * 간단한 레시피 검색 (30분 이내, 쉬운 난이도)
         */
        fun quickAndEasy(query: String = ""): RecipeSearchRequest {
            return RecipeSearchRequest(
                query = query,
                maxCookingTime = 30,
                maxDifficulty = 2,
                sortBy = RecipeSortBy.COOKING_TIME
            )
        }

        /**
         * 인기 레시피 검색
         */
        fun popular(category: String? = null): RecipeSearchRequest {
            return RecipeSearchRequest(
                categories = if (category != null) listOf(category) else emptyList(),
                sortBy = RecipeSortBy.POPULARITY,
                limit = 10
            )
        }

        /**
         * 특정 재료 활용 검색
         */
        fun withIngredients(ingredients: List<String>): RecipeSearchRequest {
            return RecipeSearchRequest(
                requiredIngredients = ingredients,
                sortBy = RecipeSortBy.MATCHING_SCORE
            )
        }

        /**
         * 시간대별 레시피 검색
         */
        fun forMealTime(mealType: String, maxTime: Int = 60): RecipeSearchRequest {
            return RecipeSearchRequest(
                mealTypes = listOf(mealType),
                maxCookingTime = maxTime,
                sortBy = RecipeSortBy.POPULARITY
            )
        }
    }
}

/**
 * 레시피 정렬 기준 열거형
 */
enum class RecipeSortBy(val displayName: String, val description: String) {
    RELEVANCE("관련도", "검색어와의 관련성 순"),
    POPULARITY("인기순", "조회수가 높은 순"),
    NEWEST("최신순", "최근에 추가된 순"),
    COOKING_TIME("조리시간", "조리시간이 짧은 순"),
    DIFFICULTY("난이도", "쉬운 난이도 순"),
    MATCHING_SCORE("매칭점수", "보유 재료와의 매칭도 순"),
    RATING("평점순", "평점이 높은 순"),
    ALPHABETICAL("가나다순", "레시피명 가나다 순"),
    INGREDIENT_COUNT("재료수", "필요한 재료가 적은 순")
}

/**
 * 검색 결과 메타데이터
 * 검색 성능 및 결과 분석을 위한 정보
 */
data class RecipeSearchResult(
    val recipes: List<Recipe>,
    val totalCount: Int,                    // 전체 결과 수 (페이징 고려)
    val searchTime: Long,                   // 검색 소요 시간 (ms)
    val appliedFilters: RecipeSearchRequest,
    val suggestions: List<String> = emptyList(), // 추천 검색어
    val categoryBreakdown: Map<String, Int> = emptyMap() // 카테고리별 결과 수
) {
    /**
     * 검색 결과 요약
     */
    fun getSummary(): String {
        val timeText = if (searchTime < 1000) "${searchTime}ms" else "${searchTime/1000}s"
        return "${recipes.size}개 결과 (전체 ${totalCount}개) | 검색 시간: $timeText"
    }

    /**
     * 검색이 성공적인지 판단
     */
    fun isSuccessful(): Boolean {
        return recipes.isNotEmpty()
    }

    /**
     * 페이징이 필요한지 확인
     */
    fun hasMoreResults(): Boolean {
        return totalCount > recipes.size
    }
}

/**
 * 자동완성 검색 결과
 */
data class RecipeSearchSuggestion(
    val text: String,                       // 제안 텍스트
    val type: SuggestionType,              // 제안 타입
    val resultCount: Int = 0,              // 예상 결과 수
    val metadata: Map<String, Any> = emptyMap() // 추가 메타데이터
) {
    /**
     * 제안 표시용 텍스트
     */
    fun getDisplayText(): String {
        val countText = if (resultCount > 0) " (${resultCount}개)" else ""
        return "$text$countText"
    }
}

/**
 * 자동완성 제안 타입
 */
enum class SuggestionType(val displayName: String) {
    RECIPE_NAME("레시피명"),
    INGREDIENT("재료"),
    CATEGORY("카테고리"),
    TAG("태그"),
    RECENT_SEARCH("최근 검색"),
    POPULAR_SEARCH("인기 검색")
}

/**
 * 검색 히스토리 관리
 */
data class SearchHistory(
    val query: String,
    val searchedAt: Long,
    val resultCount: Int,
    val wasSuccessful: Boolean
) {
    /**
     * 검색한 지 며칠 지났는지
     */
    fun getDaysAgo(): Long {
        val now = System.currentTimeMillis()
        return (now - searchedAt) / (24 * 60 * 60 * 1000)
    }

    /**
     * 최근 검색인지 확인 (7일 이내)
     */
    fun isRecent(): Boolean {
        return getDaysAgo() <= 7
    }
}

/**
 * 검색 필터 프리셋
 * 사용자가 자주 사용하는 검색 조합을 저장
 */
data class SearchFilterPreset(
    val name: String,                       // 프리셋 이름
    val description: String,                // 설명
    val searchRequest: RecipeSearchRequest,
    val useCount: Int = 0,                 // 사용 횟수
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * 프리셋 요약
     */
    fun getSummary(): String {
        return "$name | ${searchRequest.getSummary()} | 사용: ${useCount}회"
    }

    companion object {
        /**
         * 기본 프리셋들
         */
        fun getDefaultPresets(): List<SearchFilterPreset> {
            return listOf(
                SearchFilterPreset(
                    name = "빠른 요리",
                    description = "30분 이내로 만들 수 있는 간단한 요리",
                    searchRequest = RecipeSearchRequest.quickAndEasy()
                ),
                SearchFilterPreset(
                    name = "인기 한식",
                    description = "조회수가 높은 한식 레시피",
                    searchRequest = RecipeSearchRequest.popular("한식")
                ),
                SearchFilterPreset(
                    name = "간식 레시피",
                    description = "간단하게 만들 수 있는 간식",
                    searchRequest = RecipeSearchRequest(
                        categories = listOf("간식"),
                        maxCookingTime = 45,
                        sortBy = RecipeSortBy.POPULARITY
                    )
                ),
                SearchFilterPreset(
                    name = "초보자용",
                    description = "요리 초보자도 쉽게 만들 수 있는 레시피",
                    searchRequest = RecipeSearchRequest(
                        maxDifficulty = 2,
                        maxCookingTime = 60,
                        sortBy = RecipeSortBy.DIFFICULTY
                    )
                )
            )
        }
    }
}