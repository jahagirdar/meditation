# Serenity — Unguided Meditation Timer

A focused, distraction-free Android meditation app with Vipassana self-assessment and WearOS stress monitoring. **All features free, no paywall.**

---

## Project Structure

```
serenity/
├── app/                          # Phone app (Android 8.0+)
│   └── src/main/java/com/serenity/
│       ├── data/
│       │   ├── db/               # Room database, DAOs, entities
│       │   ├── preferences/      # DataStore user preferences
│       │   └── repository/       # Session, Preset, Assessment repositories
│       ├── domain/model/         # Domain models & enums
│       ├── di/                   # Hilt dependency injection
│       ├── service/              # MeditationTimerService (foreground), ReminderReceiver
│       ├── watch/                # PhoneWearableListenerService (receives stress alerts)
│       └── ui/
│           ├── theme/            # Material3 theme, 4 modes, 6 accent colours
│           ├── navigation/       # NavHost + route definitions
│           ├── onboarding/       # 3-page welcome flow
│           ├── home/             # Home screen + HomeViewModel
│           ├── session/          # Active timer screen + SessionViewModel
│           ├── history/          # History + stats screen
│           ├── settings/         # Settings screen
│           ├── assessment/       # Daily Dhamma self-assessment
│           └── components/       # Shared Compose components
│
└── wear/                         # WearOS 3.0+ companion app
    └── src/main/java/com/serenity/wear/
        ├── health/               # StressMonitorService (Health Services HR)
        ├── ui/                   # WearMainScreen, WearViewModel, WearTheme
        └── di/                   # Hilt module
```

---

## Setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 35
- WearOS device or emulator (API 30+) for the watch module

### Build
```bash
# Clone and open in Android Studio, or:
./gradlew :app:assembleDebug
./gradlew :wear:assembleDebug
```

### Sound assets
The app references raw audio files that you must supply under `app/src/main/res/raw/`:

| File name                 | Description              |
|---------------------------|--------------------------|
| `bell_tibetan_bowl.mp3`   | Tibetan singing bowl     |
| `bell_temple_gong.mp3`    | Deep temple gong         |
| `bell_crystal_bowl.mp3`   | Crystal singing bowl     |
| `bell_soft_chime.mp3`     | Soft chime               |
| `bell_zen.mp3`            | Zen bell                 |
| `bell_rain_stick.mp3`     | Rain stick               |
| `bell_wooden_block.mp3`   | Wooden block             |
| `ambient_rain.mp3`        | Light rain (looping)     |
| `ambient_forest.mp3`      | Forest / birdsong        |
| `ambient_ocean.mp3`       | Ocean waves              |
| `ambient_white_noise.mp3` | White noise              |
| `ambient_brown_noise.mp3` | Brown noise              |
| `ambient_fireplace.mp3`   | Crackling fire           |

Free sources: [freesound.org](https://freesound.org), [mixkit.co](https://mixkit.co).  
If a file is missing, that sound simply won't play — the app won't crash.

---

## Architecture

| Layer         | Technology                              |
|---------------|-----------------------------------------|
| UI            | Jetpack Compose + Material3             |
| Architecture  | MVVM + Clean Architecture               |
| DI            | Hilt                                    |
| Database      | Room (SQLite)                           |
| Preferences   | DataStore (Proto)                       |
| Timer service | Foreground Service + WakeLock           |
| Bell audio    | SoundPool (low-latency)                 |
| Ambient audio | MediaPlayer (looping)                   |
| Notifications | NotificationManager + AlarmManager      |
| Watch comms   | Wearable Data Layer (MessageClient)     |
| HR monitoring | Health Services (MeasureClient)         |

---

## Key Features

### Timer
- Duration: 1 min – 8 hours
- Quick chips: 5, 10, 15, 20, 30, 45, 60 min
- Warm-up period: 0 s – 5 min before the main bell
- Cool-down period: 0 s – 5 min after the session
- Interval bells: **15 s, 30 s, 1 m, 2 m, 5 m, 10 m, 15 m, 20 m, 30 m, 45 m, 60 m**
- 8 bell sounds (start / interval / end independently selectable)
- 7 ambient soundscapes with 3-second fade in/out
- Silent / haptic-only mode (3 distinct vibration patterns)
- Up to 10 named presets

### Self-Assessment (Daily Dhamma Tracker)
20 parameters from the Vipassana tradition, grouped into 5 categories:

| Category   | Parameters |
|------------|------------|
| 🧘 Practice | Morning 5-min sit, Morning 1-hr, Observing breath in free time, Metta bhavana, Evening 1-hr, Evening 5-min |
| 🗣️ Speech   | No harsh words, No slander, No lying, No gossip |
| 🌊 Mind     | Equanimity with fear/anxiety, Tolerating criticism, Equanimity in pleasant situations, Equanimity in unpleasant situations |
| ⚖️ Conduct  | No stealing, No sexual misconduct, Admit mistakes, Righteous duty |
| 🤝 Relations | Metta to family, Harmonious with colleagues |

- ✓ / ✗ toggles per parameter, tap cycles null → yes → no → null
- Month heatmap (colour-coded by score: ≥80% green, ≥50% blue, <50% amber)
- Progress ring per day
- "May all beings be happy… be peaceful… be liberated…" closing

### WearOS Stress Monitoring
- Subscribes to `HEART_RATE_BPM` via Health Services `MeasureClient`
- 10-reading sliding-window average
- Triggers when HR sustained above 85 bpm with stress score > 0.6
- 10-minute cooldown between nudges
- Sends `/stress/alert` JSON message over Wearable Data Layer
- Phone shows a high-priority notification with "Start 2-min calm" action
- If user is already meditating, nudge is suppressed

### Statistics
Current streak, longest streak, total sessions, total minutes, average session length, weekly chart, daily goal progress.

### Settings
- 4 themes: System / Light / Dark / AMOLED black
- 6 accent colours
- Show elapsed vs remaining time
- Breathing animation toggle (4-7-8 cycle)
- Daily goal (5–120 min)
- Screen dim delay (10–120 s)
- Stress nudge toggle
- Up to 3 daily reminder notifications with time picker

---

## Permissions

| Permission | Reason |
|---|---|
| `FOREGROUND_SERVICE` | Timer survives backgrounding |
| `WAKE_LOCK` | CPU stays on during session |
| `POST_NOTIFICATIONS` | Active session + reminders |
| `SCHEDULE_EXACT_ALARM` | Precise reminder timing |
| `ACCESS_NOTIFICATION_POLICY` | Optional DND management |
| `READ_MEDIA_IMAGES` | Optional gallery background |
| `BODY_SENSORS` (wear) | Heart rate monitoring |

---

## Contributing

PRs welcome. Key areas for contribution:
- Real audio assets (see table above)
- WearOS tile / complication
- Home screen widget (quick-start)
- Data export (CSV / JSON)
- Custom sound upload
- Health Connect integration for session export

---

*"May all beings be happy… be peaceful… be liberated…"*
