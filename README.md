# Strange Eons

Strange Eons is a powerful tool for creating and customizing
components for paper-based games. It started out as a research tool
for investigating human creativity, but has since grown into a
comprehensive toolset for game enthusiasts and creators.

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
[Strange Eons website](https://strangeeons.cgjennings.ca/).
A [user manual and other documentation are available](https://se3docs.cgjennings.ca/index.html)
if you would like help getting started.

This GitHub page is for developers and other experienced users who
want to try the latest features or contribute to the project.
The rest of this page assumes that description applies to you.

## Building Strange Eons from source

### Prerequisites

These are the tools you'll need to build Strange Eons from this repository:

1. **Git** (to clone the repository). If you are new to Git,
you may find the [GitHub Desktop](https://desktop.github.com/) client
easier to use than the command line.
(Your IDE might also have built-in support for Git.)

2. **Java 11 JDK**. The main branch is currently based on Java 11.
You can download a suitable JDK from
[Adoptium](https://adoptium.net/temurin/releases/?version=11).

3. **Maven**. Maven is used to build the project. Again, this may
already be included with your IDE if you use one. If not, you can
[install it separately](https://maven.apache.org/download.cgi).

To build the project from the command line, you can use the
following commands:

Build the project:
```bash
mvn compile
```

Start the main application:
```bash
mvn exec:exec -Papp
```

> This will run the application with an argument that causes it to
> skip the usual Java version checks. You may encounter errors
> or the application may not run at all if your default Java version
> doesn't match the version used to build the project.

Start the script debugging client:
```bash
mvn exec:exec -Pdebugger
```

To work with the project in an IDE, you will likely need to configure
"launch options" to run the project. In particular, you will need to
add a VM argument to enable a "Java agent". Refer to `.vscode/launch.json`
(under the property `vmArgs`) for the current recommended arguments.

Configurations for
[Visual Studio Code](https://code.visualstudio.com/) and
[NetBeans](https://netbeans.apache.org/)
are included in this repository:

**In NetBeans,** open the project (**File/Open Project**) then select and run it
(**Run/Run Project**, default key F6).

**In VS Code,** open the project folder (**File/Open Folder**) then select and
run the project (**Run/Run Without Debugging**, default key Ctrl+F5).

To build installers for deployment to Windows, macOS, and Linux, you can use:

```bash
node deployment/deploy.js
```

This relies on a commercial third-party tool,
[Install4J](https://www.ej-technologies.com/products/install4j/overview.html).
The script will guess at the location of Install4J, but if that fails
you can set the `INSTALL4J` environment variablet to the path to `install4jc`.

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
mvn package -Pdeploy
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