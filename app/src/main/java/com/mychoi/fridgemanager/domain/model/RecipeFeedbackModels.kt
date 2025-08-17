// app/src/main/java/com/mychoi/fridgemanager/domain/model/RecipeFeedbackModels.kt
package com.mychoi.fridgemanager.domain.model

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*

/**
 * 레시피 피드백 및 기록 관련 데이터 모델들
 * 사용자 리뷰, 요리 기록, 평점 등을 위한 클래스들
 */

/**
 * 레시피 피드백
 * 사용자가 실제로 요리를 만든 후 제공하는 피드백
 */
@Serializable
data class RecipeFeedback(
    val rating: Int,                    // 평점 (1-5)
    val review: String? = null,         // 리뷰 내용
    val difficulty: Int? = null,        // 실제 느낀 난이도
    val actualCookingTime: Int? = null, // 실제 소요 시간 (분)
    val suggestions: String? = null,    // 개선 제안
    val wouldCookAgain: Boolean? = null, // 다시 만들 의향
    val photoUrls: List<String> = emptyList(), // 요리 사진들
    val usedIngredients: List<String> = emptyList(), // 실제 사용한 재료
    val skippedSteps: List<Int> = emptyList(), // 건너뛴 조리 단계
    val addedSteps: List<String> = emptyList() // 추가한 조리 과정
) {
    init {
        require(rating in 1..5) { "평점은 1-5 사이여야 합니다" }
        difficulty?.let { require(it in 1..5) { "난이도는 1-5 사이여야 합니다" } }
        actualCookingTime?.let { require(it > 0) { "조리시간은 0보다 커야 합니다" } }
        review?.let { require(it.length <= 500) { "리뷰는 500자를 초과할 수 없습니다" } }
        suggestions?.let { require(it.length <= 300) { "제안사항은 300자를 초과할 수 없습니다" } }
    }

    /**
     * 피드백 요약 텍스트
     */
    fun getSummary(): String {
        val ratingText = "⭐".repeat(rating)
        val difficultyText = difficulty?.let { " | 난이도 $it" } ?: ""
        val timeText = actualCookingTime?.let { " | ${it}분 소요" } ?: ""
        val againText = wouldCookAgain?.let { if (it) " | 재도전 의향 ✅" else " | 재도전 안함 ❌" } ?: ""

        return "$ratingText$difficultyText$timeText$againText"
    }

    /**
     * 피드백이 긍정적인지 판단
     */
    fun isPositive(): Boolean {
        return rating >= 4 && (wouldCookAgain ?: true)
    }

    /**
     * 피드백 완성도 계산
     */
    fun getCompletenessScore(): Double {
        var score = 0.0

        score += 20.0 // 평점 (필수)
        if (review?.isNotBlank() == true) score += 30.0
        if (difficulty != null) score += 15.0
        if (actualCookingTime != null) score += 15.0
        if (wouldCookAgain != null) score += 10.0
        if (photoUrls.isNotEmpty()) score += 10.0

        return score
    }

    /**
     * 레시피 개선에 도움이 되는 피드백인지 확인
     */
    fun isHelpfulForImprovement(): Boolean {
        return (suggestions?.isNotBlank() == true) ||
                (skippedSteps.isNotEmpty()) ||
                (addedSteps.isNotEmpty()) ||
                (difficulty != null && actualCookingTime != null)
    }
}

/**
 * 요리 기록
 * 사용자가 실제로 만든 요리들의 기록
 */
@Serializable
data class CookingRecord(
    val id: String = "",
    val recipeId: String,
    val recipeName: String,
    val cookedAt: Long,                 // 요리한 시간
    val feedback: RecipeFeedback? = null,
    val usedIngredients: List<String> = emptyList(), // 실제 사용한 재료
    val notes: String? = null,          // 개인 메모
    val cookingContext: CookingContext? = null, // 요리 상황
    val totalCookingTime: Int? = null,  // 총 소요 시간 (준비+조리)
    val servingsCooked: Int? = null     // 실제 만든 인분수
) {
    init {
        require(recipeId.isNotBlank()) { "레시피 ID는 공백일 수 없습니다" }
        require(recipeName.isNotBlank()) { "레시피명은 공백일 수 없습니다" }
        notes?.let { require(it.length <= 300) { "메모는 300자를 초과할 수 없습니다" } }
        totalCookingTime?.let { require(it > 0) { "조리시간은 0보다 커야 합니다" } }
        servingsCooked?.let { require(it > 0) { "인분수는 0보다 커야 합니다" } }
    }

    /**
     * 요리한 날짜 표시용 텍스트
     */
    fun getCookedDateDisplay(): String {
        val date = Date(cookedAt)
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return format.format(date)
    }

    /**
     * 요리한 지 며칠 지났는지 계산
     */
    fun getDaysAgo(): Long {
        val now = System.currentTimeMillis()
        return (now - cookedAt) / (24 * 60 * 60 * 1000)
    }

    /**
     * 요리 기록 요약
     */
    fun getSummary(): String {
        val timeAgo = when (val days = getDaysAgo()) {
            0L -> "오늘"
            1L -> "어제"
            in 2..6 -> "${days}일 전"
            else -> getCookedDateDisplay().split(" ")[0] // 날짜만
        }

        val ratingText = feedback?.let { " (⭐${it.rating})" } ?: ""
        val contextText = cookingContext?.let { " | ${it.occasion}" } ?: ""
        return "$recipeName | $timeAgo$ratingText$contextText"
    }

    /**
     * 성공적인 요리였는지 판단
     */
    fun wasSuccessful(): Boolean {
        return feedback?.rating?.let { it >= 3 } ?: true
    }

    /**
     * 요리 시간 효율성 계산 (예상 대비 실제)
     */
    fun getTimeEfficiency(expectedTime: Int): Double? {
        return totalCookingTime?.let { actual ->
            if (expectedTime > 0) {
                expectedTime.toDouble() / actual
            } else null
        }
    }
}

