package com.mychoi.fridgemanager.domain.request

import com.mychoi.fridgemanager.domain.model.StorageLocation
import com.mychoi.fridgemanager.domain.model.UserIngredient
import java.text.SimpleDateFormat
import java.util.*

/**
 * 새로운 재료 추가 요청 데이터 클래스
 * 필수 필드에 대한 기본 검증을 포함
 */
data class CreateIngredientRequest(
    val ingredientName: String,
    val amount: String,
    val unit: String,
    val expiryDate: String,              // "2025-08-20" 형식
    val storageLocation: StorageLocation,
    val memo: String = ""
) {

    init {
        // 필수 필드 검증 (우선순위 순서로)
        require(ingredientName.isNotBlank()) {
            "재료명을 입력해주세요"
        }
        require(amount.isNotBlank()) {
            "재료양을 입력해주세요"
        }
        require(unit.isNotBlank()) {
            "단위를 입력해주세요"
        }
        require(expiryDate.isNotBlank()) {
            "소비기한을 입력해주세요"
        }

        // 날짜 형식 검증
        validateDateFormat(expiryDate)
    }

    /**
     * 날짜 형식 검증 (yyyy-MM-dd)
     * @param dateString 검증할 날짜 문자열
     * @throws IllegalArgumentException 잘못된 날짜 형식인 경우
     */
    private fun validateDateFormat(dateString: String) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateFormat.isLenient = false // 엄격한 파싱
            dateFormat.parse(dateString)
        } catch (e: Exception) {
            throw IllegalArgumentException("날짜 형식이 올바르지 않습니다. 2025-12-31 형식으로 입력해주세요")
        }
    }

    /**
     * UserIngredient로 변환하기 위한 헬퍼 메서드
     * @param userId 사용자 ID
     * @return 변환된 UserIngredient 객체
     */
    fun toUserIngredient(userId: String): UserIngredient {
        val currentDateTime = UserIngredient.getCurrentISODateTime()

        return UserIngredient(
            userId = userId,
            ingredientName = ingredientName,
            amount = amount,
            unit = unit,
            expiryDate = expiryDate,
            storageLocation = storageLocation.displayName, // enum을 String으로 변환
            memo = memo,
            createdAt = currentDateTime,
            updatedAt = currentDateTime
        )
    }
}
