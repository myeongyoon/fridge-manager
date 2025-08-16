package com.mychoi.fridgemanager.domain.model

/**
 * 재료 추가 과정에서 발생할 수 있는 모든 에러 케이스를 정의
 * Repository 레이어에서 사용되는 에러 상태 관리
 *
 * sealed class를 사용하여 컴파일 타임에 모든 에러 케이스를 보장하고,
 * when 표현식에서 exhaustive checking을 지원
 */
sealed class AddIngredientError {

    /**
     * 동일한 재료가 이미 냉장고에 존재하는 경우
     * 사용자에게 기존 재료 수정을 권할 수 있음
     */
    data class DuplicateIngredient(
        val existingIngredient: UserIngredient
    ) : AddIngredientError()

    /**
     * 네트워크 연결 문제로 서버 동기화 실패
     * 로컬 저장은 성공했지만 원격 동기화가 실패한 상황
     */
    data class NetworkError(
        val message: String = "네트워크 연결을 확인해주세요"
    ) : AddIngredientError()

    /**
     * 날짜 형식 오류 (CreateIngredientRequest에서 1차 검증 후 추가 검증)
     * 예: 과거 날짜, 너무 먼 미래 날짜 등
     */
    data class InvalidDateFormat(
        val providedDate: String,
        val reason: String = "올바른 날짜 형식이 아닙니다"
    ) : AddIngredientError()

    /**
     * 필수 필드 누락 (CreateIngredientRequest 검증 후 추가 발견된 경우)
     * 예: trim 후 빈 문자열, 특수 문자만 포함된 경우
     */
    data class EmptyRequiredField(
        val fieldName: String,
        val message: String = "${fieldName}을(를) 올바르게 입력해주세요"
    ) : AddIngredientError()

    /**
     * 로컬 데이터베이스 오류 (Room DB 저장 실패)
     * 디스크 공간 부족, 권한 문제 등
     */
    data class DatabaseError(
        val cause: String = "데이터 저장 중 오류가 발생했습니다"
    ) : AddIngredientError()

    /**
     * 서버 응답 오류 (Supabase API 에러)
     * 4xx, 5xx HTTP 응답 또는 API 제한 등
     */
    data class ServerError(
        val statusCode: Int? = null,
        val message: String = "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요"
    ) : AddIngredientError()

    /**
     * 재료 분량 형식 오류
     * 예: 음수 값, 0 이하 값, 너무 큰 값 등
     */
    data class InvalidAmountFormat(
        val providedAmount: String,
        val reason: String = "올바른 분량을 입력해주세요"
    ) : AddIngredientError()

    /**
     * 보관 위치 오류 (StorageLocation enum과 불일치)
     * 예상 외의 보관 위치가 전달된 경우
     */
    data class InvalidStorageLocation(
        val providedLocation: String,
        val availableLocations: List<String> = StorageLocation.getAllDisplayNames()
    ) : AddIngredientError()

    /**
     * 사용자 인증 오류
     * 로그인 만료, 권한 부족 등
     */
    data class AuthenticationError(
        val message: String = "로그인이 필요합니다"
    ) : AddIngredientError()

    /**
     * 알 수 없는 오류 (예상하지 못한 예외 상황)
     * 개발 중에 발견되지 않은 케이스들
     */
    data class UnknownError(
        val exception: Throwable? = null,
        val message: String = "예상치 못한 오류가 발생했습니다"
    ) : AddIngredientError()
}

/**
 * AddIngredientError를 사용자 친화적인 메시지로 변환하는 확장 함수
 * UI에서 에러를 표시할 때 사용
 */
fun AddIngredientError.toUserMessage(): String {
    return when (this) {
        is AddIngredientError.DuplicateIngredient -> {
            "${existingIngredient.ingredientName}이(가) 이미 냉장고에 있습니다. 기존 재료를 수정하시겠어요?"
        }
        is AddIngredientError.NetworkError -> {
            "네트워크 연결을 확인해주세요. 재료는 로컬에 임시 저장되었습니다."
        }
        is AddIngredientError.InvalidDateFormat -> {
            "소비기한 '$providedDate'이(가) 올바르지 않습니다. $reason"
        }
        is AddIngredientError.EmptyRequiredField -> {
            message
        }
        is AddIngredientError.DatabaseError -> {
            "데이터 저장 중 오류가 발생했습니다. 저장공간을 확인해주세요."
        }
        is AddIngredientError.ServerError -> {
            if (statusCode != null) {
                "서버 오류 ($statusCode): $message"
            } else {
                message
            }
        }
        is AddIngredientError.InvalidAmountFormat -> {
            "재료 분량 '$providedAmount'이(가) 올바르지 않습니다. $reason"
        }
        is AddIngredientError.InvalidStorageLocation -> {
            "보관 위치 '$providedLocation'을(를) 찾을 수 없습니다. 다음 중 선택해주세요: ${availableLocations.joinToString(", ")}"
        }
        is AddIngredientError.AuthenticationError -> {
            message
        }
        is AddIngredientError.UnknownError -> {
            if (exception != null) {
                "알 수 없는 오류: ${exception.message ?: "상세 정보 없음"}"
            } else {
                message
            }
        }

        else -> {

        }
    }
}

/**
 * 에러 타입에 따른 사용자 액션 제안
 * UI에서 적절한 버튼이나 액션을 제공할 때 사용
 */
