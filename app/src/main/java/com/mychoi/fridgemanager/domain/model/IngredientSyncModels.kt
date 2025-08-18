package com.mychoi.fridgemanager.domain.model

import kotlinx.serialization.Serializable

/**
 * 재료 동기화 관련 데이터 모델들
 * IngredientRepository의 동기화, 사용자 제안, 통계 기능에서 사용되는 모델 정의
 */

/**
 * 재료 동기화 결과
 * 로컬 재료 마스터 데이터와 서버 간 동기화 결과 정보
 */
@Serializable
data class IngredientSyncResult(
    val isSuccess: Boolean,                                    // 동기화 성공 여부
    val syncedCount: Int = 0,                                 // 성공적으로 동기화된 항목 수
    val newIngredientsCount: Int = 0,                         // 새로 추가된 재료 수
    val updatedIngredientsCount: Int = 0,                     // 업데이트된 재료 수
    val failedCount: Int = 0,                                 // 실패한 항목 수
    val errorMessages: List<String> = emptyList(),            // 에러 메시지들 (직렬화를 위해 String 사용)
    val lastSyncTime: Long = System.currentTimeMillis(),     // 마지막 동기화 시간
    val syncDurationMs: Long = 0,                             // 동기화 소요 시간 (밀리초)
    val dataVersion: String = "1.0",                          // 동기화된 데이터 버전
    val serverVersion: String? = null,                        // 서버 데이터 버전
    val conflictCount: Int = 0,                               // 충돌 발생 항목 수
    val resolvedConflictCount: Int = 0,                       // 해결된 충돌 수
    val categoryUpdates: Map<String, Int> = emptyMap(),      // 카테고리별 업데이트 수
    val syncMode: SyncMode = SyncMode.AUTOMATIC               // 동기화 모드
) {
    /**
     * 동기화 요약 메시지
     */
    fun getSummaryMessage(): String {
        return if (isSuccess) {
            when {
                newIngredientsCount > 0 && updatedIngredientsCount > 0 ->
                    "동기화 완료: 신규 ${newIngredientsCount}개, 업데이트 ${updatedIngredientsCount}개"
                newIngredientsCount > 0 ->
                    "동기화 완료: 신규 재료 ${newIngredientsCount}개 추가"
                updatedIngredientsCount > 0 ->
                    "동기화 완료: ${updatedIngredientsCount}개 재료 정보 업데이트"
                else ->
                    "동기화 완료: 모든 데이터가 최신 상태입니다"
            }
        } else {
            "동기화 실패: ${failedCount}개 항목에서 오류 발생"
        }
    }

    /**
     * 성공률 계산 (0.0 ~ 100.0)
     */
    fun getSuccessRate(): Double {
        val total = syncedCount + failedCount
        return if (total > 0) {
            (syncedCount.toDouble() / total) * 100
        } else {
            100.0
        }
    }

    /**
     * 동기화 속도 계산 (항목/초)
     */
    fun getSyncSpeed(): Double {
        return if (syncDurationMs > 0) {
            (syncedCount.toDouble() / syncDurationMs) * 1000
        } else {
            0.0
        }
    }

    /**
     * 충돌 해결률 계산 (0.0 ~ 100.0)
     */
    fun getConflictResolutionRate(): Double {
        return if (conflictCount > 0) {
            (resolvedConflictCount.toDouble() / conflictCount) * 100
        } else {
            100.0 // 충돌이 없으면 100%
        }
    }

    /**
     * 동기화 상태 등급 (A~F)
     */
    fun getSyncGrade(): String {
        val successRate = getSuccessRate()
        val conflictResolutionRate = getConflictResolutionRate()
        val averageScore = (successRate + conflictResolutionRate) / 2

        return when {
            averageScore >= 95 -> "A"
            averageScore >= 85 -> "B"
            averageScore >= 75 -> "C"
            averageScore >= 65 -> "D"
            else -> "F"
        }
    }

    /**
     * 상세 동기화 리포트 생성
     */
    fun getDetailedReport(): String {
        val duration = if (syncDurationMs > 0) "${syncDurationMs}ms" else "알 수 없음"
        val speed = "%.1f".format(getSyncSpeed())

        return """
            === 재료 동기화 리포트 ===
            상태: ${if (isSuccess) "성공" else "실패"}
            등급: ${getSyncGrade()}
            성공률: ${"%.1f".format(getSuccessRate())}%
            
            === 동기화 결과 ===
            • 총 처리: ${syncedCount + failedCount}개
            • 성공: ${syncedCount}개
            • 실패: ${failedCount}개
            • 신규 추가: ${newIngredientsCount}개
            • 업데이트: ${updatedIngredientsCount}개
            
            === 성능 정보 ===
            • 소요 시간: $duration
            • 처리 속도: ${speed}개/초
            • 충돌 발생: ${conflictCount}개
            • 충돌 해결: ${resolvedConflictCount}개
            
            === 모드 정보 ===
            • 동기화 모드: ${syncMode.displayName}
            • 데이터 버전: $dataVersion
            • 서버 버전: ${serverVersion ?: "알 수 없음"}
        """.trimIndent()
    }

    companion object {
        /**
         * 성공 결과 생성 헬퍼
         */
        fun success(
            syncedCount: Int,
            newCount: Int = 0,
            updatedCount: Int = 0,
            durationMs: Long = 0
        ): IngredientSyncResult {
            return IngredientSyncResult(
                isSuccess = true,
                syncedCount = syncedCount,
                newIngredientsCount = newCount,
                updatedIngredientsCount = updatedCount,
                syncDurationMs = durationMs
            )
        }

        /**
         * 실패 결과 생성 헬퍼
         */
        fun failure(
            failedCount: Int,
            errorMessages: List<String>,
            durationMs: Long = 0
        ): IngredientSyncResult {
            return IngredientSyncResult(
                isSuccess = false,
                failedCount = failedCount,
                errorMessages = errorMessages,
                syncDurationMs = durationMs
            )
        }
    }
}

