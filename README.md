# MathWake – Báo thức giải toán

MathWake là ứng dụng báo thức Android chạy hoàn toàn offline. Khi đến giờ, ứng dụng phát chuông, rung và mở màn hình câu hỏi; người dùng phải giải đủ số câu đã chọn để dừng báo thức. Dự án viết bằng Java + Android XML, lưu SQLite và build trực tiếp từ terminal trong VS Code, không cần Android Studio và không tạo file `.exe`.

## Chức năng đã triển khai

- Tạo, sửa, xóa, bật/tắt và sắp xếp báo thức theo giờ.
- Lặp một lần, hằng ngày, Thứ 2–Thứ 6 hoặc chọn từng ngày.
- Chọn nhạc chuông hệ thống, rung và mức âm lượng.
- Ba độ khó; 1, 3 hoặc 5 câu; cộng, trừ, nhân, chia hoặc ngẫu nhiên.
- Phép chia luôn chia hết, không chia 0; mặc định không sinh kết quả âm.
- Báo thức chính xác bằng `AlarmManager.setAlarmClock()`.
- Foreground service phát chuông/rung kể cả khi ứng dụng không mở.
- Thông báo ưu tiên cao và full-screen intent khi Android cho phép.
- Báo lại sau 5 phút và nút dừng khẩn cấp có xác nhận.
- Đăng ký lại lịch sau khi khởi động máy hoặc cập nhật ứng dụng.
- Lưu bền vững bằng SQLite; không cần Internet, tài khoản hay backend.
- Giữ `PARTIAL_WAKE_LOCK` có timeout trong suốt phiên reo để tránh CPU ngủ giữa chừng.
- Lưu câu hỏi, đáp án đang nhập và tiến độ khi xoay màn hình.
- Chọn tệp âm thanh qua SAF và giữ quyền đọc URI sau khi khởi động lại.
- Audio Focus tự nhường âm thanh khi có cuộc gọi hoặc ứng dụng ưu tiên hơn.
- Notification fallback và màn hình Activity tự thử khởi động lại service nếu ROM chặn FGS nền.
- Cảnh báo rõ khi âm lượng báo thức hệ thống đang bị tắt; ứng dụng không tự ý thay đổi âm lượng hệ thống.

## Yêu cầu máy phát triển

- Windows 11 và PowerShell.
- JDK 17. Máy hiện tại đã cài Temurin tại:
  `C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot`
- Android SDK. Máy hiện tại dùng:
  `C:\Users\LENOVO\AppData\Local\Android\Sdk`
- Android SDK Platform 37 và Build Tools (đã có trên máy hiện tại).
- Điện thoại Android 8.0 (API 26) trở lên.

Mở terminal tại thư mục dự án và cấu hình cho phiên PowerShell hiện tại:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.21.9-hotspot'
$env:ANDROID_HOME = 'C:\Users\LENOVO\AppData\Local\Android\Sdk'
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
java -version
adb version
```

`local.properties` đã trỏ đến SDK trên máy này. Nếu chuyển sang máy khác, sửa `sdk.dir` cho đúng đường dẫn Android SDK của máy đó.

### Thêm ADB vào Path vĩnh viễn

Nếu PowerShell báo `adb is not recognized`, chạy **một lần**:

```powershell
$adbPath = 'C:\Users\LENOVO\AppData\Local\Android\Sdk\platform-tools'
$userPath = [Environment]::GetEnvironmentVariable('Path', 'User')

if (($userPath -split ';') -notcontains $adbPath) {
    [Environment]::SetEnvironmentVariable(
        'Path',
        ($userPath.TrimEnd(';') + ';' + $adbPath),
        'User'
    )
}
```

Sau đó **đóng hoàn toàn VS Code**, mở lại VS Code và terminal mới rồi kiểm tra:

```powershell
adb version
adb devices -l
```

Terminal đã mở trước khi cập nhật `Path` sẽ không tự nhận thay đổi. Muốn dùng ngay trong terminal hiện tại:

```powershell
$env:Path = "C:\Users\LENOVO\AppData\Local\Android\Sdk\platform-tools;$env:Path"
```

Hoặc luôn gọi bằng đường dẫn đầy đủ:

```powershell
& 'C:\Users\LENOVO\AppData\Local\Android\Sdk\platform-tools\adb.exe' devices -l
```

## Chạy test và build bằng terminal

Không cần file chạy sẵn. Dùng Gradle Wrapper:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat assembleDebug
```

Workspace có tên tiếng Việt. `gradlew.bat` đã được điều chỉnh để tự ánh xạ tạm dự án sang ổ `W:` nhằm tránh lỗi mã hóa đường dẫn của Windows/Java, sau đó tự gỡ ánh xạ. Hãy bảo đảm ổ `W:` đang trống. Nếu một lần chạy bị ngắt đột ngột và còn ánh xạ, gỡ bằng:

