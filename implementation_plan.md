# Implementation Plan - Notes Application Mega Upgrade

This plan outlines the design and steps to upgrade the Notes application with a new **To-do List** second page, **Offline Geolocation Reminders**, and a set of premium utilities: **Canvas Sketching, Rich Text Markdown, Voice Transcription, OCR Scanner, and Biometric Note Vault**.

---

## Proposed Features Roadmap

### 1. 🗺️ To-do List Screen & Offline Geolocation
- **Bottom Navigation**: Add a bottom navigation bar to switch between the Notes tab and the To-dos tab.
- **Task List**: Checked/unchecked tasks, due dates, and locations.
- **Offline Geocoding**: Address lookups using Android's native `Geocoder` API (no keys needed).
- **Background Geofencing**: Automatically trigger system notifications when the user walks near a task's geofenced address using Play Services location.

### 2. 🎨 Canvas Sketching & Handwriting (Scribble Mode)
- Create a drawing view with stroke thickness and color adjustments.
- Save the drawings as local PNG files in internal app files and link them as note image attachments.

### 3. 📝 Markdown & Rich Text Preview
- Add a text formatting toolbar in the editor (Bold, Italic, Headers, Checklist toggle).
- Format selections with Markdown syntax and render styled text previews on the card.

### 4. 🎙️ AI Voice Transcription (Speech-to-Text)
- Integrate Android's native `SpeechRecognizer` to transcribe voice notes into typed text in the editor.

### 5. 🔍 OCR Text Scanner (ML Kit)
- Implement Google ML Kit Text Recognition to scan pictures of physical text using CameraX and insert the extracted text directly into notes.

### 6. 🔒 Biometric Note Lock (Private Vault)
- Lock confidential note cards behind Android's `BiometricPrompt` system (Fingerprint / Face Unlock).

---

## Proposed Code Changes

### Dependencies & Permissions

#### [MODIFY] [libs.versions.toml](file:///c:/Users/Soham%20Tawde/Music/ANDRIO_APP/gradle/libs.versions.toml)
- Add Play Services Location, CameraX, and ML Kit Text Recognition dependencies:
  ```toml
  playServicesLocation = "21.2.0"
  camerax = "1.3.4"
  mlkitTextRecognition = "19.0.0"
  ```

#### [MODIFY] [build.gradle.kts](file:///c:/Users/Soham%20Tawde/Music/ANDRIO_APP/app/build.gradle.kts)
- Implement Location, CameraX, and ML Kit dependencies.

#### [MODIFY] [AndroidManifest.xml](file:///c:/Users/Soham%20Tawde/Music/ANDRIO_APP/app/src/main/AndroidManifest.xml)
- Add location permissions (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`).
- Add camera permissions (`CAMERA`).
- Add biometric permissions (`USE_BIOMETRIC`).
- Register background location receivers.

---

### Database & Models

#### [MODIFY] [NoteEntity.kt](file:///c:/Users/Soham%20Tawde/Music/ANDRIO_APP/app/src/main/java/com/example/notes/data/local/NoteEntity.kt)
- Add columns: `val isLocked: Boolean = false`, `val scribblePath: String? = null`.

#### [NEW] [TodoEntity.kt](file:///c:/Users/Soham%20Tawde/Music/ANDRIO_APP/app/src/main/java/com/example/notes/data/local/TodoEntity.kt)
- Define a table `todos` storing: task title, checked/completed state, due date, location name (address query result), latitude, and longitude.

#### [NEW] [TodoDao.kt](file:///c:/Users/Soham%20Tawde/Music/ANDRIO_APP/app/src/main/java/com/example/notes/data/local/TodoDao.kt)
- Define queries to fetch tasks (`Flow<List<TodoEntity>>`), insert/update tasks, delete tasks, and update completed state.

#### [MODIFY] [NoteDatabase.kt](file:///c:/Users/Soham%20Tawde/Music/ANDRIO_APP/app/src/main/java/com/example/notes/data/local/NoteDatabase.kt)
- Include `TodoEntity` in entities list.
- Increase database version to `2`.
- Add `todoDao()` abstract getter.
- Enable `fallbackToDestructiveMigration()` in the builder to seamlessly recreate the database during schema update.

---

### Repository & ViewModel

#### [MODIFY] [NoteRepository.kt](file:///c:/Users/Soham%20Tawde/Music/ANDRIO_APP/app/src/main/java/com/example/notes/data/NoteRepository.kt)
- Expose CRUD flows and suspend methods for `TodoEntity` using `TodoDao`.

#### [MODIFY] [NoteViewModel.kt](file:///c:/Users/Soham%20Tawde/Music/ANDRIO_APP/app/src/main/java/com/example/notes/ui/viewmodel/NoteViewModel.kt)
- Observe tasks list `StateFlow<List<TodoEntity>>`.
- Expose functions to save, delete, and toggle tasks.
- Expose functions for background geocoding (runs asynchronously using Android `Geocoder` on a Dispatcher thread).

---

### Geolocation & Background Alerts

#### [NEW] [GeofenceHelper.kt](file:///c:/Users/Soham%20Tawde/Music/ANDRIO_APP/app/src/main/java/com/example/notes/data/GeofenceHelper.kt)
- Build class to wrap Google `GeofencingClient` helper functions to register/unregister geofence coordinates for active tasks.

#### [NEW] [GeofenceBroadcastReceiver.kt](file:///c:/Users/Soham%20Tawde/Music/ANDRIO_APP/app/src/main/java/com/example/notes/data/GeofenceBroadcastReceiver.kt)
- Create background receiver that listens for geofence entry events.
- On entry event, triggers a high-priority system **Notification** displaying the due task details.

---

### User Interface & Screens

#### [NEW] [TodoTabContent.kt](file:///c:/Users/Soham%20Tawde/Music/ANDRIO_APP/app/src/main/java/com/example/notes/ui/screens/TodoTabContent.kt)
- Create a dedicated component view for the Todo tab:
  - Add task input field (Neo-Brutalist popup bar).
  - Checkable task cards with 3D shadow press offsets and custom location tags.
  - Runtime request dialog for Location permissions.
  - Dialog picker with offline Address lookup search (geocoder-based).

#### [NEW] [ScribbleCanvasDialog.kt](file:///c:/Users/Soham%20Tawde/Music/ANDRIO_APP/app/src/main/java/com/example/notes/ui/components/ScribbleCanvasDialog.kt)
- Implement interactive painting canvas.

#### [MODIFY] [DashboardScreen.kt](file:///c:/Users/Soham%20Tawde/Music/ANDRIO_APP/app/src/main/java/com/example/notes/ui/screens/DashboardScreen.kt)
- Introduce a bottom `NavigationBar` with two tabs: **Notes** and **To-dos**.
- Switch the active screen content dynamically using `AnimatedContent` based on selected tab selection.

---

## Verification Plan

### Automated Tests
- Run unit tests to check database insertion integrity:
  ```powershell
  ./gradlew testDebugUnitTest
  ```

### Manual Verification
- Deploy the app to the emulator/device.
- Verify bottom navigation switches tabs smoothly.
- test OCR text recognition and voice typing in editor.
- lock a note and check that fingerprint verification prompt opens on click.
- Add location-tagged tasks in To-do screen and verify background alert notifications.