/**
 * 요리 상황 정보
 */
@Serializable
data class CookingContext(
    val occasion: String,               // 요리 상황 ("저녁식사", "손님접대", "간식", "도시락")
    val companionCount: Int = 1,        // 함께 먹는 사람 수
    val timeOfDay: String? = null,      // 시간대 ("아침", "점심", "저녁", "야식")
    val weather: String? = null,        // 날씨 ("맑음", "비", "추움", "더움")
    val mood: String? = null,           // 기분 ("좋음", "스트레스", "피곤", "특별한날")
    val availableTime: Int? = null      // 사용 가능한 시간 (분)
) {
    /**
     * 상황 요약
     */
    fun getSummary(): String {
        val parts = mutableListOf<String>()
        parts.add(occasion)
        if (companionCount > 1) parts.add("${companionCount}명")
        timeOfDay?.let { parts.add(it) }
        mood?.let { parts.add(it) }

        return parts.joinToString(" | ")
    }
}

/**
 * 레시피 리뷰 집계
 * 특정 레시피에 대한 모든 리뷰의 집계 정보
 */
data class RecipeReviewSummary(
    val recipeId: String,
    val totalReviews: Int,
    val averageRating: Double,
    val ratingDistribution: Map<Int, Int>, // 평점별 개수
    val averageCookingTime: Double?,
    val averageDifficulty: Double?,
    val wouldCookAgainRate: Double, // 재도전 의향률
    val topPositiveKeywords: List<String>, // 긍정적 키워드
    val topNegativeKeywords: List<String>, // 부정적 키워드
    val commonSuggestions: List<String>    // 공통 개선 제안
) {
    /**
     * 리뷰 등급 계산
     */
    fun getReviewGrade(): String {
        return when {
            averageRating >= 4.5 -> "A+"
            averageRating >= 4.0 -> "A"
            averageRating >= 3.5 -> "B+"
            averageRating >= 3.0 -> "B"
            averageRating >= 2.5 -> "C"
            else -> "D"
        }
    }

    /**
     * 리뷰 요약
     */
    fun getSummary(): String {
        val grade = getReviewGrade()
        val ratingText = String.format("%.1f", averageRating)
        val againRate = String.format("%.0f", wouldCookAgainRate * 100)

        return "평점: $ratingText/5.0 ($grade 등급) | ${totalReviews}개 리뷰 | 재도전률: ${againRate}%"
    }

    /**
     * 인기도 판단
     */
    fun isPopular(): Boolean {
        return totalReviews >= 10 && averageRating >= 4.0 && wouldCookAgainRate >= 0.8
    }
}

/**
 * 사용자 요리 통계
 * 개별 사용자의 요리 활동 통계
 */
