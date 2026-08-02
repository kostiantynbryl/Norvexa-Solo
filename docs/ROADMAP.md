# Development roadmap

## Milestone 0 — project foundation

- [x] Android project and Gradle configuration
- [x] Compose theme and application navigation
- [x] Room schema foundation
- [x] Hilt configuration
- [x] money-safe calculation models
- [x] available-funds calculator
- [x] cash-flow forecast engine
- [x] minimum-price calculator
- [x] margin calculator
- [x] calculation unit tests
- [x] overview screen foundation

## Milestone 1 — local MVP data flow

- [ ] profile and base currency setup
- [ ] wallet create/edit/archive
- [ ] income and expense forms
- [ ] transaction list and filters
- [ ] client create/edit/archive
- [ ] expected payment create/edit/partial payment
- [ ] planned expense create/edit
- [ ] repository layer combining Room flows
- [ ] dashboard populated from real local data
- [ ] 30-day forecast from persisted events

## Milestone 2 — security and resilience

- [ ] SQLCipher database encryption
- [ ] Android Keystore key wrapping
- [ ] PIN and biometric lock
- [ ] sensitive-screen protection
- [ ] encrypted `.nvxflow` backup format
- [ ] restore validation and emergency rollback backup

## Milestone 3 — planning tools

- [ ] recurring transactions
- [ ] tax reserve rules
- [ ] protected reserve goals
- [ ] price calculator UI
- [ ] margin calculator UI
- [ ] conservative/base/optimistic scenarios
- [ ] 365-day forecast

## Milestone 4 — reports and release

- [ ] CSV export and import
- [ ] PDF reports
- [ ] notifications
- [ ] accessibility audit
- [ ] Russian, Ukrainian and Polish translations
- [ ] Pro entitlement layer
- [ ] Play Billing
- [ ] privacy policy and legal notices
- [ ] release AAB
