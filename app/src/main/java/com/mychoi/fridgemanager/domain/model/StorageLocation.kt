package com.mychoi.fridgemanager.domain.model

/**
 * 재료 보관 위치 열거형
 * UI에서 드롭다운으로 선택하고, 데이터베이스에는 displayName으로 저장
 */
enum class StorageLocation(val displayName: String) {
    ROOM_TEMPERATURE("상온"),
    REFRIGERATED("냉장"),
    FROZEN("냉동");

    companion object {
        /**
         * 데이터베이스에서 가져온 문자열을 enum으로 변환
         * @param displayName 데이터베이스에 저장된 한글 이름
         * @return 해당하는 StorageLocation, 없으면 REFRIGERATED(기본값)
         */
        fun fromDisplayName(displayName: String): StorageLocation {
            return entries.find { it.displayName == displayName } ?: REFRIGERATED
        }

        /**
         * UI 드롭다운에서 사용할 모든 옵션 목록
         * @return 사용자에게 보여줄 한글 이름들
         */
        fun getAllDisplayNames(): List<String> {
            return entries.map { it.displayName }
        }
    }
}