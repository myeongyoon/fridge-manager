package com.mychoi.fridgemanager.domain.repository

import com.mychoi.fridgemanager.domain.model.*
import com.mychoi.fridgemanager.domain.error.AddIngredientError
import com.mychoi.fridgemanager.domain.request.CreateIngredientRequest
import kotlinx.coroutines.flow.Flow

/**
 * 사용자 관련 데이터 처리를 위한 Repository 인터페이스
 * Clean Architecture의 Domain Layer에서 정의하고
 * Data Layer에서 구현하는 패턴을 따름
 *
 * 오프라인 우선 전략을 적용하여 로컬 DB(Room)를 메인으로 하고
 * 백그라운드에서 Supabase와 동기화하는 방식
 */
interface UserRepository {

    // ========================================
    // 재료 관리 (User Ingredients)
    // ========================================

    /**
     * 냉장고에 새로운 재료 추가
     *
     * @param request 재료 추가 요청 정보 (검증 완료된 상태)
     * @return 성공 시 생성된 UserIngredient, 실패 시 AddIngredientError
     */
    suspend fun addIngredient(request: CreateIngredientRequest): Result<UserIngredient, AddIngredientError>

    /**
     * 사용자의 모든 냉장고 재료 조회 (실시간 업데이트)
     * Flow를 사용하여 데이터 변경 시 자동으로 UI 업데이트
     *
     * @return 재료 목록 Flow (에러 발생 시에도 기존 데이터 유지)
     */
    fun getAllIngredients(): Flow<List<UserIngredient>>

    /**
     * 특정 재료 조회 (ID 기반)
     *
     * @param ingredientId 재료 고유 ID
     * @return 재료 정보 또는 null (찾지 못한 경우)
     */
    suspend fun getIngredientById(ingredientId: String): UserIngredient?

    /**
     * 재료명으로 검색 (중복 확인용)
     *
     * @param ingredientName 재료명
     * @param userId 사용자 ID
     * @return 동일한 이름의 재료 목록 (보관 위치별로 다를 수 있음)
     */
    suspend fun getIngredientsByName(ingredientName: String, userId: String): List<UserIngredient>

    /**
     * 재료 정보 업데이트
     *
     * @param ingredientId 수정할 재료 ID
     * @param updates 업데이트할 필드들 (null인 필드는 변경하지 않음)
     * @return 성공 시 업데이트된 재료, 실패 시 에러
     */
    suspend fun updateIngredient(
        ingredientId: String,
        updates: UpdateIngredientRequest
    ): Result<UserIngredient, AddIngredientError>

    /**
     * 재료 삭제
     *
     * @param ingredientId 삭제할 재료 ID
     * @return 성공 여부
     */
    suspend fun deleteIngredient(ingredientId: String): Result<Unit, AddIngredientError>

    /**
     * 재료 사용량 차감 (요리 후 재료 소모 처리)
     *
     * @param ingredientId 재료 ID
     * @param usedAmount 사용한 양 (단위 포함, 예: "100g", "1개")
     * @return 성공 시 업데이트된 재료, 실패 시 에러
     */
    suspend fun consumeIngredient(
        ingredientId: String,
        usedAmount: String
    ): Result<UserIngredient, AddIngredientError>

    // ========================================
    // 재료 필터링 및 검색
    // ========================================

    /**
     * 보관 위치별 재료 조회
     *
     * @param storageLocation 보관 위치 (냉장/냉동/상온)
     * @return 해당 위치의 재료 목록 Flow
     */
    fun getIngredientsByLocation(storageLocation: StorageLocation): Flow<List<UserIngredient>>

    /**
     * 유통기한 임박 재료 조회
     *
     * @param daysThreshold 임박 기준 일수 (기본 3일)
     * @return 유통기한이 임박한 재료 목록 Flow
     */
    fun getExpiringSoonIngredients(daysThreshold: Int = 3): Flow<List<UserIngredient>>

    /**
     * 유통기한 지난 재료 조회
     *
     * @return 유통기한이 지난 재료 목록 Flow
     */
    fun getExpiredIngredients(): Flow<List<UserIngredient>>

    /**
     * 재료명으로 검색 (부분 일치 지원)
     *
     * @param query 검색어
     * @return 검색 결과 재료 목록
     */
    suspend fun searchIngredients(query: String): List<UserIngredient>