/**
 * 동기화 모드
 */
enum class SyncMode(val displayName: String, val description: String) {
    AUTOMATIC("자동 동기화", "주기적으로 자동 동기화"),
    MANUAL("수동 동기화", "사용자가 직접 요청한 동기화"),
    FORCED("강제 동기화", "모든 데이터를 강제로 다시 동기화"),
    CONFLICT_RESOLUTION("충돌 해결", "데이터 충돌 해결을 위한 동기화"),
    INITIAL("초기 동기화", "앱 설치 후 첫 동기화")
}

/**
 * 새 재료 제안 요청
 * 사용자가 마스터 데이터에 없는 재료를 제안할 때 사용
 */
@Serializable
data class NewIngredientRequest(
    val name: String,                                         // 재료명
    val category: String,                                     // 카테고리
    val subcategory: String? = null,                         // 서브카테고리
    val description: String? = null,                         // 재료 설명
    val alternatives: List<String> = emptyList(),            // 대체 가능한 재료들
    val commonUnit: String = "개",                           // 기본 단위
    val storageDays: Int = 7,                               // 보관 가능 기간 (일)
    val storageMethod: String = "냉장",                      // 보관 방법
    val submittedBy: String,                                // 제안자 사용자 ID
    val submissionReason: String? = null,                   // 제안 이유
    val referenceRecipeId: String? = null,                  // 참조 레시피 ID (해당 재료가 필요한 레시피)
    val estimatedPrice: Int? = null,                        // 예상 가격 (원)
    val nutritionInfo: String? = null,                      // 영양 정보 (간단한 텍스트)
    val seasonality: List<Season> = emptyList(),            // 제철 계절
    val tags: List<String> = emptyList(),                   // 태그 (유기농, 수입산 등)
    val imageUrl: String? = null,                           // 재료 이미지 URL
    val submissionDate: Long = System.currentTimeMillis(), // 제안 일시
    val priority: IngredientRequestPriority = IngredientRequestPriority.NORMAL, // 제안 우선순위
    val status: IngredientRequestStatus = IngredientRequestStatus.PENDING // 제안 상태
) {

    init {
        // 필수 필드 검증
        require(name.isNotBlank()) { "재료명을 입력해주세요" }
        require(category.isNotBlank()) { "카테고리를 선택해주세요" }
        require(submittedBy.isNotBlank()) { "사용자 정보가 필요합니다" }
        require(storageDays > 0) { "보관 기간은 1일 이상이어야 합니다" }
        require(name.length <= 50) { "재료명은 50자 이내로 입력해주세요" }
        require(description?.length ?: 0 <= 200) { "설명은 200자 이내로 입력해주세요" }
        require(alternatives.size <= 10) { "대체 재료는 최대 10개까지 입력 가능합니다" }
    }

    /**
     * Ingredient 모델로 변환 (승인 후 사용)
     */
    fun toIngredient(id: String): Ingredient {
        return Ingredient(
            id = id,
            name = name,
            category = category,
            subcategory = subcategory,
            storageDays = storageDays,
            storageMethod = storageMethod,
            alternatives = alternatives,
            commonUnit = commonUnit,
            createdAt = UserIngredient.getCurrentISODateTime(),
            updatedAt = UserIngredient.getCurrentISODateTime(),
            isCommon = false, // 신규 재료는 기본적으로 일반 재료
            keywords = generateKeywords(),
            averagePrice = estimatedPrice
        )
    }

    /**
     * 제안 점수 계산 (승인 우선순위 결정용)
     */
    fun calculateSuggestionScore(): Int {
        var score = 0

        // 기본 점수
        score += 10

        // 우선순위 보너스
        score += when (priority) {
            IngredientRequestPriority.URGENT -> 50
            IngredientRequestPriority.HIGH -> 30
            IngredientRequestPriority.NORMAL -> 10
            IngredientRequestPriority.LOW -> 5
        }

        // 상세 정보 보너스
        if (!description.isNullOrBlank()) score += 5
        if (alternatives.isNotEmpty()) score += 3
        if (!referenceRecipeId.isNullOrBlank()) score += 10
        if (estimatedPrice != null) score += 3
        if (!nutritionInfo.isNullOrBlank()) score += 3
        if (seasonality.isNotEmpty()) score += 2
        if (tags.isNotEmpty()) score += 2
        if (!imageUrl.isNullOrBlank()) score += 5

        return score
    }

    /**
     * 검색용 키워드 자동 생성
     */
    private fun generateKeywords(): List<String> {
        val keywords = mutableListOf<String>()

        // 재료명 분해
        keywords.add(name)
        if (name.length > 2) {
            // 2글자 이상인 경우 부분 키워드 생성
            for (i in 0..name.length - 2) {
                keywords.add(name.substring(i, minOf(i + 2, name.length)))
            }
        }

        // 카테고리, 서브카테고리
        keywords.add(category)
        subcategory?.let { keywords.add(it) }

        // 대체 재료들
        keywords.addAll(alternatives)

        // 태그들
        keywords.addAll(tags)

        return keywords.distinct()
    }

    /**
     * 제안 요약 정보
     */
    fun getSummary(): String {
        val statusText = status.displayName
        val priorityText = priority.displayName
        val categoryText = if (subcategory != null) "$category > $subcategory" else category

        return "$name ($categoryText) - $statusText [$priorityText]"
    }

    /**
     * 제안 상세 정보
     */
    fun getDetailedInfo(): String {
        val parts = mutableListOf<String>()

        parts.add("재료명: $name")
        parts.add("카테고리: $category")
        subcategory?.let { parts.add("서브카테고리: $it") }
        parts.add("보관방법: $storageMethod (${storageDays}일)")
        parts.add("기본단위: $commonUnit")

        if (alternatives.isNotEmpty()) {
            parts.add("대체재료: ${alternatives.joinToString(", ")}")
        }

        description?.let { parts.add("설명: $it") }
        estimatedPrice?.let { parts.add("예상가격: ${it}원") }

        if (seasonality.isNotEmpty()) {
            parts.add("제철: ${seasonality.joinToString(", ") { it.displayName }}")
        }

        if (tags.isNotEmpty()) {
            parts.add("태그: ${tags.joinToString(", ")}")
        }

        return parts.joinToString("\n")
    }

    companion object {
        /**
         * 빠른 제안 생성 (최소 정보만으로)
         */
        fun createQuick(
            name: String,
            category: String,
            submittedBy: String,
            reason: String? = null
        ): NewIngredientRequest {
            return NewIngredientRequest(
                name = name,
                category = category,
                submittedBy = submittedBy,
                submissionReason = reason
            )
        }

        /**
         * 레시피 기반 제안 생성
         */
        fun createFromRecipe(
            name: String,
            category: String,
            submittedBy: String,
            recipeId: String,
            reason: String = "레시피에 필요한 재료"
        ): NewIngredientRequest {
            return NewIngredientRequest(
                name = name,
                category = category,
                submittedBy = submittedBy,
                referenceRecipeId = recipeId,
                submissionReason = reason,
                priority = IngredientRequestPriority.HIGH
            )
        }
    }
}

