# About `build-tools`

This directory contains tools that support building Strange Eons.
These tools perform tasks that are only required intermittently; they do not need to be run every time a build is performed.

## Using the scripts

The tools require Node.js and the `npm` command; in addition,
a JDK must also be installed and available on the `PATH`.
Before you can run the tools the first time, you must install
additional dependencies with:

```
npm install
```
    
You can then run individual tools by name using `npm run [tool name]` where
`[tool name]` is one of the tools described below. To run all of the tools,
use:

```
npm run build
```

## `flex`: rebuild the custom tokenizer classes

This rebuilds the custom tokenizer classes used by the code editor.
The tokenizers are built using JFlex from the `*.flex` sources found
in `build-tools/tokenizers`. The resulting `*.java` files are
written to `strange-eons/src/ca/cgjennings/ui/textedit`.

## `ts`: update the TypeScript services library

This updates the TypeScript services library that is used to
transpile TypeScript code from within Strange Eons. It downloads
the latest version from Microsoft, minifies it, and packages it
as `strange-eons/lib/typescript-servcies.jar`.
