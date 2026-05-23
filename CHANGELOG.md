# Changelog

## 3.5.0

- Migrated stored `theme` / `dark-theme` user preferences pointing at the removed `ca.cgjennings.ui.theme.YuggothTheme` over to `DagonTheme`, so 3.3 installs upgrading to 3.5 keep their chosen dark theme instead of silently falling back to Hydra. Also fixed `CatTheme`'s hardcoded in-code default for `dark-theme`, which still named the dead `YuggothTheme` class.
- Discard remembered tool-window bounds that no longer intersect any attached display so windows last placed on a now-disconnected monitor reopen at their default position instead of off-screen.
- Tagged the plug-in catalog cache file with the source URL it was fetched from and skip the cache when the user's primary `catalog-url-1` no longer matches, so changing the catalog source picks up the new feed instead of serving the stale one.

## 3.5.0-beta4

- Plug-in catalog overlays every configured `catalog-url-*` plus any typed URL into a single unified view; same-UUID conflicts resolve to the newer date (ties to the lower-indexed source), and listing URLs are absolutized per source so installs route to the right host (issue #3).

## 3.5.0-beta3

- Removed the FlatLaf Light/Dark and `NoTheme` entries added in beta2 from the built-in registration; with the per-theme chrome overrides in place they were duplicative of IntelliJ/Darcula and the system L&F.

## 3.5.0-beta2

- Added FlatLaf Light/Dark, IntelliJ, and Darcula themes; added `NoTheme` that leaves Swing's default L&F in place; cleared the forced chrome overrides for FlatLaf, `NoTheme`, and the Tcho Tcho theme so each renders its own window decorations. Enabled `--enable-native-access` for FlatLaf so its native-decoration hooks load on JDK 25.
- Signed macOS release builds in the publish pipeline; fenced the signing-only shell steps off on Windows runners (issues #20, #21).
- Plugin Manager caches a plug-in's icon during the info probe and reuses it after the plug-in is stopped, and `InstalledPlugin.getIcon()` now guards against a null plug-in instance so disabled tool plug-ins render their icon instead of throwing.
- Update-notification dialog resolves catalog icon names through the same mapping the catalog UI uses, so notifications no longer fall back to the generic placeholder.
- Scoped the CI release workflow to `v*` tags and the `release` environment so feature-branch pushes no longer trigger a publish.

## 3.5.0-beta1

- Plugged the long-standing post-close memory leak: `AbstractGameComponentEditor` now removes its `pcl` listener under the property name it actually registered with, so closed editors no longer stay pinned in `AppFrame` (issue #6 root cause).
- Released sheet rasters and viewer image caches for inactive editors after a 60 s grace window, dropping working-set heap by ~1 GB on large projects (issue #6).
- Removed wait-cursor flashes around heartbeat-driven and synchronous renders so normal editing no longer flickers a spinner.
- Plugged five remaining `GameComponent` retention paths (weak-ref `lastTarget`, removed `MarkupTargetFactory` cache, eager `ContextBar` clear, dropped `Settings.tlStyleEvaluator` ThreadLocal, `PortraitPanel.removeNotify`) so closed editors stop anchoring Rhino reflection graphs (issue #7).
- Added on-demand recycling of per-DIY scripted-plugin engines plus stripping of Rhino-proxy listeners on editor dispose, releasing 10+ engines per session that previously stayed pinned (issues #14, #19).
- Bundled spelling library upgraded to a 1.1 leak-fix build that lets `JSpellingTextField`/`Area`/`ComboBox` deregister their shared-instance listeners on detach (issue #19).
- Shared the Rhino standard-objects scope across all `SEScriptEngine` instances, cutting `NativeJavaMethod`/`MemberBox`/slot-map counts ~85% on plugin-heavy boots (issue #16).
- Added a process-wide compiled-script cache for the JSR-223 engine: bootstrap-library re-eval drops 5–25× and component-open construction time drops ~5×, with 97% hit rate on real plug-in mixes.
- Memoized JS `{...}` style-literal evaluation so a typical AHLCG deck open creates 92 `ScriptMonkey` instances instead of 5,422.
- Stopped the editor from immediately marking saved components dirty when a plug-in adds a new UI binding to an existing component type.
- Friendlier failure message when an `.eon` file is corrupt or truncated, instead of a raw stack trace (issue #5).
- Fixed a macOS bug where Cmd-key menu accelerators (Cmd+S, Cmd+V, etc.) silently did nothing under FlatLaf with the screen menu bar enabled.
- Swept three reflective JDK-internals shims (`AWTUtilities`, `OSXAdapter`, Aqua menu UI copy) and dropped the matching `--add-opens=com.apple.laf` carve-out from all run/jpackage profiles (issue #1).
- Cut over the default update-catalog URL from `strangeeons.fizmo.org` to `strangeeons.org`, now driven by the new `strange-eons-registry` repo with a 301 redirect for legacy installs.
- Scoped the CI workflow's `push` trigger to `main` so feature-branch pushes no longer duplicate the PR run.
- Added JUnit 5 + Mockito as test dependencies and a regression test pinning the `pcl` property-name match.
