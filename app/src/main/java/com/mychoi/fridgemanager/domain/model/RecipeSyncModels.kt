// app/src/main/java/com/mychoi/fridgemanager/domain/model/RecipeSyncModels.kt
package com.mychoi.fridgemanager.domain.model

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*

/**
 * 레시피 동기화 및 캐시 관련 데이터 모델들
 * 서버 동기화, 로컬 캐시 관리 등을 위한 클래스들
 */

/**
 * 레시피 동기화 결과
 */
data class RecipeSyncResult(
    val isSuccess: Boolean,
    val syncedRecipeCount: Int = 0,
    val newRecipeCount: Int = 0,
    val updatedRecipeCount: Int = 0,
    val deletedRecipeCount: Int = 0,
    val failedRecipeCount: Int = 0,
    val errors: List<String> = emptyList(),
    val syncStartTime: Long,
    val syncEndTime: Long = System.currentTimeMillis(),
    val syncType: SyncType = SyncType.INCREMENTAL
) {
    /**
     * 동기화 소요 시간 (밀리초)
     */
    val syncDuration: Long
        get() = syncEndTime - syncStartTime

    /**
     * 동기화 요약 메시지
     */
    fun getSummaryMessage(): String {
        return if (isSuccess) {
            when {
                newRecipeCount > 0 && updatedRecipeCount > 0 ->
                    "동기화 완료: 새 레시피 ${newRecipeCount}개, 업데이트 ${updatedRecipeCount}개"
                newRecipeCount > 0 ->
                    "동기화 완료: 새 레시피 ${newRecipeCount}개 추가"
                updatedRecipeCount > 0 ->
                    "동기화 완료: ${updatedRecipeCount}개 레시피 업데이트"
                deletedRecipeCount > 0 ->
                    "동기화 완료: ${deletedRecipeCount}개 레시피 제거"
                else ->
                    "동기화 완료: 변경사항 없음"
            }
        } else {
            "동기화 실패: ${errors.size}개 오류 발생"
        }
    }

    /**
     * 동기화 시간 표시
     */
    fun getSyncTimeDisplay(): String {
        val date = Date(syncEndTime)
        val format = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        return format.format(date)
    }

    /**
     * 동기화 성능 점수 계산
     */
    fun getPerformanceScore(): SyncPerformance {
        val successRate = if (syncedRecipeCount > 0) {
            (syncedRecipeCount - failedRecipeCount).toDouble() / syncedRecipeCount
        } else 1.0

        val timeScore = when {
            syncDuration < 5000 -> 1.0  // 5초 미만
            syncDuration < 15000 -> 0.8 // 15초 미만
            syncDuration < 30000 -> 0.6 // 30초 미만
            else -> 0.4
        }

        val overallScore = (successRate * 0.7) + (timeScore * 0.3)

        return when {
            overallScore >= 0.9 -> SyncPerformance.EXCELLENT
            overallScore >= 0.7 -> SyncPerformance.GOOD
            overallScore >= 0.5 -> SyncPerformance.FAIR
            else -> SyncPerformance.POOR
        }
    }
}

/**
 * 동기화 타입
 */
enum class SyncType(val displayName: String, val description: String) {
    FULL("전체 동기화", "모든 데이터를 다시 동기화"),
    INCREMENTAL("증분 동기화", "변경된 데이터만 동기화"),
    FORCE("강제 동기화", "충돌 무시하고 강제 동기화"),
    BACKGROUND("백그라운드", "백그라운드에서 자동 동기화")
}

/**
 * 동기화 성능 등급
 */
enum class SyncPerformance(val displayName: String, val emoji: String) {
    EXCELLENT("우수", "🟢"),
    GOOD("양호", "🔵"),
    FAIR("보통", "🟡"),
    POOR("불량", "🔴")
}

/**
 * 레시피 캐시 상태
 */
