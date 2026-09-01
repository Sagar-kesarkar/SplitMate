# Features

## Supported locally

- Demo and Live modes with separate Room databases
- Deterministic Demo records and explicit Demo reset
- Groups, friends, member selection and contacts-assisted entry
- Shared expenses with equal, exact, percentage and share splits
- Add, edit and delete recalculation
- Settlements and signed pairwise netting across zero
- Group budgets, total spent, remaining and over-budget states
- Auditable balance event timeline
- Personal expense add, edit, delete, monthly total and category breakdown
- All/Groups/Personal history filtering on the Personal Expenses child screen
- Expense search and detail views
- Local group chat messages
- Long-press single/multiple group selection
- Persistent timed/indefinite group mute state and unmute
- Ownership-aware local leave behavior
- Owner-only recoverable local deletion with Undo
- State persistence across force-stop/restart

## Experimental or incomplete

- The group mute setting is local state; no notification-delivery system is currently implemented.
- Reminder controls require an online service and show an honest unavailable message.
- Group membership, leave, deletion and chat do not synchronize across devices.
- Receipt-image/attachment picking and OCR are not implemented. Shared expenses support local receipt notes only.
- There is no authentication, cloud backup, Firebase integration or production backend.
- The current money model uses `Double`; split reconciliation is tested at currency precision, but a future schema may adopt integer minor units.

## Data boundaries

Personal expenses never affect group/friend balances, settlements, debt simplification or group spending. Settlements affect balances but not spending. Demo records never seed Live mode.
