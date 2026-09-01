# Architecture

## Overview

SplitMate is a single-module native Android application. It follows a Compose UI → ViewModel → repository → Room structure and treats the database as the source of truth for financial records.

```text
Jetpack Compose screens and dialogs
              ↓ events / StateFlow
       SplitmateViewModel
              ↓ suspend calls / Flow
       SplitmateRepository
              ↓ DAO operations
  Room demo DB or Room live DB
```

## Active module

- `:app` — application ID and namespace `com.splitmate.app`

There is no active web, Expo, React Native, Firebase, Cloudflare, Gemini or remote-backend module.

## UI and navigation

`SplitmateApp` owns the Compose application shell and bottom destinations: Home, Expenses, Groups, Friends and Insights. Personal Expenses, Group Details, Balance Details, Group Chat and Friend Details are child states with explicit Back handling. Dialogs are state-driven and dismiss before parent navigation.

## State management

`SplitmateViewModel` combines repository `Flow` streams into immutable `SplitmateUiState` exposed as `StateFlow`. User operations call suspend repository methods and update Room; observed screens update from database emissions rather than independently maintained financial totals.

## Persistence

`AppDatabase` uses Room schema version 4 and these entities:

- users
- friends
- groups
- expenses
- settlements
- app preferences
- personal expenses
- chat messages
- balance history

The app opens separate `splitmate_demo.db` and `splitmate_live.db` databases. Demo seeding is guarded by a persisted initialization marker and explicit reset behavior. Live mode is never seeded with Demo records.

### Migrations

- 1 → 2: adds nullable group budget
- 2 → 3: creates persisted balance history
- 3 → 4: adds group ownership, membership visibility, mute and pending-deletion metadata

All builders register the same explicit migrations. Destructive fallback is not used.

## Money and balance calculations

Amounts currently use the existing `Double` representation and centralized INR formatter. Split calculation reconciles allocation cents to the original rounded total.

Pairwise balances use the current user's perspective:

- positive: the other member owes the current user
- negative: the current user owes the other member
- zero: settled

Balances are rebuilt from active expense allocations and settlements. Group spending is the sum of active shared expenses; settlements and personal expenses are excluded. Personal expense totals and categories query only personal-expense records within `[startOfMonth, startOfNextMonth)`.

## Group lifecycle operations

Mute, leave and delete policies are enforced in the repository. Mute expiration is stored on the group and expired values are cleared. Leave changes only local current-user membership and blocks unsupported owner transfer cases. Delete marks owned groups pending, provides a short Undo period, then removes dependent Room data transactionally.

## Network boundary

The beta has no production API client or cloud sync. Two seeded avatar strings are remote-looking metadata, but the app has no image-loading network integration. Online-only controls report their unavailable state rather than simulating success.
