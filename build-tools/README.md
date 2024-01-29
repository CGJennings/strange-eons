# About `build-tools`

This directory contains tools that support building Strange Eons.
These tools generate source files or resources, so they only need
to run when their inputs change rather than every build.

## Using the tools

The tools require Node.js and the `npm` command; in addition,
a JDK must also be installed and the `JAVA_HOME` environment
variable defined to point to it.
Before you can run the tools the first time, you must install
additional dependencies with:

```
npm install
```

You can then run individual tools using the NPM command
listed under each tool's subsection below.

### `flex`: rebuild the custom tokenizer classes

```sh
npm run flex
```

This rebuilds the custom tokenizer classes used by the code editor.
The tokenizers are built using JFlex from the `*.flex` sources found
in `build-tools/tokenizers`. The resulting `*.java` files are
written to `src/main/java/ca/cgjennings/ui/textedit`.

### `ts`: update the TypeScript services library

```sh
npm run ts
```

This updates the TypeScript services library that is used to
transpile TypeScript code from within Strange Eons. It downloads
the latest version from Microsoft, minifies it, and packages it
as `lib/local/typescript-services/1.0/typescript-servcies-1.0.jar`.

## `ts-bridge`: Java ⇄ TypeScript services bridge

```sh
cd ts-bridge
npm run build
```

The `ts-bridge` subdirectory contains TypeScript source for the
JavaScript code that adapts calls to the `TSLanguageServices`
Java class into calls to the TypeScript services library.
The compiled JavaScript can be found in
`src/main/resources/ca/cgjennings/apps/arkham/plugins/typescript/java-bridge.js`.

To watch for changes and automatically rebuild on save:

```sh
cd ts-bridge
npm run watch
```