package com.mychoi.fridgemanager.domain.model

import kotlinx.serialization.Serializable

/**
 * 재료 분석 관련 데이터 모델들
 * IngredientRepository의 분석 기능에서 사용되는 모델 정의
 */

/**
 * 사용자 재료 선호도 분석 결과
 * 사용자의 재료 사용 패턴, 선호도, 다양성 등을 종합 분석한 결과
 */
@Serializable
data class IngredientPreferenceAnalysis(
    val userId: String,                                           // 분석 대상 사용자 ID
    val favoriteCategories: List<Pair<String, Double>>,          // 카테고리와 선호도 점수 (0.0~1.0)
    val frequentIngredients: List<Pair<String, Int>>,            // 자주 사용하는 재료와 사용 횟수
    val avoidedIngredients: List<String>,                        // 피하는 재료들 (검색했지만 사용하지 않은 재료)
    val seasonalPatterns: Map<String, List<String>>,             // 계절별 선호 재료 (Season.name -> 재료 목록)
    val cookingStylePreference: CookingStylePreference,          // 요리 스타일 선호도
    val diversityScore: Double,                                  // 재료 다양성 점수 (0.0 ~ 1.0)
    val analysisDate: Long = System.currentTimeMillis(),        // 분석 수행 일시
    val analysisPeriod: Int = 90,                               // 분석 기간 (일)
    val totalRecordsAnalyzed: Int = 0,                          // 분석에 사용된 총 기록 수
    val confidenceLevel: Double = 0.0,                         // 분석 신뢰도 (0.0 ~ 1.0)
    val trendAnalysis: TrendAnalysis? = null,                   // 트렌드 분석 결과
    val comparisonWithAverage: ComparisonResult? = null,        // 평균 사용자와의 비교 결과
    val recommendations: List<AnalysisRecommendation> = emptyList() // 분석 기반 추천사항
) {
    /**
     * 주요 선호 카테고리 (상위 N개)
     */
    fun getTopCategories(limit: Int = 3): List<String> {
        return favoriteCategories.take(limit).map { it.first }
    }

    /**
     * 재료 모험 성향 분석
     */
    fun getAdventureLevel(): AdventureLevel {
        return when {
            diversityScore >= 0.8 -> AdventureLevel.ADVENTUROUS
            diversityScore >= 0.6 -> AdventureLevel.MODERATE
            diversityScore >= 0.4 -> AdventureLevel.CONSERVATIVE
            else -> AdventureLevel.VERY_CONSERVATIVE
        }
    }

    /**
     * 분석 품질 등급 (A~F)
     */
    fun getAnalysisQuality(): String {
        val recordScore = when {
            totalRecordsAnalyzed >= 100 -> 1.0
            totalRecordsAnalyzed >= 50 -> 0.8
            totalRecordsAnalyzed >= 20 -> 0.6
            totalRecordsAnalyzed >= 10 -> 0.4
            else -> 0.2
        }

        val periodScore = when {
            analysisPeriod >= 90 -> 1.0
            analysisPeriod >= 60 -> 0.8
            analysisPeriod >= 30 -> 0.6
            analysisPeriod >= 14 -> 0.4
            else -> 0.2
        }

        val overallScore = (recordScore + periodScore + confidenceLevel) / 3

        return when {
            overallScore >= 0.9 -> "A"
            overallScore >= 0.8 -> "B"
            overallScore >= 0.7 -> "C"
            overallScore >= 0.6 -> "D"
            else -> "F"
        }
    }

    /**
     * 가장 선호하는 계절 재료
     */
    fun getFavoriteSeasonalIngredients(): Map<String, String> {
        return seasonalPatterns.mapValues { (_, ingredients) ->
            ingredients.firstOrNull() ?: "없음"
        }
    }

    /**
     * 선호도 변화 트렌드
     */
    fun getPreferenceTrend(): PreferenceTrend {
        return trendAnalysis?.preferenceTrend ?: PreferenceTrend.STABLE
    }

    /**
     * 사용자 프로필 요약
     */
    fun getProfileSummary(): String {
        val adventureLevel = getAdventureLevel().displayName
        val topCategory = getTopCategories(1).firstOrNull() ?: "없음"
        val diversityPercent = (diversityScore * 100).toInt()
        val quality = getAnalysisQuality()

        return """
            재료 선호도 프로필 (신뢰도: ${quality}등급)
            • 모험 성향: $adventureLevel
            • 주요 선호 카테고리: $topCategory
            • 재료 다양성: ${diversityPercent}%
            • 요리 스타일: ${cookingStylePreference.getStyleDescription()}
        """.trimIndent()
    }

    /**
     * 개인화 추천 점수 계산
     */
    fun calculatePersonalizationScore(): Double {
        var score = 0.0

        // 기본 점수
        score += confidenceLevel * 0.3
        score += diversityScore * 0.2

        // 데이터 충분성
        val dataScore = when {
            totalRecordsAnalyzed >= 100 -> 0.3
            totalRecordsAnalyzed >= 50 -> 0.2
            totalRecordsAnalyzed >= 20 -> 0.1
            else -> 0.05
        }
        score += dataScore

        // 분석 기간
        val periodScore = when {
            analysisPeriod >= 90 -> 0.2
            analysisPeriod >= 60 -> 0.15
            analysisPeriod >= 30 -> 0.1
            else -> 0.05
        }
        score += periodScore

        return score.coerceAtMost(1.0)
    }

    companion object {
        /**
         * 빈 분석 결과 생성 (데이터 부족 시)
         */
        fun empty(userId: String): IngredientPreferenceAnalysis {
            return IngredientPreferenceAnalysis(
                userId = userId,
                favoriteCategories = emptyList(),
                frequentIngredients = emptyList(),
                avoidedIngredients = emptyList(),
                seasonalPatterns = emptyMap(),
                cookingStylePreference = CookingStylePreference.default(),
                diversityScore = 0.0,
                confidenceLevel = 0.0
            )
        }

        /**
         * 최소 데이터로 분석 결과 생성
         */
        fun createMinimal(
            userId: String,
            ingredients: List<Pair<String, Int>>,
            categories: List<String>
        ): IngredientPreferenceAnalysis {
            val diversity = if (ingredients.isNotEmpty()) {
                ingredients.size.toDouble() / (ingredients.maxOfOrNull { it.second } ?: 1)
            } else {
                0.0
            }

            val categoryPreferences = categories.mapIndexed { index, category ->
                category to (1.0 - index * 0.1).coerceAtLeast(0.1)
            }

            return IngredientPreferenceAnalysis(
                userId = userId,
                favoriteCategories = categoryPreferences,
                frequentIngredients = ingredients,
                avoidedIngredients = emptyList(),
                seasonalPatterns = emptyMap(),
                cookingStylePreference = CookingStylePreference.default(),
                diversityScore = diversity.coerceAtMost(1.0),
                totalRecordsAnalyzed = ingredients.sumOf { it.second },
                confidenceLevel = if (ingredients.size >= 5) 0.6 else 0.3
            )
        }
    }
}

