plugins {
    // 표준 구성: 루트에서 버전만 선언하고 모듈에서 적용
    id("com.android.application") version "7.4.2" apply false
    kotlin("android") version "1.8.10" apply false
    kotlin("jvm") version "1.8.10" apply false
}

// 개별 모듈(:app)에서 실제 플러그인을 적용하고 설정합니다.