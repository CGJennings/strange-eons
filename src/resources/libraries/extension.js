/*
  extension.js - version 8
  Convert a normal plug-in script into an extension plug-in.
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