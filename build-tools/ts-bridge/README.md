# Java/TypeScript bridge script

## Overview
The Strange Eons (SE) package `ca.cgjennings.apps.arkham.plugins.typescript` contains
support code for transpiling TypeScript source to JavaScript that is compatible with
the SE plug-in API. On the Java side, the class `TSLanguageServices` is the entry point
for this support. This class creates a private script context that loads the TypeScript
compiler and language services, along with a *bridge script* that adapts calls and
datatypes from the Java side to the TypeScript library side and vice-versa.

This folder contains the source code, in TypeScript, for that bridge script.

## Building

To build the bridge script, you must transpile the TypeScript source to JavaScript
and copy the result to the SE `typescript` package (`src/ca/cgjennings/apps/arkham/plugins/typescript`).
This is trivial if you have Node.js installed. From this folder, run `npm install` to
install the needed dependencies. Then you can build the bridge script by running
`npx tsc` (use `npx tsc -w` to watch and automatically rebuild when the source changes).

## Type definitions

The SE classes, interfaces, and other type information that the bridge script uses
are given minimal definitions in `java-types.d.ts`. These definitions only
reflect the subset of the SE API actually needed by the bridge script.
*These definitions must be kept in sync with the Java source code.*