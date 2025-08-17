// app/src/main/java/com/mychoi/fridgemanager/domain/model/RecipeAnalyticsModels.kt
package com.mychoi.fridgemanager.domain.model

import kotlinx.serialization.Serializable

/**
 * 레시피 통계 및 분석 관련 데이터 모델들
 * 사용자 패턴 분석, 추천 정확도, 레시피 통계 등을 위한 클래스들
 */

/**
 * 레시피 통계 정보
 */
data class RecipeStatistics(
    val totalRecipeCount: Int,
    val categoryDistribution: Map<String, Int>,
    val difficultyDistribution: Map<Int, Int>,
    val averageCookingTime: Double,
    val averageIngredientCount: Double,
    val mostPopularRecipes: List<Recipe>,
    val mostUsedIngredients: List<Pair<String, Int>>, // 재료명과 사용 횟수
    val seasonalTrends: Map<String, List<String>>, // 계절별 인기 레시피
    val timeOfDayTrends: Map<String, List<String>>, // 시간대별 인기 레시피
    val weeklyTrends: Map<String, Int> // 요일별 요리 횟수
) {
    /**
     * 가장 인기 있는 카테고리
     */
    fun getMostPopularCategory(): String? {
        return categoryDistribution.maxByOrNull { it.value }?.key
    }

    /**
     * 평균 난이도 계산
     */
    fun getAverageDifficulty(): Double {
        val totalWeightedDifficulty = difficultyDistribution.entries.sumOf { it.key * it.value }
        val totalRecipes = difficultyDistribution.values.sum()
        return if (totalRecipes > 0) totalWeightedDifficulty.toDouble() / totalRecipes else 0.0
    }

    /**
     * 통계 요약
     */
    fun getSummary(): String {
        val categoryText = getMostPopularCategory() ?: "없음"
        val avgDifficulty = String.format("%.1f", getAverageDifficulty())
        val avgTime = String.format("%.0f", averageCookingTime)
        val avgIngredients = String.format("%.0f", averageIngredientCount)

        return "총 ${totalRecipeCount}개 | 인기 카테고리: $categoryText | 평균 난이도: $avgDifficulty | 평균 조리시간: ${avgTime}분 | 평균 재료 수: ${avgIngredients}개"
    }

    /**
     * 카테고리 다양성 지수 계산
     */
    fun getCategoryDiversityIndex(): Double {
        if (categoryDistribution.isEmpty()) return 0.0

        val total = categoryDistribution.values.sum().toDouble()
        val entropy = categoryDistribution.values.sumOf { count ->
            val probability = count / total
            if (probability > 0) -probability * kotlin.math.ln(probability) else 0.0
        }

        return entropy / kotlin.math.ln(categoryDistribution.size.toDouble())
    }

    /**
     * 레시피 복잡도 분포 분석
     */
    fun getComplexityAnalysis(): ComplexityAnalysis {
        val simple = difficultyDistribution.filterKeys { it <= 2 }.values.sum()
        val moderate = difficultyDistribution.filterKeys { it == 3 }.values.sum()
        val complex = difficultyDistribution.filterKeys { it >= 4 }.values.sum()
        val total = simple + moderate + complex

        return ComplexityAnalysis(
            simpleRecipes = simple,
            moderateRecipes = moderate,
            complexRecipes = complex,
            simplePercentage = if (total > 0) (simple.toDouble() / total) * 100 else 0.0,
            moderatePercentage = if (total > 0) (moderate.toDouble() / total) * 100 else 0.0,
            complexPercentage = if (total > 0) (complex.toDouble() / total) * 100 else 0.0
        )
    }
}

/**
 * 레시피 복잡도 분석
 */
data class ComplexityAnalysis(
    val simpleRecipes: Int,
    val moderateRecipes: Int,
    val complexRecipes: Int,
    val simplePercentage: Double,
    val moderatePercentage: Double,
    val complexPercentage: Double
) {
    /**
     * 복잡도 분포 요약
     */
    fun getSummary(): String {
        return "간단: ${simpleRecipes}개(${String.format("%.1f", simplePercentage)}%) | " +
                "보통: ${moderateRecipes}개(${String.format("%.1f", moderatePercentage)}%) | " +
                "복잡: ${complexRecipes}개(${String.format("%.1f", complexPercentage)}%)"
    }
}