/**
 * 요리 스타일 선호도
 * 사용자의 요리 성향을 다차원적으로 분석한 결과
 */
@Serializable
data class CookingStylePreference(
    val traditionalVsModern: Double,        // -1.0(전통적) ~ 1.0(현대적)
    val simplicityVsComplexity: Double,     // -1.0(단순) ~ 1.0(복잡)
    val localVsInternational: Double,       // -1.0(현지) ~ 1.0(국제적)
    val healthFocus: Double,                // 0.0(관심없음) ~ 1.0(매우 중요)
    val budgetConsciousness: Double = 0.5,  // 0.0(예산 무관심) ~ 1.0(예산 중시)
    val seasonalAwareness: Double = 0.5,    // 0.0(계절 무관심) ~ 1.0(계절 중시)
    val experimentalness: Double = 0.5,     // 0.0(보수적) ~ 1.0(실험적)
    val socialCooking: Double = 0.5         // 0.0(혼자 요리) ~ 1.0(여럿이 요리)
) {
    /**
     * 요리 스타일 설명
     */
    fun getStyleDescription(): String {
        val traditional = if (traditionalVsModern < -0.3) "전통적"
        else if (traditionalVsModern > 0.3) "현대적"
        else "절충적"

        val complexity = if (simplicityVsComplexity < -0.3) "간단한"
        else if (simplicityVsComplexity > 0.3) "복잡한"
        else "적당한"

        val cuisine = if (localVsInternational < -0.3) "한식 위주"
        else if (localVsInternational > 0.3) "다국적"
        else "다양한"

        val health = when {
            healthFocus >= 0.7 -> "건강 중시"
            healthFocus >= 0.4 -> "건강 고려"
            else -> "맛 우선"
        }

        return "$traditional, $complexity 요리를 선호하며 $cuisine 음식을 즐기는 $health 성향"
    }

    /**
     * 요리 스타일 카테고리
     */
    fun getStyleCategory(): CookingStyleCategory {
        val avgModernness = (traditionalVsModern + localVsInternational) / 2
        val avgComplexity = (simplicityVsComplexity + experimentalness) / 2

        return when {
            avgModernness > 0.3 && avgComplexity > 0.3 -> CookingStyleCategory.MODERN_ADVENTUROUS
            avgModernness > 0.3 && avgComplexity < -0.3 -> CookingStyleCategory.MODERN_SIMPLE
            avgModernness < -0.3 && avgComplexity > 0.3 -> CookingStyleCategory.TRADITIONAL_COMPLEX
            avgModernness < -0.3 && avgComplexity < -0.3 -> CookingStyleCategory.TRADITIONAL_SIMPLE
            healthFocus > 0.7 -> CookingStyleCategory.HEALTH_FOCUSED
            budgetConsciousness > 0.7 -> CookingStyleCategory.BUDGET_CONSCIOUS
            socialCooking > 0.7 -> CookingStyleCategory.SOCIAL_COOK
            else -> CookingStyleCategory.BALANCED
        }
    }

    /**
     * 추천 레시피 필터 생성
     */
    fun createRecipeFilter(): Map<String, Any> {
        val filter = mutableMapOf<String, Any>()

        // 난이도 필터
        val preferredDifficulty = when {
            simplicityVsComplexity < -0.5 -> 1..2
            simplicityVsComplexity > 0.5 -> 4..5
            else -> 2..4
        }
        filter["difficulty"] = preferredDifficulty

        // 카테고리 필터
        val preferredCategories = mutableListOf<String>()
        if (localVsInternational < -0.3) {
            preferredCategories.add("한식")
        }
        if (localVsInternational > 0.3) {
            preferredCategories.addAll(listOf("양식", "중식", "일식"))
        }
        if (preferredCategories.isNotEmpty()) {
            filter["categories"] = preferredCategories
        }

        // 건강 관련 태그
        if (healthFocus > 0.5) {
            filter["tags"] = listOf("건강식", "저칼로리", "영양가득")
        }

        return filter
    }

    /**
     * 스타일 호환성 점수 계산
     */
    fun calculateCompatibility(other: CookingStylePreference): Double {
        val diff1 = kotlin.math.abs(traditionalVsModern - other.traditionalVsModern)
        val diff2 = kotlin.math.abs(simplicityVsComplexity - other.simplicityVsComplexity)
        val diff3 = kotlin.math.abs(localVsInternational - other.localVsInternational)
        val diff4 = kotlin.math.abs(healthFocus - other.healthFocus)

        val avgDiff = (diff1 + diff2 + diff3 + diff4) / 4
        return (1.0 - avgDiff).coerceAtLeast(0.0)
    }

    companion object {
        /**
         * 기본 요리 스타일 (중립적)
         */
        fun default(): CookingStylePreference {
            return CookingStylePreference(
                traditionalVsModern = 0.0,
                simplicityVsComplexity = 0.0,
                localVsInternational = 0.0,
                healthFocus = 0.5
            )
        }

        /**
         * 한국인 평균 요리 스타일
         */
        fun koreanAverage(): CookingStylePreference {
            return CookingStylePreference(
                traditionalVsModern = -0.2,     // 약간 전통적
                simplicityVsComplexity = -0.3,  // 단순한 편
                localVsInternational = -0.4,    // 한식 선호
                healthFocus = 0.6,              // 건강 관심 높음
                budgetConsciousness = 0.7,      // 가격 의식적
                seasonalAwareness = 0.8         // 계절 의식 높음
            )
        }
    }
}

