# Strange Eons

Strange Eons is a powerful tool for creating and customizing
components for paper-based games. It started out as a research tool
for investigating human creativity, but has since grown into a
comprehensive toolset for game enthusiasts and creators.

[![CI](https://github.com/christophercurrie/strange-eons/actions/workflows/ci.yaml/badge.svg)](https://github.com/christophercurrie/strange-eons/actions/workflows/ci.yaml)

## Features

Strange Eons 3 is a free, open source tool for creating paper
game components, such as cards, tokens, boards, and boxes. It is
primarily intended to be used to create homebrew expansions for
existing games, but it can also be used to create entirely new
games from scratch.

Strange Eons is designed to let you focus on the creative aspects
of game design while it takes care of the technical aspects of
layout and design. It also has tools to support creativity, such as
"spinning off" designs, design guidance (where supported by the
component type), and a philosophy of not starting you on a "blank
page" but instead providing a starting point that you can modify to
suit your needs.

Strange Eons is scalable. You can create a single card, a whole
deck, or even an entire board game. Collections of cards and other
components can be laid out as printable decks, either manually or
automatically. You can design each component individually, or use a
factory to build a set of components from a template.

Strange Eons is extensible. It has a built-in development
environment and debugger for creating plug-ins. Plug-ins can be
used to add support for new games, component type, tools, commands,
and more.

## Getting started

Typical users can download and install Strange Eons from the
[Strange Eons download page](https://strangeeons.org/).
A [user manual and other documentation](https://se3docs.cgjennings.ca/index.html)
remain hosted on the original project's site if you would like help
getting started.

This GitHub page is for developers and other experienced users who
want to try the latest features or contribute to the project.
The rest of this page assumes that description applies to you.

## Building Strange Eons from source

### Prerequisites

These are the tools you'll need to build Strange Eons from this repository:

1. **Git** (to clone the repository). If you are new to Git,
you may find the [GitHub Desktop](https://desktop.github.com/) client
easier to use than the command line.
(Your IDE might also have support for Git, either built in or as an extension/plug-in.)

2. **JDK 25**. The main branch requires Java 25 or later.
You can download a suitable JDK from
[Adoptium](https://adoptium.net/temurin/releases/?version=25).

The project ships with the Gradle Wrapper (`gradlew`), so no separate
Gradle installation is needed.

To build the project from the command line, you can use the
following commands:

Build the project:
```bash
./gradlew :strange-eons-app:build
```

Start the main application:
```bash
./gradlew :strange-eons-app:run
```

Start the script debugging client:
```bash
./gradlew :strange-eons-app:runDebugger
```

> **Note:** The script debugging client is used to debug script code
> in Strange Eons, typically plug-in code. This does not start a Java
> debugging session to debug the main application. Do that as you
> normally would with your IDE.

See `build.gradle.kts` for additional tasks.

To work with the project in an IDE, import it as a Gradle project and
let the IDE drive the build through Gradle's tooling API. Any IDE that
understands Gradle should pick up the JDK 25 toolchain, the JUnit 5
test classpath, and the `:strange-eons-app:run` task automatically.

### Additional tools

If you want to contribute new code, you may also need the following:

1. **NetBeans**: This IDE is essential for UI modifications.
The NetBeans form editor was used to design most interface elements.
These are stored in `*.form` files alongside `*.java` sources.
NetBeans also generates code within Java source files, which should
not be manually altered as it's automatically overwritten upon
reopening the form in the design tool.

2. **Node.js** and **npm**. Tooling in `build-tools` uses Node.js. These are used to:
    - build the custom tokenizers used for syntax highlighting by the code editor
    - update the TS language service library that Strange Eons uses to edit and compile TypeScript code
    - compile and update the `java-bridge.ts` file that Strange Eons loads to communicate between Java and the TypeScript library

### Deployment packaging

You can build an all-in-one JAR file (equivalent to what is distributed
for "other" operating systems than Windows, macOS, and Linux amd64/arm64)
by running the following command:

```bash
./gradlew :strange-eons-app:shadowJar
```

The JAR file will be found at `deployment/assembled/strange-eons.jar`.



## Contributing

Contributions (pull requests) are welcome. By making a pull request
or otherwise contributing code or data, you agree that your
contribution can be combined with, and made available under the
same [license terms](LICENSE.txt) as, the rest of the project.

The user documentation is also open to contributions.
It can be found in the separate
[se3docs repository](https://github.com/CGJennings/se3docs).

*There be dragons...*
I don't want to deter anyone from contributing, but I do want to
give fair warning on a few points before you dive in:

Strange Eons began many years ago as a simple editor for a single
type of game card, with some features meant to help users find more
creative designs. These early versions were developed to support
research connected to my doctoral dissertation, and contain some of
the first Java code I ever wrote. Although it has grown beyond its
humble beginnings, as you might expect it contains some... sharp
corners and unfortunate choices.

### Organization

Source code is found under
[`src/main/java`](https://github.com/CGJennings/strange-eons/tree/main/src/main/java)
and
[`src/main/resources`](https://github.com/CGJennings/strange-eons/tree/main/src/main/resources),
organized into standard Java packages. Here is an outline:

`ca.cgjennings.*`
Contains the bulk of the application code. The application proper
is mostly found under `ca.cgjennings.apps.arkham.*`. (The name
`arkham` is historic in origin; the original app created characters
for a board game called Arkham Horror.)

`gamedata.*`
Contains only those classes used to register new content with the
application.

`resources.*`
Contains binary resources, string tables, and the libraries that
support scripted plugins. Also contains a few classes related to
loading and managing those resources, such as `ResourceKit`,
`Settings`, and `Language`.

## Change log/version history

See the release notes in the
[user manual](https://se3docs.cgjennings.ca/um-release-notes.html)
for a detailed history.

## License information

Original source code in this repository is licensed under an [MIT license](LICENSE.txt). Third party components are licensed under the terms in the relevant source files and/or their description in the About dialog box.