```powershell
subst W: /d
```

APK debug được tạo tại:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Kết nối và cài lên điện thoại qua USB

1. Trên điện thoại, vào **Cài đặt → Thông tin điện thoại → Thông tin phần mềm**.
2. Chạm **Số hiệu bản tạo** 7 lần để bật Tùy chọn nhà phát triển.
3. Vào **Tùy chọn nhà phát triển**, bật **Gỡ lỗi USB**.
4. Kết nối cáp có truyền dữ liệu và chấp nhận khóa RSA trên điện thoại.
5. Chạy:

```powershell
adb devices -l
```

Hoặc cài trực tiếp APK đã build:

```powershell
adb install --no-streaming -r ".\app\build\outputs\apk\debug\app-debug.apk"
```

Nếu kết nối nhiều điện thoại, lấy serial ở cột đầu của `adb devices -l`:

```powershell
adb -s SERIAL install --no-streaming -r ".\app\build\outputs\apk\debug\app-debug.apk"
```

Ví dụ với thiết bị đã dùng trong quá trình kiểm thử:

```powershell
adb -s 205e15e7 install --no-streaming -r ".\app\build\outputs\apk\debug\app-debug.apk"
```

Không thêm dấu `\` sau cùng của câu lệnh.

Nếu trạng thái là `unauthorized`, mở khóa điện thoại, chấp nhận hộp thoại gỡ lỗi rồi chạy lại `adb devices`.

### Cài qua USB trên Xiaomi/MIUI

Trong **Cài đặt → Cài đặt bổ sung → Tùy chọn nhà phát triển**, bật:

- Gỡ lỗi USB.
- Cài đặt qua USB.
- Gỡ lỗi USB (Cài đặt bảo mật), nếu thiết bị có mục này.

MIUI có thể mở `AdbInstallActivity` vài giây sau khi chạy lệnh. Giữ điện thoại mở khóa và nhấn **Cài đặt/Cho phép**. Nếu bỏ qua hoặc hủy hộp thoại, terminal trả về:

```text
INSTALL_FAILED_USER_RESTRICTED: Install canceled by user
```

Đây là điện thoại từ chối phiên cài, không phải lỗi APK.

### Chữ đỏ trong PowerShell

Một số phiên bản PowerShell tô đỏ dữ liệu mà chương trình native ghi vào `stderr`. Kết luận dựa trên `Success` và exit code, không dựa riêng vào màu chữ:

```powershell
$output = adb -s 205e15e7 install --no-streaming -r ".\app\build\outputs\apk\debug\app-debug.apk" 2>&1
$exitCode = $LASTEXITCODE
$output | ForEach-Object { Write-Host $_ }

