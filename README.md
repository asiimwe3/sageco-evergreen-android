# SAGECO Evergreen Android

Native Android app built with Jetpack Compose for the SAGECO Evergreen real estate platform.

## Architecture
- UI: Jetpack Compose with Material 3
- Navigation: Navigation Compose with bottom navigation bar
- Backend: Calls live API at sageco-evergreen-co.vercel.app (Vercel + Supabase)
- Image loading: Coil
- Local storage: DataStore Preferences (agent ID persistence)

## Features
1. Home feed with featured properties and quick actions
2. Property listings with search, filter, sort, and pagination
3. Property detail with image gallery, contact actions
4. Brokers directory with search
5. MLM Agent network with wallet, downline, commissions, withdrawals
6. Local AI chatbot (rule-based, no external LLM)
7. Account screen with links to web features

## Build
cd property-masters
./gradlew assembleRelease

Package: com.sagecoevergreen.app
Min SDK: 24 (Android 7.0)
Target SDK: 34 (Android 14)