data class RecipeCacheStatus(
    val totalRecipeCount: Int,
    val cachedRecipeCount: Int,
    val lastUpdateTime: Long,
    val cacheSize: Long, // bytes
    val isUpdateAvailable: Boolean,
    val cacheVersion: String = "1.0",
    val corruptedFiles: Int = 0,
    val pendingDownloads: Int = 0
) {
    /**
     * 캐시 적중률 계산
     */
    fun getCacheHitRate(): Double {
        return if (totalRecipeCount > 0) {
            (cachedRecipeCount.toDouble() / totalRecipeCount) * 100
        } else 0.0
    }

    /**
     * 캐시 크기 표시용 텍스트
     */
    fun getCacheSizeDisplay(): String {
        return when {
            cacheSize < 1024 -> "${cacheSize}B"
            cacheSize < 1024 * 1024 -> "${cacheSize / 1024}KB"
            else -> "${cacheSize / (1024 * 1024)}MB"
        }
    }

    /**
     * 마지막 업데이트 시간 표시
     */
    fun getLastUpdateDisplay(): String {
        val date = Date(lastUpdateTime)
        val format = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        return format.format(date)
    }

    /**
     * 캐시 상태 요약
     */
    fun getSummary(): String {
        val hitRate = String.format("%.1f", getCacheHitRate())
        val sizeText = getCacheSizeDisplay()
        val updateText = if (isUpdateAvailable) " (업데이트 가능)" else ""

        return "캐시: ${cachedRecipeCount}/${totalRecipeCount}개 (${hitRate}%) | ${sizeText}${updateText}"
    }

    /**
     * 캐시 상태 건강도 확인
     */
    fun getHealthStatus(): CacheHealth {
        return when {
            corruptedFiles > 0 -> CacheHealth.CORRUPTED
            getCacheHitRate() < 50 -> CacheHealth.INCOMPLETE
            isUpdateAvailable -> CacheHealth.OUTDATED
            pendingDownloads > 10 -> CacheHealth.SYNCING
            else -> CacheHealth.HEALTHY
        }
    }
}

/**
 * 캐시 건강 상태
 */
enum class CacheHealth(val displayName: String, val emoji: String) {
    HEALTHY("정상", "✅"),
    OUTDATED("구버전", "🔄"),
    INCOMPLETE("불완전", "⚠️"),
    CORRUPTED("손상됨", "❌"),
    SYNCING("동기화 중", "🔄")
}

/**
 * 동기화 설정
 */
@Serializable
data class SyncSettings(
    val autoSync: Boolean = true,           // 자동 동기화 여부
    val syncInterval: Long = 24 * 60 * 60 * 1000, // 동기화 간격 (ms)
    val wifiOnly: Boolean = true,           // WiFi에서만 동기화
    val backgroundSync: Boolean = true,     // 백그라운드 동기화
    val syncOnLaunch: Boolean = false,      // 앱 시작 시 동기화
    val maxRetryAttempts: Int = 3,          // 최대 재시도 횟수
    val syncTimeout: Long = 30000,          // 동기화 타임아웃 (ms)
    val compressionEnabled: Boolean = true,  // 압축 전송 여부
    val priorityRecipes: List<String> = emptyList() // 우선 동기화 레시피 ID
) {
    /**
     * 설정 요약
     */
    fun getSummary(): String {
        val autoText = if (autoSync) "자동" else "수동"
        val intervalText = "${syncInterval / (60 * 60 * 1000)}시간마다"
        val networkText = if (wifiOnly) "WiFi만" else "모든 네트워크"

        return "$autoText | $intervalText | $networkText"
    }

    /**
     * 배터리 친화적 설정인지 확인
     */
    fun isBatteryFriendly(): Boolean {
        return wifiOnly && !syncOnLaunch && syncInterval >= 12 * 60 * 60 * 1000
    }
}

/**
 * 동기화 충돌 정보
 */
data class SyncConflict(
    val recipeId: String,
    val recipeName: String,
    val conflictType: ConflictType,
    val localVersion: RecipeVersion?,
    val remoteVersion: RecipeVersion?,
    val conflictFields: List<String> = emptyList(),
    val detectedAt: Long = System.currentTimeMillis()
) {
    /**
     * 충돌 요약
     */
    fun getSummary(): String {
        val typeText = conflictType.displayName
        val fieldsText = if (conflictFields.isNotEmpty()) {
            " (${conflictFields.joinToString(", ")})"
        } else ""

        return "$recipeName: $typeText$fieldsText"
    }

    /**
     * 해결 옵션 제공
     */
    fun getResolutionOptions(): List<ConflictResolution> {
        return when (conflictType) {
            ConflictType.CONTENT_CONFLICT -> listOf(
                ConflictResolution.KEEP_LOCAL,
                ConflictResolution.KEEP_REMOTE,
                ConflictResolution.MERGE
            )
            ConflictType.VERSION_CONFLICT -> listOf(
                ConflictResolution.KEEP_LOCAL,
                ConflictResolution.KEEP_REMOTE
            )
            ConflictType.DELETE_CONFLICT -> listOf(
                ConflictResolution.RESTORE,
                ConflictResolution.CONFIRM_DELETE
            )
        }
    }
}

