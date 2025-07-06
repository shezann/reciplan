# Reciplan - Recipe Planning App

A modern Android application built with Kotlin, Jetpack Compose, and Firebase for managing recipes and meal planning.

## 🚀 Features Implemented

### Authentication Flow (Phase 1-5)

- ✅ **Firebase Authentication** - Email link and Google Sign-In
- ✅ **JWT Token Management** - Secure token storage with automatic refresh
- ✅ **Username Selection** - Real-time availability checking
- ✅ **Modern UI** - Built with Jetpack Compose and Material 3
- ✅ **Network Layer** - Retrofit with automatic authentication

### Architecture

- **MVVM** - Modern Android architecture pattern
- **Hilt** - Dependency injection
- **Room** - Local database (ready for implementation)
- **Compose Navigation** - Type-safe navigation
- **Coroutines & Flow** - Reactive programming

## 📋 Prerequisites

Before running the app, you need to set up Firebase:

1. **Firebase Project Setup**

   - Create a new Firebase project at [Firebase Console](https://console.firebase.google.com)
   - Enable Authentication with Email/Password and Google providers
   - Enable Firestore Database

2. **Google Services Configuration**

   - Download your `google-services.json` file from Firebase Console
   - Replace the placeholder `app/google-services.json` with your actual file

3. **Google Sign-In Configuration**
   - In `AuthRepository.kt`, replace `"YOUR_WEB_CLIENT_ID"` with your actual Web Client ID from Firebase Console
   - You can find this in Firebase Console → Authentication → Sign-in method → Google → Web SDK configuration

## 🛠️ Setup Instructions

### 1. Clone and Build

```bash
git clone <repository-url>
cd reciplan
./gradlew build
```

### 2. Configure Firebase

- Replace `app/google-services.json` with your Firebase configuration
- Update the Web Client ID in `AuthRepository.kt`

### 3. Backend Setup

The app is configured to work with the backend at:

- **Development**: `http://10.0.2.2:8000` (Android emulator)
- **Production**: `https://api.reciplan.app`

Update the URLs in `app/build.gradle.kts` if needed:

```kotlin
buildConfigField("String", "BASE_URL", "\"your-backend-url\"")
```

### 4. Run the App

```bash
./gradlew assembleDevDebug  # For development build
./gradlew assembleProdDebug # For production build
```

## 📁 Project Structure

```
app/src/main/java/com/example/reciplan/
├── data/
│   ├── api/           # API interfaces
│   ├── auth/          # Authentication logic
│   ├── model/         # Data models
│   └── network/       # Network interceptors
├── di/                # Dependency injection modules
├── ui/
│   ├── auth/          # Authentication screens
│   ├── splash/        # Splash screen
│   └── theme/         # UI theming
├── MainActivity.kt    # Main activity
└── ReciplanApplication.kt # Application class
```

## 🔐 Authentication Flow

The app implements a complete authentication flow:

1. **Splash Screen** - Health check and authentication state verification
2. **Login Options** - Email link or Google Sign-In
3. **Username Selection** - If user doesn't have a display name
4. **Main App** - Authenticated user experience

### Key Components

- **AuthRepository** - Handles Firebase authentication and backend token exchange
- **TokenManager** - Secure JWT token storage using EncryptedSharedPreferences
- **AuthInterceptor** - Automatic token injection for API calls
- **AuthViewModel** - Manages authentication state and UI interactions

## 🎨 UI/UX Features

- **Material 3 Design** - Modern Material Design components
- **Dark Theme Support** - Automatic theme switching
- **Responsive Layout** - Optimized for different screen sizes
- **Loading States** - Proper loading indicators and error handling
- **Form Validation** - Real-time input validation

## 🔧 Build Variants

The app supports two build variants:

### Development (`dev`)

- Base URL: `http://10.0.2.2:8000`
- Package: `com.example.reciplan.dev`
- Debug logging enabled

### Production (`prod`)

- Base URL: `https://api.reciplan.app`
- Package: `com.example.reciplan`
- Optimized for release

## 📱 Testing

Run tests with:

```bash
./gradlew test           # Unit tests
./gradlew connectedTest  # Instrumented tests
```

## 🔄 API Integration

The app integrates with a backend API that provides:

- `POST /auth/exchange` - Exchange Firebase token for JWT
- `POST /auth/refresh` - Refresh JWT tokens
- `GET /users/username/available` - Check username availability
- `POST /users/username` - Set user username
- `GET /health` - Health check endpoint

## 🚧 TODOs for Complete Implementation

1. **Firebase Configuration** - Replace placeholder values
2. **Google Sign-In** - Complete Activity Result API implementation
3. **Error Handling** - Add comprehensive error handling
4. **Offline Support** - Implement caching strategies
5. **Recipe Features** - Add recipe management functionality

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 💡 Next Steps

To continue development:

1. **Complete Google Sign-In** - Implement proper Activity Result API
2. **Add Recipe Features** - Implement recipe CRUD operations
3. **Offline Support** - Add Room database integration
4. **Push Notifications** - Add Firebase Cloud Messaging
5. **Recipe Sharing** - Implement sharing functionality

The core authentication foundation is now complete and ready for building upon!
