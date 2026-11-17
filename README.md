# FinAI 💸

**A personal-finance Android app with an AI assistant, built for students in Mexico.**

FinAI keeps track of income, expenses, budgets and savings goals, and uses AI to cut down on manual entry: snap a photo of a receipt and the app fills in the transaction for you, or ask the built-in assistant questions about your own spending.

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-24-blue)
![Database](https://img.shields.io/badge/Room-SQLite-orange)

<!--
## Screenshots
Add 3–4 screenshots here (Home, Budget, AI Assistant, Receipt scan), e.g.:
<p align="center">
  <img src="docs/screenshots/home.png" width="200"/>
  <img src="docs/screenshots/budget.png" width="200"/>
  <img src="docs/screenshots/assistant.png" width="200"/>
  <img src="docs/screenshots/camera.png" width="200"/>
</p>
-->

---

## Features

| Feature | What it does |
|---|---|
| **Dashboard** | Current balance, total income and expenses, the latest transactions, and the balance converted to USD using live exchange rates. |
| **Transactions** | Add, edit and delete income and expenses by category. Lists update automatically through Room + Kotlin `Flow`. |
| **Budgets** | Set a monthly limit per category and track spent vs. remaining, with progress bars and a custom pie chart (`PieChartView`). |
| **Savings goals** | Create goals (e.g. "Laptop – $25,000") and add contributions over time. |
| **AI assistant** | A chat powered by OpenAI (`gpt-4o-mini`) that gets a summary of your balance, recent transactions and budgets, so its advice is about *your* finances, in Spanish and in MXN. |
| **Receipt scanning** | Take a photo of a receipt; the vision model pulls out the merchant, amount and category and pre-fills a new transaction. |
| **Shake to add** | Shake the phone on the main screen to open the "new transaction" dialog right away (accelerometer-based `ShakeDetector`). |
| **Bank data import** | Imports SAT invoices through the [Belvo](https://belvo.com) open-finance API (sandbox), skipping duplicates by their external ID. |
| **Authentication** | Local sign-up and log-in with SHA-256 hashed passwords stored in DataStore, plus **fingerprint log-in** through `BiometricPrompt`. |
| **Profile** | Choose your preferred currency, import bank data and log out. |

---

## Tech stack

- **Language:** Kotlin
- **UI:** XML layouts, ViewBinding, Fragments + `BottomNavigationView`, Material Components, SplashScreen API
- **Architecture:** MVVM (`ViewModel` / `AndroidViewModel`, `StateFlow`, `LiveData`)
- **Local persistence:** Room (SQLite) for transactions, goals and budgets · Preferences DataStore for session and settings
- **Networking:** Volley with a small home-made request layer (`TargetType` → `RequestManager` → `RequestListener`), plus Gson
- **Device features:** Camera (`TakePicturePreview`), accelerometer, biometrics (`androidx.biometric`)
- **External APIs:** OpenAI Chat Completions (text + vision) · ExchangeRate-API · Belvo (sandbox)
- **Build:** Gradle Kotlin DSL, version catalog (`libs.versions.toml`), KSP for the Room compiler

---

## Architecture

```
┌────────────────────────────── UI ──────────────────────────────┐
│ Activities: Main (router) · Login · Register · Home            │
│             AIAssistant · Camera                               │
│ Fragments:  Home · Movimientos · Presupuesto · Metas · Perfil  │
└───────────────────────────────┬────────────────────────────────┘
                                │ observes StateFlow / LiveData
┌───────────────────────────────▼────────────────────────────────┐
│ ViewModels: FinanzasViewModel · ChatViewModel · ExchangeViewModel │
└───────────────┬───────────────────────────────┬────────────────┘
                │                               │
┌───────────────▼──────────────┐  ┌─────────────▼─────────────────┐
│ data/                        │  │ networking/                   │
│  Room: Movimiento, Meta,     │  │  RequestManager (Volley)      │
│        Presupuesto + DAO     │  │  TargetType / AnahuacAPI      │
│  DataStoreManager (session)  │  │  → OpenAI · ExchangeRate · Belvo │
└──────────────────────────────┘  └───────────────────────────────┘
```

`MainActivity` reads the saved session from DataStore and sends the user to `HomeActivity` or `LoginActivity`. `HomeActivity` holds the five fragments behind a bottom navigation bar, two floating buttons (AI assistant and camera) and the shake listener.

### Project structure

```
app/src/main/java/emilio/tolosa/finai/
├── activities/     # Main, Login, Register, Home, AIAssistant, Camera
├── ui/             # Fragments, RecyclerView adapters, dialog, PieChartView
├── viewmodel/      # FinanzasViewModel, ChatViewModel, ExchangeViewModel
├── data/           # Room entities, DAO, database, DataStoreManager
├── networking/     # RequestManager, API constants, request models
├── biometrics/     # BiometricUtil + auth listener
├── ShakeDetector.kt
└── Utils.kt        # sha256() and MXN currency formatting
```

---

## Getting started

### Requirements

- A recent version of **Android Studio** (the project uses AGP 9.x and `compileSdk 37`)
- JDK 17 or newer
- An Android device or emulator running **Android 7.0 (API 24)** or newer
  - A physical device is recommended for the camera, accelerometer and fingerprint features

### 1. Clone the repository

```bash
git clone https://github.com/EmilioATS01/FinAI.git
```

Then open the folder in Android Studio and let Gradle sync.

### 2. Add your API keys

API keys are read from `local.properties` (already in `.gitignore`) and exposed through `BuildConfig`. Add these lines to `local.properties` in the project root:

```properties
OPENAI_KEY=sk-...
EXCHANGE_KEY=your_exchangerate_api_key
BELVO_ID=your_belvo_secret_id
BELVO_SECRET=your_belvo_secret_password
```

| Key | Where to get it | Used for |
|---|---|---|
| `OPENAI_KEY` | [platform.openai.com](https://platform.openai.com/api-keys) | AI assistant and receipt scanning |
| `EXCHANGE_KEY` | [exchangerate-api.com](https://www.exchangerate-api.com) | MXN → USD balance |
| `BELVO_ID` / `BELVO_SECRET` | [Belvo dashboard](https://dashboard.belvo.com) (sandbox) | Importing invoices |

The app still runs without the keys; only the features that depend on them stop working.

### 3. Run

Pick a device and press **Run ▶**, or from the terminal:

```bash
./gradlew installDebug
```

On first launch, create an account on the sign-up screen. After that you can log in with your password or your fingerprint.

---

## How the AI features work

**Assistant.** When the chat opens, `FinanzasViewModel.resumenParaIa()` builds a short text summary of the balance, the last 15 transactions and each budget's usage. That summary goes into the system prompt, and the whole conversation history is sent with every request, so the model has context for follow-up questions.

**Receipt scanning.** The photo is compressed to JPEG, encoded in Base64 and sent to `gpt-4o-mini` as an `image_url` content block, together with a prompt that asks for **JSON only**:

```json
{ "titulo": "merchant", "monto": 0.0, "categoria": "Comida|Transporte|Entretenimiento|Compras|Salud|Otros" }
```

The response is parsed with Gson and pre-fills `NuevoMovimientoDialog`, so the user can check it before saving.

---

## Known limitations & next steps

- **API keys live in the APK.** `BuildConfig` keeps keys out of the repository, but they can still be pulled out of a compiled app. A production version should send OpenAI and Belvo calls through a small backend.
- **Authentication is local only.** Accounts are stored on the device, and there is no cloud sync or multi-device support.
- **Belvo runs in sandbox mode**, using Belvo's test institution and credentials.
- Ideas for later: monthly filters and history charts, CSV export, notifications when a budget is close to its limit, and unit tests for the ViewModels.

---

## Author

**Emilio Antonio Tolosa Soto**, Information Technology Engineering student at Universidad Anáhuac Querétaro
GitHub: [@EmilioATS01](https://github.com/EmilioATS01)

