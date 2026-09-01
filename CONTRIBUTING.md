# Contributing

Thank you for helping improve SplitMate.

## Before starting

1. Search existing issues and open a focused proposal for larger behavior or schema changes.
2. Do not include personal expense data, Room databases, screenshots with private information, secrets or signing material.
3. Keep changes within the native Android architecture unless a separately approved integration is being designed.

## Development workflow

1. Use JDK 17 and Android SDK 36.
2. Create a topic branch from `main`.
3. Preserve Demo/Live isolation and explicit Room migrations.
4. Add focused tests for financial, persistence and navigation behavior.
5. Run `./gradlew testDebugUnitTest` and `./gradlew lintDebug` one at a time.
6. Open a pull request using the repository template.

## Commit style

Use concise imperative messages, for example:

```text
fix: reconcile settlement edits across zero
docs: clarify offline data boundaries
test: cover personal month rollover
```

## Financial correctness

Do not maintain competing UI totals. Derive balances and budgets from persisted source records, preserve cent-level split totals, and include tests for add/edit/delete/reversal behavior.

## Licence

By contributing, you agree that your contribution is provided under the repository's [MIT License](LICENSE).