/**
 * 충돌 타입
 */
enum class ConflictType(val displayName: String) {
    CONTENT_CONFLICT("내용 충돌"),
    VERSION_CONFLICT("버전 충돌"),
    DELETE_CONFLICT("삭제 충돌")
}

/**
 * 충돌 해결 방법
 */
enum class ConflictResolution(val displayName: String, val description: String) {
    KEEP_LOCAL("로컬 유지", "현재 기기의 버전을 유지"),
    KEEP_REMOTE("서버 버전 사용", "서버의 버전으로 덮어쓰기"),
    MERGE("병합", "두 버전을 합쳐서 새 버전 생성"),
    RESTORE("복원", "삭제된 항목을 복원"),
    CONFIRM_DELETE("삭제 확인", "삭제를 확정")
}

/**
 * 오프라인 변경사항 큐
 */
data class OfflineChangeQueue(
    val changes: List<OfflineChange>,
    val totalSize: Int,
    val oldestChangeTime: Long,
    val newestChangeTime: Long
) {
    /**
     * 큐가 비어있는지 확인
     */
    fun isEmpty(): Boolean = changes.isEmpty()

    /**
     * 대기 중인 변경사항 개수
     */
    fun getPendingCount(): Int = changes.size

    /**
     * 큐 요약
     */
    fun getSummary(): String {
        if (isEmpty()) return "대기 중인 변경사항 없음"

        val typeCount = changes.groupBy { it.type }.mapValues { it.value.size }
        val summary = typeCount.entries.joinToString(", ") { "${it.key.displayName} ${it.value}개" }

        return "대기 중: $summary"
    }
}

/**
 * 오프라인 변경사항
 */
@Serializable
data class OfflineChange(
    val id: String,
    val type: ChangeType,
    val recipeId: String,
    val changeData: String, // JSON 직렬화된 변경 데이터
    val timestamp: Long,
    val retryCount: Int = 0,
    val maxRetries: Int = 3
) {
    /**
     * 재시도 가능한지 확인
     */
    fun canRetry(): Boolean = retryCount < maxRetries

    /**
     * 만료된 변경사항인지 확인 (7일 이상 된 것)
     */
    fun isExpired(): Boolean {
        val now = System.currentTimeMillis()
        return (now - timestamp) > (7 * 24 * 60 * 60 * 1000)
    }
}

/**
 * 변경사항 타입
 */
enum class ChangeType(val displayName: String) {
    CREATE("생성"),
    UPDATE("수정"),
    DELETE("삭제"),
    FAVORITE("즐겨찾기"),
    RATING("평점")
}

/**
 * 동기화 통계
 */
data class SyncStatistics(
    val totalSyncCount: Int,
    val successfulSyncCount: Int,
    val averageSyncTime: Long, // 평균 동기화 시간 (ms)
    val totalDataTransferred: Long, // 총 전송된 데이터량 (bytes)
    val lastWeekSyncCount: Int,
    val conflictResolutionStats: Map<ConflictResolution, Int>,
    val errorFrequency: Map<String, Int> // 에러 타입별 발생 횟수
) {
    /**
     * 동기화 성공률
     */
    fun getSuccessRate(): Double {
        return if (totalSyncCount > 0) {
            (successfulSyncCount.toDouble() / totalSyncCount) * 100
        } else 0.0
    }

    /**
     * 평균 동기화 시간 표시
     */
    fun getAverageSyncTimeDisplay(): String {
        return when {
            averageSyncTime < 1000 -> "${averageSyncTime}ms"
            averageSyncTime < 60000 -> "${averageSyncTime / 1000}초"
            else -> "${averageSyncTime / 60000}분"
        }
    }

    /**
     * 데이터 사용량 표시
     */
    fun getDataUsageDisplay(): String {
        return when {
            totalDataTransferred < 1024 -> "${totalDataTransferred}B"
            totalDataTransferred < 1024 * 1024 -> "${totalDataTransferred / 1024}KB"
            else -> "${totalDataTransferred / (1024 * 1024)}MB"
        }
    }

    /**
     * 통계 요약
     */
    fun getSummary(): String {
        val successRate = String.format("%.1f", getSuccessRate())
        val avgTime = getAverageSyncTimeDisplay()
        val dataUsage = getDataUsageDisplay()

        return "성공률: ${successRate}% | 평균 시간: $avgTime | 데이터 사용: $dataUsage | 이번 주: ${lastWeekSyncCount}회"
    }
}