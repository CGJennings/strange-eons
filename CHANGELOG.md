# Changelog

## 3.5.1

- Register `.eon` and `.se*` file associations on the macOS, Windows, and Linux installers, which the jpackage migration had dropped.
- Restore the Script Debugger (`sedebug`) launcher on all platforms and the Windows console launcher (`eons.exe`).
- Note in installer config that the macOS bundle identifier and Windows MSI upgrade GUID changed between install4j and the v3.5.0 jpackage build; upgrading from a pre-3.5 install4j build is a fresh install on both platforms.
- Known limitation: jpackage builds no longer ship the user-editable `.vmoptions` file the install4j builds did. To override JVM options, launch from the command line and prefix each option with `-J` (e.g. `strangeeons -J-Xmx8g`).

## 3.5.0

- Migrate stored YuggothTheme preferences to DagonTheme on upgrade.
- Reset remembered window bounds that no longer fit any attached display.
- Tag the catalog cache by source URL so changing the catalog URL discards the stale cache.

## 3.5.0-beta4

- Combine all configured catalog sources into a single unified view (#3).

## 3.5.0-beta3

- Drop the FlatLight, FlatDark, and NoTheme entries added in beta2.

## 3.5.0-beta2

- Add FlatLaf Light/Dark, IntelliJ, Darcula, and NoTheme themes with per-theme chrome overrides.
- Sign macOS release builds in the publish pipeline (#20, #21).
- Cache plug-in icons during the info probe and null-guard `InstalledPlugin.getIcon()`.
- Resolve update-notification icons through the catalog mapping.
- Scope the release workflow to `v*` tags.

## 3.5.0-beta1

- Plug the post-close editor leak from a `pcl` listener name mismatch (#6).
- Release sheet rasters and viewer caches for inactive editors after a 60s grace (#6).
- Remove wait-cursor flashes around heartbeat-driven renders.
- Plug five remaining `GameComponent` retention paths (#7).
- Recycle per-DIY scripted-plugin engines and strip Rhino-proxy listeners on dispose (#14, #19).
- Upgrade the bundled spelling library to a 1.1 leak-fix build (#19).
- Share the Rhino standard-objects scope across all `SEScriptEngine` instances (#16).
- Add a process-wide compiled-script cache for the JSR-223 engine.
- Memoize JS `{...}` style-literal evaluation.
- Stop dirtying saved components when a plug-in adds a new UI binding.
- Friendlier error when an `.eon` file is corrupt or truncated (#5).
- Fix Cmd-key menu accelerators silently doing nothing under FlatLaf on macOS.
- Sweep reflective JDK-internals shims and drop the `--add-opens=com.apple.laf` carve-out (#1).
- Cut the default catalog URL over from `strangeeons.fizmo.org` to `strangeeons.org`.
- Scope the CI `push` trigger to `main`.
- Add JUnit 5 + Mockito and a regression test pinning the `pcl` property-name match.
