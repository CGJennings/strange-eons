/*

  modifiers.js - version 3
  Support for interpreting modifier key bitmasks.


The SE JavaScript Library Copyright © 2008-2012
Christopher G. Jennings and contributors. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

This software is provided by the author "as is" and any express or implied
warranties, including, but not limited to, the implied warranties of
merchantability and fitness for a particular purpose are disclaimed. In no
event shall the author be liable for any direct, indirect, incidental, special,
exemplary, or consequential damages (including, but not limited to, procurement
of substitute goods or services; loss of use, data, or profits; or business
interruption) however caused and on any theory of liability, whether in
contract, strict liability, or tort (including negligence or otherwise) arising
in any way out of the use of this software, even if advised of the possibility
of such damage.

*/

/**
 * Helper functions that simplifys using <tt>PluginContext.getModifiers()</tt>
 * This method is used to detect the modifier keys that were held down
 * when a plug-in's menu item was clicked.
 *
 * These methods can also be used when interpreting various <tt>InputEvent</tt>s
 * in event listeners.
 */

/**
 * Global Variables
 * The library defines several constants to represent the different modifier keys.
 * Modifiers are encoded as bit masks: to represent multiple keys, take their
 * logical OR. For example, Control and Shift together is represented by the expression
 * <tt>CONTROL|SHIFT</tt>.
 *
 * SHIFT : represents the Shift key
 * CONTROL : represents the Control key
 * CTRL : same as <tt>CONTROL</tt> (different platforms use different spellings)
 * ALT : represents the Alt key
 * META : represents the Meta key found on some *NIX systems
 * COMMAND : represents the Command key under OS X (the same value as <tt>META</tt>)
 * MENU : represents the key used for menu accelerators on this platform (Control on Windows, Command on OS X)
 */
const SHIFT = PluginContext.SHIFT;
const CONTROL = PluginContext.CONTROL;
const CTRL = PluginContext.CTRL;
const ALT = PluginContext.ALT;
const META = PluginContext.META;
const COMMAND = PluginContext.COMMAND;
const MENU = PluginContext.MENU;

/**
 * allPressed( [modifierState], modifiers )
 * Returns <tt>true</tt> if <i>all</i> of the modifier keys represented by
 * <tt>modifiers</tt> are indicated by <tt>modifierState</tt> as
 * being pressed.
 *
 * modifierState : optional bits indicating the modifiers to test against; ...
 *                 defaults to <tt>PluginContext.getModifiers()</tt>.
 * modifiers : the logical OR of all modifiers to include in the test
 */
function allPressed( modifierState, modifiers ) {
	if( modifiers === undefined ) {
		modifiers = modifierState;
		modifierState = PluginContext.getModifiers();
	}
	return (modifierState & (SHIFT|CTRL|ALT|META)) == modifiers;
}

/**
 * anyPressed( [modifierState], modifiers )
 * Returns <tt>true</tt> if <i>any</i> of the modifier keys represented by
 * <tt>modifiers</tt> are indicated by <tt>modifierState</tt> as
 * being pressed.
 *
 * modifierState : optional bits indicating the modifiers to test against; ...
 *                 defaults to <tt>PluginContext.getModifiers()</tt>.
 * modifiers : the logical OR of all modifiers to include in the test
 */
function anyPressed( modifierState, modifiers ) {
	if( modifiers === undefined ) {
		modifiers = modifierState;
		modifierState = PluginContext.getModifiers();
	}
	return (modifierState & modifiers) != 0;
}