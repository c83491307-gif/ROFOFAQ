# ROFOF — Signed Release APK

## Signing is intentionally external to Git

The release signing workflow never stores a keystore or passwords in the repository. Configure these four **repository Actions secrets** under Settings → Secrets and variables → Actions:

- `ROFOF_KEYSTORE_BASE64`: base64-encoded JKS/PKCS12 keystore bytes.
- `ROFOF_KEYSTORE_PASSWORD`: keystore password.
- `ROFOF_KEY_ALIAS`: key alias in the keystore.
- `ROFOF_KEY_PASSWORD`: private-key password.

Generate a dedicated upload/release key on a trusted machine and keep an encrypted offline backup. Example (choose a long unique password; do not paste it into chat or commit it):

```bash
keytool -genkeypair -v -keystore rofof-release.jks -storetype JKS \
  -keyalg RSA -keysize 4096 -validity 10000 -alias rofof-release
base64 -w 0 rofof-release.jks
```

Copy the base64 output directly into the `ROFOF_KEYSTORE_BASE64` secret. Add the three password/alias values as separate secrets. Do not upload the keystore file to GitHub, issues, artifacts, or this repository.

## Build and verification

After all four secrets are configured, open Actions → **ROFOF Signed Release APK** → Run workflow. The workflow:
1. Fails early if any required secret is missing.
2. Decodes the keystore into the ephemeral runner and validates the alias/password.
3. Builds the minified `assembleRelease` variant using the configured release signing config.
4. Checks ZIP alignment with `zipalign`.
5. Verifies the APK signature and prints certificate details using `apksigner` and `keytool`.
6. Tests the APK ZIP structure and publishes the APK, certificate verification output and SHA-256 checksum as a 30-day artifact.

No emulator is started or required. The workflow is manually runnable and also runs for version tags matching `v*`.

## Important

- The signing key is a long-lived app identity. Back it up securely and reuse the same key for future upgrades; losing it can prevent updates to installs signed with it.
- A successful build is not a Play Store publication. This workflow uploads a verified APK artifact only.
- Do not change or rotate the key casually. Keep the private key and passwords out of source control.
- A new key creates a new signing identity; it cannot update an installation signed with a different key unless the platform's signing-key upgrade path is deliberately configured.
