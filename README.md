# Prism Grade

An Android app that inspects a trading card from photos and returns an estimated
grade, the four standard subgrades, and a market value range — the way a PSA,
BGS or CGC grader reads a card.

Point the camera at the front and back, run the inspection, and you get back a
grade on the 1–10 scale with the reasoning behind it: centering measured against
the borders, corner whitening, edge chipping, surface scratches and print lines,
plus anything that looks altered or trimmed.

## What it does

- **Photograph a card** — front required, back optional but it sharpens the read.
- **Inspect** — Claude examines both images and returns a structured grade.
- **Describe instead** — no camera? Type what the card is and what it looks like,
  and it grades from the description (clearly marked, since it can only reflect
  what you wrote).
- **History** — every inspection is stored on-device with its photos and can be
  reopened or deleted.

## Setup

The app runs inspections against the Claude API on **your own Anthropic
account**, so it needs a key before it will do anything.

1. Create a key at [console.anthropic.com](https://console.anthropic.com).
2. Build and install the app (see below).
3. Open **Settings** and paste the key.

```bash
./gradlew assembleDebug          # build
./gradlew installDebug           # install on a connected device
./gradlew testDebugUnitTest      # run unit tests
```

Requires Android Studio (or a local Android SDK with `ANDROID_HOME` set) and
JDK 17+. Minimum device: **Android 8.0 (API 26)**.

## Architecture

Three layers, one direction of dependency: `ui` → `data` → `domain`. Nothing in
`domain` knows about Android or Claude.

```
com.prismgrade
├── domain/model/         GradeReport, Subgrades, GradeScale — the vocabulary
│                         everything else speaks
├── data/
│   ├── remote/           ClaudeGradingService (Anthropic SDK), the prompts,
│   │                     and the tolerant reply parser
│   ├── image/            ImageProcessor — EXIF rotation, downscaling, base64
│   ├── local/            Room database + DataStore for the API key
│   └── repository/       InspectionRepository — the only entry point the UI uses
├── di/                   ServiceLocator (manual DI; the graph is all singletons)
└── ui/
    ├── capture/          Photo bay and description form + ViewModel
    ├── result/           The console readout
    ├── history/          Past inspections
    ├── settings/         API key management
    ├── components/       GradeBadge, SubgradeMeter, chips, fine print
    ├── navigation/       NavHost and the three tabs
    └── theme/            The console palette
```

**State flows one way.** Each screen has a ViewModel exposing a single immutable
`UiState` via `StateFlow`; screens are stateless composables that take state and
emit events. The repository owns the order of operations — prepare images, call
Claude, persist the result — so no ViewModel has to.

### Notes on the pieces that matter

**Image handling** (`ImageProcessor`) decodes with a sample size so a 12MP photo
never lands in memory whole, applies EXIF rotation so a sideways phone shot
doesn't reach the model rotated, and caps the longest edge at 1600px. That last
part is deliberate: the API downsizes anyway, so anything larger is upload time
spent on detail the model won't use.

**Reply parsing** (`GradeReportParser`) reads Claude's answer tolerantly. It asks
for a bare JSON object but accepts a fenced block or prose around it, coerces
scores written as text, clamps out-of-range grades into the 1–10 scale, and
degrades missing fields rather than throwing. This is the one place a model's
sloppiness would otherwise become a crash, so it is also the best-tested file in
the project.

**Model choice** — `claude-opus-5` with adaptive thinking at `HIGH` effort.
Grading is a judgement call across four categories from imperfect photographs;
it is not a task that rewards a cheaper pass.

## Security: the API key

The key is stored in the app's private DataStore and excluded from device
backups (`res/xml/backup_rules.xml`). It never leaves the device except in
requests to Anthropic.

**That is fine for personal use and wrong for a published app.** Anyone with
access to an unlocked or rooted device can read it, and shipping one key per
install means every user needs their own Anthropic account. A distributed
version should route requests through a backend you control, which holds the key
and authenticates your users — the app would then talk to your server instead of
`api.anthropic.com`, and `ClaudeGradingService` is the only file that changes.

## Honest limits

- **The grade is an estimate, not an appraisal.** It is not a submission to, and
  is not affiliated with, PSA, BGS, CGC or SGC. Lighting, glare and camera
  quality all move the reading.
- **Values come from the model's general knowledge, not live sold listings.**
  The app has no pricing feed. Check recent comps before buying or selling.
- **Description mode is weaker than photo mode** by construction — it can only
  reflect what you typed, and scores unmentioned categories conservatively.

## Testing status

`GradeReportParserTest` covers the parser against the reply shapes Claude
produces in practice — clean JSON, fenced JSON, prose-wrapped JSON, missing
fields, out-of-range and text-encoded scores, and unparseable replies. These
pass.

The parser, domain model and `ClaudeGradingService` were compiled and verified
against the real `com.anthropic:anthropic-java:2.34.0` SDK. **The Android app as
a whole has not been compiled** — the environment it was written in has no
Android SDK — so expect to fix dependency versions and any Compose API drift on
your first `./gradlew assembleDebug`.
