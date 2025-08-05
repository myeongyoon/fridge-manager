package com.mychoi.fridgemanager

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 냉장고 매니저 Application 클래스
 * Hilt 의존성 주입을 위한 진입점
 */
@HiltAndroidApp
class FridgeManagerApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // TODO: 추후 필요한 초기화 작업 추가
        // - DataStore 초기화
        // - 로깅 설정
        // - 크래시 리포팅 설정
    }
}