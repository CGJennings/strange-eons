import { exec, join, getArg, verbose, warn } from "./io.js";

let install4jBin = getArg("--install4j") || process.env["INSTALL4J_BIN"];
if (install4jBin) {
    install4jBin = `"${join(install4jBin, "install4jc")}"`;
} else {
    if (process.platform.startsWith("win")) {
        install4jBin = `"${process.env["PROGRAMFILES"]}\\install4j10\\bin\\install4jc.exe"`;
    } else if (process.platform.startsWith("darwin")) {
        install4jBin = `"/Applications/install4j9/bin/install4jc"`;
    } else {
        install4jBin = "install4jc";
    }
}

export function compileInstallers(deployDir, buildTag, platform) {
    if (platform === "other") return;
    if (platform == null ) {
        platform = "";
    } else {
        platform = {
            "": "",
            "windows": "--media-types=windows",
            "macos": "--media-types=macos,macosFolder,macosArchive,macosFolderArchive",
            "linux": "--media-types=linuxRPM,linuxDeb"
        }[platform];
        if (platform == null) {
            warn(`unknown platform ${platform}`);
        }
    }

    verbose("compiling installers");
    try {
        exec(`${install4jBin} --release=${buildTag} ${platform} build-installers.install4j`, { cwd: deployDir });
    } catch (ex) {
        warn("unable to compile installers", ex);
    }
}