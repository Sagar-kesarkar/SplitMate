# Security Policy

## Supported versions

SplitMate is beta software. Only the newest published beta receives security fixes.

## Reporting a vulnerability

Use GitHub private vulnerability reporting for `Sagar-kesarkar/SplitMate` when available. Otherwise contact the repository owner privately through GitHub. Do not disclose a vulnerability, credential, signing file, private database or personal financial record in a public issue.

Include:

- affected version and Android version
- reproduction steps using non-sensitive sample data
- impact and expected behavior
- relevant logs with tokens, paths and personal data removed

## Secrets and signing

The repository must not contain API keys, tokens, `local.properties`, service-account files, keystores or passwords. V0 is debug-signed. A permanent private release keystore is required for long-term public updates and must never be committed.

## Beta warning

Do not rely on SplitMate Beta as the only copy of important financial records.
