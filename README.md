# 충전시 분할실행 (SplitChargeApp)

충전을 시작하면 지정한 두 앱을 분할화면으로 자동 실행하는 안드로이드 앱입니다.
갤럭시 루틴의 "충전 시작 → 동작 실행" 개념을 일반 안드로이드 기기에서 구현한 것입니다.

## 빌드 방법 (Android Studio)

1. 이 폴더(`SplitChargeApp`) 전체를 Android Studio에서 `Open` (Import Project).
2. 처음 열면 Gradle Sync가 자동으로 진행됩니다 (인터넷 필요, Gradle/Android SDK/라이브러리를
   다운로드합니다. PC에 인터넷이 연결돼 있으면 자동으로 됩니다).
3. 상단 메뉴 `Build > Build Bundle(s) / APK(s) > Build APK(s)` 클릭.
4. 빌드가 끝나면 `app/build/outputs/apk/debug/app-debug.apk` 생성됨.
5. 이 APK를 폰에 옮겨 설치 (출처를 알 수 없는 앱 설치 허용 필요).

USB로 폰을 연결하고 `Run ▶` 버튼을 누르면 빌드 + 설치 + 실행이 한 번에 됩니다.

## 앱 사용법

1. 앱 실행 후 "앱 A"와 "앱 B" 드롭다운에서 원하는 두 앱 선택
   (A가 먼저 실행되어 위/왼쪽, B가 나중에 실행되어 아래/오른쪽에 배치됨).
2. "충전 시작 시 자동 실행" 스위치 켜기.
3. 기기가 루팅되어 있지 않다면 "접근성 권한 설정 열기" 버튼을 눌러
   이 앱의 접근성 서비스를 켜주세요 (분할화면 버튼을 자동으로 눌러주는 용도).
4. "지금 테스트 실행" 버튼으로 충전 없이도 바로 동작을 확인할 수 있습니다.
5. 이후 충전 케이블을 꽂으면 자동으로 두 앱이 분할화면으로 실행됩니다.

## 동작 방식 (2가지 전략)

- **루트 있는 기기**: `su`로 `am start-activity --windowingMode 3/4` 셸 명령을 실행해
  두 앱을 각각 분할화면 상/하단에 직접 배치합니다. 가장 안정적입니다.
- **루트 없는 기기**: 안드로이드 자체 앱 권한으로는 다른 앱을 분할화면으로 강제로 띄우는
  공개 API가 없습니다. 대신 접근성 서비스로 "최근 앱 화면 열기 → 분할화면 버튼 찾아서
  자동 클릭 → 두 번째 앱 실행"을 사람이 누르는 것처럼 흉내냅니다
  (`SplitScreenAccessibilityService.kt`).

## 알려진 한계

- 접근성 방식은 제조사/런처(순정 안드로이드, 원플러스, 샤오미 등)마다 "분할화면" 버튼의
  문구나 위치가 달라 일부 기기에서 버튼을 못 찾을 수 있습니다.
  이 경우 `SplitScreenAccessibilityService.kt`의 `SPLIT_BUTTON_KEYWORDS` 목록에
  실제 기기에 표시되는 문구를 추가하고 다시 빌드하면 됩니다.
- 루트 방식은 루팅된 기기 + 루트 관리 앱(Magisk 등)에서 이 앱에 su 권한을 허용해야 동작합니다.
- 일부 제조사는 자체적으로 백그라운드 앱 실행/접근성 서비스를 더 강하게 제한하므로,
  설정에서 배터리 최적화 예외 등록이 필요할 수 있습니다.

## 프로젝트 구조

```
app/src/main/java/com/minsu/splitroutine/
  MainActivity.kt                    앱 선택 UI, 설정 저장
  ChargingReceiver.kt                충전 시작 감지
  SplitScreenLauncher.kt             루트/접근성 전략 분기, 실제 실행 로직
  SplitScreenAccessibilityService.kt 접근성 서비스 (분할화면 버튼 자동 클릭)
  Prefs.kt                           SharedPreferences 저장소
  AppInfo.kt                         설치 앱 정보 모델
```

## PC/Android Studio 없이 무료로 빌드하기 (GitHub Actions)

Android Studio를 설치하지 않고, 브라우저만으로 무료로 APK를 빌드할 수 있습니다.
(GitHub 무료 계정 필요, 결제 없음)

1. https://github.com 에서 무료 계정 생성.
2. 우측 상단 `+` → `New repository` 클릭. 이름 아무거나 입력 (예: split-charge-app),
   Public/Private 상관없음, "Add a README" 체크박스는 꺼둔 채로 `Create repository`.
3. 저장소 화면에서 `Add file` → `Upload files` 클릭.
4. 압축 푼 `SplitChargeApp` 폴더 안의 내용물(파일/폴더 전체, `.github` 폴더 포함)을
   그대로 드래그 앤 드롭. `Commit changes` 클릭.
   (안 보이는 폴더가 있다면 파일 탐색기에서 "숨김 파일 표시"를 켜고 `.github` 폴더가
   업로드됐는지 확인하세요.)
5. 업로드가 끝나면 자동으로 빌드가 시작됩니다. 상단 `Actions` 탭 클릭 →
   진행 중인 작업(Build APK) 클릭해서 3~5분 정도 기다립니다.
   (자동으로 안 뜨면 `Actions` 탭 → `Build APK` → `Run workflow` 버튼으로 직접 실행)
6. 완료되면(초록 체크) 해당 실행 결과 페이지 맨 아래 `Artifacts`에서
   `app-debug-apk`를 다운로드. zip 파일이며 안에 `app-debug.apk`가 들어있습니다.
7. 이 apk 파일을 폰으로 옮겨 설치(출처를 알 수 없는 앱 설치 허용 필요).

빌드는 GitHub의 서버에서 실행되므로 내 컴퓨터 사양과 무관하게 항상 동일하게 동작합니다.
