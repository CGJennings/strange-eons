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

const MT_WIN = "--media-types=windows";
const MT_MAC = "--media-types=macos,macosFolder,macosArchive,macosFolderArchive";
const MT_LINUX = "--media-types=linuxRPM,linuxDeb";

export function compileInstallers(deployDir, buildTag, platform) {
    if (platform === "other") return;
    if (platform == null ) {
        platform = "";
    } else {
        platform = {
            "": "",
            "win": MT_WIN,
            "windows": MT_WIN,
            "mac": MT_MAC,
            "macos": MT_MAC,
            "linux": MT_LINUX
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