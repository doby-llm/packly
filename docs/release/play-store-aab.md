# Play Store signed AAB release checklist

Packly release bundles are generated only by GitHub Actions. Do not commit a
keystore, keystore password, key alias, or key password to this repository, and
do not paste private signing values into issues, pull requests, chats, or Kanban
comments.

## Current release milestone

The first milestone is a signed `.aab` artifact uploaded by GitHub Actions. Play
Console upload is still manual. Automated Play upload can be added later after a
limited-permission service account is created and stored as a separate GitHub
secret.

## Required GitHub Actions secrets

Create these repository secrets in GitHub under:

`Settings -> Secrets and variables -> Actions -> New repository secret`

| Secret name | Purpose |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded Packly upload keystore file (`.jks`). |
| `ANDROID_KEYSTORE_PASSWORD` | Store password for the upload keystore. |
| `ANDROID_KEY_ALIAS` | Alias of the upload key inside the keystore. |
| `ANDROID_KEY_PASSWORD` | Password for the upload key alias. |

## Manu setup steps

1. Decide that `com.dobyllm.packly` is the final package name before first Play
   publication. The package name is effectively permanent after Play release.
2. In Play Console, create the Packly app and enroll in Play App Signing. The
   recommended first-release flow is a Google-generated app signing key plus a
   separate Packly upload key.
3. Generate the upload keystore locally and keep the `.jks` file and passwords in
   a private/offline password manager or secure backup location.

   ```bash
   keytool -genkeypair -v \
     -keystore packly-upload.jks \
     -alias packly-upload \
     -keyalg RSA \
     -keysize 4096 \
     -validity 10000 \
     -storetype JKS
   ```

4. Export the upload certificate for Play Console registration when requested.

   ```bash
   keytool -export -rfc \
     -keystore packly-upload.jks \
     -alias packly-upload \
     -file packly-upload-certificate.pem
   ```

5. Base64-encode the keystore and copy the encoded text into the
   `ANDROID_KEYSTORE_BASE64` GitHub secret.

   Linux:

   ```bash
   base64 -w0 packly-upload.jks > packly-upload.jks.base64
   ```

   macOS:

   ```bash
   base64 -i packly-upload.jks | tr -d '\n' > packly-upload.jks.base64
   ```

6. Create the remaining GitHub secrets with the upload keystore password, upload
   key alias, and upload key password.
7. Run the manual release workflow in GitHub:

   `Actions -> Release AAB -> Run workflow -> main`

8. After the workflow succeeds, download the `packly-release-aab` artifact and
   upload the `.aab` to a Play Console internal testing track first.

   ```bash
   gh run list --workflow release-aab.yml --limit 10
   gh run download <RUN_ID> --name packly-release-aab --dir ./artifacts/release
   ```

## Release workflow behavior

`.github/workflows/release-aab.yml` runs only on `workflow_dispatch`. It performs
Gradle wrapper validation, Android lint, JVM unit tests, decodes the upload
keystore from GitHub Actions secrets into the runner's temporary directory, runs
`:app:bundleRelease`, and uploads `app/build/outputs/bundle/release/*.aab` as the
`packly-release-aab` artifact.

The workflow intentionally does not upload to Play Console. For the first release,
Manu should manually upload the artifact to internal testing, complete Data safety,
privacy policy, content rating, target audience, store listing assets, and then
promote only after validation.

## Versioning reminders

`versionCode` must increase monotonically for every Play upload. `versionCode = 1`
and `versionName = "0.1.0"` can be used for the first internal-test upload, but
future Play uploads must bump `versionCode` before running the release workflow.