    /**
     * 카테고리별 재료 통계 조회
     *
     * @return 카테고리별 재료 개수 맵
     */
    suspend fun getIngredientStats(): Map<String, Int>

    // ========================================
    // 사용자 프로필 관리
    // ========================================

    /**
     * 사용자 선호도 조회
     *
     * @param userId 사용자 ID
     * @return 사용자 선호도 정보 또는 null
     */
    suspend fun getUserPreference(userId: String): UserPreference?

    /**
     * 사용자 선호도 업데이트
     *
     * @param userPreference 업데이트할 선호도 정보
     * @return 성공 시 업데이트된 선호도, 실패 시 에러
     */
    suspend fun updateUserPreference(userPreference: UserPreference): Result<UserPreference, AddIngredientError>

    /**
     * 신규 사용자 기본 선호도 생성
     *
     * @param userId 사용자 ID
     * @return 생성된 기본 선호도
     */
    suspend fun createDefaultPreference(userId: String): Result<UserPreference, AddIngredientError>

    // ========================================
    // 데이터 동기화 관리
    // ========================================

    /**
     * 로컬 데이터를 서버와 동기화
     * 백그라운드에서 실행되며 실패해도 로컬 데이터는 유지
     *
     * @param forceSync 강제 동기화 여부 (기본 false)
     * @return 동기화 결과 정보
     */
    suspend fun syncWithServer(forceSync: Boolean = false): SyncResult

    /**
     * 동기화 상태 조회
     *
     * @return 동기화 상태 정보 Flow
     */
    fun getSyncStatus(): Flow<SyncStatus>

    /**
     * 동기화가 필요한 재료 목록 조회
     *
     * @return 동기화 대기 중인 재료 목록
     */
    suspend fun getPendingSyncIngredients(): List<UserIngredient>

    /**
     * 오프라인 모드 강제 설정
     * 네트워크 문제 시 사용자가 수동으로 오프라인 모드 활성화
     *
     * @param enabled 오프라인 모드 활성화 여부
     */
    suspend fun setOfflineMode(enabled: Boolean)

    // ========================================
    // 데이터 백업 및 복원
    // ========================================

    /**
     * 사용자 데이터 전체 백업
     *
     * @return 백업 성공 여부와 백업 정보
     */
    suspend fun backupUserData(): Result<BackupInfo, AddIngredientError>

    /**
     * 백업 데이터로부터 복원
     *
     * @param backupInfo 복원할 백업 정보
     * @param replaceExisting 기존 데이터 덮어쓰기 여부
     * @return 복원 성공 여부
     */
    suspend fun restoreUserData(
        backupInfo: BackupInfo,
        replaceExisting: Boolean = false
    ): Result<Unit, AddIngredientError>

    // ========================================
    // 통계 및 분석
    // ========================================

    /**
     * 냉장고 사용 통계 조회
     *
     * @param periodDays 분석 기간 (일 단위, 기본 30일)
     * @return 사용 통계 정보
     */
    suspend fun getUsageStatistics(periodDays: Int = 30): UsageStatistics

    /**
     * 재료 소모 패턴 분석
     *
     * @return 자주 사용하는 재료, 남기는 재료 등의 패턴 정보
     */
    suspend fun getConsumptionPattern(): ConsumptionPattern
}

// ========================================
// 지원 데이터 클래스들
// ========================================

/**
 * 재료 업데이트 요청 데이터 클래스
 * null인 필드는 업데이트하지 않음
 */
data class UpdateIngredientRequest(
    val amount: String? = null,
    val unit: String? = null,
    val expiryDate: String? = null,
    val storageLocation: StorageLocation? = null,
    val memo: String? = null
) {
    /**
     * 업데이트할 필드가 있는지 확인
     */
    fun hasUpdates(): Boolean {
        return amount != null || unit != null || expiryDate != null ||
                storageLocation != null || memo != null
    }

    /**
     * 업데이트 필드 목록 반환 (로깅용)
     */
    fun getUpdateFields(): List<String> {
        val fields = mutableListOf<String>()
        if (amount != null) fields.add("amount")
        if (unit != null) fields.add("unit")
        if (expiryDate != null) fields.add("expiryDate")
        if (storageLocation != null) fields.add("storageLocation")
        if (memo != null) fields.add("memo")
        return fields
    }
}

