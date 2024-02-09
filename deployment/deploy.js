import { clean, join, log, logIfReal, hasArg, getArg, read } from "./scripts/io.js";
import { getBuildInfo, writeBuildInfo, removeBuildInfo } from "./scripts/build-info.js";
import { assembleJar } from "./scripts/assemble-jar.js";
import { createGenericTar } from "./scripts/generic-tar.js";
import { compileInstallers } from "./scripts/compile-installers.js";
import { runSigningStep, runPostDeployStep } from "./scripts/custom-steps.js";
import { fileURLToPath } from "node:url";
import { dirname } from "node:path";
import { detectBuildOutputs } from "./scripts/build-outputs.js";

const deployDir = fileURLToPath(dirname(import.meta.url));
const projectDir = join(deployDir, "..");
const srcDir = join(projectDir, "src");
const assetsDir = join(deployDir, "assets");
const assembledDir = join(deployDir, "assembled");
const distDir = join(deployDir, "dist");

if (hasArg("-h", "--help", "-?", "--?", "/?")) {
    log(read(join(deployDir, "scripts", "help.txt")));
    process.exit(0);
}

const platform = getArg("-p", "--platform", "");
const buildInfo = getBuildInfo(assetsDir, srcDir);
log(`building artifacts for ${buildInfo.fullVersion}, build ${buildInfo.thisRev} (last build was ${buildInfo.lastRev})`);

clean(distDir);
writeBuildInfo(srcDir, buildInfo);
assembleJar(projectDir, assembledDir);
removeBuildInfo(srcDir);
createGenericTar(assembledDir, distDir, buildInfo.buildTag, platform);
compileInstallers(deployDir, buildInfo.buildTag, platform);
clean(distDir, "strange-eons");
const buildOutputs = detectBuildOutputs(distDir, buildInfo);
await runSigningStep(projectDir, distDir, buildInfo, buildOutputs);
await runPostDeployStep(projectDir, distDir, buildInfo, buildOutputs);
logIfReal(`artifacts written to "${distDir}"`);