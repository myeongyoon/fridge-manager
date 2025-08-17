package com.mychoi.fridgemanager.domain.repository

import com.mychoi.fridgemanager.domain.model.*
import com.mychoi.fridgemanager.domain.error.RecipeError
import kotlinx.coroutines.flow.Flow

/**
 * 레시피 관련 데이터 처리를 위한 Repository 인터페이스
 * Clean Architecture의 Domain Layer에서 정의하고
 * Data Layer에서 구현하는 패턴을 따름
 *
 * 레시피 검색, 추천, 저장, 커스텀 레시피 관리 등의 기능 제공
 * 로컬 캐싱과 원격 데이터 동기화를 통한 오프라인 우선 전략 적용
 */
interface RecipeRepository {

    // ========================================
    // 레시피 검색 및 조회
    // ========================================

    /**
     * 모든 레시피 조회 (실시간 업데이트)
     * 로컬 캐시된 300개 레시피 데이터 제공
     *
     * @return 레시피 목록 Flow
     */
    fun getAllRecipes(): Flow<List<Recipe>>

    /**
     * 특정 레시피 조회 (ID 기반)
     *
     * @param recipeId 레시피 고유 ID
     * @return 레시피 정보 또는 null (찾지 못한 경우)
     */
    suspend fun getRecipeById(recipeId: String): Recipe?

    /**
     * 레시피명으로 검색 (부분 일치 지원)
     * 재료명, 태그, 카테고리도 함께 검색
     *
     * @param query 검색어
     * @param limit 결과 제한 수 (기본 20개)
     * @return 검색 결과 레시피 목록
     */
    suspend fun searchRecipes(query: String, limit: Int = 20): List<Recipe>

    /**
     * 고급 검색 (필터 조건 지원)
     * 카테고리, 난이도, 조리시간, 재료 포함/제외 등
     *
     * @param searchRequest 검색 조건
     * @return 필터링된 레시피 목록
     */
    suspend fun searchRecipesAdvanced(searchRequest: RecipeSearchRequest): List<Recipe>

    // ========================================
    // 레시피 추천 시스템
    // ========================================

    /**
     * 사용자 냉장고 재료 기반 레시피 추천
     * 백엔드 추천 알고리즘과 동일한 로직 적용
     *
     * @param userIngredients 사용자 냉장고 재료 목록
     * @param userPreference 사용자 선호도 (선택사항)
     * @param limit 추천 결과 수 (기본 10개)
     * @return 추천 결과 목록 (점수순 정렬)
     */
    suspend fun getRecommendedRecipes(
        userIngredients: List<UserIngredient>,
        userPreference: UserPreference? = null,
        limit: Int = 10
    ): List<RecommendationResult>

    /**
     * 특정 재료 기반 레시피 추천
     * 특정 재료를 반드시 사용하는 레시피만 추천
     *
     * @param targetIngredients 포함되어야 할 재료 목록
     * @param userIngredients 사용자 냉장고 재료 (선택사항)
     * @param limit 추천 결과 수
     * @return 추천 레시피 목록
     */
    suspend fun getRecipesByIngredients(
        targetIngredients: List<String>,
        userIngredients: List<UserIngredient> = emptyList(),
        limit: Int = 10
    ): List<Recipe>

    /**
     * 유통기한 임박 재료 활용 레시피 추천
     * 유통기한이 임박한 재료를 우선 사용하는 레시피 추천
     *
     * @param expiringSoonIngredients 유통기한 임박 재료 목록
     * @param allUserIngredients 전체 사용자 재료
     * @param limit 추천 결과 수
     * @return 우선순위 추천 레시피 목록
     */
    suspend fun getUrgentRecipeRecommendations(
        expiringSoonIngredients: List<UserIngredient>,
        allUserIngredients: List<UserIngredient>,
        limit: Int = 5
    ): List<RecommendationResult>

    // ========================================
    // 레시피 카테고리 및 필터링
    // ========================================

    /**
     * 카테고리별 레시피 조회
     *
     * @param category 카테고리명 ("한식", "양식", "중식", "일식", "간식", "음료")
     * @return 해당 카테고리 레시피 목록 Flow
     */
    fun getRecipesByCategory(category: String): Flow<List<Recipe>>

    /**
     * 난이도별 레시피 조회
     *
     * @param difficulty 난이도 (1-5)
     * @return 해당 난이도 레시피 목록 Flow
     */
    fun getRecipesByDifficulty(difficulty: Int): Flow<List<Recipe>>

    /**
     * 조리시간별 레시피 조회
     *
     * @param maxCookingTime 최대 조리시간 (분)
     * @return 조리시간 이내 레시피 목록 Flow
     */
    fun getRecipesByTime(maxCookingTime: Int): Flow<List<Recipe>>

    /**
     * 인기 레시피 조회 (조회수 기준)
     *
     * @param limit 결과 제한 수 (기본 10개)
     * @return 인기 레시피 목록
     */
    suspend fun getPopularRecipes(limit: Int = 10): List<Recipe>

    /**
     * 최근 추가된 레시피 조회
     *
     * @param limit 결과 제한 수 (기본 10개)
     * @return 최신 레시피 목록
     */
    suspend fun getRecentRecipes(limit: Int = 10): List<Recipe>

    // ========================================
    // 사용자 저장 레시피 관리
    // ========================================

    /**
     * 레시피 즐겨찾기 추가/제거
     *
     * @param recipeId 레시피 ID
     * @param isFavorite 즐겨찾기 여부
     * @return 성공 여부
     */
    suspend fun updateRecipeFavorite(
        recipeId: String,
        isFavorite: Boolean
    ): Result<Unit, RecipeError>

