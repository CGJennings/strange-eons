const fs = require("fs");
const path = require("path");
const { execSync } = require("child_process");

const JAVA = "java";
const JFLEX = path.join(__dirname, "..", "lib", "jflex-1.4.1.jar");
const INPUT = path.join(__dirname, "..", "tokenizers", "*.flex");
const OUTPUT = path.join(__dirname, "..", "..", "src", "ca", "cgjennings", "ui", "textedit");

const CLASSES = fs.readdirSync(path.resolve(INPUT, ".."))
    .filter(f => f.endsWith(".flex"))
    .map(f => f.replace(".flex", ".java"));

// delete existing class files so JFlex doesn't copy them to backup files
for (let oldClass of CLASSES) {
    if (fs.existsSync(path.resolve(OUTPUT, oldClass))) {
        fs.unlinkSync(path.resolve(OUTPUT, oldClass));
    }
}

// build tokenizer classes with JFlex
console.log(`Building tokenizer classes...`);
execSync(`${JAVA} -Dfile.encoding=UTF-8 -jar ${JFLEX} -q -d ${OUTPUT} ${INPUT}`, { stdio: "inherit" });

// patch the generated classes
const PATCHES = [
    {
        pattern: `private char zzBuffer[] = new char[ZZ_BUFFERSIZE];`,
        replacement: `private char zzBuffer[];`
    }, {
        pattern: `
        /**
         * Refills the input buffer.
         *
         * @return      <code>false</code>, iff there was new input.
         * 
         * @exception   java.io.IOException  if any I/O-Error occurs
         */
        private boolean zzRefill() throws java.io.IOException {
      
          /* first: make room (if you can) */
          if (zzStartRead > 0) {
            System.arraycopy(zzBuffer, zzStartRead,
                             zzBuffer, 0,
                             zzEndRead-zzStartRead);
      
            /* translate stored positions */
            zzEndRead-= zzStartRead;
            zzCurrentPos-= zzStartRead;
            zzMarkedPos-= zzStartRead;
            zzPushbackPos-= zzStartRead;
            zzStartRead = 0;
          }
      
          /* is the buffer big enough? */
          if (zzCurrentPos >= zzBuffer.length) {
            /* if not: blow it up */
            char newBuffer[] = new char[zzCurrentPos*2];
            System.arraycopy(zzBuffer, 0, newBuffer, 0, zzBuffer.length);
            zzBuffer = newBuffer;
          }
      
          /* finally: fill the buffer with new input */
          int numRead = zzReader.read(zzBuffer, zzEndRead,
                                                  zzBuffer.length-zzEndRead);
      
          if (numRead < 0) {
            return true;
          }
          else {
            zzEndRead+= numRead;
            return false;
          }
        }
        `,
        replacement: ``
    }, {
        pattern: `
            /**
             * Resets the scanner to read from a new input stream.
             * Does not close the old reader.
             *
             * All internal variables are reset, the old input stream 
             * <b>cannot</b> be reused (internal buffer is discarded and lost).
             * Lexical state is set to <tt>ZZ_INITIAL</tt>.
             *
             * @param reader   the new input stream 
             */
            public final void yyreset(java.io.Reader reader) {
              zzReader = reader;
              zzAtBOL  = true;
              zzAtEOF  = false;
              zzEndRead = zzStartRead = 0;
              zzCurrentPos = zzMarkedPos = zzPushbackPos = 0;
              yyline = yychar = yycolumn = 0;
              zzLexicalState = YYINITIAL;
            }
            `,
        replacement: ``
    }
];

for (let newClass of CLASSES) {
    console.log(`Patching ${newClass}...`);
    let javaCode = fs.readFileSync(path.resolve(OUTPUT, newClass), "utf8");
    for (let patch of PATCHES) {
        let pattern = "\\s*" + patch.pattern.trim()
                // escape regex special characters
                .replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
                // match any amount of whitespace
                .replace(/\s+/g, "\\s+") + "\\s*";
        javaCode = javaCode.replace(new RegExp(pattern, "g"), patch.replacement);
    }
    fs.writeFileSync(path.resolve(OUTPUT, newClass), javaCode);
}

console.log(`Done`);