package com.mychoi.fridgemanager.domain.repository

import com.mychoi.fridgemanager.domain.model.*
import com.mychoi.fridgemanager.domain.error.IngredientError
import kotlinx.coroutines.flow.Flow

/**
 * 재료 마스터 데이터 관리를 위한 Repository 인터페이스
 * ingredients_master 테이블을 기반으로 한 표준 재료 정보 처리
 *
 * 주요 기능:
 * - 재료 검색 및 자동완성
 * - 대체 재료 제안
 * - 재료 카테고리 관리
 * - 재료 정보 동기화
 */
interface IngredientRepository {

    // ========================================
    // 재료 검색 및 조회
    // ========================================

    /**
     * 재료명으로 검색 (부분 일치 지원)
     * 자동완성과 사용자 입력 검증에 활용
     *
     * @param query 검색어 (최소 1자 이상)
     * @param limit 검색 결과 제한 (기본 20개)
     * @return 매칭 점수 순으로 정렬된 재료 목록
     */
    suspend fun searchIngredients(
        query: String,
        limit: Int = 20
    ): Result<List<Ingredient>, IngredientError>

    /**
     * 재료명으로 정확한 재료 정보 조회
     * 사용자가 입력한 재료의 표준 정보 확인용
     *
     * @param name 정확한 재료명
     * @return 재료 정보 또는 null (찾지 못한 경우)
     */
    suspend fun getIngredientByName(name: String): Result<Ingredient?, IngredientError>

    /**
     * ID로 특정 재료 조회
     *
     * @param ingredientId 재료 고유 ID
     * @return 재료 정보 또는 null
     */
    suspend fun getIngredientById(ingredientId: String): Result<Ingredient?, IngredientError>

    /**
     * 모든 재료 목록 조회 (실시간 업데이트)
     * 관리자 화면이나 전체 재료 목록이 필요한 경우
     *
     * @return 재료 목록 Flow (카테고리별 정렬)
     */
    fun getAllIngredients(): Flow<List<Ingredient>>

    /**
     * 재료 자동완성 제안
     * 사용자 입력 시 실시간 제안 기능
     *
     * @param query 입력 중인 텍스트
     * @param maxSuggestions 최대 제안 개수 (기본 5개)
     * @return 제안 재료명 목록 (빈도순 정렬)
     */
    suspend fun getIngredientSuggestions(
        query: String,
        maxSuggestions: Int = 5
    ): Result<List<String>, IngredientError>

    // ========================================
    // 카테고리 및 분류 관리
    // ========================================

    /**
     * 카테고리별 재료 조회
     * 재료 추가 시 카테고리별 탐색 지원
     *
     * @param category 카테고리명 ("채소", "육류", "유제품" 등)
     * @return 해당 카테고리의 재료 목록 Flow
     */
    fun getIngredientsByCategory(category: String): Flow<List<Ingredient>>

    /**
     * 모든 카테고리 목록 조회
     * 카테고리 필터 UI 구성용
     *
     * @return 카테고리 목록 (재료 개수 포함)
     */
    suspend fun getAllCategories(): Result<List<IngredientCategoryInfo>, IngredientError>

    /**
     * 서브카테고리별 재료 조회
     * 세부 분류로 재료 탐색 (예: 잎채소, 뿌리채소)
     *
     * @param category 메인 카테고리
     * @param subcategory 서브카테고리 (nullable)
     * @return 해당 서브카테고리의 재료 목록
     */
    suspend fun getIngredientsBySubcategory(
        category: String,
        subcategory: String?
    ): Result<List<Ingredient>, IngredientError>

    /**
     * 자주 사용되는 재료 목록 조회
     * 재료 추가 시 빠른 선택을 위한 인기 재료
     *
     * @param limit 조회할 개수 (기본 30개)
     * @return 사용 빈도순 재료 목록
     */
    suspend fun getPopularIngredients(limit: Int = 30): Result<List<Ingredient>, IngredientError>

    // ========================================
    // 대체 재료 제안 시스템
    // ========================================

