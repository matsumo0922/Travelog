# Setting Up a Project from `InitialProjectStructure.json`

This document is a checklist for rebuilding a new project from an `InitialProjectStructure.json` template and rebranding it for a new application (e.g. renaming the original "Grabee" app to "Koto").

## Prerequisites

- `python3`, `curl`, and `jq` available on the PATH
- `keytool` (ships with the JDK) for generating keystores if the template provides only debug credentials
- Android SDK installed locally (record its path for `sdk.dir`)
- Kotlin/Gradle toolchain provided by the repo (`./gradlew`)

## Step-by-step

1. **Hydrate the template**
   - From the repo root run a short Python script that parses the JSON and writes each `files` entry to disk:
     ```bash
     python3 - <<'PY'
     import json
     from pathlib import Path

     root = Path('.')
     with open('InitialProjectStructure.json', 'r', encoding='utf-8') as f:
         data = json.load(f)

     for raw_path, info in data['files'].items():
         path = raw_path.lstrip('/')
         target = root / path
         target.parent.mkdir(parents=True, exist_ok=True)
         if info.get('type') == 'content':
             target.write_text(info.get('content', ''), encoding='utf-8')
     PY
     ```
   - Download any binary artifacts listed in the JSON (e.g. Gradle wrapper jar, debug keystore) with the provided URLs:
     ```bash
     curl -L -o gradle/wrapper/gradle-wrapper.jar <url-from-json>
     curl -L -o gradle/keystore/debug.keystore <url-from-json>
     ```
   - Mark the wrapper executable: `chmod +x gradlew`.

2. **Rename packages and identifiers**
   - Replace the original package prefix in directory names (`me/matsumo/grabee`) with the new prefix (`me/matsumo/koto`). A quick Python pass can rename directories recursively.
   - Perform a text search/replace for `me.matsumo.grabee` → `me.matsumo.koto` across Kotlin, Gradle, and resource files.
   - Update the application entry points:
     - Rename `GrabeeApplication` → `KotoApplication` and fix references (manifest `android:name`, Koin wiring).
     - Rename the root composable `GrabeeApp` → `KotoApp` and adjust call sites (MainActivity, iOS entry point, etc.).
     - Rename the theme `GrabeeTheme` → `KotoTheme` and update imports/usages.
   - Change any variant display names or strings that expose the old branding (e.g. Gradle `onVariants` logic, `app_name` resource, marketing copy).
   - Update Firebase config (`google-services.json`) package ids and, if needed, the project metadata.

3. **Set project metadata**
   - Adjust `settings.gradle.kts` so `rootProject.name` matches the new app name.
   - Ensure the shared Android application plugin sets `applicationId = "me.matsumo.koto"`.

4. **Configure signing and SDK access**
   - If the template expects a release keystore, either supply an existing one or generate a placeholder:
     ```bash
     keytool -genkeypair -v \
       -keystore gradle/keystore/release.keystore \
       -alias <alias> -keyalg RSA -keysize 2048 -validity 10000 \
       -storepass <storePass> -keypass <keyPass> \
       -dname "CN=<App>, OU=Dev, O=<Org>, L=<City>, S=<State>, C=<CC>"
     ```
   - Create/update `local.properties` with:
     ```properties
     sdk.dir=/absolute/path/to/Android/sdk
     storePassword=<storePass>
     keyPassword=<keyPass>
     keyAlias=<alias>
     ```
     (replace with real secrets before release).

5. **Run and verify the build**
   - Execute `./gradlew clean` followed by `./gradlew build`.
   - Resolve any static analysis findings (e.g. Detekt `UnusedParameter` warnings) using targeted fixes or `@Suppress` annotations where appropriate.
   - Re-run the build to confirm it is fully green.

6. **Final checks**
   - Search for leftover references to the original app:
     ```bash
     rg "Grabee" --glob '!**/build/**'
     rg "me.matsumo.grabee" --glob '!**/build/**'
     ```
     Both commands should return no results.
   - Confirm generated artifacts (APKs, bundles) use the new identifiers by inspecting `build/outputs` if necessary.

## Notes

- Keep `InitialProjectStructure.json` out of version control if it is only a bootstrap artifact.
- Replace placeholder Firebase project IDs/keys with the actual ones before distribution.
- When reusing this checklist for a different app name, substitute the relevant identifiers accordingly.
