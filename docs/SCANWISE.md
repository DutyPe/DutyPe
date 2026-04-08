# ScanWise Android App Specification

## Overview
ScanWise is an Android application that helps users understand food products by scanning a barcode or uploading a product image. The app identifies product details and generates practical health guidance using AI, including localized tips in English, Telugu, and Hindi.

## Product Goals
- Identify products quickly via barcode or image.
- Show clear nutrition and usage guidance.
- Deliver AI recommendations that are practical and short.
- Support Indian usage context with localized language tips.

## Technology Stack
- Language: Kotlin
- UI: Jetpack Compose (Material 3)
- Architecture: MVVM + Clean Architecture
- Navigation: Navigation Compose
- Dependency Injection: Hilt
- Networking: Retrofit + OkHttp
- Image Loading: Coil
- Barcode Scanning: ML Kit Barcode Scanning
- Camera: CameraX
- Local Storage: Room Database
- Async: Coroutines + Flow
- AI: Gemini API
- Preferences: DataStore

## Android Configuration
- Package name: com.scanwise.app
- Min SDK: 24
- Target SDK: 34
- Build system: Gradle Kotlin DSL

## Required Permissions
Add these in AndroidManifest.xml:
- CAMERA
- INTERNET
- READ_EXTERNAL_STORAGE (for legacy image pick flows)

## Module Structure
```text
com.scanwise.app/
|-- data/
|   |-- api/ (ApiService.kt, ProductApi.kt)
|   |-- db/ (AppDatabase.kt, ProductDao.kt, entities/)
|   |-- model/ (Product.kt, Recommendation.kt, User.kt)
|   `-- repository/ (ProductRepository.kt, AiRepository.kt)
|-- domain/
|   `-- usecase/ (GetProductUseCase.kt, GetAiTipsUseCase.kt)
|-- presentation/
|   |-- home/ (HomeScreen.kt, HomeViewModel.kt)
|   |-- scan/ (ScanScreen.kt, ScanViewModel.kt)
|   |-- result/ (ResultScreen.kt, ResultViewModel.kt)
|   |-- profile/ (ProfileScreen.kt)
|   `-- components/ (shared composables)
|-- di/ (AppModule.kt, NetworkModule.kt, DatabaseModule.kt)
|-- utils/ (Constants.kt, Extensions.kt)
`-- ScanWiseApp.kt
```

## UI Theme
### Color Palette
- Primary: #1D9E75
- PrimaryDark: #0F6E56
- Background: #F8FAF9
- Surface: #FFFFFF
- OnSurface: #1A1A1A
- Secondary: #EAF3DE
- TextSecondary: #6B7280
- Error: #E24B4A
- Warning: #EF9F27

### Typography
- Headings: SemiBold
- Body: Normal
- Captions: Light, smaller size

## Screens
### SplashScreen
- Full screen logo and app name.
- Primary green background.
- Tagline: "Scan. Know. Live Better."
- 2-second delay with fade animation.
- Navigate to Home.

### HomeScreen
- Header with logo and language switch.
- Greeting prompt for user action.
- Main scan card with camera icon.
- Search field for product lookup.
- Popular search chips.
- Recent scans list from Room data.
- Bottom navigation: Home, Scan, Profile.

### ScanScreen
- CameraX live preview.
- Top bar with back and title.
- Scanning frame overlay.
- Upload image button below camera area.
- Real-time barcode detection via ML Kit.
- On barcode detection: vibrate and navigate to Result.
- Loading state while processing.

### ResultScreen
- Top bar with product and brand details.
- "AI Analyzed" status badge.
- Scroll content cards:
  - Product info and nutrition grid.
  - AI recommendations.
  - Localized language tips.
  - Doctor recommended tips.
  - Health benefit scores with progress bars.
  - Community tips and add-tip action.
  - Cautions card with warning style.

### ProfileScreen
- User avatar and profile basics.
- Language preference.
- Daily scan count.
- Favorite products.
- Notification and dark mode toggles.
- App version/about section.

## Reusable Components
Create these shared composables in presentation/components:
- ProductCard
- SectionHeader
- LoadingShimmer
- ChipRow
- NutritionGrid
- ProgressBar
- LanguageToggle
- VerifiedBadge
- EmptyState
- ErrorState

## Data Integrations
### Open Food Facts API
Base URL:
- https://world.openfoodfacts.org/

Endpoints:
- GET api/v0/product/{barcode}.json
- GET cgi/search.pl with query params for search_terms, json, page_size, fields

Repository requirements:
- Fetch product by barcode.
- Search products by text query.
- Handle errors with try/catch.
- Map API models to local Product domain model.
- Cache successful responses in Room.

### Gemini AI Integration
Repository responsibilities:
- Generate short, practical product recommendations.
- Generate localized tips for selected language/region.
- Generate health scores and parse structured JSON output.
- Return Kotlin Result wrappers for success/failure.

Prompt guidelines:
- Keep response practical and concise.
- Limit recommendation output to short bullet points.
- Enforce consistent language and formatting.

## State and Navigation Guidance
- Use state hoisting for reusable composables.
- Use local remember state where appropriate.
- Use typed navigation args for product and barcode flow.
- Include @Preview for all major screens/components.
- Move all UI strings to strings.xml.

## Build and Security Notes
- Keep Gemini API key out of source code.
- Load secrets from local/private configuration.
- Add network logging interceptor only in debug builds.
- Handle no-network and no-camera-permission states gracefully.

## Delivery Checklist
- [ ] App boots to Splash and routes correctly.
- [ ] Barcode scan flow reaches Result screen.
- [ ] Image upload flow reaches Result screen.
- [ ] Product API fetch works and caches locally.
- [ ] AI recommendations render with loading/error states.
- [ ] Language toggle switches localized tips.
- [ ] All user-facing strings use resources.
- [ ] Critical screens include empty and error states.
- [ ] Basic instrumentation/unit tests added for core flows.

## Suggested Implementation Order
1. Foundation setup (theme, navigation, DI).
2. Data layer (API + Room + repositories).
3. Scan and search flows.
4. Result screen and AI integration.
5. Profile and preferences.
6. QA, error handling, and optimization.
