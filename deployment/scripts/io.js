import fs from "node:fs";
import path from "node:path";
import { execSync } from "node:child_process";

export const simulate = hasArg("-s", "--simulate");

/**
 * Checks if a specific command line argument is present.
 * @param {string} argSynonyms - One or more equivalent arguments to check for.
 * @returns {boolean} True if the argument is present, false otherwise.
 */
export function hasArg(...argSynonyms) {
    for (let i=0; i<argSynonyms.length; i++) {
        if (process.argv.includes(argSynonyms[i])) return true;
    }
    return false;
}

/**
 * Retrieves the value of a specific command line argument.
 * @param {string} arg - The argument to retrieve the value for.
 * @param {*} defaultValue - Optional. The default value to return if the argument is not found.
 * @returns {*} The value of the argument, or the default value if the argument is not found.
 * @throws {Error} If the argument is found but no value is provided.
 */
export function getArg(...synonymsFollowedByDefault) {
    let synonyms = [];
    let defaultValue = null;
    let i=0;
    for ( ; i<synonymsFollowedByDefault.length; ++i) {
        let syn = synonymsFollowedByDefault[i];
        if (typeof syn === "string" && syn.startsWith("-")) {
            synonyms.push(syn);
        } else {
            defaultValue = syn;
            ++i;
            break;
        }
    }
    if (i === 0 || i < synonymsFollowedByDefault.length) {
        throw new Error(`getArg: expected argument synonyms followed by optional default: ${synonymsFollowedByDefault.join(", ")}`);
    }
    for (let i=0; i<synonyms.length; i++) {
        let index = process.argv.indexOf(synonyms[i]);
        if (index >= 0) {
            if (
                index === process.argv.length - 1
                || process.argv[index + 1].startsWith("-")
            ) {
                error(`missing value for argument ${synonyms[i]}`);
            }
            return process.argv[index + 1];
        }
    }
    return defaultValue;
}

/**
 * Reads the content of a file.
 * @param {string} file - The path to the file.
 * @returns {string} The content of the file.
 */
export function read(file) {
    return fs.readFileSync(file, "utf-8");
}

/**
 * Writes content to a file.
 * @param {string} file - The path to the file.
 * @param {string} content - The content to write to the file.
 */
export function write(file, content) {
    if (simulate) {
        console.log(`[SIMULATE] write to ${file}:\n---\n${content}\n---`);
    } else {
        fs.writeFileSync(file, content, "utf-8");
    }
}

/**
 * Cleans a directory by removing all files and subdirectories.
 * @param {string} dir - The path to the directory.
 * @param {string} exceptPrefix - Optional. Files starting with this prefix will be kept.
 */
export function clean(dir, exceptPrefix) {
    if (simulate) {
        console.log(`[SIMULATE] clean "${dir}"${exceptPrefix ? ` except "${exceptPrefix}*"` : ""}`);
        return;
    }
    if (hasArg("--noclean")) {
        verbose("not cleaning, --noclean specified");
        return;
    }
    verbose(`cleaning "${dir}"`);
    if (fs.existsSync(dir)) {
        for (let file of fs.readdirSync(dir)) {
            if (exceptPrefix && file.startsWith(exceptPrefix)) continue;
            let target = path.join(dir, file);
            if (fs.lstatSync(target).isDirectory()) {
                clean(target, exceptPrefix);
            } else {
                fs.unlinkSync(target);
            }
        }
    }
}

/**
 * Removes a file.
 * @param {string} file - The path to the file.
 */
export function rm(file) {
    if (simulate) {
        console.log(`[SIMULATE] remove ${file}`);
    } else {
        fs.unlinkSync(file);
    }
}

/**
 * Joins the given path parts.
 * @param  {...string} parts - The path parts to join.
 * @returns {string} The joined path.
 */
export const join = path.join;

/**
 * Executes a command in a shell.
 * @param {string} command - The command to execute.
 * @param {object} options - Optional. The options for the command.
 * @returns {Buffer} The output of the command.
 */
export function exec(command, options) {
    if (simulate) {
        console.log(`[SIMULATE] exec: ${command}`);
        console.log(`             in: ${options?.cwd || process.cwd()}`);
    } else {
        execSync(command, options);
    }
}

/**
 * Checks if a file or directory exists.
 * @param {string} path - The path to the file or directory.
 * @returns {boolean} True if the file or directory exists, false otherwise.
 */
export const exists = fs.existsSync;

/**
 * Retrieves the list of files and directories in a directory.
 * @param {string} dir - The path to the directory.
 * @returns {string[]} The list of files and directories in the directory.
 */
export const dir = fs.readdirSync;

/**
 * Log an error message and optional exception, then exit.
 * @param {string} message 
 * @param {*} ex 
 */
export function error(message, ex) {
    if (ex) {
        console.error(`Error: ${message}`, ex);
    } else {
        console.error(`Error: ${message}`);
    }
    process.exit(20);
}

/**
 * Log a warning message and optional exception.
 * @param {string} message 
 * @param {*} ex 
 */
export function warn(message, ex) {
    if (ex) {
        console.warn(`Warning: ${message}`, ex);
    } else {
        console.warn(`Warning: ${message}`);
    }
}

/**
 * Log a message.
 * @param {string} message 
 */
export const log = hasArg("-q", "--quiet") ? () => {} : console.log;

/**
 * Log a message if not in simulation mode.
 * @param {string} message 
 */
export const logIfReal = simulate ? () => {} : log;

/**
 * Log a message if in verbose mode.
 * @param {string} message 
 */
export const verbose = hasArg("-V", "--verbose") ? log : () => {};