data class UserCookingStats(
    val userId: String,
    val totalCookingCount: Int,
    val successfulCookingCount: Int,
    val averageRating: Double,
    val favoriteCategories: List<Pair<String, Int>>, // 카테고리와 횟수
    val mostCookedRecipes: List<Pair<String, Int>>,  // 레시피명과 횟수
    val averageCookingTimeEfficiency: Double, // 시간 효율성 평균
    val cookingStreak: Int,                   // 연속 요리 일수
    val lastCookedAt: Long,
    val improvementTrend: CookingTrend        // 실력 향상 추세
) {
    /**
     * 요리 성공률 계산
     */
    fun getSuccessRate(): Double {
        return if (totalCookingCount > 0) {
            (successfulCookingCount.toDouble() / totalCookingCount) * 100
        } else 0.0
    }

    /**
     * 요리 레벨 계산
     */
    fun getCookingLevel(): CookingLevel {
        return when {
            totalCookingCount >= 100 && getSuccessRate() >= 90 -> CookingLevel.EXPERT
            totalCookingCount >= 50 && getSuccessRate() >= 80 -> CookingLevel.ADVANCED
            totalCookingCount >= 20 && getSuccessRate() >= 70 -> CookingLevel.INTERMEDIATE
            totalCookingCount >= 5 -> CookingLevel.BEGINNER
            else -> CookingLevel.NOVICE
        }
    }

    /**
     * 통계 요약
     */
    fun getSummary(): String {
        val level = getCookingLevel().displayName
        val successRate = String.format("%.1f", getSuccessRate())
        val efficiency = String.format("%.1f", averageCookingTimeEfficiency * 100)

        return "레벨: $level | 총 ${totalCookingCount}회 요리 | 성공률: ${successRate}% | 시간효율: ${efficiency}%"
    }

    /**
     * 활발한 요리 활동인지 확인
     */
    fun isActiveChef(): Boolean {
        val daysSinceLastCooking = (System.currentTimeMillis() - lastCookedAt) / (24 * 60 * 60 * 1000)
        return daysSinceLastCooking <= 7 && cookingStreak >= 3
    }
}

/**
 * 요리 레벨
 */
enum class CookingLevel(val displayName: String, val description: String) {
    NOVICE("초보", "요리를 시작한 단계"),
    BEGINNER("입문", "기본적인 요리를 할 수 있는 단계"),
    INTERMEDIATE("중급", "다양한 요리에 도전하는 단계"),
    ADVANCED("고급", "복잡한 요리도 능숙하게 하는 단계"),
    EXPERT("전문가", "요리의 달인 단계")
}

/**
 * 요리 실력 향상 추세
 */
enum class CookingTrend(val displayName: String, val emoji: String) {
    IMPROVING("향상 중", "📈"),
    STABLE("안정", "➡️"),
    DECLINING("하락", "📉"),
    INSUFFICIENT_DATA("데이터 부족", "❓")
}

/**
 * 요리 도전 과제
 * 사용자에게 제시할 요리 관련 도전 과제
 */
data class CookingChallenge(
    val id: String,
    val title: String,
    val description: String,
    val type: ChallengeType,
    val difficulty: Int,                    // 1-5
    val requirements: ChallengeRequirements,
    val reward: String,                     // 보상 설명
    val deadline: Long? = null,             // 마감일 (선택사항)
    val isCompleted: Boolean = false,
    val progress: Double = 0.0              // 진행률 0.0-1.0
) {
    /**
     * 도전 과제 진행률 텍스트
     */
    fun getProgressText(): String {
        val percentage = (progress * 100).toInt()
        return "$percentage%"
    }

    /**
     * 마감일까지 남은 일수
     */
    fun getDaysUntilDeadline(): Long? {
        return deadline?.let {
            val now = System.currentTimeMillis()
            (it - now) / (24 * 60 * 60 * 1000)
        }
    }

    /**
     * 도전 과제 요약
     */
    fun getSummary(): String {
        val progressText = getProgressText()
        val difficultyText = "★".repeat(difficulty)
        val deadlineText = getDaysUntilDeadline()?.let { " | ${it}일 남음" } ?: ""

        return "$title | $difficultyText | $progressText$deadlineText"
    }
}

/**
 * 도전 과제 타입
 */
enum class ChallengeType(val displayName: String) {
    RECIPE_COUNT("레시피 개수"),
    CATEGORY_EXPLORATION("카테고리 탐험"),
    DIFFICULTY_PROGRESS("난이도 도전"),
    TIME_EFFICIENCY("시간 효율성"),
    STREAK("연속 도전"),
    RATING_TARGET("평점 목표")
}

/**
 * 도전 과제 요구사항
 */
@Serializable
data class ChallengeRequirements(
    val targetCount: Int? = null,           // 목표 횟수
    val targetCategories: List<String> = emptyList(), // 목표 카테고리
    val targetDifficulty: Int? = null,      // 목표 난이도
    val targetRating: Double? = null,       // 목표 평점
    val timeLimit: Int? = null              // 시간 제한 (분)
) {
    /**
     * 요구사항 요약
     */
    fun getSummary(): String {
        val parts = mutableListOf<String>()

        targetCount?.let { parts.add("${it}회") }
        if (targetCategories.isNotEmpty()) parts.add(targetCategories.joinToString(", "))
        targetDifficulty?.let { parts.add("난이도 ${it}+") }
        targetRating?.let { parts.add("평점 ${it}+") }
        timeLimit?.let { parts.add("${it}분 이내") }

        return parts.joinToString(" | ")
    }
}