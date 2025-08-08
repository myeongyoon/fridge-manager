package com.mychoi.fridgemanager.domain.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * 사용자 냉장고 재료 도메인 모델
 * user_ingredients 테이블을 기반으로 한 실제 냉장고 재료 정보
 *
 * 실제 DB 스키마:
 * - id: UUID PRIMARY KEY
 * - user_id: UUID REFERENCES user_profiles(id)
 * - ingredient_name: TEXT NOT NULL (ingredients_master의 name과 매칭)
 * - amount: TEXT ("3개", "500g")
 * - unit: TEXT ("개", "g", "ml")
 * - expiry_date: DATE (유통기한)
 * - purchase_date: DATE DEFAULT CURRENT_DATE (구매일)
 * - storage_location: TEXT DEFAULT '냉장실' ("냉장실", "냉동실", "실온")
 * - memo: TEXT (사용자 메모)
 * - created_at, updated_at: TIMESTAMP
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
    val createdAt: String = getCurrentISODate(), // DB의 created_at
    val updatedAt: String = getCurrentISODate(), // DB의 updated_at
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY
) {

    /**
     * 유통기한까지 남은 일수 계산
     * @return 남은 일수 (음수면 유통기한 지남)
     */
    fun getDaysUntilExpiry(): Long {
        return try {
            val expiry = LocalDate.parse(expiryDate)
            val today = LocalDate.now()
            ChronoUnit.DAYS.between(today, expiry)
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
     * 표시용 수량 문자열 생성 (이미 amount가 텍스트이므로 그대로 반환)
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
        val isNameMatch = ingredientName.equals(recipeIngredient.name, ignoreCase = true)

        if (!isNameMatch) {
            return IngredientMatch(
                ingredient = this,
                recipeIngredient = recipeIngredient,
                isAvailable = false,
                isSufficientQuantity = false,
                shortfall = 0.0
            )
        }

        // 단위 변환 (간단한 버전, 실제로는 더 복잡한 변환 로직 필요)
        val convertedQuantity = convertUnit(quantity, unit, recipeIngredient.unit)
        val required = parseQuantity(recipeIngredient.amount)

        val isSufficient = convertedQuantity >= required
        val shortfall = if (isSufficient) 0.0 else required - convertedQuantity

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
         * 현재 ISO 8601 날짜 문자열 반환
         */
        private fun getCurrentISODate(): String {
            return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
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
         * 레시피 재료의 수량 문자열 파싱
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
            !isSufficientQuantity -> "⚠️ 부족함 (${shortfall}${recipeIngredient.unit} 모자람)"
            else -> "✅ 충분함"
        }
    }
}