# Desktop consistency fixes on Vista 0.26.1

The initial test build used `main`, which still contained Vista 0.10.0. The user's existing 0.26.1 desktop is saved on `feature/vista-extended-taskbar` at `a9d004f3ac6088ad8eb83db9bff8dc9626b0aaf2`. This corrected patch starts from that existing version. Its normal package name, version code, wallpapers, icon artwork and window styling are preserved. The requested taskbar label fit and explicit Welcome Center navigation are applied to that established desktop.

## Requested changes

- Retains the existing classic Task Manager and adds its Services/Users navigation without replacing the window or taskbar integration.
- Adds an atomic, rebuildable app-name/icon cache. Cached inventories appear before worker refresh; package/locale/manual refresh invalidates the short memory cache. Failed scans retain a working inventory, and a first-scan failure leaves existing desktop state in place.
- Changes Welcome Center to **Restore a desktop** and places restoring first in Backup and Restore.
- Imports compatible backup settings, filters unsupported shortcuts and backgrounds, lists skipped items, preserves destination values for skipped settings, and retains restore preview and undo. New backup metadata identifies the variant and stock background names; older files and the historical Eight format remain supported.

The 0.26.1 backup schema is kept, including expanded taskbar, icon/grid, folder and window preferences. Installing or opening the launcher does not apply a desktop backup. File permission grants and the contents of linked files are not transferred by desktop backups.

## Validation

`python checks/CheckVistaBaseline.py` retains the original 0.26.1 hashes and verifies the unchanged identity, 97 existing runtime files, seven existing shell methods and every original icon artwork command. Explicitly pinned requested changes cover static icon caching, parsed model caching, label/grid geometry and work-area sizing; the original hashes remain in the manifest for review. In addition, the unit suite checks five source-family fixtures, unsupported items, background names, corruption rejection, restore/undo, existing expanded-taskbar preferences, cached icons, cold scan failure and Task Manager navigation.

Build and test with Java 17 / Android SDK 36:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
```

The instrumented test APK is compiled. Android device/emulator instrumentation has not been run in this workspace. Test APKs use this workspace's development signing certificate; updating an older handoff APK requires its matching private signing key. No pull request is merged without the user's individual approval.

## Extended taskbars and rendering caches

The desktop grid uses the actual work area above the taskbar. It measures each label before fitting the icon, keeps complete rows visible, and starts overflow rows outside the first viewport so they can be scrolled into view. Expanded taskbars temporarily fit saved positions without writing those coordinates back to preferences. The normal taskbar layout returns when the taskbar collapses. Larger system text participates in row sizing.

All five variants now share static built-in artwork in a bounded 4 MiB bitmap cache keyed by icon kind, pixel size, padding, density and UI mode. Existing drawing commands remain unchanged; live gadget faces are separate. Folder names, shortcut aliases and normalized preference lists reuse parsed snapshots until their raw saved data changes. Mutable folder/list return values remain independent so edits cannot accidentally corrupt the cache. Desktop placement plans are reused until dimensions, orientation, cutouts, shortcut membership or saved positions change. An unchanged in-memory app catalog no longer recreates the desktop and hidden Start contents on every Home/resume.

`LauncherRenderingTest` checks expanded taskbars with automatic/requested rows, two-line labels, larger system text, whole overflow rows, temporary saved-position fitting, geometry invalidation, mutable model safety, static icon reuse and bounded cache size. Phone/emulator timing and visual checks remain pending; the APKs are development-signed test builds.

Vista Start now has an explicit **Welcome Center** navigation item targeting the existing center. Its restore action remains **Restore a desktop**; startup preferences are preserved. `WelcomeNavigationTest` exercises the actual Start action.
