package com.mychoi.fridgemanager.domain.error

import com.mychoi.fridgemanager.domain.model.Ingredient

/**
 * 재료 마스터 데이터 관리 과정에서 발생할 수 있는 모든 에러 케이스 정의
 * IngredientRepository에서 사용되는 에러 상태 관리
 *
 * sealed class를 사용하여 컴파일 타임에 모든 에러 케이스를 보장하고,
 * when 표현식에서 exhaustive checking을 지원
 */
sealed class IngredientError {

    /**
     * 재료를 찾을 수 없는 경우
     * 사용자가 입력한 재료명이 마스터 데이터에 존재하지 않음
     */
    data class IngredientNotFound(
        val searchQuery: String,
        val suggestion: String? = null
    ) : IngredientError()

    /**
     * 검색어가 너무 짧거나 유효하지 않은 경우
     * 최소 검색 길이 미만이거나 특수문자만 포함된 경우
     */
    data class InvalidSearchQuery(
        val query: String,
        val reason: String = "검색어는 최소 1자 이상 입력해주세요"
    ) : IngredientError()

    /**
     * 재료 카테고리가 유효하지 않은 경우
     * 정의되지 않은 카테고리나 잘못된 형식
     */
    data class InvalidCategory(
        val providedCategory: String,
        val validCategories: List<String> = listOf("채소", "육류", "유제품", "곡류", "조미료", "해산물", "과일")
    ) : IngredientError()

    /**
     * 새 재료 제안 시 중복된 재료명인 경우
     * 이미 마스터 데이터에 존재하는 재료
     */
    data class DuplicateIngredientName(
        val ingredientName: String,
        val existingIngredient: Ingredient
    ) : IngredientError()

    /**
     * 새 재료 제안 정보가 유효하지 않은 경우
     * 필수 필드 누락, 형식 오류 등
     */
    data class InvalidIngredientData(
        val fieldName: String,
        val providedValue: String,
        val reason: String
    ) : IngredientError()

    /**
     * 네트워크 연결 문제로 마스터 데이터 동기화 실패
     * 서버에서 최신 재료 정보를 가져올 수 없는 상황
     */
    data class SyncError(
        val message: String = "재료 정보 동기화에 실패했습니다",
        val cause: String? = null
    ) : IngredientError()

    /**
     * 대체 재료 제안 실패
     * 원본 재료에 대한 적절한 대체재료를 찾을 수 없는 경우
     */
    data class NoAlternativesFound(
        val originalIngredient: String,
        val searchedCategories: List<String> = emptyList()
    ) : IngredientError()

    /**
     * 검색 결과가 너무 많은 경우
     * 성능 보호를 위한 결과 수 제한
     */
    data class TooManyResults(
        val query: String,
        val resultCount: Int,
        val maxAllowed: Int = 100
    ) : IngredientError()

    /**
     * 서버 응답 오류 (API 에러)
     * 재료 마스터 데이터 서버에서 발생한 오류
     */
    data class ServerError(
        val statusCode: Int? = null,
        val message: String = "재료 정보 서버에서 오류가 발생했습니다"
    ) : IngredientError()

    /**
     * 로컬 데이터베이스 오류
     * 재료 마스터 데이터 로컬 저장 실패
     */
    data class DatabaseError(
        val operation: String, // "read", "write", "update", "delete"
        val cause: String = "로컬 데이터베이스 오류가 발생했습니다"
    ) : IngredientError()

    /**
     * 사용자 권한 오류
     * 재료 제안이나 통계 조회 권한 부족
     */
    data class PermissionError(
        val requiredPermission: String,
        val message: String = "해당 기능을 사용할 권한이 없습니다"
    ) : IngredientError()

    /**
     * 데이터 캐시 만료
     * 로컬 캐시된 재료 데이터가 너무 오래되어 갱신 필요
     */
    data class CacheExpired(
        val lastUpdateTime: Long,
        val maxCacheAge: Long = 7 * 24 * 60 * 60 * 1000L // 7일
    ) : IngredientError()

    /**
     * 재료 통계 데이터 부족
     * 분석에 필요한 최소 데이터가 없는 경우
     */
    data class InsufficientData(
        val dataType: String, // "usage", "search", "preference"
        val minimumRequired: Int,
        val currentCount: Int
    ) : IngredientError()

