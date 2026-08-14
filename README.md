# Norvexa Flow

**Norvexa Flow** is a local-first Android financial assistant for freelancers and micro-business owners with irregular income.

The GitHub repository keeps its original name `Norvexa-Solo`; the product name inside the application is **Norvexa Flow**.

## Product principle

Norvexa Flow is not a bank aggregator, accounting suite, tax service, or investment adviser. It helps answer practical questions:

- How much money is actually available now?
- Will the balance stay above a safe level until expected income arrives?
- Which clients still owe money?
- Which mandatory and recurring expenses are coming next?
- What is the minimum safe project price?
- How much should be reserved using a user-configured tax percentage?

Core functionality works locally without bank APIs or a mandatory account.

## Current release

Current audit release: **`0.2.1-alpha01`**.

Implemented:

- Apple-inspired Material 3 / Jetpack Compose UI;
- wallets and actual income/expense transactions;
- clients and expected receivables;
- settlement of receivables into a selected wallet;
- planned and recurring expenses with settlement from a selected wallet;
- protected reserves and a user-configured tax-reserve estimate;
- 7/30-day cash-flow forecast and cash-gap detection;
- overdue obligation handling;
- ISO 4217-aware currency fraction digits and manual exchange rates;
- minimum project-price calculator;
- cash and economic margin calculator;
- CSV export with spreadsheet formula-injection protection;
- paginated PDF reports;
- `.nvxflow` backup format v2 with financial settings and validation;
- compatibility checks for legacy v1 backups;
- local notifications;
- light, dark, and system themes;
- optional `FLAG_SECURE` screen protection;
- Room schema v2 with a non-destructive `1 -> 2` migration;
- unit tests for critical finance calculations and audit regressions.

## Data integrity rules

- Monetary values are stored as `Long` minor units.
- Financial calculations use integer/`BigDecimal` arithmetic; `Double` is not used for monetary calculations.
- Currency precision follows ISO currency metadata, including zero-decimal currencies such as JPY.
- Historical exchange rates are stored with financial records.
- The base currency is fixed after onboarding because changing it without a full historical-rate conversion would invalidate aggregates.
- Settling an expected payment or planned expense creates a real wallet transaction atomically.
- Automatically generated settlement transactions are linked to their source and cannot be deleted independently.
- Database upgrades do not use destructive migration fallback.

## Backup format

`.nvxflow` v2 stores:

- wallets;
- transactions;
- clients;
- receivables;
- planned expenses;
- reserves;
- base currency;
- user-configured tax percentage;
- safe-balance setting;
- settlement-source metadata.

Backup files are validated before replacing the current database. Legacy v1 backups remain readable with additional base-currency safety checks.

## Security status

Current alpha security is intentionally described without overclaiming:

- Android app backup is disabled;
- optional screen protection blocks screenshots and recent-app previews;
- financial data remains local unless the user explicitly exports it;
- the Room database is **not encrypted at rest yet**;
- `.nvxflow` backup files are **not encrypted yet**.

Database and password-protected backup encryption are planned as a dedicated data-preserving security migration before production use with sensitive real-world financial data.

## Toolchain

- Android Gradle Plugin: `8.11.1`
- Gradle: `8.13`
- Kotlin / Compose compiler plugin: `2.1.21`
- Compose BOM: `2026.06.00`
- Room: `2.8.4`
- compileSdk / targetSdk: `36`
- minSdk: `26`
- Java: `17`

## Build

Requirements:

- JDK 17
- Android SDK 36
- Gradle 8.13

The repository currently uses CI-provided Gradle rather than a committed Gradle wrapper.

```bash
gradle clean testDebugUnitTest lintDebug assembleDebug
```

GitHub Actions runs the same verification on `main` and uploads a debug APK artifact.

## Financial and legal scope

Norvexa Flow is a planning tool. It does not provide accounting, tax, legal, banking, credit, or investment advice. Tax percentages, exchange rates, dates, probabilities, and other assumptions are configured by the user and results are planning estimates.

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Roadmap](docs/ROADMAP.md)
