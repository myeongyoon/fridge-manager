package com.mychoi.fridgemanager.domain.model

import kotlinx.serialization.Serializable

/**
 * 레시피 재료 데이터 모델
 * 레시피에 포함되는 각 재료의 정보를 담는 클래스
 * recipe_ingredients 테이블과 매핑됨
 */
@Serializable
data class RecipeIngredient(
    val id: String? = null,                     // 재료 고유 ID (DB 생성)
    val recipeId: String,                       // 소속 레시피 ID
    val ingredientName: String,                 // 재료명
    val amount: String? = null,                 // 분량 ("2개", "100g", "1/2컵")
    val unit: String? = null,                   // 단위 ("개", "g", "ml", "컵", "큰술")
    val isEssential: Boolean = true,            // 필수 재료 여부
    val preparationNote: String? = null,        // 준비 방법 ("다진 것", "슬라이스한 것")
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(ingredientName.isNotBlank()) { "재료명은 공백일 수 없습니다" }
        require(recipeId.isNotBlank()) { "레시피 ID는 공백일 수 없습니다" }
    }

    /**
     * 재료의 전체 표시명 (분량 + 재료명 + 준비방법)
     * UI에서 사용자에게 보여줄 때 사용
     */
    fun getDisplayName(): String {
        val parts = mutableListOf<String>()

        // 분량 정보 추가
        if (!amount.isNullOrBlank()) {
            val unitText = if (!unit.isNullOrBlank()) unit else ""
            parts.add("$amount$unitText")
        }

        // 재료명 추가
        parts.add(ingredientName)

        // 준비방법 추가
        if (!preparationNote.isNullOrBlank()) {
            parts.add("(${preparationNote})")
        }

        return parts.joinToString(" ")
    }

    /**
     * 간단한 표시명 (재료명 + 분량만)
     */
    fun getSimpleDisplayName(): String {
        return if (!amount.isNullOrBlank()) {
            val unitText = if (!unit.isNullOrBlank()) unit else ""
            "$ingredientName $amount$unitText"
        } else {
            ingredientName
        }
    }

    /**
     * 재료가 필수인지 선택인지 텍스트로 반환
     */
    fun getImportanceText(): String {
        return if (isEssential) "필수" else "선택"
    }

    /**
     * 분량이 명시되어 있는지 확인
     */
    fun hasQuantityInfo(): Boolean {
        return !amount.isNullOrBlank()
    }

    /**
     * 준비 방법이 명시되어 있는지 확인
     */
    fun hasPreparationNote(): Boolean {
        return !preparationNote.isNullOrBlank()
    }

    /**
     * 재료 정보의 완성도 계산 (0.0 ~ 1.0)
     */
    fun getCompletenessScore(): Double {
        var score = 0.4 // 기본 점수 (재료명)

        if (hasQuantityInfo()) score += 0.3
        if (!unit.isNullOrBlank()) score += 0.2
        if (hasPreparationNote()) score += 0.1

        return score
    }

    /**
     * 사용자 냉장고 재료와 매칭되는지 확인
     */
    fun matchesUserIngredient(userIngredient: UserIngredient): Boolean {
        return ingredientName.equals(userIngredient.ingredientName, ignoreCase = true)
    }

    /**
     * 분량을 숫자로 파싱 시도 (계산용)
     * 예: "2개" -> 2.0, "1/2컵" -> 0.5, "적당량" -> null
     */
    fun parseQuantity(): Double? {
        if (amount.isNullOrBlank()) return null

        return try {
            // 분수 처리
            if (amount.contains("/")) {
                val parts = amount.split("/")
                if (parts.size == 2) {
                    val numerator = parts[0].filter { it.isDigit() }.toDoubleOrNull()
                    val denominator = parts[1].filter { it.isDigit() }.toDoubleOrNull()
                    if (numerator != null && denominator != null && denominator != 0.0) {
                        return numerator / denominator
                    }
                }
            }

            // 일반 숫자 추출
            val numberString = amount.filter { it.isDigit() || it == '.' }
            numberString.toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 분량 단위 정규화 (계산이나 비교를 위해)
     */
    fun getNormalizedUnit(): String? {
        return unit?.let { rawUnit ->
            when (rawUnit.lowercase()) {
                "개", "마리", "장", "쪽" -> "개"
                "g", "그램" -> "g"
                "kg", "킬로그램" -> "kg"
                "ml", "밀리리터" -> "ml"
                "l", "리터" -> "l"
                "컵", "cup" -> "컵"
                "큰술", "숟가락", "tbsp" -> "큰술"
                "작은술", "tsp" -> "작은술"
                else -> rawUnit
            }
        }
    }

    /**
     * 레시피 재료를 UserIngredient 형태로 변환
     * (장보기 목록 생성 시 사용)
     */
    fun toShoppingItem(userId: String): UserIngredient? {
        return if (hasQuantityInfo()) {
            UserIngredient(
                userId = userId,
                ingredientName = ingredientName,
                amount = amount ?: "적당량",
                unit = unit ?: "",
                expiryDate = "", // 장보기 목록에서는 빈 값
                storageLocation = "미정", // 구매 후 결정
                memo = preparationNote ?: ""
            )
        } else {
            null // 분량 정보가 없으면 장보기 항목으로 변환 불가
        }
    }

    companion object {
        /**
         * 기본 재료 생성 (분량 정보 없이)
         */
        fun createBasic(recipeId: String, ingredientName: String): RecipeIngredient {
            return RecipeIngredient(
                recipeId = recipeId,
                ingredientName = ingredientName,
                isEssential = true
            )
        }

        /**
         * 완전한 재료 생성
         */
        fun createComplete(
            recipeId: String,
            ingredientName: String,
            amount: String,
            unit: String,
            isEssential: Boolean = true,
            preparationNote: String? = null
        ): RecipeIngredient {
            return RecipeIngredient(
                recipeId = recipeId,
                ingredientName = ingredientName,
                amount = amount,
                unit = unit,
                isEssential = isEssential,
                preparationNote = preparationNote
            )
        }

        /**
         * 재료 목록에서 필수 재료만 필터링
         */
        fun getEssentialIngredients(ingredients: List<RecipeIngredient>): List<RecipeIngredient> {
            return ingredients.filter { it.isEssential }
        }

        /**
         * 재료 목록에서 선택 재료만 필터링
         */
        fun getOptionalIngredients(ingredients: List<RecipeIngredient>): List<RecipeIngredient> {
            return ingredients.filter { !it.isEssential }
        }

        /**
         * 재료 목록을 카테고리별로 그룹화
         * (재료명 기반으로 간단한 카테고리 추정)
         */
        fun groupByEstimatedCategory(ingredients: List<RecipeIngredient>): Map<String, List<RecipeIngredient>> {
            return ingredients.groupBy { ingredient ->
                // 간단한 카테고리 추정 로직
                when {
                    ingredient.ingredientName.contains(Regex("고기|닭|돼지|소|양")) -> "육류"
                    ingredient.ingredientName.contains(Regex("생선|새우|조개|오징어")) -> "해산물"
                    ingredient.ingredientName.contains(Regex("양파|마늘|대파|당근|배추")) -> "채소"
                    ingredient.ingredientName.contains(Regex("소금|설탕|간장|된장|고추장")) -> "조미료"
                    ingredient.ingredientName.contains(Regex("우유|치즈|요거트|버터")) -> "유제품"
                    ingredient.ingredientName.contains(Regex("쌀|밀가루|면|떡")) -> "곡류"
                    else -> "기타"
                }
            }
        }

        /**
         * 재료 목록의 총 예상 비용 계산 (임시 구현)
         */
        fun calculateEstimatedCost(ingredients: List<RecipeIngredient>): Int {
            // 간단한 비용 추정 (실제로는 가격 DB 연동 필요)
            return ingredients.sumOf { ingredient ->
                val quantity = ingredient.parseQuantity() ?: 1.0
                val baseCost = when {
                    ingredient.ingredientName.contains(Regex("고기|생선")) -> 3000
                    ingredient.ingredientName.contains(Regex("채소|과일")) -> 1000
                    ingredient.ingredientName.contains(Regex("조미료|양념")) -> 500
                    else -> 1500
                }
                (baseCost * quantity).toInt()
            }
        }
    }
}