    /**
     * 외부 API 연동 실패
     * 제철 재료, 영양 정보 등 외부 서비스 오류
     */
    data class ExternalApiError(
        val apiName: String,
        val errorCode: String? = null,
        val message: String = "외부 서비스 연동에 실패했습니다"
    ) : IngredientError()

    /**
     * 알 수 없는 오류 (예상하지 못한 예외 상황)
     * 개발 중에 발견되지 않은 케이스들
     */
    data class UnknownError(
        val exception: Throwable? = null,
        val context: String? = null,
        val message: String = "예상치 못한 오류가 발생했습니다"
    ) : IngredientError()
}

/**
 * IngredientError를 사용자 친화적인 메시지로 변환하는 확장 함수
 * UI에서 에러를 표시할 때 사용
 */
fun IngredientError.toUserMessage(): String {
    return when (this) {
        is IngredientError.IngredientNotFound -> {
            if (suggestion != null) {
                "'$searchQuery'을(를) 찾을 수 없습니다. '$suggestion'을(를) 찾으셨나요?"
            } else {
                "'$searchQuery'을(를) 찾을 수 없습니다. 다른 검색어를 시도해보세요."
            }
        }
        is IngredientError.InvalidSearchQuery -> {
            reason
        }
        is IngredientError.InvalidCategory -> {
            "올바르지 않은 카테고리입니다. 다음 중 선택해주세요: ${validCategories.joinToString(", ")}"
        }
        is IngredientError.DuplicateIngredientName -> {
            "'$ingredientName'은(는) 이미 등록된 재료입니다."
        }
        is IngredientError.InvalidIngredientData -> {
            "${fieldName}이(가) 올바르지 않습니다: $reason"
        }
        is IngredientError.SyncError -> {
            "재료 정보를 업데이트할 수 없습니다. 네트워크 연결을 확인해주세요."
        }
        is IngredientError.NoAlternativesFound -> {
            "'$originalIngredient'의 대체 재료를 찾을 수 없습니다."
        }
        is IngredientError.TooManyResults -> {
            "검색 결과가 너무 많습니다($resultCount 개). 더 구체적인 검색어를 입력해주세요."
        }
        is IngredientError.ServerError -> {
            if (statusCode != null) {
                "재료 정보 서버 오류 ($statusCode): $message"
            } else {
                message
            }
        }
        is IngredientError.DatabaseError -> {
            "데이터 저장 중 오류가 발생했습니다. 앱을 다시 시작해보세요."
        }
        is IngredientError.PermissionError -> {
            message
        }
        is IngredientError.CacheExpired -> {
            "재료 정보가 오래되었습니다. 새로고침해주세요."
        }
        is IngredientError.InsufficientData -> {
            when (dataType) {
                "usage" -> "사용 기록이 부족해 분석할 수 없습니다. 더 많은 재료를 사용해보세요."
                "search" -> "검색 기록이 부족해 통계를 생성할 수 없습니다."
                "preference" -> "선호도 분석에 필요한 데이터가 부족합니다."
                else -> "분석에 필요한 데이터가 부족합니다."
            }
        }
        is IngredientError.ExternalApiError -> {
            when (apiName) {
                "nutrition" -> "영양 정보를 가져올 수 없습니다."
                "seasonal" -> "제철 재료 정보를 불러올 수 없습니다."
                else -> "외부 서비스 연결에 실패했습니다."
            }
        }
        is IngredientError.UnknownError -> {
            if (context != null) {
                "$context: $message"
            } else {
                message
            }
        }
    }
}

/**
 * 에러 타입에 따른 사용자 액션 제안
 * UI에서 적절한 버튼이나 액션을 제공할 때 사용
 */
