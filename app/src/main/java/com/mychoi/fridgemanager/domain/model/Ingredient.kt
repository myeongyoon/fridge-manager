package com.mychoi.fridgemanager.domain.model

import kotlinx.serialization.Serializable

/**
 * 재료 도메인 모델
 * ingredients_master 테이블을 기반으로 한 표준 재료 정보
 *
 * 실제 DB 스키마:
 * - id: UUID PRIMARY KEY
 * - name: TEXT UNIQUE NOT NULL (표준 재료명)
 * - category: TEXT NOT NULL ("채소", "육류", "유제품", "곡류", "조미료", "해산물")
 * - subcategory: TEXT (세부 분류, 예: "잎채소", "뿌리채소")
 * - storage_days: INTEGER DEFAULT 7 (평균 보관 기간)
 * - storage_method: TEXT DEFAULT '냉장' ("상온", "냉장", "냉동")
 * - alternatives: TEXT[] DEFAULT '{}' (대체 가능한 재료들)
 * - common_unit: TEXT DEFAULT '개' (기본 단위)
 * - created_at, updated_at: TIMESTAMP
 */

@Serializable
data class Ingredient(
    val id: String,                              // UUID를 String으로 처리
    val name: String,
    val category: String,                        // DB의 category (채소, 육류 등)
    val subcategory: String? = null,             // DB의 subcategory (잎채소, 뿌리채소 등)
    val storageDays: Int,                        // DB의 storage_days
    val storageMethod: String,                   // DB의 storage_method (상온, 냉장, 냉동)
    val alternatives: List<String> = emptyList(), // DB의 alternatives 배열
    val commonUnit: String,                      // DB의 common_unit
    val createdAt: String,                       // DB의 created_at
    val updatedAt: String,                       // DB의 updated_at

    // 추가 확장 필드들 (앱에서만 사용)
    val keywords: List<String> = emptyList(),    // 검색용 키워드
    val nutritionPer100g: NutritionInfo? = null, // 영양정보 (추후 확장)
    val isCommon: Boolean = false,               // 자주 사용되는 재료 여부
    val averagePrice: Int? = null                // 평균 가격 (추후 확장)
) {

    /**
     * 재료명으로 검색 시 매칭 점수 계산
     * @param searchQuery 검색어
     * @return 매칭 점수 (0.0~1.0, 1.0이 완벽 일치)
     */
    fun calculateSearchScore(searchQuery: String): Double {
        val query = searchQuery.trim().lowercase()
        if (query.isEmpty()) return 0.0

        // 완전 일치
        if (name.lowercase() == query) return 1.0

        // 포함 검사
        val nameContains = name.lowercase().contains(query)
        val keywordsMatch = keywords.any { it.lowercase().contains(query) }
        val alternativesMatch = alternatives.any { it.lowercase().contains(query) }

        return when {
            nameContains && name.lowercase().startsWith(query) -> 0.9 // 시작 일치
            nameContains -> 0.7 // 포함
            keywordsMatch -> 0.6 // 키워드 매칭
            alternativesMatch -> 0.5 // 대체재료 매칭
            else -> 0.0 // 매칭 없음
        }
    }

    /**
     * 다른 재료와 대체 가능한지 확인
     * @param other 비교할 재료
     * @return 대체 가능 여부
     */
    fun canSubstitute(other: Ingredient): Boolean {
        return alternatives.contains(other.name) ||
                other.alternatives.contains(this.name) ||
                (category == other.category && storageMethod == other.storageMethod)
    }

    /**
     * 재료 유통기한 계산
     * @param purchaseDate 구매일 (ISO 8601 형식)
     * @return 유통기한 (ISO 8601 형식)
     */
    fun calculateExpiryDate(purchaseDate: String): String {
        // 간단한 날짜 계산 (실제로는 더 정확한 날짜 라이브러리 사용 권장)
        val days = storageDays
        return "${purchaseDate}+${days}days" // 임시 형식
    }

    /**
     * 표시용 재료 정보 생성
     * @return 사용자에게 표시할 재료 정보
     */
    fun getDisplayInfo(): String {
        val commonMark = if (isCommon) "⭐" else ""
        val priceMark = averagePrice?.let { " (₩${it})" } ?: ""
        val subcatText = subcategory?.let { " - $it" } ?: ""
        return "$commonMark$name ($category$subcatText)$priceMark"
    }

    /**
     * 재료 보관 가이드 제공
     * @return 보관 방법 가이드 텍스트
     */
    fun getStorageGuide(): String {
        return when (storageMethod) {
            "냉장" -> "냉장 보관 (0-4°C), 유통기한: ${storageDays}일"
            "냉동" -> "냉동 보관 (-18°C 이하), 유통기한: ${storageDays}일"
            "상온" -> "실온 보관 (서늘하고 건조한 곳), 유통기한: ${storageDays}일"
            else -> "적절한 곳에 보관, 유통기한: ${storageDays}일"
        }
    }

    companion object {
        /**
         * 재료 목록에서 이름으로 검색
         * @param ingredients 재료 목록
         * @param query 검색어
         * @param limit 결과 제한 수
         * @return 매칭 점수 순으로 정렬된 재료 목록
         */
        fun searchIngredients(
            ingredients: List<Ingredient>,
            query: String,
            limit: Int = 10
        ): List<Pair<Ingredient, Double>> {
            return ingredients
                .map { it to it.calculateSearchScore(query) }
                .filter { it.second > 0.0 }
                .sortedByDescending { it.second }
                .take(limit)
        }

        /**
         * 카테고리별 재료 그룹화
         * @param ingredients 재료 목록
         * @return 카테고리별로 그룹화된 재료 맵
         */
        fun groupByCategory(ingredients: List<Ingredient>): Map<IngredientCategory, List<Ingredient>> {
            return ingredients.groupBy { it.category }
        }

        /**
         * 자주 사용하는 재료만 필터링
         * @param ingredients 재료 목록
         * @return 자주 사용하는 재료 목록
         */
        fun getCommonIngredients(ingredients: List<Ingredient>): List<Ingredient> {
            return ingredients.filter { it.isCommon }
        }
    }
}

/**
 * 영양 정보 (추후 확장용)
 * 현재는 기본 구조만 정의
 */
@Serializable
data class NutritionInfo(
    val calories: Double = 0.0,        // 칼로리 (kcal)
    val protein: Double = 0.0,         // 단백질 (g)
    val carbohydrates: Double = 0.0,   // 탄수화물 (g)
    val fat: Double = 0.0,             // 지방 (g)
    val fiber: Double = 0.0,           // 식이섬유 (g)
    val sodium: Double = 0.0           // 나트륨 (mg)
) {
    /**
     * 영양 정보 요약 텍스트
     */
    fun getSummary(): String {
        return "칼로리: ${calories}kcal, 단백질: ${protein}g, 탄수화물: ${carbohydrates}g, 지방: ${fat}g"
    }
}
