/*
 common.js - version 26
 Core functionality included in every script.
 */

const ca = Packages.ca;
const gamedata = Packages.gamedata;
const resources = Packages.resources;

const arkham = Packages.ca.cgjennings.apps.arkham;
const swing = javax.swing;

const ResourceKit = resources.ResourceKit;
const Language = resources.Language;
const Settings = resources.Settings;
const Color = Settings.Colour;
const Colour = Settings.Colour;
const Region = Settings.Region;
const Region2D = Settings.Region2D;
const Font = java.awt.Font;
const URL = java.net.URL;

const exit = arkham.plugins.ScriptMonkey.breakScript;

Error.error = function error(exception) {
    // previously we had to jump through hoops to get a stack trace;
    // now this is basically shorthand for throwing an error
    if (exception == null) {
        exception = 'unspecified error';
    } else if (exception instanceof java.lang.Throwable) {
        org.mozilla.javascript.Context.throwAsScriptRuntimeEx(exception);
    } else if (exception instanceof Error) {
        throw exception;
    }
    throw new Error(exception.toString());
};

if (Settings.shared.getBoolean('script-warnings')) {
    Error.warn = function warn(message, frame) {
        message = message || "unspecified warning";
        frame = frame == null || frame != frame ? 1 : Math.max(0, -frame);
        let stack = arkham.plugins.LibImpl.getScriptTrace();
        frame = Math.min(Math.max(0, stack.length - 1), frame);
        org.mozilla.javascript.Context.reportWarning(
                message, stack[frame].file, stack[frame].line, null, -1
        );
    };
    Error.deprecated = function deprecated(message, frame) {
        message = '[DEPRECATED] ' + (message || "unspecified feature");
        frame = frame == null || isNaN(frame) ? -3 : Math.min(0, frame) - 1;
        warn(message, frame);
    };
} else {
    Error.warn = () => undefined;
    Error.deprecated = Error.warn;
}

Error.handleUncaught = function handleUncaught(exception) {
    if (exception['javaException'] instanceof arkham.plugins.ScriptMonkey.BreakException) {
        throw exception;
    }
    let rex = exception['rhinoException'];
    if (rex) {
        arkham.plugins.ScriptMonkey.scriptError(rex);
    } else {
        Console.err.print("\nUncaught ");
        Console.err.print(exception);
    }
};

const prompt = function prompt(promptMessage, initialValue) {
    useLibrary.__threadassert();
    return swing.JOptionPane.showInputDialog(
            Eons.safeStartupParentWindow, promptMessage || "", initialValue || ""
            );
};

const confirm = (function () {
    let helper = (p, t, o) => {
        useLibrary.__threadassert();
        if (p == null)
            throw new TypeError("no promptMessage");
        t = t || useLibrary.defaultDialogTitle();
        return swing.JOptionPane.OK_OPTION ===
                swing.JOptionPane.showConfirmDialog(
                        Eons.safeStartupParentWindow, p, t, o
                        )
                ;
    };
    function confirm(promptMessage, title) {
        return helper(promptMessage, title, swing.JOptionPane.OK_CANCEL_OPTION);
    }
    confirm.confirm = confirm;
    confirm.yesno = function yesno(promptMessage, title) {
        return helper(promptMessage, title, swing.JOptionPane.YES_NO_OPTION);
    };
    confirm.choose = function choose(promptMessage, title, options) {
        useLibrary.__threadassert();
        if (arguments.length < 3)
            throw new TypeError("no options");
        title = title || useLibrary.defaultDialogTitle();
        options = Array.prototype.slice.call(arguments, 2);
        return swing.JOptionPane.showOptionDialog(
                Eons.safeStartupParentWindow, promptMessage, title,
                swing.JOptionPane.DEFAULT_OPTION,
                swing.JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]
                );
    };
    return confirm;
})();

const alert = function alert(message, isErrorMessage) {
    useLibrary.__threadassert();
    if (!message)
        throw new TypeError("no message");
    let M = arkham.dialog.Messenger;
    M = isErrorMessage === true ? M.displayErrorMessage
            : isErrorMessage === false ? M.displayWarningMessage
            : M.displayMessage;
    M(null, message);
};

const sleep = function sleep(msDelay) {
    msDelay = msDelay >= 0 ? msDelay : 1000;
    try {
        java.lang.Thread.currentThread().sleep(msDelay);
    } catch (interrupted) {
        return true;
    }
    return false;
};

