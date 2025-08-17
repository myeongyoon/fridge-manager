package com.mychoi.fridgemanager.domain.model

import kotlinx.serialization.Serializable

/**
 * 커스텀 레시피 관련 데이터 모델들
 * 사용자가 만든 레시피, 기존 레시피 수정 등을 위한 클래스들
 */

/**
 * 커스텀 레시피 생성 요청
 */
@Serializable
data class CustomRecipe(
    val name: String,
    val description: String? = null,
    val cookingTimeMinutes: Int? = null,
    val difficulty: Int? = null,
    val servings: Int = 1,
    val category: String? = null,
    val mealType: String? = null,
    val instructions: List<String> = emptyList(),
    val ingredients: List<RecipeIngredient> = emptyList(),
    val tags: List<String> = emptyList(),
    val notes: String? = null,
    val imageUrl: String? = null,
    val originalRecipeId: String? = null // 기존 레시피 기반인 경우
) {
    init {
        require(name.isNotBlank()) { "레시피 이름을 입력해주세요" }
        require(name.length <= 100) { "레시피 이름은 100자를 초과할 수 없습니다" }
        require(ingredients.isNotEmpty()) { "재료를 하나 이상 추가해주세요" }
        require(ingredients.size <= 50) { "재료는 최대 50개까지 추가할 수 있습니다" }
        require(instructions.isNotEmpty()) { "조리 과정을 하나 이상 추가해주세요" }
        require(instructions.size <= 20) { "조리 과정은 최대 20단계까지 가능합니다" }
        difficulty?.let { require(it in 1..5) { "난이도는 1-5 사이로 설정해주세요" } }
        require(servings > 0 && servings <= 20) { "인분수는 1-20 사이로 설정해주세요" }
        cookingTimeMinutes?.let { require(it > 0 && it <= 600) { "조리시간은 1-600분 사이로 설정해주세요" } }
    }

    /**
     * 레시피 복잡도 계산
     * 재료 수, 조리 단계 수, 조리 시간을 종합하여 계산
     */
    fun getComplexityScore(): Double {
        val ingredientScore = ingredients.size * 0.3
        val instructionScore = instructions.size * 0.5
        val timeScore = (cookingTimeMinutes ?: 30) / 60.0 * 0.2

        return ingredientScore + instructionScore + timeScore
    }

    /**
     * 레시피 정보 요약
     */
    fun getSummary(): String {
        val difficultyText = difficulty?.let { "난이도 $it" } ?: "난이도 미설정"
        val timeText = cookingTimeMinutes?.let { "${it}분" } ?: "시간 미설정"
        val categoryText = category ?: "카테고리 미설정"

        return "$name | $categoryText | $difficultyText | $timeText | ${ingredients.size}개 재료"
    }

    /**
     * 기존 레시피 기반인지 확인
     */
    fun isBasedOnExisting(): Boolean {
        return originalRecipeId != null
    }

    /**
     * 레시피 완성도 검증
     */
    fun getCompleteness(): RecipeCompleteness {
        var score = 0
        var total = 0

        // 필수 항목 (70점)
        total += 70
        score += 20 // 이름 (필수)
        score += 25 // 재료 (필수)
        score += 25 // 조리과정 (필수)

        // 선택 항목 (30점)
        total += 30
        if (description?.isNotBlank() == true) score += 5
        if (cookingTimeMinutes != null) score += 5
        if (difficulty != null) score += 5
        if (category?.isNotBlank() == true) score += 5
        if (tags.isNotEmpty()) score += 5
        if (imageUrl?.isNotBlank() == true) score += 5

        val percentage = (score.toDouble() / total * 100).toInt()

        return when {
            percentage >= 90 -> RecipeCompleteness.EXCELLENT
            percentage >= 80 -> RecipeCompleteness.GOOD
            percentage >= 70 -> RecipeCompleteness.BASIC
            else -> RecipeCompleteness.INCOMPLETE
        }
    }
}

/**
 * 레시피 완성도 레벨
 */
enum class RecipeCompleteness(val displayName: String, val description: String) {
    EXCELLENT("훌륭함", "모든 정보가 완벽하게 입력됨"),
    GOOD("좋음", "대부분의 정보가 입력됨"),
    BASIC("기본", "필수 정보만 입력됨"),
    INCOMPLETE("불완전", "일부 필수 정보가 누락됨")
}

/**
 * 레시피 수정사항
 * 기존 레시피의 일부 필드만 수정할 때 사용
 */
