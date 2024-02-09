import { clean, exec, verbose } from "./io.js";

export function assembleJar(projectDir, assembleDir) {
    verbose("assembling app jar");
    clean(assembleDir);
    exec(`mvn clean package -Pdeploy -f "${projectDir}/pom.xml"`);
}