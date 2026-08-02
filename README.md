# Norvexa Flow

Android financial assistant for freelancers and micro-business owners with irregular income.

The repository keeps its original GitHub name `Norvexa-Solo`, while the product name inside the application is **Norvexa Flow**.

## Product principle

Norvexa Flow does not try to be a bank aggregator or full accounting system. It answers practical questions:

- How much money is actually available now?
- Will the balance remain safe until the next payment?
- Which clients still owe money?
- What mandatory expenses are coming next?
- What is the minimum safe project price?
- How much should be protected as a user-configured tax reserve?

Core functions remain local and do not require bank APIs or an account.

## Current implementation

Version `0.1.0` foundation contains:

- Kotlin and Jetpack Compose application shell;
- Material 3 overview screen;
- five-section navigation;
- Room database schema for wallets, transactions, clients, receivables, planned expenses and reserves;
- Hilt dependency injection;
- money-safe `Long`/`BigDecimal` calculation layer;
- available-funds calculation;
- confidence-weighted cash-flow forecast and cash-gap detection;
- minimum project price calculation;
- cash and economic margin calculation;
- unit tests for critical formulas.

The screen currently displays an empty state. Data entry forms and repository wiring are the next milestone.

## Toolchain

- Android Gradle Plugin 9.3.0
- Gradle 9.5.0
- Kotlin / Compose compiler 2.4.10
- compileSdk and targetSdk 36
- minSdk 26
- Compose BOM 2026.06.00
- Room 2.8.4
- Hilt 2.60.1

## Build

Requirements:

- JDK 17
- Android SDK 36

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

No APK or AAB is committed to the repository.

## Financial and legal scope

Norvexa Flow is a planning tool. It does not provide accounting, tax, legal, banking or investment advice. Tax percentages and assumptions are configured by the user and calculations are estimates for planning.

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Roadmap](docs/ROADMAP.md)