fun AddIngredientError.getSuggestedAction(): UserAction {
    return when (this) {
        is AddIngredientError.DuplicateIngredient -> UserAction.UPDATE_EXISTING
        is AddIngredientError.NetworkError -> UserAction.RETRY_LATER
        is AddIngredientError.InvalidDateFormat -> UserAction.CORRECT_INPUT
        is AddIngredientError.EmptyRequiredField -> UserAction.CORRECT_INPUT
        is AddIngredientError.DatabaseError -> UserAction.CHECK_STORAGE
        is AddIngredientError.ServerError -> UserAction.RETRY_LATER
        is AddIngredientError.InvalidAmountFormat -> UserAction.CORRECT_INPUT
        is AddIngredientError.InvalidStorageLocation -> UserAction.CORRECT_INPUT
        is AddIngredientError.AuthenticationError -> UserAction.LOGIN_REQUIRED
        is AddIngredientError.UnknownError -> UserAction.CONTACT_SUPPORT
        else -> {}
    }
}

/**
 * 사용자가 취할 수 있는 액션 타입
 * UI에서 에러 상황별 적절한 버튼/액션을 제공하기 위한 가이드
 */
enum class UserAction(val displayName: String, val description: String) {
    UPDATE_EXISTING("기존 재료 수정", "기존 재료 정보를 업데이트합니다"),
    RETRY_LATER("나중에 다시 시도", "네트워크 연결 후 다시 시도합니다"),
    CORRECT_INPUT("입력 수정", "입력한 정보를 올바르게 수정합니다"),
    CHECK_STORAGE("저장공간 확인", "기기의 저장공간을 확인합니다"),
    LOGIN_REQUIRED("로그인 필요", "로그인 화면으로 이동합니다"),
    CONTACT_SUPPORT("문의하기", "개발자에게 문의합니다")
}

/**
 * 에러 심각도 레벨
 * 로깅이나 모니터링에서 에러의 우선순위를 판단할 때 사용
 */
fun AddIngredientError.getSeverityLevel(): ErrorSeverity {
    return when (this) {
        is AddIngredientError.DuplicateIngredient -> ErrorSeverity.INFO
        is AddIngredientError.NetworkError -> ErrorSeverity.WARNING
        is AddIngredientError.InvalidDateFormat -> ErrorSeverity.INFO
        is AddIngredientError.EmptyRequiredField -> ErrorSeverity.INFO
        is AddIngredientError.DatabaseError -> ErrorSeverity.ERROR
        is AddIngredientError.ServerError -> {
            when (statusCode) {
                in 400..499 -> ErrorSeverity.WARNING // 클라이언트 오류
                in 500..599 -> ErrorSeverity.ERROR   // 서버 오류
                else -> ErrorSeverity.WARNING
            }
        }
        is AddIngredientError.InvalidAmountFormat -> ErrorSeverity.INFO
        is AddIngredientError.InvalidStorageLocation -> ErrorSeverity.INFO
        is AddIngredientError.AuthenticationError -> ErrorSeverity.WARNING
        is AddIngredientError.UnknownError -> ErrorSeverity.CRITICAL
        else -> {}
    }
}

/**
 * 에러 심각도 레벨 정의
 */
enum class ErrorSeverity(val level: Int, val displayName: String) {
    INFO(1, "정보"),        // 사용자 입력 오류 등, 쉽게 해결 가능
    WARNING(2, "경고"),     // 네트워크 오류 등, 일시적 문제
    ERROR(3, "오류"),       // 시스템 오류, 해결 필요
    CRITICAL(4, "심각")     // 예상치 못한 오류, 즉시 조치 필요
}

/**
 * 에러 발생 시 자동 재시도 가능 여부 판단
 * Repository 구현에서 자동 재시도 로직에 활용
 */
fun AddIngredientError.canAutoRetry(): Boolean {
    return when (this) {
        is AddIngredientError.NetworkError -> true
        is AddIngredientError.ServerError -> {
            // 5xx 서버 오류는 재시도 가능, 4xx 클라이언트 오류는 재시도 불가
            statusCode == null || statusCode >= 500
        }
        is AddIngredientError.DatabaseError -> false // 디스크 공간 등은 즉시 재시도 무의미
        else -> false // 나머지는 사용자 수정이 필요하므로 자동 재시도 불가
    }
}

/**
 * 에러 로깅을 위한 구조화된 정보 생성
 * 모니터링 시스템에서 에러 분석에 활용
 */
fun AddIngredientError.toLogEntry(): Map<String, Any?> {
    val baseInfo = mapOf(
        "errorType" to this::class.simpleName,
        "severity" to getSeverityLevel().displayName,
        "canRetry" to canAutoRetry(),
        "userMessage" to toUserMessage()
    )

    val specificInfo = when (this) {
        is AddIngredientError.DuplicateIngredient -> mapOf(
            "existingIngredientId" to existingIngredient.id,
            "existingIngredientName" to existingIngredient.ingredientName
        )
        is AddIngredientError.NetworkError -> mapOf(
            "networkMessage" to message
        )
        is AddIngredientError.InvalidDateFormat -> mapOf(
            "providedDate" to providedDate,
            "reason" to reason
        )
        is AddIngredientError.ServerError -> mapOf(
            "statusCode" to (statusCode ?: "unknown"),
            "serverMessage" to message
        )
        is AddIngredientError.UnknownError -> mapOf(
            "exceptionType" to (exception?.javaClass?.simpleName ?: "unknown"),
            "exceptionMessage" to (exception?.message ?: "no message")
        )
        else -> emptyMap()
    }

    return baseInfo + specificInfo
}