/**
 * 재료 제안 우선순위
 */
enum class IngredientRequestPriority(val displayName: String, val score: Int) {
    URGENT("긴급", 4),    // 많은 사용자가 요청하거나 중요한 재료
    HIGH("높음", 3),      // 레시피에서 필요하거나 자주 검색되는 재료
    NORMAL("보통", 2),    // 일반적인 제안
    LOW("낮음", 1)        // 참고용 제안
}

/**
 * 재료 제안 상태
 */
enum class IngredientRequestStatus(val displayName: String, val description: String) {
    PENDING("검토 중", "관리자 검토 대기 중"),
    APPROVED("승인됨", "승인되어 재료 DB에 추가됨"),
    REJECTED("거절됨", "부적절하거나 중복으로 인해 거절됨"),
    IN_REVIEW("심사 중", "상세 검토 진행 중"),
    NEEDS_INFO("정보 부족", "추가 정보가 필요함"),
    DUPLICATE("중복", "이미 존재하는 재료"),
    INVALID("유효하지 않음", "유효하지 않은 제안")
}

/**
 * 재료 사용 유형
 * 사용자의 재료 사용 패턴 분석 및 통계를 위한 분류
 */
enum class IngredientUsageType(val displayName: String, val weight: Int, val description: String) {
    SEARCH("검색", 1, "재료를 검색함"),
    ADD_TO_FRIDGE("냉장고 추가", 3, "냉장고에 재료를 추가함"),
    USE_IN_RECIPE("레시피 사용", 5, "레시피에서 재료를 사용함"),
    ALTERNATIVE_USED("대체재료 사용", 2, "대체재료로 사용함"),
    SUGGESTION_ACCEPTED("제안 수락", 4, "자동완성 제안을 수락함"),
    SHOPPING_LIST("장보기 목록", 2, "장보기 목록에 추가함"),
    FAVORITE("즐겨찾기", 3, "자주 사용하는 재료로 등록함"),
    PURCHASE("구매", 4, "실제로 구매함"),
    CONSUME("소모", 5, "재료를 사용하여 소모함"),
    WASTE("폐기", -2, "유통기한 지나 폐기함"),
    SHARE("공유", 1, "다른 사용자와 재료 정보 공유");

