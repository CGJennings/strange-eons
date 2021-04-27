/*
 
 threads.js - version 9
 Threaded execution support.
 
 */

const Thread = function Thread(name, functionToRun) {
    if (functionToRun === undefined) {
        functionToRun = name;
        name = null;
    }
    if (name == null) {
        name = arkham.plugins.ScriptedRunnable.getDefaultThreadName();
    }
    var r = new arkham.plugins.ScriptedRunnable(functionToRun);
    var t = new java.lang.Thread(r, name);
    t.setDaemon(true);
    this.__defineGetter__("name", function name() {
        return name;
    });
    this.__defineGetter__("alive", function alive() {
        return t.isAlive();
    });
    this.__defineGetter__("returnValue", function returnValue() {
        return r.getReturnValue();
    });
    this.__defineGetter__("hasReturnValue", function returnValue() {
        return r.hasReturnValue();
    });
    this.interrupt = function () {
        t.interrupt();
    };
    this.join = function join(waitTime) {
        if (waitTime == null) {
            t.join();
        } else {
            t.join(waitTime);
        }
        return this.returnValue;
    };
    this.start = function start() {
        t.start();
    };
    this.toString = function toString() {
        return name;
    };
};

Thread.interrupted = function interrupted() {
    return java.lang.Thread.interrupted();
};

Thread.busyWindow = function busyWindowContext() {
    function wrap(t) {
        var isCancelled = false;
        var callback = {
            set maximumProgress(max) {
                arkham.BusyDialog.maximumProgress(max);
            },
            set currentProgress(cur) {
                arkham.BusyDialog.currentProgress(cur, 50);
            },
            set title(text) {
                arkham.BusyDialog.titleText(text);
            },
            set status(text) {
                arkham.BusyDialog.statusText(text, 50);
            },
            get cancelled() {
                return isCancelled;
            }
        };
        var wrappedTask = function task() {
            try {
                task.retVal = t(callback);
            } catch (ex) {
                Error.handleUncaught(ex);
            }
        };
        wrappedTask.retVal = null;
        wrappedTask.createCancelAction = function createCancelAction() {
            return new java.awt.event.ActionListener() {
                actionPerformed: function actionPerformed(evt) {
                    isCancelled = true;
                }
            };
        };
        return wrappedTask;
    }

    return function busyWindow(task, message, canCancel) {
        useLibrary.__threadassert();
        if (!message) {
            message = string("busy-script");
        }
        var action = wrap(task);
        var cancelAction = canCancel ? action.createCancelAction() : null;
        new arkham.BusyDialog(null, message, action, cancelAction);
        return action.retVal;
    };
}();

Thread.run = function run(task) {
    var t = new Thread(task);
    t.start();
    return t;
};

Thread.invokeLater = function invokeLater(task) {
    java.awt.EventQueue.invokeLater(function invokeLaterTask() {
        try {
            task();
        } catch (ex) {
            Error.handleUncaught(ex);
        }
    }
    );
};

Thread.invokeAndWait = function invokeAndWait(task) {
    var returnValue = undefined;
    try {
        if (java.awt.EventQueue.isDispatchThread()) {
            return task();
        } else {
            java.awt.EventQueue.invokeAndWait(function invokeLaterTask() {
                try {
                    returnValue = task();
                } catch (ex) {
                    Error.handleUncaught(ex);
                }
            });
        }
    } catch (ex) {
        Error.handleUncaught(ex);
    }
    return returnValue;
};

Thread.Lock = java.util.concurrent.locks.ReentrantLock;