/**
 * 동기화 결과 정보
 */
data class SyncResult(
    val isSuccess: Boolean,
    val syncedCount: Int = 0,
    val failedCount: Int = 0,
    val errors: List<AddIngredientError> = emptyList(),
    val lastSyncTime: Long = System.currentTimeMillis()
) {
    /**
     * 동기화 요약 메시지
     */
    fun getSummaryMessage(): String {
        return if (isSuccess) {
            "동기화 완료: ${syncedCount}개 항목"
        } else {
            "동기화 실패: ${failedCount}개 항목에서 오류 발생"
        }
    }
}

/**
 * 동기화 상태
 */
enum class SyncStatus(val displayName: String) {
    SYNCED("동기화 완료"),
    PENDING("동기화 대기"),
    SYNCING("동기화 중"),
    FAILED("동기화 실패"),
    OFFLINE("오프라인 모드")
}

/**
 * 백업 정보
 */
data class BackupInfo(
    val backupId: String,
    val createdAt: Long,
    val ingredientCount: Int,
    val preferenceIncluded: Boolean,
    val fileSize: Long, // bytes
    val checksum: String? = null
) {
    /**
     * 백업 생성일 표시용 텍스트
     */
    fun getCreatedAtDisplay(): String {
        val date = java.util.Date(createdAt)
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        return format.format(date)
    }

    /**
     * 파일 크기 표시용 텍스트
     */
    fun getFileSizeDisplay(): String {
        return when {
            fileSize < 1024 -> "${fileSize}B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024}KB"
            else -> "${fileSize / (1024 * 1024)}MB"
        }
    }
}

/**
 * 사용 통계 정보
 */
data class UsageStatistics(
    val totalIngredients: Int,
    val addedInPeriod: Int,
    val consumedInPeriod: Int,
    val expiredInPeriod: Int,
    val averageShelfLife: Double, // 평균 보관 기간 (일)
    val mostUsedIngredients: List<String>,
    val leastUsedIngredients: List<String>,
    val wasteRate: Double // 폐기율 (0.0 ~ 1.0)
) {
    /**
     * 폐기율 퍼센트 표시
     */
    fun getWasteRatePercent(): String {
        return "${(wasteRate * 100).toInt()}%"
    }

    /**
     * 효율성 등급 계산 (A~F)
     */
    fun getEfficiencyGrade(): String {
        return when {
            wasteRate <= 0.1 -> "A" // 10% 이하
            wasteRate <= 0.2 -> "B" // 20% 이하
            wasteRate <= 0.3 -> "C" // 30% 이하
            wasteRate <= 0.4 -> "D" // 40% 이하
            wasteRate <= 0.5 -> "E" // 50% 이하
            else -> "F" // 50% 초과
        }
    }
}

/**
 * 소비 패턴 분석 정보
 */
data class ConsumptionPattern(
    val frequentIngredients: List<Pair<String, Int>>, // 재료명과 사용 횟수
    val wastedIngredients: List<Pair<String, Int>>,   // 재료명과 폐기 횟수
    val seasonalTrends: Map<String, List<String>>,    // 계절별 선호 재료
    val recommendedBuyingCycle: Map<String, Int>,     // 재료별 권장 구매 주기 (일)
    val storageOptimization: List<StorageRecommendation> // 보관 최적화 제안
) {
    /**
     * 자주 사용하는 재료 Top N
     */
    fun getTopUsedIngredients(limit: Int = 5): List<String> {
        return frequentIngredients.take(limit).map { it.first }
    }

    /**
     * 자주 버리는 재료 Top N
     */
    fun getTopWastedIngredients(limit: Int = 3): List<String> {
        return wastedIngredients.take(limit).map { it.first }
    }
}

/**
 * 보관 최적화 제안
 */
data class StorageRecommendation(
    val ingredientName: String,
    val currentLocation: StorageLocation,
    val recommendedLocation: StorageLocation,
    val reason: String,
    val expectedShelfLifeIncrease: Int // 일 단위
) {
    /**
     * 제안 메시지 생성
     */
    fun getRecommendationMessage(): String {
        return "${ingredientName}을(를) ${currentLocation.displayName}에서 " +
                "${recommendedLocation.displayName}으로 옮기면 " +
                "${expectedShelfLifeIncrease}일 더 오래 보관할 수 있습니다. ($reason)"
    }
}