import { pathToFileURL } from "node:url";
import { join, exists, hasArg, log, warn, verbose } from "./io.js";


/**
 * Runs the signing step for the project. This looks for a file
 * named `local-private/sign.js` in the project directory and, if found,
 * requires it and calls a function for each platform (if the function exists).
 * The function names are `windows`, `mac`, `linux`, and `other`.
 * The function is passed the project base directory, the distributable assets
 * directory (containing the installers), and the build info.
 * 
 * @param {string} projectDir - The directory of the project.
 * @param {string} distDir - The directory where the build artifacts are located.
 * @param {object} buildInfo - Information about the build.
 * @param {object} buildOutputs - The artifact files from the build process.
 */
export async function runSigningStep(projectDir, distDir, buildInfo, buildOutputs) {
    if (hasArg("--nosign")) return;
    let signingScript = join(projectDir, "local-private", "sign.js");
    if (exists(signingScript)) {
        const signingFns = await import(pathToFileURL(signingScript));
        for (let platform of ["windows", "macos", "linux", "other"]) {
            if (!signingFns[platform] || buildOutputs[platform].length === 0) continue;
            try {
                verbose(`signing ${platform} build`);
                signingFns[platform](projectDir, distDir, buildInfo, buildOutputs);
            } catch (ex) {
                warn(`error signing ${platform} build`, ex);
            }
        }
    }
}

/**
 * Runs the post-deploy step for the project. This checks if a file
 * named `local-private/post-deploy.js` exists in the project directory,
 * and if so, requires it and executes the exported function.
 * The function is passed the project base directory, the distributable assets
 * directory (containing the installers), and the build info.
 * 
 * @param {string} projectDir - The directory of the project.
 * @param {string} distDir - The directory where the build artifacts are located.
 * @param {object} buildInfo - Information about the build.
 * @param {object} buildOutputs - The artifact files from the build process.
 */
export async function runPostDeployStep(projectDir, distDir, buildInfo, buildOutputs) {
    if (hasArg("--nopost")) return;
    const postDeployScript = join(projectDir, "local-private", "post-deploy.js");
    if (exists(postDeployScript)) {
        try {
            verbose("running post-deploy script");
            const { postDeploy } = await import(pathToFileURL(postDeployScript));
            await postDeploy(projectDir, distDir, buildInfo, buildOutputs);
        } catch (ex) {
            warn("error running post-deploy script", ex);
        }
    }
}