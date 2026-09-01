# Privacy

SplitMate Beta is offline-first. Core user-entered records are stored in Room databases on the Android device.

## Data stored locally

- Profile and friend metadata
- Group membership and budgets
- Shared and personal expenses
- Split allocations and settlements
- Balance history
- Local group chat messages
- App mode/preferences and group lifecycle state

Demo and Live records are stored in separate databases.

## Permissions

- Contacts: requested only for the user-initiated contacts import flow.
- Internet/network state: declared in the beta manifest, but the current core application has no production server, Firebase, cloud synchronization or remote notification service.

## Backups

The Android manifest allows platform backup. Whether device or cloud backup occurs depends on Android version, device policy and user settings. The beta does not provide its own export/restore workflow. Users should keep independent records of important financial information.

## External services

No API key is required. The source contains no production backend credentials, Firebase configuration, analytics SDK or advertising SDK. Online-only controls must report that their service is unavailable.

## Responsible use

Do not include real financial data, contact data, Room database files or screenshots containing personal information in public bug reports.
