# Strange Eons 3

[Strange Eons](https://strangeeons.cgjennings.ca) is a popular tool for creating and customizing components for paper-based games.

Running under Java 1.8 is recommended at this time, [though it should also run under Java 9 to 16 with the right command line arguments](http://se3docs.cgjennings.ca/um-install-other.html). (If the Windows, macOS or Linux `.deb` installation package is used, a suitable private Java JRE is included.)

User documentation is managed in a [separate repository](https://github.com/CGJennings/se3docs).

## Hacking the code

Contributions (pull requests) are welcome. By making a pull request or otherwise contributing code or other data, you agree that your contribution can be combined with, and made available under the same [license terms](LICENSE.txt) as, the rest of the project.

### Tools

For non-trivial changes consider using the free [Apache NetBeans IDE](https://netbeans.apache.org/). Several interface elements (dialog boxes, etc.) use the NetBeans interface design tools, which insert (clearly marked) generated code into the class source files. Generated code **must not** be modified by hand, as any changes are overwritten automatically when the file is next opened in the design tool.

### There be dragons

I don't want to deter anyone from contributing, but I do want to give fair warning on a few points before you dive in:

Strange Eons began years ago as a simple editor for a single type of game card, with some features meant to help users find more creative designs. These early versions were developed to support research connected to my doctoral dissertation, and contain some of the first Java code I ever wrote. Although it has grown beyond its humble beginnings, as you might expect it contains some... sharp corners and unfortunate choices.

### Organization

Source code is found under `src`, organized into standard Java packages. Here is an outline:

`ca.cgjennings.*`  
Contains the bulk of the application code. The application proper is mostly found under `ca.cgjennings.apps.arkham.*`. (The name `arkham` is historic in origin; the original app created characters for a board game called Arkham Horror.)

`gamedata.*`  
Contains only those classes used to register new content with the application.

`resources.*`  
Contains binary resources, string tables, and the libraries that support scripted plugins. Also contains a few classes related to loading and managing those resources, such as `ResourceKit`, `Settings`, and `Language`.

## License information

Original source code in this repository is licensed under an [MIT license](LICENSE.txt). Third party components are licensed under the terms in the relevant source files and/or their description in the About dialog box.
