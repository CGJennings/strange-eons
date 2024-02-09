import { exec, verbose } from "./io.js";

/**
 * Creates the platform independent ("other platforms") tar archive from the assembled jar.
 * 
 * @param {*} assembledDir dir containing the assembled `strange-eons.jar`
 * @param {*} distDir deployment assets directory
 * @param {*} buildTag build number tag
 */
export function createGenericTar(assembledDir, distDir, buildTag, platform) {
    if (platform != null && platform != "" && platform !== "other") {
        return;
    }
    verbose("creating platform independent tar archive from assembled jar");
    try {
        exec(`tar -czf "${distDir}/strange-eons-b${buildTag}.tgz" strange-eons.jar`, { cwd: assembledDir});
    } catch (ex) {
        error("could not create tar archive", ex);
    }
}