    /**
     * 사용 유형이 긍정적인지 확인
     */
    fun isPositive(): Boolean {
        return weight > 0
    }

    /**
     * 사용 유형의 중요도 레벨
     */
    fun getImportanceLevel(): UsageImportanceLevel {
        return when {
            weight >= 5 -> UsageImportanceLevel.CRITICAL
            weight >= 3 -> UsageImportanceLevel.HIGH
            weight >= 1 -> UsageImportanceLevel.MEDIUM
            else -> UsageImportanceLevel.LOW
        }
    }

    /**
     * 통계 그룹 분류
     */
    fun getStatGroup(): UsageStatGroup {
        return when (this) {
            SEARCH, SUGGESTION_ACCEPTED -> UsageStatGroup.DISCOVERY
            ADD_TO_FRIDGE, SHOPPING_LIST, PURCHASE -> UsageStatGroup.ACQUISITION
            USE_IN_RECIPE, CONSUME, ALTERNATIVE_USED -> UsageStatGroup.CONSUMPTION
            FAVORITE, SHARE -> UsageStatGroup.ENGAGEMENT
            WASTE -> UsageStatGroup.WASTE
        }
    }

    companion object {
        /**
         * 긍정적인 사용 유형들만 반환
         */
        fun getPositiveTypes(): List<IngredientUsageType> {
            return values().filter { it.isPositive() }
        }

        /**
         * 높은 가중치 유형들만 반환
         */
        fun getHighWeightTypes(): List<IngredientUsageType> {
            return values().filter { it.weight >= 3 }
        }

        /**
         * 통계 그룹별 유형들 반환
         */
        fun getTypesByGroup(group: UsageStatGroup): List<IngredientUsageType> {
            return values().filter { it.getStatGroup() == group }
        }
    }
}

