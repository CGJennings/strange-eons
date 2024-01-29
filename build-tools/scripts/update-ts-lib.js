const fs = require("fs");
const path = require("path");
const uglify = require("uglify-js");
const JSZip = require("jszip");

// if true, uses dummy library instead of downloading
const DEBUG = false;

// was https://rawgit.com/Microsoft/TypeScript/master/lib/typescriptServices.js
// see https://github.com/microsoft/TypeScript/issues/50758
const SOURCE = "https://unpkg.com/typescript/lib/typescript.js";
const JAR_PATH = "ca/cgjennings/apps/arkham/plugins/typescript/typescriptServices.js";
const LIB_NAME = "typescript-services";
const LIB_VERSION = "1.0"; // if version changes you must update the .pom files
const LIB_PATH = path.join(__dirname, "..", "..", "lib", "local", LIB_NAME, LIB_VERSION, `${LIB_NAME}-${LIB_VERSION}.jar`);

async function downloadLib(url) {
    if (DEBUG) {
        return `
            (function ts() {
                let ts;
                ts.versionMajorMinor = "0.0";
                ts.version = ts.versionMajorMinor + ".0";
                console.log("🙂");
            })();
        `;
    }

    const response = await fetch(url);
    return await response.text();
}

async function doOrDie(task, f) {
    try {
        return await f();
    } catch (ex) {
        console.error(`failed to ${task}`);
        console.error(ex.message || ex);
        process.exit(20);
    }
}


(async function() {
    let lib;
    let jarBuff;

    await doOrDie("download library", async () => {
        let version;
        lib = await downloadLib(SOURCE);
        try {
            version = lib.match(/version = "(\d\.\d\.\d)"/)[1];
        } catch (ex) {
            console.log("could not extract version number from library, possible incompatibility");
            fs.writeFileSync(path.join(__dirname, "..", "..", "typescriptServices.js"), lib);
            console.log("wrote lib to project root as typescriptServices.js for examination");  
            exit(20);          
        }
        console.log(`downloaded library v${version}`);
    });

    await doOrDie("minify library", () => {
        const originalSize = Math.round(lib.length / 1024);
        lib = uglify.minify(lib).code;
        const minifiedSize = Math.round(lib.length / 1024);
        console.log(`minified from ${originalSize} kb to ${minifiedSize} kb`);
    });

    await doOrDie("prepare JAR", async () => {
        const zip = new JSZip();
        zip.file(JAR_PATH, lib);
        zip.file("META-INF/MANIFEST.MF", "Manifest-Version: 1.0");
        jarBuff = await zip.generateAsync({type: "nodebuffer"});
        console.log(`prepared JAR buffer, ${Math.round(jarBuff.length / 1024)} kb`);
    });

    doOrDie("write JAR file", () => {
        const outpath = path.resolve(__dirname, LIB_PATH);
        fs.writeFileSync(outpath, jarBuff);
        console.log(`wrote JAR file to ${LIB_PATH}`);
    });
})();