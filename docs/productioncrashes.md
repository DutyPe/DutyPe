# Production Crash Notes

## Release 61 Startup Crash

Observed in Firebase Crashlytics after publishing release 61 to production.
The app crashed immediately while opening on affected devices.

### Crash 1: AppCompat Attach Listener

```text
Fatal Exception: java.lang.AbstractMethodError
abstract method "void android.view.View$OnAttachStateChangeListener.onViewAttachedToWindow(android.view.View)"
on receiver java.lang.Class<androidx.appcompat.view.menu.CascadingMenuPopup$2>
android.view.View.dispatchAttachedToWindow
android.view.ViewGroup.dispatchAttachedToWindow
android.view.ViewRootImpl.performTraversals
android.app.ActivityThread.main
```

Interpretation:

- The failure happens while Android is attaching a view to the window.
- The receiver is an AppCompat internal popup menu listener.
- The exception shape matches an interface-method mismatch after R8 optimization or dependency/runtime mismatch.

Hotfix applied:

- Removed `-mergeinterfacesaggressively` from `app/proguard-rules.pro`.
- Added explicit AppCompat dependencies so the resolved runtime is stable.
- Bumped the hotfix release to `versionCode = 62`, `versionName = "2.6.10"`.

### Crash 2: Compose Draw Modifier

```text
Fatal Exception: java.lang.AbstractMethodError
abstract method "void androidx.compose.ui.node.DrawModifierNode.a(androidx.compose.ui.node.LayoutNodeDrawScope)"
on receiver java.lang.Class<androidx.compose.foundation.text.modifiers.TextAnnotatedStringNode>
```

Interpretation:

- This is another interface-method failure, this time inside Compose drawing.
- It supports the same root-cause direction: unsafe R8 interface merging or inconsistent optimized bytecode.

Hotfix applied:

- Same R8 safety change: removed aggressive interface merging.
- Kept Compose artifacts resolved by the Compose BOM.

### Crash 3: SQLCipher Local Database Open

```text
Fatal Exception: net.zetetic.database.sqlcipher.SQLiteNotADatabaseException
file is not a database (code 26), while compiling: SELECT COUNT(*) FROM sqlite_schema;
```

Interpretation:

- SQLCipher is trying to open a local Room database that is not readable with the current encrypted passphrase.
- Possible causes include old plain Room database files, corrupted local cache, or lost encrypted DB key material.
- This is local offline cache data, not server Firestore data.

Hotfix applied:

- Added guarded database-open verification in `AppModule`.
- If SQLCipher/SQLite open fails, the app deletes the local Room cache files and rebuilds the cache instead of crashing.

## Verification

- `./gradlew.bat --no-daemon :app:bundleRelease` passed for the hotfix.
- Generated release bundle: `app/build/outputs/bundle/release/app-release.aab`.
- Release size budget reported: AAB `17.33 MB`, compressed dex `4.56 MB`, one dex file.
