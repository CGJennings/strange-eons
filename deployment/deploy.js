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
        INSTALL4J = `"${process.env["PROGRAMFILES"]}\\install4j10\\bin\\install4jc.exe"`;
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

let RELEASE_TYPE = ""; // general release
if(process.argv.includes("--beta")) {
    RELEASE_TYPE = "\nBETA";
} else if (process.argv.includes("--alpha")) {
    RELEASE_TYPE = "\nALPHA";
} else if (process.argv.includes("--dev")) {
    RELEASE_TYPE = "\nDEVELOPMENT";
}
// short form to use in file names
const RELEASE_TYPE_SHORT = RELEASE_TYPE.length > 0 ? RELEASE_TYPE.trim().toLowerCase()[0] : "";

// since the original method of determining the build number is no
// longer possible, this generates a number based on the number
// of 4 day "mini weeks" since build 3784 (Mar 5 2017), the last
// build for which the original method could be used
let THIS_REV = 3784 + Math.floor((Date.now() - new Date("Mar 5 2017").getTime()) / (1000 * 60 * 60 * 24 * 4));
if (THIS_REV <= LAST_REV) THIS_REV = LAST_REV + 1;
console.log(`preparing build ${THIS_REV} ${RELEASE_TYPE.trim().toLowerCase()}`);

if (!process.argv.includes("--noclean")) {
    console.log(`cleaning dist folder`);
    clean(true);
}

// write the build number into the source tree for packaging
console.log(`updating build number`);
fs.writeFileSync(FILE_REV, `${THIS_REV}${RELEASE_TYPE}`);
fs.writeFileSync(FILE_LAST_REV, `${THIS_REV}`);

console.log(`assembling project`);
clean(true, DIR_ASSEMBLED);
exec(`mvn clean package -Pdeploy -f "${DIR_PROJECT}/pom.xml"`);
// remove the build number from the source tree so it doesn't get committed
try { fs.unlinkSync(FILE_REV); }
catch (ex) { console.warn("could not remove build number from source tree", ex); }

console.log("creating tar archive");
try {
    exec(`tar -czf "${DIR_DIST}/strange-eons-b${THIS_REV}${RELEASE_TYPE_SHORT}.tar.gz" strange-eons.jar`, { cwd: DIR_ASSEMBLED});
} catch (ex) {
    console.warn("could not create tar archive", ex);
}

console.log("compiling installers");
exec(`${INSTALL4J} --release=${THIS_REV}${RELEASE_TYPE_SHORT} build-installers.install4j`);
clean(false);

if (!process.argv.includes("--nosign")) {
    if (fs.existsSync(path.join(DIR_PROJECT, "local-private", "sign.js"))) {        
        const signingFns = require(path.join(DIR_PROJECT, "local-private", "sign.js"));
        for (let platform of ["windows", "mac", "linux", "other"]) {
            try {
                console.log(`signing ${platform} build`);
                signingFns[platform]?.(DIR_DIST, THIS_REV);
            } catch (ex) {
                console.error(`error signing ${platform} build`, ex);
            }
        }
    } else {
        console.warn("no signing script found, skipping signing");
        console.warn("run with --nosign to suppress this warning");
    }
}

if (fs.existsSync(path.join(DIR_PROJECT, "local-private", "post-deploy.js"))) {
    console.log("running post-deploy script");
    const postDeploy = require(path.join(DIR_PROJECT, "local-private", "post-deploy.js"));
    try {
        postDeploy(DIR_DIST, THIS_REV, RELEASE_TYPE.trim().toLowerCase());
    } catch (ex) {
        console.error("error running post-deploy script", ex);
    }
}

console.log(`done, deployment artifacts can be found in:\n  ${DIR_DIST}`);





function clean(allFiles, dir = DIR_DIST) {
    fs.readdirSync(dir).forEach(file => {
        if (allFiles || !file.startsWith("strange-eons-")) {
            fs.unlinkSync(path.join(dir, file));
        }
    });
}