    /**
     * 사용자 즐겨찾기 레시피 조회
     *
     * @return 즐겨찾기 레시피 목록 Flow
     */
    fun getFavoriteRecipes(): Flow<List<Recipe>>

    /**
     * 레시피 조회수 증가
     * 사용자가 레시피 상세화면을 볼 때 호출
     *
     * @param recipeId 레시피 ID
     * @return 성공 여부
     */
    suspend fun incrementRecipeViewCount(recipeId: String): Result<Unit, RecipeError>

    // ========================================
    // 커스텀 레시피 관리
    // ========================================

    /**
     * 커스텀 레시피 생성
     * 기존 레시피를 수정하거나 새로 만든 레시피 저장
     *
     * @param customRecipe 커스텀 레시피 정보
     * @return 성공 시 저장된 레시피, 실패 시 에러
     */
    suspend fun createCustomRecipe(customRecipe: CustomRecipe): Result<Recipe, RecipeError>

    /**
     * 기존 레시피 기반 커스텀 레시피 생성
     * 원본 레시피를 베이스로 사용자가 수정한 버전 생성
     *
     * @param originalRecipeId 원본 레시피 ID
     * @param modifications 수정사항
     * @return 성공 시 커스텀 레시피, 실패 시 에러
     */
    suspend fun createRecipeVariation(
        originalRecipeId: String,
        modifications: RecipeModifications
    ): Result<Recipe, RecipeError>

    /**
     * 사용자 커스텀 레시피 조회
     *
     * @return 사용자가 만든 커스텀 레시피 목록 Flow
     */
    fun getUserCustomRecipes(): Flow<List<Recipe>>

    /**
     * 커스텀 레시피 수정
     *
     * @param recipeId 수정할 레시피 ID
     * @param updates 수정사항
     * @return 성공 시 수정된 레시피, 실패 시 에러
     */
    suspend fun updateCustomRecipe(
        recipeId: String,
        updates: RecipeModifications
    ): Result<Recipe, RecipeError>

    /**
     * 커스텀 레시피 삭제
     *
     * @param recipeId 삭제할 레시피 ID
     * @return 성공 여부
     */
    suspend fun deleteCustomRecipe(recipeId: String): Result<Unit, RecipeError>

    // ========================================
    // 레시피 실행 및 피드백
    // ========================================

    /**
     * 레시피 실행 기록
     * 사용자가 실제로 요리를 만들었을 때 호출
     *
     * @param recipeId 실행한 레시피 ID
     * @param feedback 요리 피드백 (선택사항)
     * @return 성공 여부
     */
    suspend fun recordRecipeExecution(
        recipeId: String,
        feedback: RecipeFeedback? = null
    ): Result<Unit, RecipeError>

    /**
     * 레시피 평점 및 리뷰 추가
     *
     * @param recipeId 레시피 ID
     * @param rating 평점 (1-5)
     * @param review 리뷰 내용 (선택사항)
     * @return 성공 여부
     */
    suspend fun addRecipeReview(
        recipeId: String,
        rating: Int,
        review: String? = null
    ): Result<Unit, RecipeError>

    /**
     * 사용자 요리 기록 조회
     * 통계 및 분석용
     *
     * @param periodDays 조회 기간 (일 단위, 기본 30일)
     * @return 요리 기록 목록
     */
    suspend fun getCookingHistory(periodDays: Int = 30): List<CookingRecord>

    // ========================================
    // 레시피 캐싱 및 동기화
    // ========================================

    /**
     * 레시피 데이터 동기화
     * 서버에서 최신 레시피 데이터를 받아와 로컬 캐시 업데이트
     *
     * @param forceSync 강제 동기화 여부 (기본 false)
     * @return 동기화 결과 정보
     */
    suspend fun syncRecipeData(forceSync: Boolean = false): RecipeSyncResult

    /**
     * 레시피 캐시 상태 조회
     *
     * @return 캐시 상태 정보 Flow
     */
    fun getRecipeCacheStatus(): Flow<RecipeCacheStatus>

    /**
     * 로컬 레시피 캐시 새로고침
     * 백그라운드에서 주기적으로 호출되어 캐시 갱신
     *
     * @return 새로고침 성공 여부
     */
    suspend fun refreshRecipeCache(): Result<Unit, RecipeError>

    /**
     * 특정 레시피 캐시 무효화
     * 레시피 정보가 업데이트되었을 때 캐시 제거
     *
     * @param recipeId 캐시를 무효화할 레시피 ID
     * @return 성공 여부
     */
    suspend fun invalidateRecipeCache(recipeId: String): Result<Unit, RecipeError>

    // ========================================
    // 레시피 분석 및 통계
    // ========================================

    /**
     * 레시피 통계 조회
     * 전체 레시피 수, 카테고리별 분포 등
     *
     * @return 레시피 통계 정보
     */
    suspend fun getRecipeStatistics(): RecipeStatistics

    /**
     * 사용자 레시피 선호도 분석
     * 자주 보는 카테고리, 난이도, 조리시간 등 분석
     *
     * @param periodDays 분석 기간 (일 단위, 기본 90일)
     * @return 선호도 분석 결과
     */
    suspend fun analyzeUserRecipePreferences(periodDays: Int = 90): UserRecipePreferenceAnalysis

    /**
     * 추천 정확도 분석
     * 추천한 레시피 중 실제로 실행된 비율 등
     *
     * @return 추천 정확도 통계
     */
    suspend fun getRecommendationAccuracy(): RecommendationAccuracyStats
}