# Packly Google Drive cloud sync setup

Packly uses Google Identity Services with the Google Drive `appDataFolder` scope. The Android app syncs a private JSON snapshot named `packly_snapshot.json` in the user's own Google Drive app-data space. Do not use service accounts, shared server credentials, committed keystores, refresh tokens, or client secrets.

## Manu's Google Cloud values

Create or choose a Google Cloud project, enable the Google Drive API, configure the OAuth consent screen, then create Android OAuth clients for Packly.

Required values to hand back through the agreed config/secret channel:

- Google Cloud Project ID.
- OAuth consent publishing state: Testing or Production.
- Support email and developer contact email shown on the consent screen.
- Debug Android OAuth client ID for package `com.gusanitolabs.packly`.
- Release/Play Android OAuth client ID for package `com.gusanitolabs.packly`.
- SHA-1 fingerprint registered for each Android OAuth client and which signing key/build it belongs to.
- Confirmation that Google Drive API is enabled.
- Confirmation that the OAuth scope `https://www.googleapis.com/auth/drive.appdata` is added.

Do not send access tokens, refresh tokens, service-account JSON, keystores, or client secrets. Android OAuth clients normally do not have a client secret.

## Local and CI config keys

The code reads these non-secret values from Gradle properties first and environment variables second:

- `packly.google.androidClientId` or `PACKLY_GOOGLE_ANDROID_CLIENT_ID`
- `packly.driveSyncEnabled` or `PACKLY_DRIVE_SYNC_ENABLED`

Example local `local.properties` entries, not committed:

```properties
packly.google.androidClientId=000000000000-androidclient.apps.googleusercontent.com
packly.driveSyncEnabled=true
```

Keep `packly.driveSyncEnabled=false` until the Google Cloud project and Android OAuth client are ready for manual APK validation.

## Drive storage contract

```text
appDataFolder:/packly/
  packly_snapshot.json
```

The snapshot contains:

- `PacklyAppDocument` schema version and all local-first data: items, lists, trips, categories, settings, and session state.
- Cloud metadata: monotonically increasing revision, device ID, dirty flag, upload/import revisions, outbox operation, and tombstones for app-side archive/delete events.
- Snapshot metadata: app package `com.gusanitolabs.packly`, generated timestamp, Drive root, snapshot filename, revision, and last modified device.

The app requests only `https://www.googleapis.com/auth/drive.appdata`, so the file is private to Packly and not user-visible in Drive.

## Local-first safety policy

- Read cloud before upload when Drive is available.
- Never overwrite a newer cloud snapshot with a stale or empty local document.
- On first install with cloud data, import before uploading local seed data.
- Merge local and remote documents by entity ID and `updatedAt` for items, lists, and trips.
- Treat archive/delete actions as tombstones so missing local state is not confused with a fresh install.
- If authorization needs user interaction, report blocked state instead of pretending sync succeeded.
- If network fails, keep local dirty/outbox state and surface retryable offline status.
- If the selected Google account changes later, block with `AccountChanged` until a follow-up UX explicitly confirms import/replace behavior.

## What remains after this scaffold

- Wire an ActivityResult flow for Google authorization resolutions when Play Services asks for user interaction.
- Validate OAuth consent on a signed APK whose SHA-1 is registered in Google Cloud.
- Add WorkManager for automatic background retries once product timing is decided.
- Add GitHub Actions JVM/fake-provider tests for merge policy; do not require real Google credentials in CI.
