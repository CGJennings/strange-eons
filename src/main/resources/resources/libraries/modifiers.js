/*
 modifiers.js - version 4
 Support for interpreting modifier key bitmasks.
 */

const SHIFT = PluginContext.SHIFT;
const CONTROL = PluginContext.CONTROL;
const CTRL = PluginContext.CTRL;
const ALT = PluginContext.ALT;
const META = PluginContext.META;
const COMMAND = PluginContext.COMMAND;

function allPressed(modifierState, modifiers) {
    if (modifiers === undefined) {
        modifiers = modifierState;
        modifierState = PluginContext.getModifiers();
    }
    return (modifierState & (SHIFT | CTRL | ALT | META)) === modifiers;
}

function anyPressed(modifierState, modifiers) {
    if (modifiers === undefined) {
        modifiers = modifierState;
        modifierState = PluginContext.getModifiers();
    }
    return (modifierState & modifiers) !== 0;
}