const sprintf = function sprintf() {
    let loc = arguments[0];
    if (loc instanceof Language)
        loc = loc.locale;
    let firstObj = 1;
    if (loc == null || loc instanceof java.util.Locale) {
        firstObj = 2;
    } else {
        loc = Language.interface.locale;
    }
    if (arguments.length < firstObj)
        return "";
    return arkham.plugins.LibImpl.sprintf(
            loc, arguments[firstObj - 1],
            Array.prototype.slice.call(arguments, firstObj)
            );
};

const print = function print() {
    for (let i = 0; i < arguments.length; ++i) {
        Console.out.printObj(arguments[i]);
    }
};

const println = function println() {
    for (let i = 0; i < arguments.length; ++i) {
        Console.out.printObj(arguments[i]);
    }
    Console.out.println();
};

const printf = function printf() {
    Console.out.printObj(sprintf.apply(null, arguments));
};

const Console = {
    get out() {
        return arkham.plugins.ScriptMonkey.sharedConsole.getWriter();
    },
    get err() {
        return arkham.plugins.ScriptMonkey.sharedConsole.getErrorWriter();
    },
    get visible() {
        return arkham.plugins.ScriptMonkey.sharedConsole.isVisible();
    },
    set visible(b) {
        arkham.plugins.ScriptMonkey.sharedConsole.setVisible(b);
    },
    print: print,
    println: println,
    printImage(image) {
        this.out.insertImage(image);
    },
    printComponent(uiComponent) {
        this.out.insertComponent(uiComponent);
    },
    printHTML(html) {
        this.out.insertHTML(html);
    },
    clear() {
        arkham.plugins.ScriptMonkey.sharedConsole.clear();
    },
    history() {
        return arkham.plugins.ScriptMonkey.sharedConsole.getHistoryText();
    },
    queue() {
        arkham.plugins.ScriptMonkey.sharedConsole.queue();
    },
    flush() {
        arkham.plugins.ScriptMonkey.sharedConsole.flush();
    }
};

const useInterfaceLanguage = function useInterfaceLanguage(language) {
    useLibrary.__$.setUiLangProvider(language);
};

const string = function string(key) {
    let str = useLibrary.__$.getUiLangProvider().str(key);
    if (arguments.length > 1) {
        str = arkham.plugins.LibImpl.sprintf(
                useLibrary.__$.getUiLangProvider().locale,
                str,
                Array.prototype.slice.call(arguments, 1)
                );
    }
    return str;
};
this["@"] = string;

function useGameLanguage(language) {
    useLibrary.__$.setGameLangProvider(language);
}

const gstring = function gstring(key) {
    let str = useLibrary.__$.getGameLangProvider().str(key);
    if (arguments.length > 1) {
        str = arkham.plugins.LibImpl.sprintf(
                useLibrary.__$.getGameLangProvider().locale,
                str,
                Array.prototype.slice.call(arguments, 1)
                );
    }
    return str;
};
this["#"] = gstring;

const useSettings = function useSettings(source) {
    if (source == null) {
        source = Settings.shared;
    } else if (source instanceof arkham.component.GameComponent) {
        source = source.settings;
    } else if (!(source instanceof Settings)) {
        Error.error('source is not a Settings instance or game component');
    }
    useLibrary.__$.setSettingProvider(source);
};

const $ = function $(key) {
    return useLibrary.__$.getSettingProvider().get(key);
};