/**
 * 요리 스타일 카테고리
 */
enum class CookingStyleCategory(val displayName: String, val description: String) {
    MODERN_ADVENTUROUS("모던 모험가", "새로운 요리와 복잡한 레시피를 즐기는 현대적 요리사"),
    MODERN_SIMPLE("모던 심플리스트", "현대적이지만 간단한 요리를 선호하는 효율적 요리사"),
    TRADITIONAL_COMPLEX("전통 장인", "전통적이고 정교한 요리를 추구하는 요리사"),
    TRADITIONAL_SIMPLE("전통 가정식", "간단하고 친숙한 전통 요리를 선호하는 요리사"),
    HEALTH_FOCUSED("건강 추구자", "건강한 재료와 조리법을 최우선으로 하는 요리사"),
    BUDGET_CONSCIOUS("경제적 요리사", "비용 효율성을 중시하는 실용적 요리사"),
    SOCIAL_COOK("소셜 쿡", "다른 사람과 함께 요리하고 나누는 것을 즐기는 요리사"),
    BALANCED("균형잡힌 요리사", "모든 면에서 균형잡힌 요리 스타일을 가진 요리사")
}

/**
 * 재료 모험 성향
 * 사용자가 새로운 재료를 시도하는 정도
 */
enum class AdventureLevel(val displayName: String, val description: String, val emoji: String) {
    VERY_CONSERVATIVE("매우 보수적", "익숙한 재료만 사용하며 새로운 시도를 피함", "🛡️"),
    CONSERVATIVE("보수적", "가끔 새로운 재료를 시도하지만 신중함", "🤔"),
    MODERATE("적당함", "다양한 재료를 활용하며 적절히 모험적", "⚖️"),
    ADVENTUROUS("모험적", "새로운 재료를 적극적으로 시도하고 실험함", "🚀");

