package com.mychoi.fridgemanager.domain.error

import com.mychoi.fridgemanager.domain.model.Recipe

/**
 * 레시피 관련 작업에서 발생할 수 있는 모든 에러 케이스를 정의
 * Repository 레이어에서 사용되는 에러 상태 관리
 *
 * sealed class를 사용하여 컴파일 타임에 모든 에러 케이스를 보장하고,
 * when 표현식에서 exhaustive checking을 지원
 */
sealed class RecipeError {

    /**
     * 레시피를 찾을 수 없는 경우
     * 잘못된 ID나 삭제된 레시피에 접근할 때
     */
    data class RecipeNotFound(
        val recipeId: String
    ) : RecipeError()

    /**
     * 네트워크 연결 문제로 레시피 데이터 동기화 실패
     * 로컬 캐시는 유지되지만 최신 데이터 가져오기 실패
     */
    data class NetworkError(
        val message: String = "네트워크 연결을 확인해주세요"
    ) : RecipeError()

    /**
     * 검색어 형식 오류
     * 너무 짧거나 특수문자만 포함된 경우
     */
    data class InvalidSearchQuery(
        val query: String,
        val reason: String = "올바른 검색어를 입력해주세요"
    ) : RecipeError()

    /**
     * 필수 필드 누락 (커스텀 레시피 생성 시)
     * 레시피명, 재료, 조리과정 등 필수 정보 누락
     */
    data class EmptyRequiredField(
        val fieldName: String,
        val message: String = "${fieldName}을(를) 올바르게 입력해주세요"
    ) : RecipeError()

    /**
     * 로컬 데이터베이스 오류 (Room DB 관련)
     * 캐시 저장 실패, 디스크 공간 부족 등
     */
    data class DatabaseError(
        val cause: String = "데이터 저장 중 오류가 발생했습니다"
    ) : RecipeError()

    /**
     * 서버 응답 오류 (Supabase API 에러)
     * 4xx, 5xx HTTP 응답 또는 API 제한 등
     */
    data class ServerError(
        val statusCode: Int? = null,
        val message: String = "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요"
    ) : RecipeError()

    /**
     * 잘못된 필터 조건
     * 검색 필터에서 유효하지 않은 값 제공
     */
    data class InvalidFilterCriteria(
        val filterType: String,
        val providedValue: String,
        val reason: String = "올바른 필터 조건을 설정해주세요"
    ) : RecipeError()

    /**
     * 중복된 커스텀 레시피
     * 동일한 이름의 커스텀 레시피가 이미 존재하는 경우
     */
    data class DuplicateCustomRecipe(
        val existingRecipe: Recipe
    ) : RecipeError()

    /**
     * 권한 부족 오류
     * 다른 사용자의 커스텀 레시피 수정/삭제 시도
     */
    data class PermissionDenied(
        val recipeId: String,
        val message: String = "이 레시피를 수정할 권한이 없습니다"
    ) : RecipeError()

    /**
     * 캐시 오류
     * 레시피 캐시 로딩/저장 실패
     */
    data class CacheError(
        val operation: String, // "load", "save", "invalidate"
        val message: String = "캐시 처리 중 오류가 발생했습니다"
    ) : RecipeError()

    /**
     * 동기화 충돌
     * 여러 기기에서 같은 레시피를 동시에 수정할 때
     */
    data class SyncConflict(
        val recipeId: String,
        val localVersion: Long,
        val remoteVersion: Long,
        val message: String = "다른 기기에서 수정된 내용이 있습니다"
    ) : RecipeError()

    /**
     * 잘못된 평점/피드백
     * 범위를 벗어난 평점이나 부적절한 내용
     */
    data class InvalidFeedback(
        val feedbackType: String, // "rating", "review"
        val providedValue: String,
        val reason: String = "올바른 평점을 입력해주세요"
    ) : RecipeError()

    /**
     * 레시피 용량 제한 초과
     * 너무 많은 재료나 조리과정 단계
     */
    data class RecipeSizeLimitExceeded(
        val limitType: String, // "ingredients", "instructions", "image_size"
        val currentCount: Int,
        val maxAllowed: Int,
        val message: String = "허용된 용량을 초과했습니다"
    ) : RecipeError()

    /**
     * 사용자 인증 오류
     * 로그인 만료, 권한 부족 등
     */
    data class AuthenticationError(
        val message: String = "로그인이 필요합니다"
    ) : RecipeError()

    /**
     * 알 수 없는 오류 (예상하지 못한 예외 상황)
     * 개발 중에 발견되지 않은 케이스들
     */
    data class UnknownError(
        val exception: Throwable? = null,
        val message: String = "예상치 못한 오류가 발생했습니다"
    ) : RecipeError()
}

/**
 * RecipeError를 사용자 친화적인 메시지로 변환하는 확장 함수
 * UI에서 에러를 표시할 때 사용
 */
