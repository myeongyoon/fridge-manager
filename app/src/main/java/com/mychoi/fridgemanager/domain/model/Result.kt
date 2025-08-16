package com.mychoi.fridgemanager.domain.model

/**
 * 성공/실패 결과를 나타내는 sealed class
 * Kotlin의 기본 Result와 달리 에러 타입을 명시적으로 지정 가능
 */
sealed class Result<out T, out E> {
    /**
     * 성공 결과
     */
    data class Success<out T>(val value: T) : Result<T, Nothing>()

    /**
     * 실패 결과
     */
    data class Error<out E>(val error: E) : Result<Nothing, E>()

    /**
     * 성공 여부 확인
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * 실패 여부 확인
     */
    val isError: Boolean
        get() = this is Error

    /**
     * 성공 값 가져오기 (실패 시 null)
     */
    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Error -> null
    }

    /**
     * 에러 가져오기 (성공 시 null)
     */
    fun errorOrNull(): E? = when (this) {
        is Success -> null
        is Error -> error
    }

    /**
     * 성공 시 값 변환
     */
    inline fun <R> map(transform: (T) -> R): Result<R, E> = when (this) {
        is Success -> Success(transform(value))
        is Error -> Error(error)
    }

    /**
     * 실패 시 에러 변환
     */
    inline fun <F> mapError(transform: (E) -> F): Result<T, F> = when (this) {
        is Success -> Success(value)
        is Error -> Error(transform(error))
    }

    /**
     * 성공 시 블록 실행
     */
    inline fun onSuccess(block: (T) -> Unit): Result<T, E> {
        if (this is Success) block(value)
        return this
    }

    /**
     * 실패 시 블록 실행
     */
    inline fun onError(block: (E) -> Unit): Result<T, E> {
        if (this is Error) block(error)
        return this
    }
}

/**
 * 성공 결과 생성 헬퍼 함수
 */
fun <T> success(value: T): Result<T, Nothing> = Result.Success(value)

/**
 * 실패 결과 생성 헬퍼 함수
 */
fun <E> error(error: E): Result<Nothing, E> = Result.Error(error)