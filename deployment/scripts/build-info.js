import { read, write, rm, getArg, join, log, warn, error, verbose } from "./io.js";

function lastRevFile(assetsDir) {
    return join(assetsDir, "last-rev");
}

function thisRevFile(srcDir) {
    return join(srcDir, "main", "resources", "ca", "cgjennings", "apps", "arkham", "rev");
}

export function getBuildInfo(assetsDir, srcDir) {
    let lastRev, thisRev, buildTag, version, fullVersion,
         major, minor, patch, type, fullType,
         isInternal;

    try {
        lastRev = parseInt(read(lastRevFile(assetsDir)));
        if (lastRev != lastRev || lastRev < 1111) throw new Error();
    } catch (ex) {
        lastRev = 1111;
        warn(`could not read last-rev, falling back to ${lastRev}`);
    }

    // since the original method of determining the build number is no
    // longer possible, this generates a number based on the number
    // of 4 day "mini weeks" since build 3784 (Mar 5 2017), the last
    // build for which the original method could be used
    thisRev = 3784 + Math.floor((Date.now() - new Date("Mar 5 2017").getTime()) / (1000 * 60 * 60 * 24 * 4));
    if (thisRev <= lastRev) thisRev = lastRev + 1;

    // if a build number was specified, override the computed one
    thisRev = getArg("--forcebuild", thisRev);

    // if a version was specified, decode it; if not this is an internal test build
    let versionArg = getArg("-v", "--v", "--version");

    if (versionArg) {
        // decode the version number:
        // input    major minor patch  type fullType      version fullversion
        // 3.4      "3"   "4"   ""     ""   ""            "3.4"   "3.4"
        // 3.4.1    "3"   "4"   "1"    ""   ""            "3.4.1" "3.4.1"
        // 3.4a1    "3"   "4"   "1"    "a"  "alpha"       "3.4a1" "3.4 alpha 1"
        // 3.4a     "3"   "4"   ""     "a"  "alpha"       "3.4a"  "3.4 alpha"
        // 3.4alpha "3"   "4"   ""     "a"  "alpha"       "3.4a"  "3.4 alpha"
        // 3.4b1    "3"   "4"   "1"    "b"  "beta"        "3.4b1" "3.4 beta 1"
        // 3.4d1    "3"   "4"   "1"    "d"  "development" "3.4d1" "3.4 development 1"
        let match = versionArg.match(/^(\d+)\.(\d+)(?:(\.|a(?:lpha)?|b(?:eta)?|d(?:ev)?)(\d+)?)?$/);
        if (match) {
            major = String(parseInt(match[1]));
            minor = String(parseInt(match[2]));
            let patchDot = (match[3] || ".").substring(0,1); // .|a|b|d
            type = patchDot.replace(".", ""); // ""|a|b|d
            patch = (match[4] ? String(parseInt(match[4])) : "").replace("0", "");
            if (patch === "" && patchDot === ".") patchDot = "";
            fullType = {
                "a": "alpha",
                "b": "beta",
                "d": "development"
            }[type] || "";
            version = `${major}.${minor}${patchDot}${patch}`;
            fullVersion = `${major}.${minor}${patchDot === "." ? "." : " " + fullType}${patchDot === "." ? patch : " " + patch}`.trim();
            buildTag = `${thisRev}${type}`;
            isInternal = false;
        } else {
            error(`invalid version number: ${versionArg}`);
        }
    } else {
        thisRev = 1111;
        buildTag = "1111";
        version = "0.0";
        fullVersion = "0.0";
        major = 0;
        minor = 0;
        patch = 0;
        type = "d";
        fullType = "development";
        isInternal = true;
    }

    return {
        lastRev,
        thisRev,
        buildTag,
        version,
        fullVersion,
        major,
        minor,
        patch,
        type,
        fullType,
        isInternal
    };
}

/**
 * Writes build information into the source tree so the packaged app can read it.
 * @param {string} srcDir path to the `src` directory
 * @param {*} buildInfo the build information to encode
 */
export function writeBuildInfo(srcDir, buildInfo) {
    verbose("writing build info to source tree");
    let type = buildInfo.fullType.toUpperCase() || "GENERAL";
    let rev = `${buildInfo.thisRev}\n${buildInfo.major}\n${buildInfo.minor}\n${buildInfo.patch}\n${type}`;
    write(thisRevFile(srcDir), rev);
}

/**
 * Deletes the temporary build information file from the source tree so it
 * doesn't get committed.
 * @param {string} srcDir path to the `src` directory
 */
export function removeBuildInfo(srcDir) {
    verbose("removing build info from source tree");
    rm(thisRevFile(srcDir));
}

/**
 * If this was not an internal build, update the last-rev file which ensures that
 * the next build will have a higher build number.
 * @param {string} assetsDir the `assets` directory
 * @param {*} buildInfo the build information
 */
export function updateLastRev(assetsDir, buildInfo) {
    if (!buildInfo.isInternal) {
        write(lastRevFile(assetsDir), buildInfo.thisRev);
    }
}