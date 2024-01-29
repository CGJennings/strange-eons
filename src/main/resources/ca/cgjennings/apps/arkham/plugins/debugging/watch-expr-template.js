(function(val) {
    const ARRAY_MAX = 3;
    const DEPTH_MAX = 1;
    let s;
    try {
        let f = (obj, depth) => {
            let javaPrefix = "";
            let proxyLen = 0;
            let s = "";
            let bi = "  ".repeat(depth);
            if (obj instanceof java.lang.Object) {
                let klass = obj.getClass();                
                let name = klass.toGenericString();                
                javaPrefix = bi + "<" + name + ">\n";
                if (obj instanceof java.lang.String) {
                    obj = String(obj);
                } else if (klass.isArray()) {
                    let proxy = [];
                    for (let i=0; i<=ARRAY_MAX && i<obj.length; ++i) {
                        proxy.push(obj[i]);
                    }
                    proxyLen = obj.length;
                    obj = proxy;
                }
            }
            if (Array.isArray(obj)) {
                let trueLength = Math.max(obj.length, proxyLen);
                if (obj.length === 0) {
                    s += bi + "[]";
                } else if (depth >= DEPTH_MAX) {
                    s += bi + "[...] length " + trueLength;
                } else {
                    s += bi + "[\n";
                    for (let i=0; i<obj.length && i<ARRAY_MAX; ++i) {
                        if (i > 0) {
                            s += ",\n";
                        }
                        s += f(obj[i], depth+1);
                    }
                    if (obj.length > ARRAY_MAX) {
                        s += ",\n" + bi + "  ...";
                    }
                    s += "\n" + bi + "].length = " + trueLength;
                }
            } else if (typeof obj === "string") {
                if (depth > 0 && obj.includes("\n")) {
                    obj = obj.replaceAll("\n", "\\n");
                }
                if (obj.includes('"')) {
                    obj = obj.replaceAll('"', '\\"');
                }
                s += bi + '"' + obj + '"';
            } else if (typeof obj === "object") {
                let objStr = String(obj);
                if (objStr === "[object Object]") {
                    objStr = "";
                } else {
                    objStr = bi + objStr + "\n";
                }
                if (depth >= DEPTH_MAX) {
                    s += objStr + bi + "{...}";
                } else {
                    s += objStr + bi + "{";
                    let inner = "";
                    for (let p in obj) {
                        if (inner.length > 0) inner += ",";
                        inner += "\n  " + bi + p + ": ";
                        let v = f(obj[p], depth+1)
                                .trim().replace(/\n\s*/g, "\n" + " ".repeat(bi.length + p.length + 4));
                        inner += v;
                    }
                    if (inner.length > 0) inner += "\n" + bi;
                    s += inner  + "}";
                }
            } else {
                s += bi + String(obj).replaceAll("\n", "\n" + bi);
            }
            return javaPrefix + s;
        };
        s = f(val, 0, 1);
    } catch(ex) {
        s = String(ex);
    }
    return s;
})(watchExpr);