    /**
     * 추천 재료 다양성 계수
     */
    fun getDiversityFactor(): Double {
        return when (this) {
            VERY_CONSERVATIVE -> 0.3
            CONSERVATIVE -> 0.5
            MODERATE -> 0.8
            ADVENTUROUS -> 1.0
        }
    }

    /**
     * 새로운 재료 제안 가중치
     */
    fun getNewIngredientWeight(): Double {
        return when (this) {
            VERY_CONSERVATIVE -> 0.1
            CONSERVATIVE -> 0.3
            MODERATE -> 0.6
            ADVENTUROUS -> 1.0
        }
    }

    /**
     * 다음 단계 모험 레벨
     */
    fun getNextLevel(): AdventureLevel? {
        return when (this) {
            VERY_CONSERVATIVE -> CONSERVATIVE
            CONSERVATIVE -> MODERATE
            MODERATE -> ADVENTUROUS
            ADVENTUROUS -> null
        }
    }

    /**
     * 이전 단계 모험 레벨
     */
    fun getPreviousLevel(): AdventureLevel? {
        return when (this) {
            VERY_CONSERVATIVE -> null
            CONSERVATIVE -> VERY_CONSERVATIVE
            MODERATE -> CONSERVATIVE
            ADVENTUROUS -> MODERATE
        }
    }
}

/**
 * 트렌드 분석 결과
 */
@Serializable
data class TrendAnalysis(
    val preferenceTrend: PreferenceTrend,                    // 전반적인 선호도 변화 트렌드
    val diversityTrend: TrendDirection,                      // 다양성 변화 트렌드
    val categoryShifts: Map<String, TrendDirection>,         // 카테고리별 선호도 변화
    val seasonalStability: Double,                           // 계절별 일관성 (0.0~1.0)
    val innovationRate: Double,                              // 새로운 재료 시도 비율
    val consistencyScore: Double,                            // 선호도 일관성 점수
    val trendConfidence: Double = 0.0                        // 트렌드 분석 신뢰도
) {
    /**
     * 트렌드 요약 텍스트
     */
    fun getSummary(): String {
        val trendText = preferenceTrend.displayName
        val diversityText = diversityTrend.displayName
        val stabilityText = when {
            seasonalStability >= 0.8 -> "매우 안정적"
            seasonalStability >= 0.6 -> "안정적"
            seasonalStability >= 0.4 -> "보통"
            else -> "변동적"
        }

        return "선호도 $trendText, 다양성 $diversityText, 계절별 패턴 $stabilityText"
    }
}