fun IngredientError.getSuggestedAction(): IngredientUserAction {
    return when (this) {
        is IngredientError.IngredientNotFound -> IngredientUserAction.SUGGEST_ALTERNATIVES
        is IngredientError.InvalidSearchQuery -> IngredientUserAction.CORRECT_INPUT
        is IngredientError.InvalidCategory -> IngredientUserAction.SELECT_CATEGORY
        is IngredientError.DuplicateIngredientName -> IngredientUserAction.USE_EXISTING
        is IngredientError.InvalidIngredientData -> IngredientUserAction.CORRECT_INPUT
        is IngredientError.SyncError -> IngredientUserAction.RETRY_SYNC
        is IngredientError.NoAlternativesFound -> IngredientUserAction.ADD_CUSTOM
        is IngredientError.TooManyResults -> IngredientUserAction.REFINE_SEARCH
        is IngredientError.ServerError -> IngredientUserAction.RETRY_LATER
        is IngredientError.DatabaseError -> IngredientUserAction.RESTART_APP
        is IngredientError.PermissionError -> IngredientUserAction.CHECK_PERMISSIONS
        is IngredientError.CacheExpired -> IngredientUserAction.REFRESH_DATA
        is IngredientError.InsufficientData -> IngredientUserAction.USE_MORE_FEATURES
        is IngredientError.ExternalApiError -> IngredientUserAction.TRY_OFFLINE_MODE
        is IngredientError.UnknownError -> IngredientUserAction.CONTACT_SUPPORT
    }
}

/**
 * 사용자가 취할 수 있는 액션 타입
 * UI에서 에러 상황별 적절한 버튼/액션을 제공하기 위한 가이드
 */
enum class IngredientUserAction(val displayName: String, val description: String) {
    SUGGEST_ALTERNATIVES("유사한 재료 보기", "비슷한 이름의 재료들을 확인합니다"),
    CORRECT_INPUT("검색어 수정", "올바른 재료명을 입력합니다"),
    SELECT_CATEGORY("카테고리 선택", "올바른 카테고리를 선택합니다"),
    USE_EXISTING("기존 재료 사용", "이미 등록된 재료를 사용합니다"),
    RETRY_SYNC("동기화 재시도", "재료 정보 업데이트를 다시 시도합니다"),
    ADD_CUSTOM("직접 추가", "새로운 재료로 직접 추가합니다"),
    REFINE_SEARCH("검색어 구체화", "더 구체적인 검색어를 입력합니다"),
    RETRY_LATER("나중에 다시 시도", "잠시 후 다시 시도합니다"),
    RESTART_APP("앱 재시작", "앱을 다시 시작합니다"),
    CHECK_PERMISSIONS("권한 확인", "앱 권한을 확인합니다"),
    REFRESH_DATA("새로고침", "데이터를 새로고침합니다"),
    USE_MORE_FEATURES("더 사용해보기", "다양한 기능을 사용해보세요"),
    TRY_OFFLINE_MODE("오프라인 사용", "인터넷 연결 없이 사용합니다"),
    CONTACT_SUPPORT("문의하기", "개발자에게 문의합니다")
}

/**
 * 에러 심각도 레벨
 * 로깅이나 모니터링에서 에러의 우선순위를 판단할 때 사용
 */
fun IngredientError.getSeverityLevel(): IngredientErrorSeverity {
    return when (this) {
        is IngredientError.IngredientNotFound -> IngredientErrorSeverity.INFO
        is IngredientError.InvalidSearchQuery -> IngredientErrorSeverity.INFO
        is IngredientError.InvalidCategory -> IngredientErrorSeverity.INFO
        is IngredientError.DuplicateIngredientName -> IngredientErrorSeverity.INFO
        is IngredientError.InvalidIngredientData -> IngredientErrorSeverity.WARNING
        is IngredientError.SyncError -> IngredientErrorSeverity.WARNING
        is IngredientError.NoAlternativesFound -> IngredientErrorSeverity.INFO
        is IngredientError.TooManyResults -> IngredientErrorSeverity.INFO
        is IngredientError.ServerError -> {
            when (statusCode) {
                in 400..499 -> IngredientErrorSeverity.WARNING // 클라이언트 오류
                in 500..599 -> IngredientErrorSeverity.ERROR   // 서버 오류
                else -> IngredientErrorSeverity.WARNING
            }
        }
        is IngredientError.DatabaseError -> IngredientErrorSeverity.ERROR
        is IngredientError.PermissionError -> IngredientErrorSeverity.WARNING
        is IngredientError.CacheExpired -> IngredientErrorSeverity.INFO
        is IngredientError.InsufficientData -> IngredientErrorSeverity.INFO
        is IngredientError.ExternalApiError -> IngredientErrorSeverity.WARNING
        is IngredientError.UnknownError -> IngredientErrorSeverity.CRITICAL
    }
}

/**
 * 에러 심각도 레벨 정의
 */
