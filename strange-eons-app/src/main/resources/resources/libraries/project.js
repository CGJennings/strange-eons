/*
 
 project.js - version 5
 Support for extending the project system.
 
 */

importClass(java.io.File);
importPackage(arkham.project);
importClass(arkham.project.Member);
importClass(arkham.project.Task);
importClass(arkham.project.TaskAction);
importClass(arkham.project.Project);
importClass(arkham.project.Actions);
importClass(arkham.project.Open);
importClass(arkham.project.ProjectUtilities);
const ProjectUtils = ProjectUtilities;

function TrackedRegistry(regFn, unregFn) {
    if (regFn == null)
        throw new Error('null regFn');
    if (unregFn == null)
        throw new Error('null unregFn');

    var reg = []; // tracks objects registered through this tracker

    this.register = function register(entity) {
        regFn.apply(null, arguments);
        reg.push(entity);
    };

    this.unregister = function unregister(entity) {
        unregFn.apply(null, arguments);
        for (var i = 0; i < reg.length; ++i) {
            if (reg[i] == entity) {
                reg[i].splice(i, 1);
                break;
            }
        }
    };

    this.unregisterAll = function unregisterAll() {
        for (var i = 0; i < reg.length; ++i) {
            unregFn.call(null, reg[i]);
        }
        reg = [];
    };

    // add this new registry to the list of all registries
    var trackers = TrackedRegistry.prototype.trackers;
    trackers[ trackers.length ] = this;
}
TrackedRegistry.prototype.trackers = [];

var ActionRegistry = new TrackedRegistry(
        function register(action, priority) {
            if (priority === undefined) {
                priority = Actions.PRIORITY_DEFAULT;
            }
            Actions.register(action, priority);
        },
        function unregister(action) {
            Actions.unregister(action);
        }
);

var NewActionRegistry = new TrackedRegistry(
        function register(action, after) {
            var newAction = Actions.getUnspecializedAction("new");
            var index = -1;
            if (after != null) {
                if (!(after instanceof TaskAction)) {
                    after = newAction.findActionByName(after.toString());
                }
                if (after != null) {
                    index = newAction.indexOf(after);
                }
            }
            if (index < 0)
                index = newAction.size() - 1;
            newAction.add(index, action);
        },
        function unregister(action) {
            var newAction = Actions.getUnspecializedAction("new");
            newAction.remove(action);
        }
);

var OpenerRegistry = new TrackedRegistry(
        function register(opener) {
            Actions.getUnspecializedAction('open').registerOpener(opener);
        },
        function unregister(opener) {
            Actions.getUnspecializedAction('open').unregisterOpener(opener);
        }
);

var MetadataRegistry = new TrackedRegistry(
        function register(source) {
            Member.registerMetadataSource(source);
        },
        function unregister(source) {
            Member.unregisterMetadataSource(source);
        }
);

var NewTaskTypeRegistry = new TrackedRegistry(
        function register(ntt) {
            NewTaskType.register(ntt);
        },
        function unregister(ntt) {
            NewTaskType.unregister(ntt);
        }
);

function unregisterAll() {
    var trackers = TrackedRegistry.prototype.trackers;
    for (let i = 0; i < trackers.length; ++i) {
        try {
            trackers[i].unregisterAll();
        } catch (ex) {
            Error.handleUncaught(ex);
        }
    }
}

function testProjectScript() {
    if (sourcefile == 'Quickscript') {
        var b = new swing.JButton('Unregister');
        b.addActionListener(function () {
            try {
                unload();
            } catch (error) {
                Error.handleUncaught(error);
            } finally {
                Eons.window.removeCustomComponent(b);
            }
        });
        Eons.window.addCustomComponent(b);
        run();
    }
}