@Serializable
data class RecipeModifications(
    val name: String? = null,
    val description: String? = null,
    val cookingTimeMinutes: Int? = null,
    val difficulty: Int? = null,
    val servings: Int? = null,
    val instructions: List<String>? = null,
    val ingredientModifications: List<IngredientModification>? = null,
    val addedTags: List<String>? = null,
    val removedTags: List<String>? = null,
    val notes: String? = null,
    val imageUrl: String? = null
) {
    init {
        // 수정값 검증
        name?.let {
            require(it.isNotBlank()) { "레시피 이름은 공백일 수 없습니다" }
            require(it.length <= 100) { "레시피 이름은 100자를 초과할 수 없습니다" }
        }
        difficulty?.let { require(it in 1..5) { "난이도는 1-5 사이여야 합니다" } }
        servings?.let { require(it > 0 && it <= 20) { "인분수는 1-20 사이여야 합니다" } }
        cookingTimeMinutes?.let { require(it > 0 && it <= 600) { "조리시간은 1-600분 사이여야 합니다" } }
        instructions?.let {
            require(it.isNotEmpty()) { "조리 과정은 하나 이상이어야 합니다" }
            require(it.size <= 20) { "조리 과정은 최대 20단계까지 가능합니다" }
        }
    }

    /**
     * 수정사항이 있는지 확인
     */
    fun hasModifications(): Boolean {
        return name != null || description != null || cookingTimeMinutes != null ||
                difficulty != null || servings != null || instructions != null ||
                ingredientModifications != null || addedTags != null ||
                removedTags != null || notes != null || imageUrl != null
    }

    /**
     * 수정 필드 목록 반환 (로깅용)
     */
    fun getModifiedFields(): List<String> {
        val fields = mutableListOf<String>()
        if (name != null) fields.add("name")
        if (description != null) fields.add("description")
        if (cookingTimeMinutes != null) fields.add("cookingTime")
        if (difficulty != null) fields.add("difficulty")
        if (servings != null) fields.add("servings")
        if (instructions != null) fields.add("instructions")
        if (ingredientModifications != null) fields.add("ingredients")
        if (addedTags != null || removedTags != null) fields.add("tags")
        if (notes != null) fields.add("notes")
        if (imageUrl != null) fields.add("image")
        return fields
    }

    /**
     * 수정사항 요약
     */
    fun getSummary(): String {
        val fields = getModifiedFields()
        return if (fields.isNotEmpty()) {
            "수정: ${fields.joinToString(", ")}"
        } else {
            "수정사항 없음"
        }
    }
}

/**
 * 재료 수정사항
 */
@Serializable
data class IngredientModification(
    val action: IngredientAction,
    val ingredientName: String,
    val amount: String? = null,
    val unit: String? = null,
    val isEssential: Boolean? = null,
    val preparationNote: String? = null
) {
    init {
        require(ingredientName.isNotBlank()) { "재료명은 공백일 수 없습니다" }

        // 액션별 필수 필드 검증
        when (action) {
            IngredientAction.ADD -> {
                require(!amount.isNullOrBlank()) { "재료 추가 시 분량을 입력해주세요" }
                require(!unit.isNullOrBlank()) { "재료 추가 시 단위를 입력해주세요" }
            }
            IngredientAction.MODIFY -> {
                require(amount != null || unit != null || isEssential != null || preparationNote != null) {
                    "재료 수정 시 수정할 항목을 하나 이상 지정해주세요"
                }
            }
            IngredientAction.REMOVE -> {
                // 제거 시에는 재료명만 필요
            }
        }
    }

    /**
     * 수정사항 요약
     */
    fun getSummary(): String {
        return when (action) {
            IngredientAction.ADD -> "$ingredientName ${amount}${unit} 추가"
            IngredientAction.REMOVE -> "$ingredientName 제거"
            IngredientAction.MODIFY -> {
                val changes = mutableListOf<String>()
                if (amount != null) changes.add("분량: $amount")
                if (unit != null) changes.add("단위: $unit")
                if (isEssential != null) changes.add(if (isEssential) "필수" else "선택")
                if (preparationNote != null) changes.add("준비: $preparationNote")
                "$ingredientName ${changes.joinToString(", ")} 수정"
            }
        }
    }
}

/**
 * 재료 수정 액션 타입
 */
enum class IngredientAction(val displayName: String) {
    ADD("추가"),
    REMOVE("제거"),
    MODIFY("수정")
}

/**
 * 레시피 템플릿
 * 자주 사용하는 레시피 구조를 템플릿으로 저장
 */