    /**
     * 특정 재료의 대체 재료 제안
     * 재료가 없을 때 비슷한 재료 추천
     *
     * @param ingredientName 원본 재료명
     * @param maxAlternatives 최대 대체재료 개수 (기본 5개)
     * @return 대체 가능한 재료 목록 (유사도순)
     */
    suspend fun getAlternativeIngredients(
        ingredientName: String,
        maxAlternatives: Int = 5
    ): Result<List<Ingredient>, IngredientError>

    /**
     * 레시피에 필요한 재료들의 대체재료 제안
     * 레시피 실행 전 부족한 재료의 대체재료 일괄 제안
     *
     * @param recipe 대상 레시피
     * @param userIngredients 사용자 보유 재료 (매칭 제외용)
     * @return 재료별 대체재료 맵
     */
    suspend fun suggestAlternativesForRecipe(
        recipe: Recipe,
        userIngredients: List<UserIngredient>
    ): Result<Map<String, List<Ingredient>>, IngredientError>

    /**
     * 지능형 재료 제안 (AI 기반 - 추후 구현)
     * 사용자 패턴과 레시피 분석을 통한 재료 제안
     *
     * @param context 제안 컨텍스트 (레시피, 선호도 등)
     * @return 추천 재료 목록
     */
    suspend fun getSmartIngredientSuggestions(
        context: IngredientSuggestionContext
    ): Result<List<Ingredient>, IngredientError>

    // ========================================
    // 재료 정보 업데이트 및 동기화
    // ========================================

    /**
     * 로컬 재료 마스터 데이터를 서버와 동기화
     * 새로운 재료나 정보 업데이트 반영
     *
     * @param forceRefresh 강제 새로고침 여부 (기본 false)
     * @return 동기화 결과 정보
     */
    suspend fun syncIngredientMasterData(
        forceRefresh: Boolean = false
    ): Result<IngredientSyncResult, IngredientError>

    /**
     * 사용자 제안 재료 추가 요청
     * 마스터 데이터에 없는 재료를 사용자가 제안
     *
     * @param ingredientRequest 새 재료 제안 정보
     * @return 제안 접수 결과
     */
    suspend fun submitIngredientSuggestion(
        ingredientRequest: NewIngredientRequest
    ): Result<Unit, IngredientError>

    /**
     * 재료 사용 통계 업데이트
     * 사용자가 사용한 재료의 인기도 반영
     *
     * @param ingredientName 사용된 재료명
     * @param usageType 사용 유형 (검색, 추가, 레시피 사용 등)
     * @return 업데이트 성공 여부
     */
    suspend fun updateIngredientUsageStats(
        ingredientName: String,
        usageType: IngredientUsageType
    ): Result<Unit, IngredientError>

    // ========================================
    // 재료 정보 분석 및 통계
    // ========================================

    /**
     * 재료 검색 통계 조회
     * 인기 검색어, 검색 패턴 분석
     *
     * @param period 분석 기간 (일 단위, 기본 30일)
     * @return 검색 통계 정보
     */
    suspend fun getIngredientSearchStats(
        period: Int = 30
    ): Result<IngredientSearchStats, IngredientError>

    /**
     * 사용자 재료 선호도 분석
     * 자주 사용하는 재료, 카테고리 패턴 분석
     *
     * @param userId 분석 대상 사용자 ID
     * @return 재료 선호도 분석 결과
     */
    suspend fun analyzeUserIngredientPreferences(
        userId: String
    ): Result<IngredientPreferenceAnalysis, IngredientError>

    /**
     * 계절별 재료 추천
     * 계절에 맞는 제철 재료 정보 제공
     *
     * @param season 계절 정보 (SPRING, SUMMER, AUTUMN, WINTER)
     * @param region 지역 정보 (한국 기준, 추후 확장)
     * @return 제철 재료 목록
     */
    suspend fun getSeasonalIngredients(
        season: Season,
        region: String = "KR"
    ): Result<List<Ingredient>, IngredientError>
}