/**
 * 사용 중요도 레벨
 */
enum class UsageImportanceLevel(val displayName: String) {
    CRITICAL("매우 중요"),
    HIGH("중요"),
    MEDIUM("보통"),
    LOW("낮음")
}

/**
 * 사용 통계 그룹
 */
enum class UsageStatGroup(val displayName: String, val description: String) {
    DISCOVERY("발견", "재료를 찾고 탐색하는 활동"),
    ACQUISITION("획득", "재료를 얻는 활동"),
    CONSUMPTION("소비", "재료를 실제로 사용하는 활동"),
    ENGAGEMENT("참여", "재료와 관련된 상호작용"),
    WASTE("낭비", "재료를 버리는 활동")
}

/**
 * 재료 사용 통계 집계 결과
 */
@Serializable
data class IngredientUsageStats(
    val ingredientName: String,
    val totalUsageCount: Int,
    val usageByType: Map<String, Int>,           // IngredientUsageType.name -> count
    val averageUsagePerWeek: Double,
    val lastUsedDate: Long,
    val firstUsedDate: Long,
    val totalWeight: Int,                        // 가중치 합계
    val popularityRank: Int = 0,                 // 인기도 순위
    val trendDirection: TrendDirection = TrendDirection.STABLE, // 사용 트렌드
    val seasonalPattern: Map<String, Int> = emptyMap() // 계절별 사용 패턴
) {
    /**
     * 사용 기간 (일)
     */
    fun getUsagePeriodDays(): Long {
        return if (firstUsedDate > 0 && lastUsedDate > 0) {
            (lastUsedDate - firstUsedDate) / (24 * 60 * 60 * 1000)
        } else {
            0L
        }
    }

    /**
     * 사용 빈도 레벨
     */
    fun getUsageFrequencyLevel(): UsageFrequencyLevel {
        return when {
            averageUsagePerWeek >= 3.0 -> UsageFrequencyLevel.VERY_HIGH
            averageUsagePerWeek >= 2.0 -> UsageFrequencyLevel.HIGH
            averageUsagePerWeek >= 1.0 -> UsageFrequencyLevel.MEDIUM
            averageUsagePerWeek >= 0.5 -> UsageFrequencyLevel.LOW
            else -> UsageFrequencyLevel.VERY_LOW
        }
    }

    /**
     * 가장 많이 사용된 유형
     */
    fun getMostUsedType(): String? {
        return usageByType.maxByOrNull { it.value }?.key
    }

    /**
     * 사용 점수 (인기도 계산용)
     */
    fun getUsageScore(): Double {
        return (totalWeight.toDouble() / getUsagePeriodDays().coerceAtLeast(1)) * 7 // 주간 점수로 정규화
    }
}

/**
 * 사용 빈도 레벨
 */
enum class UsageFrequencyLevel(val displayName: String) {
    VERY_HIGH("매우 높음"),
    HIGH("높음"),
    MEDIUM("보통"),
    LOW("낮음"),
    VERY_LOW("매우 낮음")
}

/**
 * 사용 트렌드 방향
 */
enum class TrendDirection(val displayName: String, val emoji: String) {
    INCREASING("증가", "📈"),
    STABLE("안정", "➡️"),
    DECREASING("감소", "📉"),
    FLUCTUATING("변동", "🔄")
}