# Solitaire Klondike Android

This project is a native Android application for playing the classic card game Solitaire, specifically the Klondike variant. 

See also: [Klondike 시나리오](docs/SCENARIOS.md)

## Features

- **Game Logic**: Implements the rules of Klondike Solitaire.
- **User Interface**: A clean and intuitive UI for an engaging user experience.
- **Undo Functionality**: Allows players to revert their last moves.
- **Game State Management**: Tracks the current state of the game, including the deck, piles, and cards.

## Project Structure

- **app/**: Contains the main application code and resources.
  - **src/**: Source code for the application.
    - **main/**: Main source set for the app.
      - **java/**: Contains Kotlin files for the app's logic and UI.
      - **res/**: Resources such as layouts, strings, and colors.
    - **androidTest/**: Instrumented tests for the application.
    - **test/**: Unit tests for the application logic.
  - **build.gradle.kts**: Gradle build configuration for the app module.
  - **proguard-rules.pro**: ProGuard rules for code obfuscation and optimization.

## Getting Started

1. Clone the repository:
   ```
   git clone <repository-url>
   ```
2. Open the project in Android Studio or your preferred IDE.
3. Build and run the application on an Android device or emulator.

## WSL Development Guide

If you prefer to develop inside WSL (Linux) and only use Windows Android Studio/emulator for device testing, follow this setup:

### 1) Prerequisites inside WSL

- Java 17
  ```bash
  sudo apt-get update && sudo apt-get install -y openjdk-17-jdk
  java -version
  ```
  If java is not found or apt has repository issues, install Amazon Corretto 17 locally (no sudo needed):
  ```bash
  # Install Corretto JDK 17 under ~/.local/share/jdk
  mkdir -p $HOME/.local/share/jdk && cd $HOME/.local/share/jdk
  curl -L -o corretto.tar.gz https://corretto.aws/downloads/latest/amazon-corretto-17-x64-linux-jdk.tar.gz
  tar -xzf corretto.tar.gz && rm corretto.tar.gz

  # Persist environment variables
  JDK_DIR=$(ls -d $HOME/.local/share/jdk/amazon-corretto-17*/ | head -n1)
  echo "export JAVA_HOME=$JDK_DIR" >> $HOME/.bashrc
  echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> $HOME/.bashrc
  source $HOME/.bashrc

  java --version
  ```
- Android SDK (commandline-tools)
  ```bash
  # Set environment variables in ~/.bashrc (or ~/.profile)
  echo 'export ANDROID_HOME="$HOME/Android/Sdk"' >> ~/.bashrc
  echo 'export ANDROID_SDK_ROOT="$ANDROID_HOME"' >> ~/.bashrc
  echo 'export PATH="$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin"' >> ~/.bashrc
  source ~/.bashrc

  mkdir -p "$ANDROID_HOME/cmdline-tools"
  cd /tmp
  curl -LO https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  unzip -q commandlinetools-linux-11076708_latest.zip
  mv cmdline-tools "$ANDROID_HOME/cmdline-tools/latest"

  yes | sdkmanager --licenses
  sdkmanager "platform-tools" "platforms;android-33" "build-tools;33.0.2"
  ```

### 2) Build and test in WSL

```bash
./gradlew clean
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Instrumented tests require a device. Running an Android emulator inside WSL is not supported. Use a real device over network ADB or use Windows emulator with Windows ADB.

### 3) Install APK to Windows emulator or device

- Using Windows ADB with a Windows emulator (recommended):
  1. Build APK in WSL (previous step).
  2. From Windows PowerShell or CMD, install the APK produced in WSL via the network path:
     ```powershell
     adb install -r \\wsl$\Ubuntu\home\jynius\projects\Android\solitaire-klondike-android\app\build\outputs\apk\debug\app-debug.apk
     ```

- Using ADB over network from WSL to a physical device:
  ```bash
  adb connect <device-ip>:5555
  ./gradlew :app:connectedDebugAndroidTest
  ```

### Notes

- Keep all development in WSL to avoid file watcher and performance issues. Only use Windows Android Studio/emulator to run or debug on a device.
- No need to revert Gradle changes; the configuration is IDE-agnostic and works for both WSL CLI and Android Studio on Windows.
- To avoid line-ending issues when occasionally editing from Windows, consider setting Git to use LF by default:
  ```bash
  git config core.autocrlf input
  ```

## Contributing

Contributions are welcome! Please open an issue or submit a pull request for any enhancements or bug fixes.

## License

This project is licensed under the MIT License. See the LICENSE file for more details.