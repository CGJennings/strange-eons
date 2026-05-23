# Changelog

## 2026-05-22

- Migrate stored `theme` / `dark-theme` user preferences pointing at the
  removed `ca.cgjennings.ui.theme.YuggothTheme` over to `DagonTheme`
  (which absorbed Yuggoth in 3b2d33d). Without this, users upgrading a
  3.3 install to the 3.5 beta silently dropped from their chosen dark
  theme to the default Hydra fallback because `Class.forName` failed in
  `ThemeInstaller.instantiateTheme`. Also fixes `CatTheme`'s hardcoded
  in-code default for `dark-theme`, which still pointed at
  `YuggothTheme` and so seeded the prefs UI with a dead class name when
  no override was set.

## 2026-05-01

- Share the Rhino standard-objects scope across all `SEScriptEngine`
  instances. Issue #16. Each engine previously called
  `new ImporterTopLevel(cx, false)` plus `Globals.defineIn` in its
  constructor, building a fresh standard-objects scope and parallel
  reflection cache (~80 wrapped Java classes / ~8k method wrappers /
  ~10k `MemberBox` per scope). On a typical AHLCG-deck open the boot
  path constructs ~48 engines, so most of those reflection wrappers
  were dead-but-uncollected, inflating pre-GC counts ~10×. Factory
  static init now builds a single `SHARED_TOP` once. Each engine
  constructs an empty per-engine `ImporterTopLevel` whose prototype
  is `SHARED_TOP`; the per-eval `BindingsScriptable` sets its
  `parentScope` to that per-engine top so `ImporterTopLevel.realScope`
  walks to the per-engine instance, keeping `importClass` /
  `importPackage` mutations isolated per engine while standard
  prototypes, `Globals` functions, and the `ClassCache` are inherited
  via the prototype chain. Verified on the same repro: post-GC
  `NativeJavaClass` reduced from 1,059 to 284 pre-GC (−73%);
  `NativeJavaMethod` 59,289 → 8,324 (−86%); `MemberBox` 76,825 →
  10,060 (−87%); `EmbeddedSlotMap` 80,832 → 19,049 (−76%).
  `DIY.recycleScriptMonkey` no longer reflects against the engine's
  `topLevel` to drop user-defined slots (per-engine state lives only
  in the cleared `ENGINE_SCOPE` bindings).
- Bumped the bundled spelling library to `1.1` with a leak fix in the
  Swing components. Issue #19. The third-party spelling-1.0 jar
  registered an anonymous `SpellingCheckerChangeListener` on
  `SpellingChecker.getSharedInstance()` for every `JSpellingTextField` /
  `Area` / `ComboBox` constructed via `Utilities.createTokenizer`, but
  never deregistered it. Entries are stored in a
  `ArrayList<SoftReference<SCCL>>`, but each soft reference's referent
  strongly retains the tokenizer, the `JTextComponent`, and (through
  the parent hierarchy) the editor and DIY game component. `GC.run`
  doesn't clear soft refs unless the JVM is under memory pressure, so
  closed editors stayed reachable — on a real project-tree open / close-
  all repro on the AHLCG plug-in, ~12 per-DIY `ScriptMonkey` engines
  stayed pinned post-close. The 1.1 build is rebuilt from a forked
  spelling source tree (decompiled); `Utilities.createTokenizer` now stores
  the listener on the JTextComponent as the
  `"SpellCheckChangeListener"` client property, `Utilities.deregister`
  removes it, and `JSpellingTextField` / `JSpellingTextArea` override
  `removeNotify` to call `deregister` on detach. `JSpellingComboBox`
  inherits the cleanup via `SpellingComboBoxEditor extends
  JSpellingTextField`. Verified on the same repro: post-close
  `ScriptMonkey` count returns to baseline (1) instead of 13;
  `NativeJavaMethod` / `MemberBox` / `EmbeddedSlotMap` deltas drop to
  single digits over baseline.