const Patch = {
    apply() {
        if (arguments.length % 2 != 0) {
            Error.error(string('scriptlib-common-patchargs'));
        }
        let s = Settings.user;
        for (let i = 0; i < arguments.length; i += 2) {
            let oldValue = s.get(arguments[i]);
            s.set(arguments[i], arguments[i + 1]);
            println(string('scriptlib-common-patch-apply', arguments[i], arguments[i + 1], oldValue));
        }
        s.flush();
        println('OK');
    },
    restore() {
        let s = Settings.user;
        for (let i = 0; i < arguments.length; ++i) {
            s.reset(arguments[i]);
        }
        s.flush();
        s = Settings.shared;
        for (i = 0; i < arguments.length; ++i) {
            let newValue = s.get(arguments[i]);
            println(string('scriptlib-common-patch-restore', arguments[i], newValue));
        }
        println('OK');
    },
    temporary() {
        if (arguments.length % 2 !== 0) {
            Error.error(string('scriptlib-common-patchargs'));
        }
        for (let i = 0; i < arguments.length; i += 2) {
            let oldValue = Settings.shared.get(arguments[i]);
            resources.RawSettings.setGlobalSetting(arguments[i], arguments[i + 1]);
            string('scriptlib-common-patch-apply', arguments[i], arguments[i + 1], oldValue);
        }
    },
    card() {
        if (arguments.length < 3 || arguments.length % 2 != 1) {
            Error.error('Patch.card(): invalid number of arguments: ' + arguments.length);
        }
        let privateSettings = arguments[0].settings;
        for (let i = 1; i < arguments.length; i += 2) {
            privateSettings.set(arguments[i], arguments[i + 1]);
        }
    },
    cardFrom(card, resourcePath) {
        card.settings.addFrom(resourcePath);
    },
    cardRestore() {
        if (arguments.length < 2) {
            Error.error('Patch.cardUnapply(): invalid number of arguments: ' + arguments.length);
        }
        let privateSettings = arguments[0].settings;
        for (let i = 1; i < arguments.length; ++i) {
            privateSettings.reset(arguments[i]);
        }
    }
};

const debug = arkham.plugins.debugging.ScriptDebugging.isInstalled()
        ? function debug(formatString) {
            let msg = sprintf.apply(null, arguments);
            Eons.log.logp(java.util.logging.Level.INFO, sourcefile, 'debug', msg);
        }
: function debug() { };

const assert = arkham.plugins.debugging.ScriptDebugging.isInstalled()
        ? function assert(condition, message) {
            if (!condition) {
                message = "ASSERTION FAILURE" + (message == null ? "" : ": " + message);
                debug(message);
                throw new Error(message);
            }
        }
: debug;

//
// LANGUAGE EXTENSIONS (documented in javascript.doc)
//

Array.from = function from(o) {
    let a;
    if (o instanceof java.lang.Iterable) {
        o = o.iterator();
    }
    if (o instanceof java.util.Iterator) {
        a = [];
        while (o.hasNext()) {
            a[a.length] = o.next();
        }
        return a;
    }
    if (o instanceof java.util.Enumeration) {
        a = [];
        while (o.hasMoreElements()) {
            a[a.length] = o.nextElement();
        }
        return a;
    }
    if (o['length'] !== undefined) {
        return Array.prototype.slice.call(o);
    }
    a = [];
    while (o[a.length] !== undefined) {
        a[a.length] = o[a.length];
    }
    if (a.length === 0) {
        throw new TypeError('Not an array-like object.');
    }
    return a;
};

Number.prototype.toInt = function toInt(n) {
    if (!n)
        n = this;
    return new java.lang.Integer['(int)'](this);
};

Number.prototype.toLong = function toLong(n) {
    if (!n)
        n = this;
    return new java.lang.Long['(long)'](this);
};

Number.prototype.toFloat = function toFloat(n) {
    if (!n)
        n = this;
    return new java.lang.Float['(float)'](this);
};

Number.prototype.dontEnum('toInt', 'toLong', 'toFloat');


{
    const esc = /([.*+?|(){}[\]\\])/g;
    const qesc = /\$/g;
    RegExp.quote = (s) => s.replace(esc, '\\$1');
    RegExp.quoteReplacement = (s) => s.replace(qesc, '$$$$');
}
RegExp.prototype.dontEnum('quote', 'quoteReplacement');


String.prototype.replaceAll = function replaceAll(pattern, replacement) {
    return this.replace(
            new RegExp(RegExp.quote(pattern), 'g'),
            RegExp.quoteReplacement(replacement)
            );
};

String.prototype.dontEnum(
        'replaceAll'
        );

Function.abstractMethod = function () {
    Error.warn('call to abstract method: this method needs to be overridden in the subclass', -2);
};

function require(modulePath) {
    if (!modulePath.includes("/")) {
        useLibrary(modulePath);
        return globalThis;
    }
    if (require.cache == null) {
        require.cache = {};
    }
    let module = {exports: {}};
    return arkham.plugins.LibImpl.require(globalThis["javax.script.filename"], modulePath, module, module.exports, require.cache);
}
require.cache = null;

if (Packages.resources.Settings.shared.getYesNo("script-compatibility-mode")) {
    useLibrary("backwards-compatibility");
}