@Serializable
data class RecipeTemplate(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val basicIngredients: List<RecipeIngredient>, // 기본 재료들
    val instructionSteps: List<String>,          // 기본 조리 과정
    val estimatedTime: Int,                      // 예상 조리 시간
    val difficulty: Int,                         // 기본 난이도
    val tags: List<String> = emptyList(),
    val useCount: Int = 0                        // 사용 횟수
) {
    /**
     * 템플릿을 CustomRecipe로 변환
     */
    fun toCustomRecipe(recipeName: String): CustomRecipe {
        return CustomRecipe(
            name = recipeName,
            description = description,
            cookingTimeMinutes = estimatedTime,
            difficulty = difficulty,
            category = category,
            ingredients = basicIngredients,
            instructions = instructionSteps,
            tags = tags
        )
    }

    /**
     * 템플릿 요약
     */
    fun getSummary(): String {
        return "$name | $category | 난이도 $difficulty | ${estimatedTime}분 | ${basicIngredients.size}개 재료 | 사용: ${useCount}회"
    }

    companion object {
        /**
         * 기본 템플릿들
         */
        fun getDefaultTemplates(): List<RecipeTemplate> {
            return listOf(
                RecipeTemplate(
                    id = "template_stir_fry",
                    name = "볶음 요리",
                    description = "기본적인 볶음 요리 구조",
                    category = "한식",
                    basicIngredients = listOf(
                        RecipeIngredient("", "주재료", "적당량", "개", true.toString()),
                        RecipeIngredient("", "양파", "1/2", "개", false.toString()),
                        RecipeIngredient("", "마늘", "2", "쪽", false.toString()),
                        RecipeIngredient("", "식용유", "1", "큰술", true.toString()),
                        RecipeIngredient("", "소금", "약간", "", true.toString()),
                        RecipeIngredient("", "후추", "약간", "", false.toString())
                    ),
                    instructionSteps = listOf(
                        "주재료를 적당한 크기로 자른다",
                        "양파와 마늘을 썬다",
                        "팬에 식용유를 두르고 가열한다",
                        "마늘을 먼저 볶아 향을 낸다",
                        "주재료를 넣고 볶는다",
                        "양파를 넣고 함께 볶는다",
                        "소금, 후추로 간을 맞춘다"
                    ),
                    estimatedTime = 20,
                    difficulty = 2,
                    tags = listOf("볶음", "간단", "기본")
                ),
                RecipeTemplate(
                    id = "template_soup",
                    name = "국물 요리",
                    description = "기본적인 국물 요리 구조",
                    category = "한식",
                    basicIngredients = listOf(
                        RecipeIngredient("", "주재료", "적당량", "개", true.toString()),
                        RecipeIngredient("", "물", "3", "컵", true.toString()),
                        RecipeIngredient("", "다시마", "1", "장", false.toString()),
                        RecipeIngredient("", "소금", "적당량", "", true.toString()),
                        RecipeIngredient("", "대파", "1", "대", false.toString())
                    ),
                    instructionSteps = listOf(
                        "물에 다시마를 넣고 끓인다",
                        "다시마를 건져낸다",
                        "주재료를 넣고 끓인다",
                        "재료가 익으면 소금으로 간을 맞춘다",
                        "대파를 넣고 마무리한다"
                    ),
                    estimatedTime = 30,
                    difficulty = 2,
                    tags = listOf("국물", "기본", "따뜻함")
                )
            )
        }
    }
}

/**
 * 레시피 버전 관리
 * 사용자가 레시피를 수정할 때마다 버전을 관리
 */
data class RecipeVersion(
    val versionNumber: Int,
    val recipe: Recipe,
    val modifications: RecipeModifications,
    val modifiedAt: Long,
    val changeDescription: String? = null
) {
    /**
     * 버전 정보 요약
     */
    fun getSummary(): String {
        val date = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(modifiedAt))
        val desc = changeDescription ?: modifications.getSummary()
        return "v$versionNumber | $date | $desc"
    }

    /**
     * 이전 버전인지 확인
     */
    fun isOlderThan(other: RecipeVersion): Boolean {
        return versionNumber < other.versionNumber
    }
}

/**
 * 레시피 변경 히스토리 관리
 */
data class RecipeChangeHistory(
    val recipeId: String,
    val versions: List<RecipeVersion>
) {
    /**
     * 최신 버전 가져오기
     */
    fun getLatestVersion(): RecipeVersion? {
        return versions.maxByOrNull { it.versionNumber }
    }

    /**
     * 특정 버전 가져오기
     */
    fun getVersion(versionNumber: Int): RecipeVersion? {
        return versions.find { it.versionNumber == versionNumber }
    }

    /**
     * 버전 개수
     */
    fun getVersionCount(): Int = versions.size

    /**
     * 변경 히스토리가 있는지 확인
     */
    fun hasHistory(): Boolean = versions.size > 1
}