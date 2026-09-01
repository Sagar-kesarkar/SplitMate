# Release Checklist

## Source

- [ ] Confirm application ID and version code have not regressed.
- [ ] Confirm Room migrations cover every supported prior schema.
- [ ] Run unit tests and lint one Gradle command at a time.
- [ ] Confirm `local.properties`, `.idea`, build output and secrets are untracked.
- [ ] Scan current source and reachable release history for credentials.
- [ ] Verify Demo/Live isolation and persistence.

## APK

- [ ] Build only the intended future version.
- [ ] Verify package ID, label, version, SDK range and ABIs.
- [ ] Verify signing certificate and record its fingerprint.
- [ ] Generate and verify SHA-256.
- [ ] Install with `adb install -r`; never clear data for an update test.
- [ ] Perform a device walkthrough and force-stop/restart test.

## GitHub

- [ ] Confirm the authenticated account and exact repository.
- [ ] Ensure the release tag matches release notes; document any immutable artifact mismatch.
- [ ] Upload APKs as Release assets, never as Git files or LFS objects.
- [ ] Mark beta builds as pre-releases.
- [ ] Inspect the published tree and release downloads.

## SplitMate Beta V0 preservation

`SplitMate-beta-V0.apk` is immutable. Do not rebuild, re-sign, repackage, rename another build as V0 or replace the published asset.