/**
 * 사용자 레시피 선호도 분석 결과
 */
data class UserRecipePreferenceAnalysis(
    val preferredCategories: List<Pair<String, Double>>, // 카테고리와 선호도 점수
    val preferredDifficulty: Int,
    val preferredCookingTime: Int,
    val frequentlyUsedIngredients: List<String>,
    val avoidedIngredients: List<String>,
    val cookingPatterns: Map<String, Any>, // 요일별, 시간대별 패턴 등
    val behaviorInsights: List<BehaviorInsight>,
    val seasonalPreferences: Map<String, List<String>>, // 계절별 선호 카테고리
    val improvementAreas: List<String>, // 개선 추천 영역
    val analysisConfidence: AnalysisConfidence
) {
    /**
     * 선호도 요약 텍스트
     */
    fun getPreferenceSummary(): String {
        val topCategory = preferredCategories.firstOrNull()?.first ?: "없음"
        return "선호 카테고리: $topCategory | 난이도: $preferredDifficulty | 조리시간: ${preferredCookingTime}분"
    }

    /**
     * 상위 3개 선호 카테고리
     */
    fun getTop3Categories(): List<String> {
        return preferredCategories.take(3).map { it.first }
    }

    /**
     * 개인화 점수 계산
     */
    fun getPersonalizationScore(): Double {
        val categoryScore = if (preferredCategories.isNotEmpty()) 0.3 else 0.0
        val ingredientScore = if (frequentlyUsedIngredients.size >= 5) 0.3 else
            frequentlyUsedIngredients.size * 0.06
        val patternScore = if (cookingPatterns.isNotEmpty()) 0.2 else 0.0
        val confidenceScore = when (analysisConfidence) {
            AnalysisConfidence.HIGH -> 0.2
            AnalysisConfidence.MEDIUM -> 0.15
            AnalysisConfidence.LOW -> 0.1
            AnalysisConfidence.INSUFFICIENT -> 0.0
        }

        return categoryScore + ingredientScore + patternScore + confidenceScore
    }

    /**
     * 분석 품질 등급
     */
    fun getAnalysisGrade(): String {
        val score = getPersonalizationScore()
        return when {
            score >= 0.8 -> "A"
            score >= 0.6 -> "B"
            score >= 0.4 -> "C"
            score >= 0.2 -> "D"
            else -> "F"
        }
    }
}

/**
 * 행동 인사이트
 */
data class BehaviorInsight(
    val type: InsightType,
    val title: String,
    val description: String,
    val confidence: Double, // 0.0-1.0
    val actionable: Boolean = true,
    val suggestions: List<String> = emptyList()
) {
    /**
     * 인사이트 신뢰도 등급
     */
    fun getConfidenceGrade(): String {
        return when {
            confidence >= 0.8 -> "높음"
            confidence >= 0.6 -> "보통"
            confidence >= 0.4 -> "낮음"
            else -> "매우 낮음"
        }
    }
}

/**
 * 인사이트 타입
 */
enum class InsightType(val displayName: String) {
    COOKING_PATTERN("요리 패턴"),
    PREFERENCE_TREND("선호도 변화"),
    EFFICIENCY_OPPORTUNITY("효율성 개선"),
    EXPLORATION_SUGGESTION("새로운 도전"),
    TIME_OPTIMIZATION("시간 최적화"),
    INGREDIENT_USAGE("재료 활용")
}

/**
 * 분석 신뢰도 레벨
 */
enum class AnalysisConfidence(val displayName: String, val description: String) {
    HIGH("높음", "충분한 데이터로 신뢰할 만한 분석"),
    MEDIUM("보통", "어느 정도 신뢰할 만한 분석"),
    LOW("낮음", "제한적인 데이터 기반 분석"),
    INSUFFICIENT("부족", "분석하기에 데이터가 부족함")
}

/**
 * 추천 정확도 통계
 */
