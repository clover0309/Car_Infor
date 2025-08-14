# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a multi-platform vehicle tracking system consisting of three components:

1. **Android Application** (`vehicletracker/`) - Native Android app with Kotlin
2. **Backend API** (`vehicle-tracker-backend/`) - Spring Boot REST API with Kotlin  
3. **Frontend Dashboard** (`vehicle-tracker-frontend/`) - Next.js React dashboard

The system tracks vehicle engine status, speed, and GPS location via Bluetooth connections and displays real-time data through a web dashboard.

## Development Commands

### Frontend (Next.js)
```bash
cd vehicle-tracker-frontend
npm run dev         # Start development server with Turbopack
npm run build       # Build for production
npm run start       # Start production server
npm run lint        # Run ESLint
```

### Backend (Spring Boot + Kotlin)
```bash
cd vehicle-tracker-backend/vehicle-tracker-backend
./gradlew bootRun   # Start development server
./gradlew build     # Build JAR
./gradlew test      # Run tests
./gradlew clean     # Clean build artifacts
```

### Android Application
```bash
cd vehicletracker
./gradlew assembleDebug        # Build debug APK
./gradlew installDebug         # Install on connected device
./gradlew connectedAndroidTest # Run instrumented tests
./gradlew test                 # Run unit tests
```

## Architecture & Key Components

### Data Flow
1. Android app connects to vehicle Bluetooth
2. Collects engine status, speed, GPS location
3. Sends data to Spring Boot backend API
4. Frontend polls backend for real-time updates
5. Displays vehicle status and location on dashboard

### Backend API Structure
- **Controllers**: Handle HTTP requests (`DeviceController.kt`, `VehicleController.kt`)
- **Services**: Business logic (`DeviceInfoService.kt`, `VehicleService.kt`)
- **Entities**: JPA data models (`DeviceInfoEntity.kt`, `VehicleStatusEntity.kt`, `DeviceLocationEntity.kt`)
- **Repositories**: Data access layer using Spring Data JPA

### Android App Structure
- **MainActivity.kt**: WebView host + native device registration
- **BluetoothGpsService.kt**: Background service for data collection
- **ApiService.kt**: HTTP client for backend communication
- **Hybrid approach**: WebView for UI + native Android for hardware access

### Frontend Architecture
- **Next.js 15** with React 19 and TypeScript
- **API Integration**: REST client in `lib/api.ts`
- **Real-time Updates**: 5-second polling intervals
- **Map Integration**: Kakao Map component for location display
- **Responsive UI**: Tailwind CSS for styling

## Configuration Notes

### Network Configuration
- Backend runs on port 8080
- Frontend development server on port 3000
- Android app configured for local network IP: `192.168.1.219:3000`
- CORS enabled in backend for frontend communication

### Database
- Uses MySQL for data persistence
- JPA entities with automatic table creation
- Time handling: Backend uses KST timezone for consistency

### Android Permissions
Required permissions in AndroidManifest.xml:
- `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` for GPS
- `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` for Bluetooth
- `FOREGROUND_SERVICE` for background data collection

## Development Environment Requirements

- **Java 17** (backend)
- **Node.js** (frontend)  
- **Android SDK** with API level 24+ (Android 7.0+)
- **Kotlin 2.0.21**
- **MySQL database**

## Testing

### Frontend
- ESLint for code quality
- Next.js built-in TypeScript checking

### Backend
- JUnit 5 for unit tests
- Spring Boot Test for integration tests
- Test configuration in `src/test/kotlin/`

### Android
- JUnit for unit tests (`src/test/`)
- Espresso for UI tests (`src/androidTest/`)

## Key Integration Points

1. **WebView Bridge**: Android app loads frontend in WebView but handles device registration natively
2. **API Endpoints**: REST API for device registration, status updates, and data retrieval
3. **Real-time Sync**: Frontend polls backend every 5 seconds for live updates
4. **Location Services**: GPS coordinates sent from Android to backend, displayed on Kakao Maps

## Build Notes

- Backend requires JVM 17 compatibility
- Android app targets API level 36 with minimum SDK 24
- Frontend uses Next.js 15 with experimental features enabled
- All components configured for local development with hardcoded IPs