- Strip JS-proxy listeners from the editor's own `listenerList` on
  dispose so the per-DIY `ScriptMonkey` engine becomes reclaimable
  even when an unrelated retention path (e.g. the third-party spelling
  library's listener cache) keeps the editor reachable. Issue #19.
  Plug-in JavaScript registers heartbeat / field-population listeners
  on `AbstractGameComponentEditor` via `uibindings.js`'s
  `bindings.bind()`. Each listener is a Rhino-generated `Proxy` whose
  `InvocationHandler` captures the engine's top-level scope through a
  `NativeCall` closure. Until now, only the Swing-children pass added
  in #14 (`stripJSProxyListeners(this)`) cleared JS proxies — the
  editor's own `listenerList` was untouched, so any path
  `... → editor → listenerList → JSProxy → NativeCall → engine` kept
  the engine alive. Issue #14's repro happened not to exercise that
  path, but a real project-tree / close-all flow on the AHLCG plug-in
  did: ~12 per-DIY engines stayed pinned post-close, each ~10 MB of
  `NativeJavaMethod` / `MemberBox` / slot-map machinery. The new
  `stripJSProxyListenersFromSelf` walks `listenerList`'s
  `[class, listener, ...]` array and removes every entry whose value
  is a JS proxy, regardless of the listener interface. Verified on
  the same repro: post-close `ScriptMonkey` count returns to baseline
  (1) instead of 13; `NativeJavaMethod` / `MemberBox` /
  `EmbeddedSlotMap` deltas drop to single digits over baseline.
- Stop the editor from immediately marking saved components dirty when a
  plug-in adds a new UI binding to a component type. Reproduced on AHLCG
  Chaos / Act / Agenda / Location cards saved before the plug-in's
  November 2025 `flipSuffix` checkbox binding was added: opening such a
  card showed the unsaved-changes asterisk before any user interaction
  and prompted to save on close. Root cause: `Binding.prototype.update`
  in `uibindings.js` saw `oldValue == null` (setting absent on disk) vs.
  `newValue == '0'` (checkbox default) on the first heartbeat after
  open, wrote the setting, and called `markChanged`. `initComponent` now
  seeds null-valued settings with the control's read-back default during
  field population so the next `update()` finds equality and the
  component stays clean. `Settings.set` does not itself dirty the
  component, so the seed is silent.

## 2026-04-30

- Memoized `Settings.textStyle` `{...}` literal evaluation. Each
  literal previously constructed a fresh `ScriptMonkey` (full
  `SEScriptEngine` + bootstrap) just to evaluate a small expression
  like `{ResourceKit.bodyFamily}`. Telemetry on a 12-component AHLCG
  deck open showed 5403 calls to only 73 distinct source strings
  (98.6% hit rate) and zero `Scriptable` results — every result was a
  plain Java value (`String`, `Double`, `Float`). Caching the result
  by source text is sound: only non-`Scriptable` results land in the
  cache, so a `NativeObject` (which would carry a parent-scope
  reference and re-introduce the issue #7 retention pattern) falls
  through to the slow path. Combined with the compile cache:
  - `ScriptMonkey` constructions per deck open: 5422 → **92** (59×).
  - Total script evals: 38126 → **816** (47×).
  - Style-literal-specific evals: 10806 → **146** (74×).
  - Cumulative `ScriptMonkey` construction time: ~5400 ms → **293 ms**.
  - JFR HashMap activity (downstream of slot-map churn): ~22% →
    **<2%** of CPU samples.
  - Hot CPU now dominated by `jj2000` JP2 image decode + Java2D
    rendering — irreducible work for drawing card thumbnails.
  - This is a tactical fix; the literal-evaluator path is removed
    entirely in the planned JS-extraction project (phase 5.1).
- Released per-DIY JavaScript engines on editor close (issue #14). After
  the issue #6 grace timer fix and the issue #7 retention sweep, the
  dominant remaining post-close retention was 10+ per-DIY Rhino engines
  per session (~7,900 `NativeJavaMethod` / 7,900 `MemberBox` / 7,000
  `EmbeddedSlotMap` each), kept alive by Rhino-proxy listeners that
  plug-in JavaScript installs on Swing components in the editor's UI.
  When the editor disposes, those proxies were still on the components'
  listener lists, and each proxy's invocation handler captured the
  entire per-DIY engine via `NativeCall` closure state.
  - `AbstractGameComponentEditor.dispose` now walks its component tree
    and removes any listener (any `EventListener` subtype) whose
    implementation is a Rhino-generated `Proxy` whose invocation
    handler comes from `org.mozilla.javascript`. SE-internal listeners
    are untouched. Detected via `Proxy.isProxyClass` +
    `Proxy.getInvocationHandler().getClass().getName()`.
  - `DIY` gains a `recycleScriptMonkey()` that drops the per-component
    engine reference, replaces the `Handler` with a no-op stub
    (`NOOP_HANDLER` returns blanks for `paintFront/paintBack/...`,
    zero portraits, etc.), clears engine bindings, and deletes the
    Rhino top-level scope's user-defined slots. If an external
    listener proxy still keeps the engine reachable, its in-engine
    footprint becomes empty.
  - `Sheet.freeCachedResources` now also nulls `cacheOnPaintMonkey`
    and its source, so the per-Sheet ON_PAINT engine releases too.
    The lazy re-creation path in paint already handles
    `cacheOnPaintMonkey == null`.
  - Hooked into `AbstractGameComponentEditor.dispose`: after stripping
    JS-proxy listeners, recycle the `GameComponent`'s engine if it's
    a `DIY`. Trade-off: a recycled DIY rendered later (clipboard
    preview, undo/redo) shows a blank face — acceptable because the
    alternative is keeping the entire engine state alive indefinitely.
  - Verified delta on a 10-editor close + project-close repro:
    `NativeJavaMethod` 96,478 → 8,000 (-92%), `MemberBox` 115,767 →
    9,649 (-92%), `EmbeddedSlotMap` 106,899 → 9,196 (-91%), heap-dump
    size 363 MB → 263 MB. The `BufferedImage` count drops less
    dramatically because most surviving images are the AHLCG plug-in's
    intentional preloaded asset cache stored in
    `StrangeEons.namedObjects` (out of scope for SE core).
- Process-wide compiled-script cache for the JSR-223 engine. Each
  `ScriptMonkey` previously created a fresh `SEScriptEngine` and
  re-parsed/re-compiled the same handful of bootstrap libraries
  (`<library bootstrap>`, `common`, `backwards-compatibility`,
  `imageutils`, etc.) on every component, sheet, and tile. New
  `CompiledScriptCache` keys compiled `Script` objects by SHA-256 of
  source plus filename, optimization level, and warnings-as-errors
  flag, and `SEScriptEngine.eval(Reader, ScriptContext)` now reads the
  source, looks up the cache, and execs the cached `Script` instead of
  going through `Context.evaluateReader`. Measured on the Arkham Horror
  LCG plug-in:
  - **Boot** (88 ScriptMonkeys): bootstrap-library re-eval down from
    1907 ms to 326 ms (5.8×). `common.js` 645→96 ms, bootstrap
    791→196 ms, backwards-compatibility 328→22 ms, imageutils
    143→12 ms.
  - **Single-component open** (271 new ScriptMonkeys): bootstrap
    re-eval down from 4290 ms to 172 ms (25×). Average ScriptMonkey
    construction time 7 ms → 1.4 ms (5×).
  - Cache hit rate stabilizes at 97% with 73 unique entries on this
    plug-in mix.
  - New `ScriptEngineMetrics` class is gated on
    `-Dstrange-eons.script-perf-log=true` (zero overhead when off);
    optional file output via `-Dstrange-eons.script-perf-file=<path>`.
  - No public API change. Shared top-level scope (heap-footprint win)
    deferred to a follow-up.
- Plugged five GameComponent post-close retention paths and added
  on-demand recycling for scripted-plugin engines (issue #7). Each path
  was a stale reference into a closed editor's component tree that, via
  Sheet → DIY → per-DIY `ScriptMonkey`, anchored Rhino's
  `JavaMembers` / `NativeJavaMethod` / `MemberBox` / slot-map graphs:
  - **`StrangeEons.lastTarget`** is now a `WeakReference<Object>`. The
    field tracks the most-recent valid markup target; previously it
    pinned the `JSpellingTextField`'s parent editor for the rest of the
    session.
  - **`MarkupTargetFactory` static cache removed.** `cachedComponent`
    and `cachedTarget` were a single-slot strong-ref pair pinning one
    editor at a time. The wrapper is cheap enough to recreate on each
    `createMarkupTarget` call; no caller depends on identity equality
    of the returned target.
  - **`ContextBar.cachedContext` cleared eagerly on target change.**
    Previously refreshed only lazily in `getContext()`; if no caller
    invoked `getContext()` between editor close and the next markup
    target arriving, the stale `Context` (and its `target` text field)
    persisted.
  - **`Settings.tlStyleEvaluator` ThreadLocal removed.** The cached
    style-literal `ScriptMonkey` accumulated cross-engine scope
    references in its bindings (a Rhino closure registered as a JS
    proxy adapter on a Swing component would link the style evaluator
    to a per-DIY engine), pinning per-DIY `ScriptMonkey`s for the
    lifetime of the thread. `nextStyle` now creates a fresh evaluator
    per `{...}` literal. Note: re-importing packages on each call is
    measurably slower on tab-switch with many style settings; the whole
    JS literal-evaluator path is removed in the JS-extraction project's
    phase 5.1.
  - **`PortraitPanel` now overrides `removeNotify`** to deregister
    itself from the shared `FileChangeMonitor`. The deprecated
    `finalize()` cleanup is gone — `removeNotify` is the proper Swing
    lifecycle hook and runs deterministically when the panel detaches
    from its editor.
- New plug-in lifecycle for `DefaultScriptedPlugin`: the engine is
  lazy + recyclable. The `monkey` field is now nullable; readers go
  through `requireMonkey()` which lazily re-instantiates a
  `ScriptMonkey` and re-evaluates the plug-in script (using a saved
  `lastBoundContext`). New `recycleScriptMonkey()` drops the engine
  on demand for internal callers (no user-facing trigger today).
  `unloadPlugin` now permanently nulls the engine and locks
  `scriptEvalsOK` to prevent re-init.
- Swept three reflective JDK-internals shims (issue #1):
  - `StyleUtilities.getWindowOpacity` no longer falls back through
    `com.sun.awt.AWTUtilities` (removed in Java 9). It now calls
    `Window.getOpacity` directly.
  - `OSXAdapter` (reflective `com.apple.eawt.Application` wrapper) is
    deleted. `AppFrame.installMacOsDesktopHandlers` already wires
    About/Preferences/Quit/OpenFile through `java.awt.Desktop`; nothing
    in tree referenced the old wrapper.
  - `OSXHelper` no longer reflectively copies Aqua's menu UI delegates
    onto Flatlaf. Flatlaf styles macOS menus competently on its own.
    The helper is reduced to setting `apple.laf.useScreenMenuBar` and
    arming `MnemonicInstaller`; `finish()` is now a no-op (still
    invoked reflectively by `ThemeInstaller`).
- Dropped the `--add-opens=java.desktop/com.apple.laf=ALL-UNNAMED`
  carve-out from the `app` exec profile and all three jpackage
  profiles. To keep `TchoTchoTheme` working on macOS (it returns
  `UIManager.getSystemLookAndFeelClassName()`, which is
  `com.apple.laf.AquaLookAndFeel`), `ThemeInstaller.installImpl` now
  routes JDK-module LaFs through `UIManager.setLookAndFeel(String)` so
  the instantiation happens with `java.desktop`'s own module
  privileges. LaFs in our unnamed module (e.g. `StrangeNimbus`) keep
  the original direct-reflection path and pre-install hook ordering.
  `Theme.modifyLookAndFeelDefaults` / `modifyLookAndFeel` Javadoc was
  updated to describe the per-path timing.
- Friendlier failure when an `.eon` file is corrupt or truncated
  (issue #5, alpha-tester report). `ResourceKit.getGameComponentFromStream`
  now catches `EOFException` / `OptionalDataException` separately from
  the generic `Exception` bucket and surfaces a localized
  "the file appears to be corrupt or incomplete" dialog (new
  `app-err-corrupt` resource key) instead of a raw Java stack trace.
  Stack trace stays in the log at INFO. Existing magic-bytes and
  generic-error paths are unchanged. Also lowered
  `ImagePreviewer.LoaderThread`'s catch-all from INFO to FINE so
  best-effort thumbnail-preview failures don't pollute the default
  log.
- Scoped the CI workflow's `push` trigger to `main`. Pushes to feature
  branches no longer fire CI; the PR run is now the single source of
  truth. Previously the unscoped `push` trigger ran on every branch and
  the subsequent `pull_request` trigger duplicated the work, with the
  branch run briefly showing "all checks passed" before the PR run had
  started. Manual `workflow_dispatch` and tag-driven release/sync flows
  unchanged.

## 2026-04-29

- Cut over the default update catalog URL from `strangeeons.fizmo.org`
  to `strangeeons.org` (`default.settings` `catalog-url-1`, plus the
  in-app download URI in `UpdateMessage.getDownloadURI`). The catalog
  is now driven by the new
  [`strange-eons-registry`](https://github.com/christophercurrie/strange-eons-registry)
  repo, which fetches plugin bundles hourly and rehosts them on
  OpalStack alongside `manifest.json`, `updates/catalog.txt`, and the
  downloads index. Existing alpha installs continue to work via a 301
  redirect from `strangeeons.fizmo.org` to `strangeeons.org` running
  on the OpalStack side; that redirect is invisible to the in-app
  catalog fetcher.

## 2026-04-28

- Removed wait-cursor toggles around the synchronous render path in
  `Sheet.paint` and around the heartbeat-driven re-render in
  `AbstractGameComponentEditor.onHeartbeat`. The toggles flashed a brief
  spinner during normal editing (every heartbeat tick after a content
  change) and during tab-reselect after the issue #6 raster release;
  they were previously masked when the open path's outer wait cursor
  was active, so the flash was only visible during in-editor activity.
  The renders themselves still happen on the EDT — see issue #9 for
  the proper background-thread treatment.
- Bumped the issue #6 raster-release grace timer from 1500 ms to 60 s.
  1.5 s was aggressive enough that switching between tabs in a typical
  multi-card workflow forced a full re-render on every reselect; 60 s
  preserves the memory savings for genuinely inactive editors while
  keeping normal tab-switching instant.

## 2026-04-27

- Released sheet rasters for inactive game-component editors to cut
  working-set memory. With many editors open, `BufferedImage` pixel
  buffers held by background editors' sheets dominated heap retention
  (per profiling on `chore/memory-profiling`: ~1 GB of `int[]`,
  ~99% traceable to live Sheet buffers via JFR `OldObjectSample`).
  `AbstractGameComponentEditor` now registers an `EditorListener` that,
  on editor deselect, schedules a 1500 ms grace timer; if the editor
  remains inactive past the grace window the timer calls
  `Sheet.freeCachedResources()` on each sheet and a new
  `SheetViewer.releaseCachedImage()` on each viewer (which nulls the
  viewer's `lastImage` cache that was independently retaining the
  rendered output). On reselect the timer is cancelled and
  `redrawPreview()` re-renders lazily on next paint. The grace timer
  absorbs rapid tab-switching. Verified delta on a 225-component
  project with ~10 editors open: `int[]` dropped from 931 MB to 252 MB
  (-680 MB), heap used 1920 MB → 906 MB (-1 GB), 35,849 → 24,961
  instances. The listener is registered against `AppFrame` rather than
  `this` because per-editor `JComponent.listenerList` does not
  reliably deliver `EditorListener` events under `JInternalFrame`.
- Fixed a macOS bug where Cmd-key menu accelerators (Cmd+S, Cmd+V,
  Cmd+Shift+S, etc.) silently did nothing unless the corresponding menu
  was being actively tracked. With `apple.laf.useScreenMenuBar=true`,
  Cocoa's native key-equivalent dispatch silently drops accelerators for
  non-Aqua menu UIs (FlatLaf), so the JMenuItem's Action never fires
  even though the keystroke does reach Java's
  `KeyboardFocusManager`. The existing keystroke dispatcher in `AppFrame`
  already finds the matching `JMenuItem` (to refresh its enabled state
  before the accelerator is processed), so it now also calls
  `match.doClick(0)` and consumes the event — but only on macOS and only
  when the matched item is enabled. `JMenuBar.isSelected()` cannot gate
  this on Mac because the Apple `ScreenMenuBar` bridge reports the bar
  as permanently selected after the first menu interaction.

## 2026-04-26

- Fixed a memory leak in `AbstractGameComponentEditor`: the `pcl`
  `PropertyChangeListener` was registered against
  `StrangeEonsAppWindow.VIEW_BACKDROP_PROPERTY` in the constructor but
  `dispose()` tried to remove it under `VIEW_QUALITY_PROPERTY`. Because the
  property names did not match, the listener was never actually removed and
  every editor ever opened stayed pinned in the long-lived `AppFrame` listener
  list, transitively retaining the editor's sheets, viewers, and game
  component (including portrait images). This explains the long-reported
  pattern of Strange Eons growing slower with each `.eons` file opened.
- Added JUnit 5 (5.10.2) and Mockito (5.18.0) as test dependencies, plus the
  Maven Surefire plugin (3.2.5). Mockito needs
  `-Dnet.bytebuddy.experimental=true` because Byte Buddy 1.17.x does not yet
  officially support JDK 25 bytecode.
- Added `AbstractGameComponentEditorTest`, a regression test that verifies
  the property name passed to `addPropertyChangeListener` in the constructor
  matches the one passed to `removePropertyChangeListener` in `dispose()`.
  Note: the test cannot run under `java.awt.headless=true` because
  `PlatformSupport.<clinit>` calls `Toolkit.getMenuShortcutKeyMask()`, which
  throws `HeadlessException`. Worth revisiting if we want CI to run tests
  headless.