if ($exitCode -eq 0) {
    Write-Host "Cài đặt thành công" -ForegroundColor Green
} else {
    Write-Host "Cài đặt thất bại, mã lỗi: $exitCode" -ForegroundColor Red
}
```

Phải viết `$_`, không viết `$\_`.

## Kết nối Android bằng Wireless Debugging

Điện thoại và máy tính phải cùng mạng Wi‑Fi. Trên Android 11 trở lên:

1. Mở **Tùy chọn nhà phát triển → Gỡ lỗi không dây**.
2. Chọn **Ghép nối thiết bị bằng mã ghép nối** và ghi lại IP, cổng, mã.
3. Chạy các lệnh với giá trị hiển thị trên điện thoại:

```powershell
adb pair 192.168.1.20:37123
adb connect 192.168.1.20:42155
adb devices
.\gradlew.bat installDebug
```

Hai cổng ghép nối và kết nối thường khác nhau. Không dùng các địa chỉ ví dụ ở trên nếu điện thoại hiển thị giá trị khác.

## Quyền cần cấp trên điện thoại

Khi chạy lần đầu, cho phép các mục mà ứng dụng hướng dẫn:

- **Thông báo** (`POST_NOTIFICATIONS`) trên Android 13+.
- **Báo thức và lời nhắc / báo thức chính xác** (`SCHEDULE_EXACT_ALARM`) trên Android 12+.
- **Thông báo toàn màn hình** (`USE_FULL_SCREEN_INTENT`) trên Android 14+ để màn hình giải toán hiện khi máy khóa.
- Rung, foreground service, wake lock và nhận sự kiện khởi động được khai báo trong manifest; không truy cập Internet.

Trên các ROM tối ưu pin mạnh như Samsung One UI hoặc Xiaomi MIUI, nên vào **Cài đặt → Ứng dụng → MathWake → Pin** và chọn **Không hạn chế** nếu chế độ tiết kiệm pin làm chậm báo thức. Không tắt các cơ chế bảo mật của Android.

## Cách sử dụng

1. Mở MathWake, nhấn nút `+`.
2. Chọn giờ, lịch lặp, độ khó, số câu, phép toán, chuông/rung và báo lại.
3. Nhấn **Lưu**. Nếu Android yêu cầu quyền báo thức chính xác, cấp quyền rồi bật lại báo thức.
4. Khi chuông reo, nhập đáp án số nguyên và nhấn **Kiểm tra**.
5. Giải đủ số câu để tắt chuông; hoặc dùng **Báo lại sau 5 phút**.
6. **Dừng khẩn cấp** luôn khả dụng và có hộp xác nhận.

## Xem lỗi bằng Logcat

```powershell
adb logcat -c
adb logcat -s AndroidRuntime:E MathWake:D
```

Để xem riêng crash gần nhất:

```powershell
adb logcat -d -s AndroidRuntime:E
```

## Cấu trúc Android Studio chuẩn

```text
MathWake/
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/mathwake/
│       │   └── res/
│       │       ├── color/ drawable/ layout/
│       │       ├── mipmap-anydpi/
│       │       ├── values/ values-night/
│       │       └── xml/
│       ├── test/java/com/example/mathwake/
│       └── androidTest/java/com/example/mathwake/
├── gradle/wrapper/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
└── gradlew.bat
```

Các Activity dùng `AppCompatActivity`, switch dùng `SwitchCompat`, tài nguyên chữ nằm trong `strings.xml`/`arrays.xml`, icon adaptive nằm trong `mipmap-anydpi`, và quy tắc sao lưu nằm trong `res/xml`.

## Kết quả kiểm tra trên máy phát triển

- `testDebugUnitTest`: đạt 7/7 test, không có failure/error.
- `assembleDebug`: thành công; APK debug được tạo.
- `assembleDebugAndroidTest`: thành công; source set kiểm thử thiết bị build được.
- `lintDebug`: thành công với **0 lỗi và 0 cảnh báo**.
- ADB đã nhận thiết bị `205e15e7` (`2201117TG`) và cài APK thành công bằng `--no-streaming` sau khi xác nhận trên MIUI.

APK đã được cài lên thiết bị Xiaomi thử nghiệm, nhưng vẫn cần hoàn thành toàn bộ checklist chuông, rung, full-screen intent, báo lại, Doze và đăng ký lại sau reboot trước khi coi là kiểm thử thiết bị hoàn tất.

## Checklist kiểm thử thủ công trên thiết bị Android

- Tạo báo thức cách hiện tại 2 phút và khóa màn hình.
- Xác nhận chuông, rung, tên báo thức và màn hình câu hỏi.
- Nhập sai rồi nhập đúng; hoàn thành đủ 1/3/5 câu.
- Thử báo lại 5 phút và dừng khẩn cấp.
- Thử nhiều báo thức, lịch ngày thường và lịch tùy chọn.
- Sửa/xóa/tắt báo thức và xác nhận lịch cũ không còn reo.
- Khởi động lại điện thoại rồi xác nhận báo thức vẫn reo.
- Từ chối từng quyền để kiểm tra thông báo hướng dẫn và khả năng phục hồi.

## Lỗi thường gặp

- **Gradle dùng Java 8:** đặt lại `JAVA_HOME` và đưa `%JAVA_HOME%\bin` lên đầu `Path` như phần trên.
- **SDK location not found:** sửa `local.properties` hoặc đặt `ANDROID_HOME` đúng SDK.
- **Ổ W: đang được dùng:** gỡ ánh xạ không cần thiết hoặc đổi ký tự `W:` trong `gradlew.bat`.
- **Báo thức được lưu nhưng đang tắt:** cấp quyền Báo thức và lời nhắc, sau đó bật switch ở màn hình chính.
- **Không hiện toàn màn hình:** cấp quyền thông báo/toàn màn hình, kiểm tra kênh Báo thức và cài đặt màn hình khóa của thiết bị.
- **ADB không thấy máy:** đổi cáp/cổng USB, chọn chế độ truyền dữ liệu, thu hồi quyền gỡ lỗi USB rồi ghép lại.
- **`adb is not recognized`:** thêm `platform-tools` vào `Path`, sau đó đóng hoàn toàn và mở lại VS Code; hoặc dùng đường dẫn đầy đủ đến `adb.exe`.
- **`unauthorized`:** mở khóa điện thoại, chọn “Luôn cho phép từ máy tính này” và nhấn Cho phép.
- **`INSTALL_FAILED_USER_RESTRICTED`:** điện thoại đã hủy cài đặt; bật Cài đặt qua USB/Gỡ lỗi USB bảo mật và xác nhận hộp thoại MIUI.
- **Terminal có chữ đỏ nhưng vẫn có `Success`:** kiểm tra `$LASTEXITCODE`; giá trị `0` nghĩa là cài thành công.
