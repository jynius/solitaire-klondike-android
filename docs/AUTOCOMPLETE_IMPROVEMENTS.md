# AutoComplete 개선 작업 로그

## 2026-02-06: Recycle 무한 루프 수정 및 Solvable 게임 분석

### 1. Recycle 무한 루프 문제 해결 ✅

**문제:**
- Stock에 6장만 남았을 때 반복적으로 "6개 카드를 자동으로 이동했다" 메시지 출력
- Recycle이 무한 반복됨

**해결:**
- `recycleCount`, `wasteUsedAfterRecycle`, `drawsAfterRecycle`, `recycleSize` 변수 추가
- Recycle 전에 Stock이 비어있는지 미리 체크
- Recycle 후 Waste 카드를 실제로 사용했는지 추적
- 조건: Stock 비어있고 + Waste 미사용 + 거의 한 사이클 완료 시 중단

**검증:**
- `RecycleDebugTest.kt`: 첫 autoComplete 85수, 두 번째 autoComplete 0수 (무한 루프 없음)
- `AutoCompleteRecycleTest.kt`: 다양한 recycle 시나리오 테스트
- APK 빌드 및 실제 기기(SM-S938N) 배포 완료
- 사용자 확인: 정상 동작 ✅

### 2. Solvable 게임에서 조기 종료 문제 분석 🔄

**문제:**
- 실제로는 해결 가능한 게임인데 autoComplete가 "더 이상 진행할 수 없다"고 멈춤
- 예: Seed 17848904495592789619 - 69수 후 멈춤 (Foundation 7/52)

**분석 결과:**
```
After autoComplete (69 moves):
  Stock: 0
  Waste: 6 (CLUBS QUEEN, HEARTS SIX, HEARTS TWO, DIAMONDS QUEEN, SPADES JACK, DIAMONDS KING)
  Foundation: 7/52
  
  Tableau:
  T[0]: 6 cards (6 face-up), top=HEARTS THREE
  T[1]: 7 cards (6 face-up), top=HEARTS FOUR
  T[2]: 2 cards (1 face-up), top=CLUBS FIVE
  T[3]: 4 cards (2 face-up), top=HEARTS EIGHT
  T[4]: 3 cards (3 face-up), top=CLUBS JACK
  T[5]: 13 cards (9 face-up), top=CLUBS FOUR
  T[6]: 4 cards (1 face-up), top=DIAMONDS JACK
```

**가능한 이동:**
- HEARTS THREE (T[0]) → CLUBS FOUR (T[5]) - 유효한 이동이지만 실행 안 됨

**근본 원인:**
- autoComplete는 **Greedy 알고리즘** 사용
- 각 단계에서 로컬 최적 선택을 하지만, 글로벌 최적 해를 보장하지 않음
- 일부 solvable 게임에서 막다른 골목에 도달 가능

### 3. 시도한 개선 방안

#### 3-1. Step 5.5 추가 (Tableau 재배치)
- Waste에 카드가 있지만 놓을 곳이 없을 때 Tableau 재배치 시도
- 결과: 개선 없음 (여전히 69수에서 멈춤)

#### 3-2. Step 3을 3a/3b로 분리
```kotlin
// 3a. 뒷면 카드 뒤집기 (기존 로직)
// 3b. Waste를 놓을 곳이 없을 때 Tableau 재배치
if (!moved && waste.isNotEmpty() && !wasteCanBePlaced) {
    // 모든 Tableau→Tableau 이동 시도
}
```
- 결과: 여전히 동일한 지점에서 멈춤

### 4. 결론 및 향후 과제

**Greedy 알고리즘의 한계:**
- 빠르고 대부분의 경우 잘 작동
- 하지만 복잡한 게임에서는 실패 가능

**해결 방안 검토:**

1. **BFSSolver 통합** (권장)
   - 장점: 모든 solvable 게임 해결 가능
   - 단점: 시간 소요 (timeout 5초)
   - 제안: Greedy 실패 시 BFSSolver fallback

2. **Greedy 알고리즘 고도화**
   - Look-ahead 추가
   - 더 정교한 휴리스틱
   - 단점: 여전히 모든 게임 보장 불가

3. **하이브리드 접근** ⭐
   ```
   1. Greedy autoComplete 시도 (빠름)
   2. 막히면 BFSSolver 사용 (정확함)
   3. 시간 초과 시 현재까지 결과 반환
   ```

### 5. 코드 변경 사항

**GameViewModel.kt:**
- `autoComplete()` 함수에 recycle 추적 변수 추가
- Step 3을 3a/3b로 분리하여 Tableau 재배치 로직 강화
- Recycle 조건 개선

**테스트:**
- `RecycleDebugTest.kt`: Recycle 무한 루프 검증
- `AutoCompleteRecycleTest.kt`: 다양한 recycle 시나리오
- `AutoCompleteSolvableTest.kt`: Solvable 게임 검증 (seed 17848904495592789619)

### 6. 다음 단계

- [ ] BFSSolver를 autoComplete에 통합 여부 결정
- [ ] 성능 테스트 (BFSSolver timeout 최적화)
- [ ] 사용자 피드백 수집
- [x] 문서화 완료
- [ ] 통계 기능 작업 진행

---

**작성:** 2026-02-06  
**상태:** Recycle 수정 완료 ✅ | Solvable 게임 개선 분석 중 🔄