data class RecommendationAccuracyStats(
    val totalRecommendations: Int,
    val executedRecommendations: Int,
    val averageRating: Double,
    val categoryAccuracy: Map<String, Double>, // 카테고리별 정확도
    val difficultyAccuracy: Map<Int, Double>,  // 난이도별 정확도
    val timeRangeAccuracy: Map<String, Double>, // 시간대별 정확도
    val weeklyTrend: List<Pair<String, Double>>, // 주간 정확도 추세
    val userFeedbackSummary: Map<String, Int>, // 피드백 유형별 집계
    val improvementMetrics: ImprovementMetrics
) {
    /**
     * 전체 추천 정확도 계산 (실행률)
     */
    fun getOverallAccuracy(): Double {
        return if (totalRecommendations > 0) {
            (executedRecommendations.toDouble() / totalRecommendations) * 100
        } else 0.0
    }

    /**
     * 추천 등급 계산
     */
    fun getAccuracyGrade(): String {
        val accuracy = getOverallAccuracy()
        return when {
            accuracy >= 80 -> "A" // 매우 정확
            accuracy >= 60 -> "B" // 정확
            accuracy >= 40 -> "C" // 보통
            accuracy >= 20 -> "D" // 부족
            else -> "F" // 매우 부족
        }
    }

    /**
     * 정확도 요약
     */
    fun getSummary(): String {
        val accuracy = String.format("%.1f", getOverallAccuracy())
        val grade = getAccuracyGrade()
        val rating = String.format("%.1f", averageRating)

        return "추천 정확도: ${accuracy}% (${grade}등급) | 평균 평점: ${rating}/5.0 | 총 ${totalRecommendations}개 추천 중 ${executedRecommendations}개 실행"
    }

    /**
     * 가장 정확한 카테고리
     */
    fun getBestCategory(): String? {
        return categoryAccuracy.maxByOrNull { it.value }?.key
    }

    /**
     * 개선이 필요한 카테고리
     */
    fun getWorstCategory(): String? {
        return categoryAccuracy.minByOrNull { it.value }?.key
    }

    /**
     * 추천 성능 트렌드
     */
    fun getPerformanceTrend(): PerformanceTrend {
        if (weeklyTrend.size < 2) return PerformanceTrend.STABLE

        val recent = weeklyTrend.takeLast(3).map { it.second }.average()
        val previous = weeklyTrend.dropLast(3).takeLast(3).map { it.second }.average()

        return when {
            recent > previous + 5 -> PerformanceTrend.IMPROVING
            recent < previous - 5 -> PerformanceTrend.DECLINING
            else -> PerformanceTrend.STABLE
        }
    }
}

/**
 * 성능 트렌드
 */
enum class PerformanceTrend(val displayName: String, val emoji: String) {
    IMPROVING("개선 중", "📈"),
    STABLE("안정", "➡️"),
    DECLINING("하락", "📉")
}

/**
 * 개선 지표
 */
data class ImprovementMetrics(
    val accuracyImprovement: Double, // 정확도 개선률 (%)
    val responseTimeImprovement: Double, // 응답 시간 개선률 (%)
    val userSatisfactionScore: Double, // 사용자 만족도 (1-10)
    val diversityScore: Double, // 추천 다양성 점수 (0-1)
    val noveltyScore: Double, // 새로운 추천 비율 (0-1)
    val serendipityEvents: Int // 예상 밖의 성공적 추천 수
) {
    /**
     * 전체 개선 점수
     */
    fun getOverallImprovementScore(): Double {
        return (accuracyImprovement * 0.3 +
                responseTimeImprovement * 0.2 +
                userSatisfactionScore * 10 * 0.25 +
                diversityScore * 100 * 0.15 +
                noveltyScore * 100 * 0.1) / 100
    }

    /**
     * 개선 요약
     */
    fun getSummary(): String {
        val overallScore = String.format("%.1f", getOverallImprovementScore() * 100)
        val satisfaction = String.format("%.1f", userSatisfactionScore)
        val diversity = String.format("%.1f", diversityScore * 100)

        return "종합 개선도: ${overallScore}% | 만족도: ${satisfaction}/10 | 다양성: ${diversity}% | 세렌디피티: ${serendipityEvents}회"
    }
}

