const { get } = require("https");
const fs = require("fs");
const path = require("path");
const uglify = require("uglify-js");
const JSZip = require("jszip");

// if true, uses dummy library instead of downloading
const DEBUG = false;

const SOURCE = "https://rawgit.com/Microsoft/TypeScript/master/lib/typescriptServices.js";
const JAR_PATH = "ca/cgjennings/apps/arkham/plugins/typescript/typescriptServices.js";
const LIB_NAME = "typescript-services.jar";
const LIB_PATH = path.join("..", "lib", LIB_NAME);

function downloadLib(url) {
    if (DEBUG) {
        return Promise.resolve(`
            (function ts() {
                let ts;
                ts.versionMajorMinor = "0.0";
                ts.version = ts.versionMajorMinor + ".0";
                console.log("🙂");
            })();
        `);
    }   

    return new Promise((resolve, reject) => {
        get(url, (response) => {
            let body = "";
            response.on("data", (chunk) => {
                body += chunk;
            });
            response.on("end", () => {
                resolve(body);
            });
        }).on("error", (err) => {
            reject(err.message);
        });
    });
}

function doOrDie(task, f) {
    try {
        return f();
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
        version = lib.match(/ts\.versionMajorMinor = "([^"]+)/)[1];
        version += lib.match(/ts\.version = ts\.versionMajorMinor \+ "([^"]+)/)[1];
        console.log(`downloaded library v${version}`);
    });

    doOrDie("minify library", () => {
        const originalSize = Math.round(lib.length / 1024);
        lib = uglify.minify(lib, {}).code;
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