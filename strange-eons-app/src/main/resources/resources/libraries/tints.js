/*
 
 tints.js - version 5
 Support for card tinting in custom components.
 
 */

const TintingFilter = ca.cgjennings.graphics.filters.TintingFilter;
const TintFilter = ca.cgjennings.graphics.filters.TintFilter;
const TintOverlayFilter = ca.cgjennings.graphics.filters.TintOverlayFilter;
const ReplaceFilter = ca.cgjennings.graphics.filters.ReplaceHueSaturationFilter;
const Tintable = arkham.Tintable;
const TintCache = ca.cgjennings.graphics.filters.TintCache;
// backwards compatibility with 2.x
const SetTintFilter = TintOverlayFilter;