fun RecipeError.toUserMessage(): String {
    return when (this) {
        is RecipeError.RecipeNotFound -> {
            "요청하신 레시피를 찾을 수 없습니다."
        }
        is RecipeError.NetworkError -> {
            "네트워크 연결을 확인해주세요. 오프라인 모드로 전환됩니다."
        }
        is RecipeError.InvalidSearchQuery -> {
            "검색어 '$query'가 올바르지 않습니다. $reason"
        }
        is RecipeError.EmptyRequiredField -> {
            message
        }
        is RecipeError.DatabaseError -> {
            "데이터 저장 중 오류가 발생했습니다. 저장공간을 확인해주세요."
        }
        is RecipeError.ServerError -> {
            if (statusCode != null) {
                "서버 오류 ($statusCode): $message"
            } else {
                message
            }
        }
        is RecipeError.InvalidFilterCriteria -> {
            "검색 필터 '$filterType'에서 '$providedValue'은(는) 올바르지 않습니다. $reason"
        }
        is RecipeError.DuplicateCustomRecipe -> {
            "'${existingRecipe.name}'과(와) 동일한 이름의 레시피가 이미 있습니다. 다른 이름으로 저장해주세요."
        }
        is RecipeError.PermissionDenied -> {
            message
        }
        is RecipeError.CacheError -> {
            "레시피 데이터 처리 중 오류가 발생했습니다. 앱을 다시 시작해보세요."
        }
        is RecipeError.SyncConflict -> {
            "$message 어떤 버전을 유지하시겠어요?"
        }
        is RecipeError.InvalidFeedback -> {
            "평점/리뷰 '$providedValue'이(가) 올바르지 않습니다. $reason"
        }
        is RecipeError.RecipeSizeLimitExceeded -> {
            "$limitType 개수가 너무 많습니다. 최대 ${maxAllowed}개까지 가능합니다. (현재: ${currentCount}개)"
        }
        is RecipeError.AuthenticationError -> {
            message
        }
        is RecipeError.UnknownError -> {
            if (exception != null) {
                "알 수 없는 오류: ${exception.message ?: "상세 정보 없음"}"
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
fun RecipeError.getSuggestedAction(): RecipeUserAction {
    return when (this) {
        is RecipeError.RecipeNotFound -> RecipeUserAction.GO_BACK
        is RecipeError.NetworkError -> RecipeUserAction.RETRY_LATER
        is RecipeError.InvalidSearchQuery -> RecipeUserAction.CORRECT_INPUT
        is RecipeError.EmptyRequiredField -> RecipeUserAction.CORRECT_INPUT
        is RecipeError.DatabaseError -> RecipeUserAction.CHECK_STORAGE
        is RecipeError.ServerError -> RecipeUserAction.RETRY_LATER
        is RecipeError.InvalidFilterCriteria -> RecipeUserAction.CORRECT_INPUT
        is RecipeError.DuplicateCustomRecipe -> RecipeUserAction.CHOOSE_DIFFERENT_NAME
        is RecipeError.PermissionDenied -> RecipeUserAction.CONTACT_SUPPORT
        is RecipeError.CacheError -> RecipeUserAction.RESTART_APP
        is RecipeError.SyncConflict -> RecipeUserAction.RESOLVE_CONFLICT
        is RecipeError.InvalidFeedback -> RecipeUserAction.CORRECT_INPUT
        is RecipeError.RecipeSizeLimitExceeded -> RecipeUserAction.REDUCE_SIZE
        is RecipeError.AuthenticationError -> RecipeUserAction.LOGIN_REQUIRED
        is RecipeError.UnknownError -> RecipeUserAction.CONTACT_SUPPORT
    }
}

/**
 * 사용자가 취할 수 있는 액션 타입 (레시피 관련)
 * UI에서 에러 상황별 적절한 버튼/액션을 제공하기 위한 가이드
 */
enum class RecipeUserAction(val displayName: String, val description: String) {
    GO_BACK("이전으로", "이전 화면으로 돌아갑니다"),
    RETRY_LATER("나중에 다시 시도", "네트워크 연결 후 다시 시도합니다"),
    CORRECT_INPUT("입력 수정", "입력한 정보를 올바르게 수정합니다"),
    CHECK_STORAGE("저장공간 확인", "기기의 저장공간을 확인합니다"),
    CHOOSE_DIFFERENT_NAME("다른 이름 선택", "중복되지 않는 레시피 이름을 입력합니다"),
    CONTACT_SUPPORT("문의하기", "개발자에게 문의합니다"),
    RESTART_APP("앱 재시작", "앱을 완전히 종료 후 다시 시작합니다"),
    RESOLVE_CONFLICT("충돌 해결", "어떤 버전을 유지할지 선택합니다"),
    REDUCE_SIZE("크기 줄이기", "재료나 조리과정 수를 줄입니다"),
    LOGIN_REQUIRED("로그인 필요", "로그인 화면으로 이동합니다")
}

/**
 * 에러 심각도 레벨
 * 로깅이나 모니터링에서 에러의 우선순위를 판단할 때 사용
 */
fun RecipeError.getSeverityLevel(): ErrorSeverity {
    return when (this) {
        is RecipeError.RecipeNotFound -> ErrorSeverity.INFO
        is RecipeError.NetworkError -> ErrorSeverity.WARNING
        is RecipeError.InvalidSearchQuery -> ErrorSeverity.INFO
        is RecipeError.EmptyRequiredField -> ErrorSeverity.INFO
        is RecipeError.DatabaseError -> ErrorSeverity.ERROR
        is RecipeError.ServerError -> {
            when (statusCode) {
                in 400..499 -> ErrorSeverity.WARNING // 클라이언트 오류
                in 500..599 -> ErrorSeverity.ERROR   // 서버 오류
                else -> ErrorSeverity.WARNING
            }
        }
        is RecipeError.InvalidFilterCriteria -> ErrorSeverity.INFO
        is RecipeError.DuplicateCustomRecipe -> ErrorSeverity.INFO
        is RecipeError.PermissionDenied -> ErrorSeverity.WARNING
        is RecipeError.CacheError -> ErrorSeverity.WARNING
        is RecipeError.SyncConflict -> ErrorSeverity.WARNING
        is RecipeError.InvalidFeedback -> ErrorSeverity.INFO
        is RecipeError.RecipeSizeLimitExceeded -> ErrorSeverity.INFO
        is RecipeError.AuthenticationError -> ErrorSeverity.WARNING
        is RecipeError.UnknownError -> ErrorSeverity.CRITICAL
    }
}

/**
 * 에러 발생 시 자동 재시도 가능 여부 판단
 * Repository 구현에서 자동 재시도 로직에 활용
 */
fun RecipeError.canAutoRetry(): Boolean {
    return when (this) {
        is RecipeError.NetworkError -> true
        is RecipeError.ServerError -> {
            // 5xx 서버 오류는 재시도 가능, 4xx 클라이언트 오류는 재시도 불가
            statusCode == null || statusCode >= 500
        }
        is RecipeError.CacheError -> true // 캐시 오류는 재시도 가능
        is RecipeError.DatabaseError -> false // 디스크 공간 등은 즉시 재시도 무의미
        else -> false // 나머지는 사용자 수정이 필요하므로 자동 재시도 불가
    }
}

/**
 * 에러 로깅을 위한 구조화된 정보 생성
 * 모니터링 시스템에서 에러 분석에 활용
 */
fun RecipeError.toLogEntry(): Map<String, Any?> {
    val baseInfo = mapOf(
        "errorType" to this::class.simpleName,
        "severity" to getSeverityLevel().displayName,
        "canRetry" to canAutoRetry(),
        "userMessage" to toUserMessage(),
        "suggestedAction" to getSuggestedAction().displayName
    )

    val specificInfo = when (this) {
        is RecipeError.RecipeNotFound -> mapOf(
            "recipeId" to recipeId
        )
        is RecipeError.NetworkError -> mapOf(
            "networkMessage" to message
        )
        is RecipeError.InvalidSearchQuery -> mapOf(
            "query" to query,
            "reason" to reason
        )
        is RecipeError.EmptyRequiredField -> mapOf(
            "fieldName" to fieldName
        )
        is RecipeError.ServerError -> mapOf(
            "statusCode" to (statusCode ?: "unknown"),
            "serverMessage" to message
        )
        is RecipeError.InvalidFilterCriteria -> mapOf(
            "filterType" to filterType,
            "providedValue" to providedValue,
            "reason" to reason
        )
        is RecipeError.DuplicateCustomRecipe -> mapOf(
            "existingRecipeId" to existingRecipe.id,
            "existingRecipeName" to existingRecipe.name
        )
        is RecipeError.PermissionDenied -> mapOf(
            "recipeId" to recipeId
        )
        is RecipeError.CacheError -> mapOf(
            "operation" to operation
        )
        is RecipeError.SyncConflict -> mapOf(
            "recipeId" to recipeId,
            "localVersion" to localVersion,
            "remoteVersion" to remoteVersion
        )
        is RecipeError.InvalidFeedback -> mapOf(
            "feedbackType" to feedbackType,
            "providedValue" to providedValue,
            "reason" to reason
        )
        is RecipeError.RecipeSizeLimitExceeded -> mapOf(
            "limitType" to limitType,
            "currentCount" to currentCount,
            "maxAllowed" to maxAllowed
        )
        is RecipeError.AuthenticationError -> mapOf(
            "authMessage" to message
        )
        is RecipeError.UnknownError -> mapOf(
            "exceptionType" to (exception?.javaClass?.simpleName ?: "unknown"),
            "exceptionMessage" to (exception?.message ?: "no message")
        )

        is RecipeError.DatabaseError -> TODO()
    }

    return baseInfo + specificInfo
}