const fs = require("fs");
const path = require("path");
const exec = require("child_process").execSync;

process.chdir(__dirname);

const DIR_PROJECT = path.join(__dirname, "..");
const DIR_SRC = path.join(__dirname, "..", "src");
const DIR_ASSETS = path.join(__dirname, "assets");
const DIR_ASSEMBLED = path.join(__dirname, "assembled");
const DIR_DIST = path.join(__dirname, "dist");
const FILE_REV = path.join(DIR_SRC, "main", "resources", "ca", "cgjennings", "apps", "arkham", "rev");
const FILE_LAST_REV = path.join(DIR_ASSETS, "last-rev");

let INSTALL4J = process.env["INSTALL4J"];
if (!INSTALL4J) {
    if (process.platform.startsWith("win")) {
        INSTALL4J = `"${process.env["PROGRAMFILES"]}\\install4j9\\bin\\install4jc.exe"`;
    } else if (process.platform.startsWith("darwin")) {
        INSTALL4J = `"/Applications/install4j9/bin/install4jc"`;
    } else {
        INSTALL4J = "install4jc";
    }
}

let LAST_REV = 1111;
try {
    LAST_REV = parseInt(fs.readFileSync(FILE_LAST_REV, "utf-8"));
    console.log(`last build number was ${LAST_REV}`);
} catch (ex) {
    console.warn(`could not read last-rev, falling back to ${LAST_REV}`);
}

// since the original method of determining the build number is no
// longer possible, this generates a number based on the number
// of 4 day "mini weeks" since build 3784 (Mar 5 2017), the last
// build for which the original method could be used
let THIS_REV = 3784 + Math.floor((Date.now() - new Date("Mar 5 2017").getTime()) / (1000 * 60 * 60 * 24 * 4));
if (THIS_REV <= LAST_REV) THIS_REV = LAST_REV + 1;
console.log(`preparing to release build ${THIS_REV}`);

if (!process.argv.includes("--noclean")) {
    console.log(`cleaning dist folder`);
    cleanDist(true);
}

// write the build number into the source tree for packaging
console.log(`updating build number`);
fs.writeFileSync(FILE_REV, `${THIS_REV}`);
fs.writeFileSync(FILE_LAST_REV, `${THIS_REV}`);

console.log(`assembling project`);
exec(`mvn clean package -Pdeploy -f "${DIR_PROJECT}/pom.xml"`);
// remove the build number from the source tree so it doesn't get committed
try { fs.unlinkSync(FILE_REV); }
catch (ex) { console.warn("could not remove build number from source tree", ex); }

console.log("creating tar archive");
try {
    exec(`tar -czf "${DIR_DIST}/strange-eons-${THIS_REV}.tar.gz" strange-eons.jar`, { cwd: DIR_ASSEMBLED});
} catch (ex) {
    console.warn("could not create tar archive", ex);
}

console.log("compiling installers");
exec(`${INSTALL4J} --release=${THIS_REV} build-installers.install4j`);
cleanDist(false);

console.log(`done, deployment artifacts can be found in:\n  ${DIR_DIST}`);





function cleanDist(allFiles) {
    fs.readdirSync(DIR_DIST).forEach(file => {
        if (allFiles || !file.startsWith("strange-eons-")) {
            fs.unlinkSync(path.join(DIR_DIST, file));
        }
    });
}