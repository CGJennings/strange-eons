/*

  extension.js - version 7
  Convert a normal plug-in script into an extension plug-in.


The SE JavaScript Library Copyright © 2008-2013
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
 * Convert this Plug-in into an Extension
 * When this library is included in a plug-in script, the script becomes
 * an extension. (It must still be packaged as a </tt>.seext</tt> bundle
 * to be discovered and loaded at the correct time.)
 *
 * This library defines a <tt>getPluginType()</tt> function that returns the
 * correct type for extensions and imports the following classes from the
 * <tt>gamedata</tt> package into the global namespace:
 * <pre>
 * Game
 * Expansion
 * ClassMap
 * ExpansionSymbolTemplate
 * AbstractExpansionSymbolTemplate
 * SymbolVariantUtilities
 * TileSet
 * Silhouette
 * </pre>
 *
 * <b>Notes:</b> Extensions are loaded and executed before the program is
 * fully initialized. The variables <tt>Editor</tt> and
 * <tt>Component</tt> will be <tt>null</tt>. However, a valid
 * <tt>PluginContext</tt> object is provided. Extensions are never "activated"
 * by the user, so they do not require or use a <tt>run()</tt> function.
 */

const Game = gamedata.Game;
const Expansion = gamedata.Expansion;
const ClassMap = gamedata.ClassMap;
const ExpansionSymbolTemplate = gamedata.ExpansionSymbolTemplate;
const AbstractExpansionSymbolTemplate = gamedata.AbstractExpansionSymbolTemplate;
const SymbolVariantUtilities = gamedata.SymbolVariantUtilities;
const TileSet = gamedata.TileSet;
const Silhouette = gamedata.Silhouette;

function getPluginType() {
    return arkham.plugins.Plugin.EXTENSION;
}

if( useLibrary.compatibilityMode ) {
	useLibrary( 'res://libraries/extension.ljs' );
}