/**
 * 선호도 트렌드
 */
enum class PreferenceTrend(val displayName: String, val description: String) {
    EXPANDING("확장 중", "새로운 카테고리와 재료로 선호도가 넓어지고 있음"),
    FOCUSING("집중 중", "특정 카테고리나 재료에 집중하고 있음"),
    STABLE("안정적", "선호도가 안정적으로 유지되고 있음"),
    SHIFTING("변화 중", "선호도가 다른 방향으로 변화하고 있음"),
    EXPLORING("탐색 중", "다양한 시도를 통해 새로운 선호도를 찾고 있음")
}

/**
 * 평균과의 비교 결과
 */
@Serializable
data class ComparisonResult(
    val diversityVsAverage: Double,                          // 평균 대비 다양성 점수 차이
    val adventureVsAverage: Double,                          // 평균 대비 모험 성향 차이
    val healthFocusVsAverage: Double,                        // 평균 대비 건강 관심도 차이
    val popularityAlignment: Double,                         // 인기 재료와의 일치도
    val uniquenessScore: Double,                             // 고유성 점수 (0.0~1.0)
    val comparisonGroup: String = "전체 사용자"              // 비교 그룹
) {
    /**
     * 비교 결과 요약
     */
    fun getSummary(): String {
        val diversityText = when {
            diversityVsAverage > 0.2 -> "평균보다 다양한"
            diversityVsAverage < -0.2 -> "평균보다 단조로운"
            else -> "평균적인"
        }

        val adventureText = when {
            adventureVsAverage > 0.2 -> "평균보다 모험적인"
            adventureVsAverage < -0.2 -> "평균보다 보수적인"
            else -> "평균적인"
        }

        val uniquenessText = when {
            uniquenessScore >= 0.8 -> "매우 독특한"
            uniquenessScore >= 0.6 -> "독특한"
            uniquenessScore >= 0.4 -> "약간 독특한"
            else -> "일반적인"
        }

        return "$comparisonGroup 대비 $diversityText 재료 사용 패턴을 보이며, $adventureText 성향을 가진 $uniquenessText 사용자"
    }
}

/**
 * 분석 기반 추천사항
 */
@Serializable
data class AnalysisRecommendation(
    val type: RecommendationType,
    val title: String,
    val description: String,
    val priority: RecommendationPriority,
    val targetCategories: List<String> = emptyList(),
    val expectedImpact: String? = null,
    val actionRequired: String? = null
) {
    /**
     * 추천사항 중요도 점수
     */
    fun getImportanceScore(): Int {
        return priority.score + when (type) {
            RecommendationType.HEALTH_IMPROVEMENT -> 10
            RecommendationType.WASTE_REDUCTION -> 8
            RecommendationType.COST_OPTIMIZATION -> 6
            RecommendationType.VARIETY_EXPANSION -> 4
            RecommendationType.SKILL_DEVELOPMENT -> 3
        }
    }
}

/**
 * 추천사항 유형
 */
enum class RecommendationType(val displayName: String) {
    HEALTH_IMPROVEMENT("건강 개선"),
    WASTE_REDUCTION("음식물 낭비 줄이기"),
    COST_OPTIMIZATION("비용 최적화"),
    VARIETY_EXPANSION("다양성 확대"),
    SKILL_DEVELOPMENT("요리 실력 향상")
}

/**
 * 추천사항 우선순위
 */
enum class RecommendationPriority(val displayName: String, val score: Int) {
    CRITICAL("긴급", 10),
    HIGH("높음", 7),
    MEDIUM("보통", 5),
    LOW("낮음", 3),
    OPTIONAL("선택사항", 1)
}