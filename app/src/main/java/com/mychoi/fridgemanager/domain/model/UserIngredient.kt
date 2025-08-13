package com.mychoi.fridgemanager.domain.model

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*

/**
 * 사용자 냉장고 재료 도메인 모델
 * user_ingredients 테이블을 기반으로 한 실제 냉장고 재료 정보
 */
@Serializable
data class UserIngredient(
    val id: String = "",                         // UUID를 String으로 처리
    val userId: String,                          // DB의 user_id
    val ingredientName: String,                  // DB의 ingredient_name
    val amount: String,                          // DB의 amount ("3개", "500g")
    val unit: String,                            // DB의 unit
    val expiryDate: String,                      // DB의 expiry_date (ISO 날짜 형식)
    val purchaseDate: String? = null,            // DB의 purchase_date
    val storageLocation: String = "냉장실",      // DB의 storage_location
    val memo: String = "",                       // DB의 memo
    val createdAt: String = "",                  // 🔧 수정: 기본값을 빈 문자열로 변경
    val updatedAt: String = "",                  // 🔧 수정: 기본값을 빈 문자열로 변경
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY
) {

    /**
     * 유통기한까지 남은 일수 계산
     * 🔧 수정: API 호환성을 위해 SimpleDateFormat 사용
     */
    fun getDaysUntilExpiry(): Long {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val expiry = dateFormat.parse(expiryDate)
            val today = Date()

            if (expiry != null) {
                val diffInMillis = expiry.time - today.time
                diffInMillis / (24 * 60 * 60 * 1000) // 밀리초를 일수로 변환
            } else {
                0L
            }
        } catch (e: Exception) {
            0L // 파싱 오류 시 0 반환
        }
    }

    /**
     * 유통기한 상태 확인
     * @return 유통기한 상태
     */
    fun getExpiryStatus(): ExpiryStatus {
        val days = getDaysUntilExpiry()
        return when {
            days < 0 -> ExpiryStatus.EXPIRED
            days == 0L -> ExpiryStatus.EXPIRES_TODAY
            days <= 3 -> ExpiryStatus.EXPIRES_SOON
            days <= 7 -> ExpiryStatus.EXPIRES_THIS_WEEK
            else -> ExpiryStatus.FRESH
        }
    }

    /**
     * 재료 사용 가능 여부 확인 (유통기한 기준)
     * @return 사용 가능 여부
     */
    fun isUsable(): Boolean {
        return getDaysUntilExpiry() >= -1 // 하루 지난 것까지는 허용
    }

    /**
     * 표시용 수량 문자열 생성
     * @return 수량과 단위를 포함한 문자열 (예: "500g", "2개")
     */
    fun getQuantityDisplay(): String {
        return "$amount$unit"
    }

    /**
     * 재료 상태 요약 정보
     * @return 사용자에게 보여줄 상태 정보
     */
    fun getStatusSummary(): String {
        val quantityText = getQuantityDisplay()
        val locationText = storageLocation
        val expiryText = when (getExpiryStatus()) {
            ExpiryStatus.EXPIRED -> "⚠️ 유통기한 지남"
            ExpiryStatus.EXPIRES_TODAY -> "🔶 오늘 만료"
            ExpiryStatus.EXPIRES_SOON -> "🔸 ${getDaysUntilExpiry()}일 후 만료"
            ExpiryStatus.EXPIRES_THIS_WEEK -> "${getDaysUntilExpiry()}일 후 만료"
            ExpiryStatus.FRESH -> "${getDaysUntilExpiry()}일 남음"
        }

        return "$quantityText · $locationText · $expiryText"
    }

    /**
     * 재료 정보 업데이트
     * @param newAmount 새로운 수량
     * @param newExpiryDate 새로운 유통기한
     * @param newMemo 새로운 메모
     * @param newStorageLocation 새로운 보관 위치
     * @return 업데이트된 재료
     */
    fun update(
        newAmount: String? = null,
        newExpiryDate: String? = null,
        newMemo: String? = null,
        newStorageLocation: String? = null
    ): UserIngredient {
        return copy(
            amount = newAmount ?: this.amount,
            expiryDate = newExpiryDate ?: this.expiryDate,
            memo = newMemo ?: this.memo,
            storageLocation = newStorageLocation ?: this.storageLocation,
            updatedAt = getCurrentISODate(),
            syncStatus = SyncStatus.PENDING_SYNC
        )
    }

    /**
     * 레시피의 재료 요구사항과 매칭 확인
     * @param recipeIngredient 레시피 재료 요구사항
     * @return 매칭 정보 (충분한 양이 있는지, 부족한 양은 얼마인지)
     */
    fun checkRecipeMatch(recipeIngredient: RecipeIngredient): IngredientMatch {
        val isNameMatch = ingredientName.equals(recipeIngredient.ingredientName, ignoreCase = true)

        if (!isNameMatch) {
            return IngredientMatch(
                ingredient = this,
                recipeIngredient = recipeIngredient,
                isAvailable = false,
                isSufficientQuantity = false,
                shortfall = 0.0
            )
        }

        // amount 문자열에서 숫자 추출 (예: "500g" -> 500.0)
        val currentQuantity = parseQuantity(amount)
        val requiredQuantity = parseQuantity(recipeIngredient.amount ?: "0")

        // 단위 변환 (간단한 버전)
        val convertedQuantity = convertUnit(currentQuantity, unit, recipeIngredient.unit ?: unit)

        val isSufficient = convertedQuantity >= requiredQuantity
        val shortfall = if (isSufficient) 0.0 else requiredQuantity - convertedQuantity

        return IngredientMatch(
            ingredient = this,
            recipeIngredient = recipeIngredient,
            isAvailable = true,
            isSufficientQuantity = isSufficient,
            shortfall = shortfall
        )
    }

    /**
     * 동기화 상태 업데이트
     * @param status 새로운 동기화 상태
     * @return 상태가 업데이트된 재료
     */
    fun updateSyncStatus(status: SyncStatus): UserIngredient {
        return copy(
            syncStatus = status,
            updatedAt = if (status == SyncStatus.SYNCED) updatedAt else getCurrentISODate()
        )
    }

    companion object {
        /**
         * 🔧 수정: API 호환성을 위해 SimpleDateFormat 사용
         * 현재 ISO 8601 날짜 문자열 반환
         */
        fun getCurrentISODate(): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return dateFormat.format(Date())
        }

        /**
         * 🆕 추가: 현재 날짜시간 문자열 반환 (UserPreference와 호환)
         */
        fun getCurrentISODateTime(): String {
            val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            return dateTimeFormat.format(Date())
        }

        /**
         * 🆕 추가: UserIngredient 생성 헬퍼 메서드
         */
        fun create(
            userId: String,
            ingredientName: String,
            amount: String,
            unit: String,
            expiryDate: String,
            storageLocation: String = "냉장실"
        ): UserIngredient {
            val currentDate = getCurrentISODate()
            return UserIngredient(
                userId = userId,
                ingredientName = ingredientName,
                amount = amount,
                unit = unit,
                expiryDate = expiryDate,
                storageLocation = storageLocation,
                createdAt = currentDate,
                updatedAt = currentDate
            )
        }

        /**
         * 간단한 단위 변환 (실제로는 더 정교한 변환 테이블 필요)
         * @param quantity 수량
         * @param fromUnit 변환 전 단위
         * @param toUnit 변환 후 단위
         * @return 변환된 수량
         */
        private fun convertUnit(quantity: Double, fromUnit: String, toUnit: String): Double {
            // 기본적인 단위 변환만 구현 (실제로는 더 복잡한 변환표 필요)
            return when {
                fromUnit == toUnit -> quantity
                fromUnit == "kg" && toUnit == "g" -> quantity * 1000
                fromUnit == "g" && toUnit == "kg" -> quantity / 1000
                fromUnit == "L" && toUnit == "ml" -> quantity * 1000
                fromUnit == "ml" && toUnit == "L" -> quantity / 1000
                else -> quantity // 단위 변환 불가능한 경우 원래 값 반환
            }
        }

        /**
         * 재료 수량 문자열 파싱
         * @param amountString 수량 문자열 (예: "200g", "2개", "1큰술")
         * @return 파싱된 수량 (실패 시 0.0)
         */
        private fun parseQuantity(amountString: String): Double {
            return try {
                // 숫자 부분만 추출하여 파싱
                val numberString = amountString.replace(Regex("[^0-9.]"), "")
                numberString.toDoubleOrNull() ?: 0.0
            } catch (e: Exception) {
                0.0
            }
        }

        /**
         * 유통기한이 임박한 재료들 필터링
         * @param ingredients 재료 목록
         * @param days 임박 기준 일수 (기본 3일)
         * @return 유통기한이 임박한 재료 목록
         */
        fun getExpiringSoon(ingredients: List<UserIngredient>, days: Int = 3): List<UserIngredient> {
            return ingredients.filter {
                val daysUntil = it.getDaysUntilExpiry()
                daysUntil in 0..days.toLong()
            }
        }

        /**
         * 보관 위치별 재료 그룹화
         * @param ingredients 재료 목록
         * @return 보관 위치별로 그룹화된 재료 맵
         */
        fun groupByLocation(ingredients: List<UserIngredient>): Map<String, List<UserIngredient>> {
            return ingredients.groupBy { it.storageLocation }
        }

        /**
         * 동기화가 필요한 재료들 필터링
         * @param ingredients 재료 목록
         * @return 동기화가 필요한 재료 목록
         */
        fun getPendingSync(ingredients: List<UserIngredient>): List<UserIngredient> {
            return ingredients.filter { it.syncStatus == SyncStatus.PENDING_SYNC }
        }
    }
}

