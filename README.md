# Reciplan — AI Recipe Extraction Platform

An Android application that transforms TikTok video URLs into fully structured, editable recipes using a 6-stage AI extraction pipeline. Built with Kotlin and Jetpack Compose, backed by a Flask/Celery/Redis microservice architecture.

> Demoed to faculty and industry guests — resulted in a job offer and a course grade of 87%.

---

## How It Works

1. User pastes a TikTok URL into the app
2. The app sends the URL to the Flask backend
3. The backend runs a 6-stage pipeline: video download → audio transcription (OpenAI Whisper) → text extraction (GPT-4) → OCR (PaddleOCR) → recipe structuring → Firestore storage
4. The app polls for pipeline progress and shows a real-time progress bar per stage
5. Once complete, the structured recipe appears — fully editable with ingredient quantities, servings, and instructions

Achieved **95% extraction accuracy** across **50+ test videos**.

---

## Features

### Recipe Extraction
- Paste any TikTok URL to trigger the AI pipeline
- Real-time progress bar showing each of the 6 pipeline stages
- Extracted recipes include title, ingredients (with quantities), instructions, and serving size

### Recipe Management
- Edit, delete, or adjust ingredient quantities on any saved recipe
- Favourite recipes for quick access
- Offline-first architecture — browse saved recipes with no internet connection, search results load in under **500ms** via Room DB cache

### Account & Profile
- Email and Google Sign-In via Firebase Authentication
- JWT token management with automatic refresh
- Profile settings and username customization

### Navigation
- Home tab — browse and search all saved recipes
- Favourites tab — quick access to starred recipes
- Settings tab — account, preferences, and profile management

---

## Tech Stack

### Android (Frontend)
| Layer | Tech |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM (ViewModels, StateFlow) |
| Dependency Injection | Hilt |
| Local DB | Room |
| Networking | Retrofit |
| Auth | Firebase Authentication, JWT |
| Storage | Firebase Firestore |

### Backend
| Layer | Tech |
|---|---|
| API | Flask (Python) |
| Task Queue | Celery + Redis |
| AI Pipeline | OpenAI Whisper, GPT-4, PaddleOCR |
| Database | Firebase Firestore |
| Containerization | Docker |

---

## Architecture

```
Android App (Kotlin/Compose)
        │
        │  REST API (Retrofit)
        ▼
Flask Backend
        │
        ├──▶ Celery Task Queue (Redis broker)
        │           │
        │           ├── Stage 1: Video Download
        │           ├── Stage 2: Audio Extraction
        │           ├── Stage 3: Whisper Transcription
        │           ├── Stage 4: GPT-4 Recipe Structuring
        │           ├── Stage 5: PaddleOCR (on-screen text)
        │           └── Stage 6: Firestore Storage
        │
        └──▶ Firebase Firestore (recipes, users)
```

The Flask backend is containerized with Docker and uses Celery with Redis as a message broker to run the pipeline asynchronously — keeping API responses fast while heavy AI processing happens in the background.

---

## Getting Started

### Prerequisites
- Android Studio (Hedgehog or later)
- Python 3.10+
- Docker & Docker Compose
- Firebase project with Firestore and Authentication enabled
- OpenAI API key

### 1. Clone the Repos

```bash
git clone https://github.com/shezann/reciplan.git
git clone https://github.com/shezann/reciplan-backend.git
```

### 2. Set Up the Backend

```bash
cd reciplan-backend
cp env_example.txt .env
# Fill in your OpenAI key, Firebase credentials, and JWT secret
docker-compose up --build
```

The API will be available at `http://localhost:5050`.

### 3. Configure the Android App

- Add your `google-services.json` to `app/`
- Set the backend URL in `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:5050\"") // emulator
```

### 4. Run the App

```bash
./gradlew assembleDevDebug
```

Or just hit **Run** in Android Studio.

---

## Project Structure

```
app/src/main/java/com/example/reciplan/
├── data/
│   ├── api/           # Retrofit interfaces
│   ├── auth/          # Firebase auth + JWT logic
│   ├── model/         # Data models (Recipe, User, PipelineStatus)
│   └── network/       # Auth interceptors, token refresh
├── di/                # Hilt dependency injection modules
├── ui/
│   ├── home/          # Recipe feed and search
│   ├── extract/       # TikTok URL input + pipeline progress UI
│   ├── recipe/        # Recipe detail and edit screens
│   ├── favourites/    # Favourites tab
│   ├── auth/          # Login and account creation screens
│   ├── settings/      # Profile and preferences
│   └── theme/         # Material 3 theming
├── MainActivity.kt
└── ReciplanApplication.kt
```

---

## Backend Repo

See [reciplan-backend](https://github.com/shezann/reciplan-backend) for the full Flask API, Celery pipeline implementation, and Docker setup.

---

## License

MIT
