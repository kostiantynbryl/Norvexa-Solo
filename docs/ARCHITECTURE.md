# Norvexa Flow architecture

## Current foundation

The first implementation intentionally starts as one Android application module. Packages already follow the future module boundaries so they can be extracted without rewriting domain logic.

```text
com.norvexa.flow
├── data/local       Room database, entities and DAO
├── di               dependency injection modules
├── domain/model     money-safe domain models
├── domain/calculation
└── ui               Compose screens and theme
```

## Rules

1. Monetary values are stored as `Long` in minor currency units.
2. `Float` and `Double` are forbidden in finance calculations.
3. Exchange rates and percentages use decimal strings or `BigDecimal`.
4. Historical converted values are stored with the rate used at the time.
5. UI code never performs financial formulas directly.
6. The local database is the source of truth.
7. Network access must remain optional for core features.
8. Financial amounts must not be written to analytics or crash logs.

## Planned module extraction

```text
:app
:core:database
:core:design-system
:core:security
:core:export
:domain:finance
:feature:dashboard
:feature:transactions
:feature:clients
:feature:forecast
:feature:calculators
:feature:settings
```

Extraction begins after the first end-to-end flow is functional: create wallet → add transaction → add expected payment → display forecast.

## Security status

The database API is isolated behind `NorvexaDatabase`, but SQLCipher encryption and Keystore-backed key management are not enabled in the bootstrap commit. They are part of the next security milestone before real user data is used.
