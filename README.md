# Karoo Battery Performance Monitor

A Kotlin-based proof of concept for monitoring and reporting battery performance on Hammerhead Karoo cycling computers.

## Overview

This project provides battery monitoring capabilities for Karoo devices using the Karoo Extensions API and Android's BatteryManager. It collects real-time battery metrics and generates performance reports.

## Features

- Real-time battery level monitoring
- Battery health assessment
- Temperature tracking
- Charging state detection
- Performance data export (JSON/CSV)
- Historical data analysis
- Power consumption metrics

## Prerequisites

- Android SDK 21+ (required for Karoo compatibility)
- Kotlin 1.8+
- Gradle 7.4+
- VSCode with Kotlin extensions (see setup below)

## VSCode Setup for Kotlin Development

### 1. Install Required Extensions

Open VSCode and install these extensions:

```bash
# Essential Extensions
- Kotlin Language (fwcd.kotlin)
- Extension Pack for Java (vscjava.vscode-java-pack)
- Gradle for Java (vscjava.vscode-gradle)

# Optional but Recommended
- Android iOS Emulator (DiemasMichiels.emulate)
- XML Tools (DotJoshJohnson.xml)
```

### 2. Configure Java SDK

1. Install Java 11 or later
2. Set JAVA_HOME environment variable
3. In VSCode, open Command Palette (Ctrl+Shift+P)
4. Run "Java: Configure Java Runtime"
5. Set your JDK path

### 3. Android SDK Setup

1. Download Android Studio or Android SDK Tools
2. Set ANDROID_HOME environment variable
3. Add SDK tools to PATH:
   ```bash
   export ANDROID_HOME=/path/to/android-sdk
   export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
   ```

### 4. Gradle Configuration

VSCode should auto-detect gradle files. If not:
1. Open Command Palette (Ctrl+Shift+P)
2. Run "Gradle: Refresh Gradle Project"

## Project Structure

```
larry2/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/
│   │   │   │   └── com/karoo/battery/
│   │   │   │       ├── BatteryMonitor.kt
│   │   │   │       ├── BatteryReporter.kt
│   │   │   │       ├── DataExporter.kt
│   │   │   │       └── MainActivity.kt
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/karoo-battery-monitor.git
   cd karoo-battery-monitor
   ```

2. Open in VSCode:
   ```bash
   code .
   ```

3. Build the project:
   ```bash
   ./gradlew build
   ```

## Usage

### Basic Battery Monitoring

```kotlin
val batteryMonitor = BatteryMonitor(context)
batteryMonitor.startMonitoring { batteryData ->
    println("Battery Level: ${batteryData.level}%")
    println("Health: ${batteryData.health}")
    println("Temperature: ${batteryData.temperature}°C")
}
```

### Generate Reports

```kotlin
val reporter = BatteryReporter()
val report = reporter.generateReport(batteryData)
reporter.exportToJson(report, "battery_report.json")
```

## API Reference

### BatteryMonitor

- `startMonitoring(callback: (BatteryData) -> Unit)`: Begin monitoring
- `stopMonitoring()`: Stop monitoring
- `getCurrentBatteryData()`: Get current battery state

### BatteryReporter

- `generateReport(data: List<BatteryData>)`: Create performance report
- `exportToJson(report: BatteryReport, filename: String)`: Export as JSON
- `exportToCsv(report: BatteryReport, filename: String)`: Export as CSV

### BatteryData

```kotlin
data class BatteryData(
    val level: Int,              // Battery percentage (0-100)
    val health: BatteryHealth,   // GOOD, OVERHEAT, DEAD, etc.
    val temperature: Float,      // Temperature in Celsius
    val voltage: Int,           // Voltage in millivolts
    val isCharging: Boolean,    // Charging state
    val timestamp: Long         // Unix timestamp
)
```

## Development

### Running Tests

```bash
./gradlew test
```

### Building APK

```bash
./gradlew assembleDebug
```

### Installing on Karoo Device

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

MIT License - see LICENSE file for details

## Support

For issues related to:
- Karoo Extensions API: [Hammerhead Support](https://support.hammerhead.io)
- This project: Create an issue in this repository

## Roadmap

- [ ] Web dashboard for remote monitoring
- [ ] Machine learning predictions for battery life
- [ ] Integration with Strava/other cycling platforms
- [ ] Custom alert thresholds
- [ ] Battery optimization recommendations

---

**Note**: This is a proof of concept. Test thoroughly before using in production environments.