enum class IngredientErrorSeverity(val level: Int, val displayName: String) {
    INFO(1, "정보"),        // 사용자 입력 문제, 쉽게 해결 가능
    WARNING(2, "경고"),     // 일시적 문제, 재시도 가능
    ERROR(3, "오류"),       // 시스템 문제, 해결 필요
    CRITICAL(4, "심각")     // 예상치 못한 오류, 즉시 조치 필요
}

/**
 * 에러 발생 시 자동 재시도 가능 여부 판단
 * Repository 구현에서 자동 재시도 로직에 활용
 */
fun IngredientError.canAutoRetry(): Boolean {
    return when (this) {
        is IngredientError.SyncError -> true
        is IngredientError.ServerError -> {
            // 5xx 서버 오류는 재시도 가능, 4xx 클라이언트 오류는 재시도 불가
            statusCode == null || statusCode >= 500
        }
        is IngredientError.DatabaseError -> {
            // 읽기 작업은 재시도 가능, 쓰기 작업은 상황에 따라
            operation == "read"
        }
        is IngredientError.ExternalApiError -> true
        is IngredientError.CacheExpired -> true
        else -> false // 나머지는 사용자 액션이 필요하므로 자동 재시도 불가
    }
}

/**
 * 에러 로깅을 위한 구조화된 정보 생성
 * 모니터링 시스템에서 에러 분석에 활용
 */
fun IngredientError.toLogEntry(): Map<String, Any?> {
    val baseInfo = mapOf(
        "errorType" to this::class.simpleName,
        "severity" to getSeverityLevel().displayName,
        "canRetry" to canAutoRetry(),
        "userMessage" to toUserMessage(),
        "suggestedAction" to getSuggestedAction().displayName
    )

    val specificInfo = when (this) {
        is IngredientError.IngredientNotFound -> mapOf(
            "searchQuery" to searchQuery,
            "suggestion" to suggestion
        )
        is IngredientError.InvalidSearchQuery -> mapOf(
            "query" to query,
            "reason" to reason
        )
        is IngredientError.InvalidCategory -> mapOf(
            "providedCategory" to providedCategory,
            "validCategories" to validCategories
        )
        is IngredientError.DuplicateIngredientName -> mapOf(
            "ingredientName" to ingredientName,
            "existingIngredientId" to existingIngredient.id
        )
        is IngredientError.ServerError -> mapOf(
            "statusCode" to (statusCode ?: "unknown"),
            "serverMessage" to message
        )
        is IngredientError.DatabaseError -> mapOf(
            "operation" to operation,
            "cause" to cause
        )
        is IngredientError.TooManyResults -> mapOf(
            "query" to query,
            "resultCount" to resultCount,
            "maxAllowed" to maxAllowed
        )
        is IngredientError.UnknownError -> mapOf(
            "exceptionType" to (exception?.javaClass?.simpleName ?: "unknown"),
            "exceptionMessage" to (exception?.message ?: "no message"),
            "context" to context
        )
        else -> emptyMap()
    }

    return baseInfo + specificInfo
}

/**
 * 에러별 복구 전략 제안
 * Repository 구현에서 에러 복구 로직에 활용
 */
fun IngredientError.getRecoveryStrategy(): IngredientRecoveryStrategy {
    return when (this) {
        is IngredientError.IngredientNotFound -> IngredientRecoveryStrategy.SUGGEST_SIMILAR
        is IngredientError.SyncError -> IngredientRecoveryStrategy.USE_CACHE
        is IngredientError.ServerError -> IngredientRecoveryStrategy.FALLBACK_LOCAL
        is IngredientError.DatabaseError -> IngredientRecoveryStrategy.REBUILD_CACHE
        is IngredientError.CacheExpired -> IngredientRecoveryStrategy.FORCE_REFRESH
        is IngredientError.ExternalApiError -> IngredientRecoveryStrategy.USE_DEFAULT_DATA
        else -> IngredientRecoveryStrategy.NONE
    }
}

/**
 * 에러 복구 전략
 */
enum class IngredientRecoveryStrategy(val displayName: String) {
    SUGGEST_SIMILAR("유사 재료 제안"),
    USE_CACHE("캐시 데이터 사용"),
    FALLBACK_LOCAL("로컬 데이터 대체"),
    REBUILD_CACHE("캐시 재구축"),
    FORCE_REFRESH("강제 새로고침"),
    USE_DEFAULT_DATA("기본 데이터 사용"),
    NONE("복구 불가")
}