/**
 * 유통기한 상태 열거형
 */
enum class ExpiryStatus(val displayName: String, val color: String) {
    EXPIRED("유통기한 지남", "#FF4444"),
    EXPIRES_TODAY("오늘 만료", "#FF8800"),
    EXPIRES_SOON("곧 만료", "#FFAA00"),
    EXPIRES_THIS_WEEK("이번 주 만료", "#88AA00"),
    FRESH("신선함", "#44AA44")
}

/**
 * 동기화 상태 열거형
 */
enum class SyncStatus(val displayName: String) {
    LOCAL_ONLY("로컬만"),
    PENDING_SYNC("동기화 대기"),
    SYNCING("동기화 중"),
    SYNCED("동기화 완료"),
    SYNC_ERROR("동기화 오류")
}

/**
 * 재료 매칭 결과
 * 사용자 냉장고 재료와 레시피 재료 요구사항의 매칭 결과
 */
data class IngredientMatch(
    val ingredient: UserIngredient,
    val recipeIngredient: RecipeIngredient,
    val isAvailable: Boolean,           // 재료 이름이 일치하는지
    val isSufficientQuantity: Boolean,  // 충분한 양이 있는지
    val shortfall: Double               // 부족한 양 (0이면 충분함)
) {
    /**
     * 매칭 상태 요약
     */
    fun getSummary(): String {
        return when {
            !isAvailable -> "❌ 재료 없음"
            !isSufficientQuantity -> "⚠️ 부족함 (${shortfall}${recipeIngredient.unit ?: ""} 모자람)"
            else -> "✅ 충분함"
        }
    }
}