import { dir, verbose, warn } from "./io.js";

/**
 * Returns an lists of the generated build outputs by platform.
 *
 * @param {string} deployDir - The deploy directory to search for build outputs.
 * @param {string} buildInfo - The build verison info.
 * @returns {Object} - The outputs object containing the categorized build outputs.
 */
export function detectBuildOutputs(deployDir, buildInfo) {
    const outputs = {
        windows: [],
        macos: [],
        linux: [],
        other: []
    };

    // find all files that look like build artifacts with the correct build tag
    const tagMatches = dir(deployDir).filter(
        file => file.includes(`-b${buildInfo.buildTag}`) && file.startsWith("strange-eons-")
    );

    let hasOutputs = false;
    tagMatches.forEach(file => {
        if (file.includes("-win")) {
            outputs.windows.push(file);
            hasOutputs = true;
        } else if (file.includes("-mac")) {
            outputs.macos.push(file);
            hasOutputs = true;
        } else if (file.includes("-linux")) {
            outputs.linux.push(file);
            hasOutputs = true;
        } else {
            outputs.other.push(file);
            hasOutputs = true;
        }
    });

    if (hasOutputs) {
        verbose("build outputs:");
        for (let platform in outputs) {
            verbose(`  ${platform}: ${outputs[platform].join(", ")}`);
        }
    } else {
        warn("no build outputs");
    }

    return outputs;
}