/**
 * 레시피 탐색 패턴 분석
 */
data class RecipeExplorationPattern(
    val explorationRate: Double, // 새로운 레시피 시도 비율
    val comfortZoneRecipes: List<String>, // 자주 반복하는 레시피들
    val exploredCategories: Set<String>, // 시도해본 카테고리들
    val unexploredCategories: Set<String>, // 아직 시도하지 않은 카테고리들
    val difficultyProgression: List<Int>, // 시간에 따른 난이도 진행
    val seasonalExploration: Map<String, List<String>>, // 계절별 새로운 시도
    val explorationTriggers: Map<String, Int> // 탐색 동기 (재료 활용, 추천, 검색 등)
) {
    /**
     * 탐험가 타입 분류
     */
    fun getExplorerType(): ExplorerType {
        return when {
            explorationRate >= 0.7 -> ExplorerType.ADVENTUROUS
            explorationRate >= 0.4 -> ExplorerType.BALANCED
            explorationRate >= 0.2 -> ExplorerType.CAUTIOUS
            else -> ExplorerType.CONSERVATIVE
        }
    }

    /**
     * 다음 도전 추천
     */
    fun getNextChallengeRecommendation(): String {
        return when (getExplorerType()) {
            ExplorerType.ADVENTUROUS -> {
                if (unexploredCategories.isNotEmpty()) {
                    "${unexploredCategories.first()} 카테고리에 도전해보세요!"
                } else {
                    "더 높은 난이도의 레시피에 도전해보세요!"
                }
            }
            ExplorerType.BALANCED -> {
                "평소 만들지 않던 ${unexploredCategories.firstOrNull() ?: "새로운"} 요리를 시도해보세요."
            }
            ExplorerType.CAUTIOUS -> {
                "익숙한 카테고리 내에서 조금 다른 레시피를 시도해보세요."
            }
            ExplorerType.CONSERVATIVE -> {
                "가장 자신 있는 레시피의 변형 버전부터 시작해보세요."
            }
        }
    }
}

/**
 * 탐험가 타입
 */
enum class ExplorerType(val displayName: String, val description: String, val emoji: String) {
    ADVENTUROUS("모험가", "새로운 것을 적극적으로 시도", "🚀"),
    BALANCED("균형형", "새로운 시도와 익숙한 것의 균형", "⚖️"),
    CAUTIOUS("신중형", "조심스럽게 새로운 것을 시도", "🤔"),
    CONSERVATIVE("보수형", "익숙한 것을 선호", "🏠")
}

/**
 * 사용자 세그먼트 분석
 */
data class UserSegmentAnalysis(
    val segment: UserSegment,
    val characteristics: List<String>,
    val recommendationStrategy: String,
    val engagementTactics: List<String>,
    val retentionRisk: RiskLevel
) {
    /**
     * 세그먼트 요약
     */
    fun getSummary(): String {
        return "${segment.displayName} | ${recommendationStrategy} | 이탈 위험: ${retentionRisk.displayName}"
    }
}

/**
 * 사용자 세그먼트
 */
enum class UserSegment(val displayName: String, val description: String) {
    POWER_USER("파워 유저", "매우 활발하게 앱을 사용하는 사용자"),
    REGULAR_USER("일반 유저", "꾸준히 앱을 사용하는 사용자"),
    CASUAL_USER("가끔 유저", "가끔씩 앱을 사용하는 사용자"),
    NEW_USER("신규 유저", "최근에 가입한 사용자"),
    DORMANT_USER("휴면 유저", "최근에 활동이 없는 사용자"),
    CHEF_ENTHUSIAST("요리 애호가", "요리에 대한 열정이 높은 사용자"),
    CONVENIENCE_SEEKER("편의 추구형", "간편한 요리를 선호하는 사용자")
}

/**
 * 위험 수준
 */
enum class RiskLevel(val displayName: String, val color: String) {
    LOW("낮음", "#4CAF50"),
    MEDIUM("보통", "#FF9800"),
    HIGH("높음", "#F44336"),
    CRITICAL("매우 높음", "#D32F2F")
}