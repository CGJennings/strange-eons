//
// Modified for SE by C.G. Jennings
//

/*******************************************************************************
 * @license
 * Copyright (c) 2012 VMware, Inc. All Rights Reserved.
 * THIS FILE IS PROVIDED UNDER THE TERMS OF THE ECLIPSE PUBLIC LICENSE
 * ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION OF THIS FILE
 * CONSTITUTES RECIPIENTS ACCEPTANCE OF THE AGREEMENT.
 * You can obtain a current copy of the Eclipse Public License from
 * http://www.opensource.org/licenses/eclipse-1.0.php
 *
 * Contributors:
 *     Andrew Eisenberg (VMware) - initial API and implementation
 ******************************************************************************/

/*
This module defines the built in types for the scripted JS inferencer.
It also contains functions for manipulating internal type signatures.
*/

/*jslint es5:true browser:true*/
/*global define doctrine console */

function types( proposalUtils, scriptedLogger/*, doctrine*/ ) {

	/**
	 * Doctrine closure compiler style type objects
	 */
	function ensureTypeObject(signature) {
		if (!signature) {
			return signature;
		}
		if (signature.type) {
			return signature;
		}
		try {
			return doctrine.parseParamType(signature);
		} catch(e) {
			console.error("doctrine failure to parse: " + signature);
			return {};
		}
	}


	function createNameType(name) {
	    if (typeof name !== 'string') {
	        throw new Error('Expected string, but found: ' + JSON.parse(name));
	    }
		return { type: 'NameExpression', name: name };
	}

	var THE_UNKNOWN_TYPE = createNameType("Object");

	var JUST_DOTS = '$$__JUST_DOTS__$$';
	var JUST_DOTS_REGEX = /\$\$__JUST_DOTS__\$\$/g;
	var UNDEFINED_OR_EMPTY_OBJ = /:undefined|:\{\}/g;


	/**
	 * The Definition class refers to the declaration of an identifier.
	 * The start and end are locations in the source code.
	 * Path is a URL corresponding to the document where the definition occurs.
	 * If range is undefined, then the definition refers to the entire document
	 * Range is a two element array with the start and end values
	 * (Exactly the same range field as is used in Esprima)
	 * If the document is undefined, then the definition is in the current document.
	 *
	 * @param String typeName
	 * @param {[Number]} range
	 * @param String path
	 */
	var Definition = function(typeObj, range, path) {
		this._typeObj = ensureTypeObject(typeObj);
		this.range = range;
		this.path = path;
	};

	Definition.prototype = {
		set typeObj(val) {
			var maybeObj = val;
			if (typeof maybeObj === 'string') {
				maybeObj = ensureTypeObject(maybeObj);
			}
			this._typeObj = maybeObj;
		},

		get typeObj() {
			return this._typeObj;
		},
		
		toString: function() {
			return '{' + this._typeObj + ': ' + this.path +
					(this.range == null ? '' : ' ' + this.range[0] + '-' + this.range[1]) +
					'}';
		}
	};

	/**
	 * Revivies a Definition object from a regular object
	 */
	Definition.revive = function(obj) {
		var defn = new Definition();
		for (var prop in obj) {
			if (obj.hasOwnProperty(prop)) {
				if (prop === 'typeSig') {
					defn.typeObj = obj[prop];
				} else {
					defn[prop] = obj[prop];
				}
			}
		}
		return defn;
	};

	// From ecma script manual 262 section 15
	// the global object when not in browser or node
	var Global = function() {};
	Global.prototype = {
		$$proto : new Definition("Object"),
		
		Eons : new Definition("Eons"),
		Editor : new Definition("Editor"),
		Component : new Definition("Component"),
		PluginContext : new Definition("PluginContext"),
		
		useLibrary : new Definition("function(nameOrUri:String)"),
		sourcefile : new Definition("String"),
		
		exit : new Definition("function()"),
		prompt : new Definition("function(promptMessage:String=,initialValue=)"),
		confirm : new Definition("confirm"),
		alert : new Definition("function(message:String,isErrorMessage:Boolean=)"),
		sleep : new Definition("function(msDelay:Number)"),
		
		Console : new Definition("Console"),
		print : new Definition("function(...values:Object=)"),
		println : new Definition("function(...values:Object=)"),
		printf : new Definition("function(format:String,...values:Object=)"),
		sprintf : new Definition("function(format:String,...values:Object=):String"),
		
		string : new Definition("function(key:String,...values:Object=):String"),
		gstring : new Definition("function(key:String,...values:Object=):String"),
		
		useSettings : new Definition("function(sourceOrGameComponent:Settings)"),
		$ : new Definition("function(settingKey:String):String"),
		$$ : new Definition("function(settingKey:String):LiveSetting"),
		
		useInterfaceLanguage : new Definition("function(language:Language=)"),
		'@' : new Definition("function(uiStringKey:String):String"),
		
		useGameLanguage : new Definition("function(language:Language=)"),
		'#' : new Definition("function(gameStringKey:String):String"),
		
		Patch : new Definition("Patch"),
		
		debug : new Definition("function(logMessage:String)"),
		
		
		Packages : new Definition("Object"), // see below; search "apiproxy"
//		Packages : new Definition("Packages"),
		
//		Color : new Definition("Colour"),
//		Colour : new Definition("Colour"),
//		Font : new Definition("Font"),
//		Region : new Definition("Region"),
//		Region2D : new Definition("Region2D"),
//		URL : new Definition("URL"),
//		ResourceKit : new Definition("ResourceKit"),
//		Language : new Definition("Language"),
		

		decodeURI : new Definition("function(uri:String):String"),
		encodeURI : new Definition("function(uri:String):String"),
		'eval' : new Definition("function(toEval:String):Object"),
		parseInt : new Definition("function(string:String,radix:Number=):Number"),
		parseFloat : new Definition("function(string:String,radix:Number=):Number"),
		Math: new Definition("Math"),
		JSON: new Definition("JSON"),
		Object: new Definition("function(new:Object,val:Object=):Object"),
		Function: new Definition("function(new:Function):Function"),
		Array: new Definition("function(new:Array,val:Array=):Array"),
		Boolean: new Definition("function(new:Boolean,val:Boolean=):Boolean"),
		Number: new Definition("function(new:Number,val:Number=):Number"),
		Date: new Definition("function(new:Date,val:Date=):Date"),
		RegExp: new Definition("function(new:RegExp,val:RegExp=):RegExp"),
		Error: new Definition("function(new:Error,err:Error=):Error"),
		'undefined' : new Definition("undefined"),
		isNaN : new Definition("function(num:Number):Boolean"),
		isFinite : new Definition("function(num:Number):Boolean"),
		"NaN" : new Definition("Number"),
		"Infinity" : new Definition("Number"),
		decodeURIComponent : new Definition("function(encodedURIString:String):String"),
		encodeURIComponent : new Definition("function(decodedURIString:String):String"),

		"this": new Definition("Global")
		// not included since not meant to be referenced directly
		// EvalError, RangeError, ReferenceError, SyntaxError, TypeError, URIError
	};

	var initialGlobalProperties = {};
	Object.keys(Global.prototype).forEach(function(key) {
		initialGlobalProperties[key] = true;
	});


	/**
	 * A prototype that contains the common built-in types
	 */
	var Types = function(globalObjName) {
		var globObj = this.Global = new Global();

		this.clearDefaultGlobal = function() {
//			Object.keys(initialGlobalProperties).forEach(function(key) {
//				delete globObj[key];
//			});
		};
	};


	/**
	 * Populate the Types object with built-in types.  These are not meant to be changed through the inferencing process
	 * This uses the built in types as defined in the ECMA script reference manual 262.  Available at
	 * http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-262.pdf section 15.
	 */
	Types.prototype = {
		
		// CGJ : common library defs
		
		Eons : {
			$$isBuiltin : true,
			$$proto : new Definition("Object"),
			getVersionString : new Definition("function():String"),
			getEditionNumber : new Definition("function():Number"),
			getBuildNumber : new Definition("function():Number"),
			getReleaseType : new Definition("function():Object"),
			getWindow : new Definition("function():Object"),
			getActiveEditor : new Definition("function():Editor"),
			getActiveGameComponent : new Definition("function():Component"),
			createEditor : new Definition("function(classMapKey:String):Editor"),
			addEditor : new Definition("function(ed:Editor)"),
			getOpenProject : new Definition("function():Object"),
			loadPlugins : new Definition("function()"),
			unloadPlugins : new Definition("function()"),
			addPluginLoadingListener : new Definition("function(listener:Object)"),
			removePluginLoadingListener : new Definition("function(listener:Object)"),
			getLoadedPlugin : new Definition("function(identifier:String):Object"),
			getLoadedPluginByName : new Definition("function(name:String):Object"),
			activatePlugin : new Definition("function(plugin:Object,modifiers:Number,show:Boolean)"),
			getLoadedPlugins : new Definition("function():[Object]"),
			getUserStorageFile : new Definition("function(child:String):Object"),
			deleteOnStartup : new Definition("function(fileToDelete:Object,shouldDelete:Boolean):Object"),
			willDeleteOnStartup : new Definition("function(fileToCheck:Object):Boolean"),
			addExitTask : new Definition("function(runnable:Object)"),
			getMarkupTarget : new Definition("function():Object"),
			getCurrentMarkupTarget : new Definition("function():Object"),
			requestNewMarkupTarget : new Definition("function(potentialTarget:Object):Boolean"),
			insertMarkup : new Definition("function(markup:String)"),
			insertMarkupTags : new Definition("function(prefix:String,suffix:String)"),
			getNamedObjects : new Definition("function():Object"),
			fileBugReport : new Definition("function(description:String,throwable:Object=)"),
			getLogEntries : new Definition("function():String"),
			setWaitCursor : new Definition("function(appIsBusy:Boolean)"),
			getCommandLineArguments : new Definition("function():Object"),
			setStartupActivityMessage : new Definition("function(message:String)"),
			getSafeStartupParentWindow : new Definition("function():Object")			
		},
		
		Editor : {
			$$isBuiltin : true,
			$$proto : new Definition("Object"),			
			setTitle : new Definition("function(title:String)"),
			getTitle : new Definition("function():String"),
			setFrameIcon : new Definition("function(icon:Object)"),
			getFrameIcon : new Definition("function():Object"),
			setToolTipText : new Definition("function(text:String)"),
			getToolTipText : new Definition("function():String"),
			select : new Definition("function()"),
			close  : new Definition("function()"),
			setAttached : new Definition("function(attach:Boolean)"),
			isAttached : new Definition("function():Boolean"),
			getTabStripPopupMenu : new Definition("function():Object"),
			getGameComponent : new Definition("function():Object"),
			getFileNameExtension : new Definition("function():String"),
			getFileTypeDescription : new Definition("function():String"),
			setFile : new Definition("function(file:Object)"),
			getFile : new Definition("function():Object"),
			hasUnsavedChanges : new Definition("function():Boolean"),
			save : new Definition("function()"),
			saveAs : new Definition("function()"),
			clear : new Definition("function()"),
			export : new Definition("function()"),
			print : new Definition("function()"),
			spinOff : new Definition("function()"),
			addEditorListener : new Definition("function(listener:Object)"),
			removeEditorListener : new Definition("function(listener:Object)"),
			isCommandApplicable : new Definition("function(command:Object):Boolean"),
			canPerformCommand : new Definition("function(command:Object):Boolean"),
			performCommand : new Definition("function(command:Object)"),
			
			// these valid when Component is an AbstractStrangeEonsEditor
			addHeartbeatListener : new Definition("function(listener:Object)"),
			removeHeartbeatListener : new Definition("function(listener:Object)"),			
			removeAllStrangeEonsListeners : new Definition("function()"),

			// these valid when Component is an AbstractGameComponentEditor
			setDesignSupport : new Definition("function(ds:Object)"),
			replaceEditedComponent : new Definition("function(gc:Object)"),
			getSheetLabels : new Definition("function():[String]"),
			getSelectedSheet : new Definition("function():Object"),
			getSelectedSheetIndex : new Definition("function():Number"),
			setSelectedSheetIndex : new Definition("function(index:Number)"),
			getSheetCount : new Definition("function():Number"),
			getSheet : new Definition("function(index:Number):Object"),
			populateFieldsFromComponent : new Definition("function()"),
			redrawPreview : new Definition("function()"),
			addFieldPopulationListener : new Definition("function(listener:Object)"),
			removeFieldPopulationListener : new Definition("function(listener:Object)"),
			setUnsavedChanges : new Definition("function(hasUnsaved:Boolean)"),
			dispose : new Definition("function()")			
		},
		
		Component : {
			getName : new Definition("function():String"),
			setName : new Definition("function(name:String)"),
			getFullName : new Definition("function():String"),
			getComment : new Definition("function():String"),
			setComment : new Definition("function(text:String)"),
			getSettings : new Definition("function():Object"),
			clearAll : new Definition("function()"),
			getSheets : new Definition("function():[Object]"),
			getSheets : new Definition("function(sheets:[Object])"),
			createDefaultSheets : new Definition("function():[Object]"),
			getSheetTitles : new Definition("function():[String]"),
			createDefaultEditor : new Definition("function():Editor"),
			markChanged : new Definition("function(sheetIndex:Number)"),
			hasChanged : new Definition("function():Boolean"),
			hasUnsavedChanges : new Definition("function():Boolean"),
			markUnsavedChanges : new Definition("function()"),
			markSaved : new Definition("function()"),
			isDeckLayoutSupported : new Definition("function():Boolean"),
			clone : new Definition("function():Component"),
			coreCheck : new Definition("function()"),
			filterComponentText : new Definition("function(text:String):String"),
			computeMinimumScaleForImage : new Definition("function(image:Object,imageKey:String):Number"),
			computeIdealScaleForImage : new Definition("function(image:Object,imageKey:String):Number"),
			
			// defined if component is DIY:
			createTestInstance : new Definition("function(handler:Object,gameCode:String):Component"),
			getHandlerScript : new Definition("function():String"),
			getExtensionName : new Definition("function():String"),
			setExtensionName : new Definition("function(name:String)"),
			getVersion : new Definition("function():Number"),
			setVersion : new Definition("function(ver:Number)"),
			getFaceStyle : new Definition("function():Object"),
			setFaceStyle : new Definition("function(style:Object)"),
			getFrontTemplateKey : new Definition("function():String"),
			setFrontTemplateKey : new Definition("function(key:String)"),
			getBackTemplateKey : new Definition("function():String"),
			setBackTemplateKey : new Definition("function(key:String)"),			
			getTemplateKey : new Definition("function(index:Number):String"),
			setBackTemplateKey : new Definition("function(index:Number,key:String)"),
			setSheetTitle : new Definition("function(index:Number,title:String)"),
			getBleedMargin : new Definition("function():Number"),
			setBleedMargin : new Definition("function(marginInPoints:Number)"),
			getHighResolutionSubstitutionMode : new Definition("function():Object"),
			setHighResolutionSubstitutionMode : new Definition("function(mode:Object)"),
			getCustomFoldMarks : new Definition("function(index:Number):[Number]"),
			setCustomFoldMarks : new Definition("function(index:Number,foldMarkTuples:[Number])"),
			getTransparentFaces : new Definition("function():Boolean"),
			setTransparentFaces : new Definition("function(transparent:Boolean)"),
			getVariableSizedFaces : new Definition("function():Boolean"),
			setVariableSizedFaces : new Definition("function(transparent:Boolean)"),
			getDeckSnappingHint : new Definition("function():Object"),
			setDeckSnappingHint : new Definition("function(hint:Object)"),
			getPortraitKey : new Definition("function():String"),
			setPortraitKey : new Definition("function(name:String)"),
			isPortraitBackgroundFilled : new Definition("function():Boolean"),
			setPortraitBackgroundFilled : new Definition("function(transparent:Boolean)"),
			getPortraitScaleUsesMinimum : new Definition("function():Boolean"),
			setPortraitScaleUsesMinimum : new Definition("function(useMinimum:Boolean)"),			
			getPortraitClipping : new Definition("function():Boolean"),
			setPortraitClipping : new Definition("function(clip:Boolean)"),		
			setPortraitClipStencil : new Definition("function(image:Object)"),
			setPortraitClipStencilRegion : new Definition("function(region:Object)"),
			isMarkerBackgroundFilled : new Definition("function():Boolean"),
			setMarkerBackgroundFilled : new Definition("function(transparent:Boolean)"),
			getMarkerScaleUsesMinimum : new Definition("function():Boolean"),
			setMarkerScaleUsesMinimum : new Definition("function(useMinimum:Boolean)"),			
			getMarkerClipping : new Definition("function():Boolean"),
			setMarkerClipping : new Definition("function(clip:Boolean)"),		
			setMarkerClipStencil : new Definition("function(image:Object)"),
			setMarkerClipStencilRegion : new Definition("function(region:Object)"),			
			getMarkerStyle : new Definition("function():Object"),
			setMarkerStyle : new Definition("function(style:Object)"),
			isCustomPortraitHandling : new Definition("function():Boolean"),
			setCustomPortraitHandling : new Definition("function(scripted:Boolean)"),
			getPortraitCount : new Definition("function():Number"),
			getPortrait : new Definition("function(index:Number):Object"),
			getPortraitSource : new Definition("function():String"),
			setPortraitSource : new Definition("function(source:String)"),
			getPortraitPanX : new Definition("function():Number"),
			setPortraitPanX : new Definition("function(x:Number)"),			
			getPortraitPanY : new Definition("function():Number"),
			setPortraitPanY : new Definition("function(y:Number)"),
			getPortraitScale : new Definition("function():Number"),
			setPortraitScale : new Definition("function(scale:Number)"),
			getNameField : new Definition("function():Object"),
			setNameField : new Definition("function(uiComponent:Object)"),
			
			// Handler implementation
			onClear : new Definition("function()"),
			create : new Definition("function(diy:Component)"),
			createInterface : new Definition("function(diy:Component,diyEditor:Editor)"),
			createFrontPainter : new Definition("function(diy:Component,diySheet:Object)"),
			createBackPainter : new Definition("function(diy:Component,diySheet:Object)"),
			paintFront : new Definition("function(g:Object,diy:Component,diySheet:Object)"),
			paintBack : new Definition("function(g:Object,diy:Component,diySheet:Object)"),
			onRead : new Definition("function(diy:Component,objectInputStream:Object)"),
			onWrite : new Definition("function(diy:Component,objectOutputStream:Object)")			
		},
		
		PluginContext : {
			getPlugin : new Definition("function():Object"),
			getInstalledPlugin : new Definition("function():Object"),
			isInformationProbe : new Definition("function():Boolean"),
			getSettings : new Definition("function():Object"),
			getModifiers : new Definition("function():Number")			
		},
		
		// Project
		
		Console : {
			$$isBuiltin : true,
			$$proto : new Definition("Object"),
			print : new Definition("function(...args:Object=)"),
			println : new Definition("function(...args:Object=)"),
			printf : new Definition("function(format:String,...values:Object=)"),
			printImage : new Definition("function(imageOrIcon:Image)"),
			printComponent : new Definition("function(uiComponent:JComponent)"),
			printHTML : new Definition("function(simpleHtml:String)"),
			clear : new Definition("function()"),
			history : new Definition("function():String"),
			visible : new Definition("Boolean"),
			queue : new Definition("function()"),
			flush : new Definition("function()"),
			out : new Definition("PrintWriter"),
			err : new Definition("PrintWriter")
		},
		
		Patch : {
			$$isBuiltin : true,
			$$proto : new Definition("Object"),
			apply : new Definition("function(...keysAndValues:String)"),
			restore : new Definition("function(...keys:String)"),
			temporary : new Definition("function(...keysAndValues:String)"),
			card : new Definition("function(gameComponent:Component,...keysAndValues:String)"),
			cardFrom : new Definition("function(gameComponent:Component,resource:String)"),
			cardRestore : new Definition("function(gameComponent:Component,...keys:String)")
		},
		
		confirm : {
			$$isBuiltin : true,
			$$proto : new Definition("Object"),
			confirm : new Definition("function(promptMsg:String,title:String=):Boolean"),
			yesno : new Definition("function(promptMsg:String,title:String=):Boolean"),
			choose : new Definition("function(promptMsg:String,title:String,option1:String,...options:String=):Boolean")
		},
		"function(new:confirm)" : {
			$$isBuiltin: true,
			$$proto : new Definition("Object"),
		},		

		/**
		 * See 15.2.4 Properties of the Object Prototype Object
		 */
		Object : {
			$$isBuiltin: true,
			// Can't use the real propoerty name here because would override the real methods of that name
			$_$prototype : new Definition("Object"),
			$_$toString: new Definition("function():String"),
			$_$toLocaleString : new Definition("function():String"),
			$_$valueOf: new Definition("function():Object"),
			$_$hasOwnProperty: new Definition("function(property:String):Boolean"),
			$_$isPrototypeOf: new Definition("function(object:Object):Boolean"),
			$_$propertyIsEnumerable: new Definition("function(property:String):Boolean")
		},

		/**
		 * See 15.3.4 Properties of the Function Prototype Object
		 */
		Function : {
			$$isBuiltin: true,
			apply : new Definition("function(func:function(),argArray:Array=):Object"),
			"arguments" : new Definition("Arguments"),
			bind : new Definition("function(func:function(),...args:Object):Object"),
			call : new Definition("function(func:function(),...args:Object):Object"),
			caller : new Definition("Function"),
			length : new Definition("Number"),
			name : new Definition("String"),
			$$proto : new Definition("Object"),
			
			// CGJ defined in common lib:
			abstractMethod : new Definition("function()"),
			subclass : new Definition("function(superConstructor:Function)")
		},

		/**
		 * See 15.4.4 Properties of the Array Prototype Object
		 */
		Array : {
			$$isBuiltin: true,

			concat : new Definition("function(first:Array,...rest:Array):Array"),
			join : new Definition("function(separator:Object):String"),
			length : new Definition("Number"),
			pop : new Definition("function():Object"),
			push : new Definition("function(...vals:Object):Object"),
			reverse : new Definition("function():Array"),
			shift : new Definition("function():Object"),
			slice : new Definition("function(start:Number,deleteCount:Number,...items:Object):Array"),
			splice : new Definition("function(start:Number,end:Number):Array"),
			sort : new Definition("function(sorter:Object=):Array"),
			unshift : new Definition("function(...items:Object):Number"),
			indexOf : new Definition("function(searchElement,fromIndex=):Number"),
			lastIndexOf : new Definition("function(searchElement,fromIndex=):Number"),
			every : new Definition("function(callbackFn:function(elt:Object),thisArg:Object=):Boolean"),
			some : new Definition("function(callbackFn:function(elt:Object),thisArg:Object=):Boolean"),
			forEach : new Definition("function(callbackFn:function(elt:Object),thisArg:Object=):Object"),
			map : new Definition("function(callbackFn:function(elt:Object):Object,thisArg:Object=):Array"),
			filter : new Definition("function(callbackFn:function(elt:Object):Boolean,thisArg:Object=):Array"),
			reduce : new Definition("function(callbackFn:function(elt:Object):Object,initialValue:Object=):Array"),
			reduceRight : new Definition("function(callbackFn:function(elt:Object):Object,initialValue:Object=):Array"),
			$$proto : new Definition("Object"),
			
			// CGJ defined in common lib:
			from : new Definition("function(arrayLikeObj:Object):Array")			
		},

		/**
		 * See 15.5.4 Properties of the String Prototype Object
		 */
		String : {
			$$isBuiltin: true,
			charAt : new Definition("function(index:Number):String"),
			charCodeAt : new Definition("function(index:Number):Number"),
			concat : new Definition("function(str:String):String"),
			indexOf : new Definition("function(searchString:String,start:Number=):Number"),
			lastIndexOf : new Definition("function(searchString:String,start:Number=):Number"),
			length : new Definition("Number"),
			localeCompare : new Definition("function(str:String):Number"),
			match : new Definition("function(regexp:(String|RegExp)):Boolean"),
			replace : new Definition("function(searchValue:(String|RegExp),replaceValue:String):String"),
			search : new Definition("function(regexp:(String|RegExp)):String"),
			slice : new Definition("function(start:Number,end:Number):String"),
			split : new Definition("function(separator:String,limit:Number=):[String]"),  // Array of string
			substring : new Definition("function(start:Number,end:Number=):String"),
			toLocaleUpperCase : new Definition("function():String"),
			toLowerCase : new Definition("function():String"),
			toLocaleLowerCase : new Definition("function():String"),
			toUpperCase : new Definition("function():String"),
			trim : new Definition("function():String"),

			$$proto : new Definition("Object"),
			
			// CGJ defined in common lib:
			trimLeft : new Definition("function():String"),
			trimRight : new Definition("function():String"),
			replaceAll : new Definition("function(pattern:String,replacement:String):String"),
			startsWith : new Definition("function(pattern:String):Boolean"),
			endsWith : new Definition("function(pattern:String):Boolean")
		},

		/**
		 * See 15.6.4 Properties of the Boolean Prototype Object
		 */
		Boolean : {
			$$isBuiltin: true,
			$$proto : new Definition("Object")
		},

		/**
		 * See 15.7.4 Properties of the Number Prototype Object
		 */
		Number : {
			$$isBuiltin: true,
			toExponential : new Definition("function(digits:Number):String"),
			toFixed : new Definition("function(digits:Number):String"),
			toPrecision : new Definition("function(digits:Number):String"),
			// do we want to include NaN, MAX_VALUE, etc?

			$$proto : new Definition("Object"),
			
			// CGJ defined in common lib:
			toInt : new Definition("function(number:Number):Integer"),
			toLong : new Definition("function(number:Number):Long"),
			toFloat : new Definition("function(number:Number):Float")
		},

		/**
		 * See 15.8.1 15.8.2 Properties and functions of the Math Object
		 * Note that this object is not used as a prototype to define other objects
		 */
		Math : {
			$$isBuiltin: true,

			// properties
			E : new Definition("Number"),
			LN2 : new Definition("Number"),
			LN10 : new Definition("Number"),
			LOG2E : new Definition("Number"),
			LOG10E : new Definition("Number"),
			PI : new Definition("Number"),
			SQRT1_2 : new Definition("Number"),
			SQRT2 : new Definition("Number"),

			// Methods
			abs : new Definition("function(val:Number):Number"),
			acos : new Definition("function(val:Number):Number"),
			asin : new Definition("function(val:Number):Number"),
			atan : new Definition("function(val:Number):Number"),
			atan2 : new Definition("function(val1:Number,val2:Number):Number1"),
			ceil : new Definition("function(val:Number):Number"),
			cos : new Definition("function(val:Number):Number"),
			exp : new Definition("function(val:Number):Number"),
			floor : new Definition("function(val:Number):Number"),
			log : new Definition("function(val:Number):Number"),
			max : new Definition("function(val1:Number,val2:Number):Number"),
			min : new Definition("function(val1:Number,val2:Number):Number"),
			pow : new Definition("function(x:Number,y:Number):Number"),
			random : new Definition("function():Number"),
			round : new Definition("function(val:Number):Number"),
			sin : new Definition("function(val:Number):Number"),
			sqrt : new Definition("function(val:Number):Number"),
			tan : new Definition("function(val:Number):Number"),
			$$proto : new Definition("Object")
		},


		/**
		 * See 15.9.5 Properties of the Date Prototype Object
		 */
		Date : {
			$$isBuiltin: true,
			toDateString : new Definition("function():String"),
			toTimeString : new Definition("function():String"),
			toUTCString : new Definition("function():String"),
			toISOString : new Definition("function():String"),
			toJSON : new Definition("function(key:String):Object"),
			toLocaleDateString : new Definition("function():String"),
			toLocaleTimeString : new Definition("function():String"),

			getTime : new Definition("function():Number"),
			getTimezoneOffset : new Definition("function():Number"),

			getDay : new Definition("function():Number"),
			getUTCDay : new Definition("function():Number"),
			getFullYear : new Definition("function():Number"),
			getUTCFullYear : new Definition("function():Number"),
			getHours : new Definition("function():Number"),
			getUTCHours : new Definition("function():Number"),
			getMinutes : new Definition("function():Number"),
			getUTCMinutes : new Definition("function():Number"),
			getSeconds : new Definition("function():Number"),
			getUTCSeconds : new Definition("function():Number"),
			getMilliseconds : new Definition("function():Number"),
			getUTCMilliseconds : new Definition("function():Number"),
			getMonth : new Definition("function():Number"),
			getUTCMonth : new Definition("function():Number"),
			getDate : new Definition("function():Number"),
			getUTCDate : new Definition("function():Number"),

			setTime : new Definition("function():Number"),
			setTimezoneOffset : new Definition("function():Number"),

			setDay : new Definition("function(dayOfWeek:Number):Number"),
			setUTCDay : new Definition("function(dayOfWeek:Number):Number"),
			setFullYear : new Definition("function(year:Number,month:Number=,date:Number=):Number"),
			setUTCFullYear : new Definition("function(year:Number,month:Number=,date:Number=):Number"),
			setHours : new Definition("function(hour:Number,min:Number=,sec:Number=,ms:Number=):Number"),
			setUTCHours : new Definition("function(hour:Number,min:Number=,sec:Number=,ms:Number=):Number"),
			setMinutes : new Definition("function(min:Number,sec:Number=,ms:Number=):Number"),
			setUTCMinutes : new Definition("function(min:Number,sec:Number=,ms:Number=):Number"),
			setSeconds : new Definition("function(sec:Number,ms:Number=):Number"),
			setUTCSeconds : new Definition("function(sec:Number,ms:Number=):Number"),
			setMilliseconds : new Definition("function(ms:Number):Number"),
			setUTCMilliseconds : new Definition("function(ms:Number):Number"),
			setMonth : new Definition("function(month:Number,date:Number=):Number"),
			setUTCMonth : new Definition("function(month:Number,date:Number=):Number"),
			setDate : new Definition("function(date:Number):Number"),
			setUTCDate : new Definition("function(date:Number):Number"),

			$$proto : new Definition("Object")
		},

		/**
		 * See 15.10.6 Properties of the RexExp Prototype Object
		 */
		RegExp : {
			$$isBuiltin: true,
			source : new Definition("String"),
			global : new Definition("Boolean"),
			ignoreCase : new Definition("Boolean"),
			multiline : new Definition("Boolean"),
			lastIndex : new Definition("Boolean"),

			exec : new Definition("function(str:String):[String]"),
			test : new Definition("function(str:String):Boolean"),

			$$proto : new Definition("Object"),
			
			// CGJ defined in common lib:
			quote : new Definition("function(string:String):String"),
			quoteReplacement : new Definition("function(string:String):String")			
		},

		"function(new:RegExp):RegExp" : {
			$$isBuiltin: true,
			$$proto : new Definition("Function"),

			$1 : new Definition("String"),
			$2 : new Definition("String"),
			$3 : new Definition("String"),
			$4 : new Definition("String"),
			$5 : new Definition("String"),
			$6 : new Definition("String"),
			$7 : new Definition("String"),
			$8 : new Definition("String"),
			$9 : new Definition("String"),
			$_ : new Definition("String"),
			$input : new Definition("String"),
			input : new Definition("String"),
			name : new Definition("String")
		},


		/**
		 * See 15.11.4 Properties of the Error Prototype Object
		 * We don't distinguish between kinds of errors
		 */
		Error : {
			$$isBuiltin: true,
			$$proto : new Definition("Object"),
			name : new Definition("String"),
			message : new Definition("String"),
			stack : new Definition("String"),
			
			// CGJ defined in common lib:
			error : new Definition("function(toThrow:Error=)"),
			warn : new Definition("function(message:String=,relativeStackFrame:Number=)"),
			deprecated : new Definition("function(message:String=,relativeStackFrame:Number=)"),
			handleUncaught : new Definition("function(exception:Object)")			
		},

		/**
		 * See 10.6 Arguments Object
		 */
		Arguments : {
			$$isBuiltin: true,
			callee : new Definition("Function"),
			length : new Definition("Number"),

			$$proto : new Definition("Object")
		},

		/**
		 * See 15.12.2 and 15.12.3 Properties of the JSON Object
		 */
		JSON : {
			$$isBuiltin: true,

			parse : new Definition("function(jsonString:String):Object"),
			stringify : new Definition("function(object:Object):String"),
			$$proto : new Definition("Object")
		},

		"undefined" : {
			$$isBuiltin: true
		}
	};

	//
	// CGJ Add Java packages and classes
	//
	
//	function apiProxyGetter( proxyParent, childCnode ) {
//		return function() {
//			if( childCnode.getJavaClass ) {
//				let name = childCnode.name;
//				return new Definition( "function(new:" + name + "):" + name );
//			}
//			
//			let name = childCnode.name;
//			println( 'creating proxy for ' + name );
//			let proxyChild = createApiProxy( childCnode );
//			delete proxyParent[ name ];
//			proxyParent[ name ] = proxyChild;
//			return proxyChild;
//		};
//	}
//
//	function createApiProxy( parentCnode ) {
//		let proxy = {
//			$$isBuiltin : true,
//			$$proto : new Definition("Object"),
//			__cnode : parentCnode
//		};
//		let it=parentCnode.children().iterator();
//		for( ; it.hasNext() ; ) {			
//			let child = it.next();
////			println("Child " + child.name);
//			proxy.__defineGetter__( child.name, apiProxyGetter( this, child ) );
//		}
//		return proxy;
//	}
//	
//	Types.prototype.Packages = createApiProxy( ca.cgjennings.ui.textedit.completion.APIDatabase.getPackageRoot() );

	var protoLength = "~proto".length;
	return {
		Types : Types,
		Definition : Definition,

		// now some functions that handle types signatures, styling, and parsing

		/** constant that defines generated type name prefixes */
		GEN_NAME : "gen~",


		// type parsing
		isArrayType : function(typeObj) {
			return typeObj.type === 'ArrayType' || typeObj.type === 'TypeApplication';
		},

		isFunctionOrConstructor : function(typeObj) {
			return typeObj.type === 'FunctionType';
		},

		isPrototypeName : function(typeName) {
			return typeName.substr( - protoLength, protoLength) === "~proto";
		},

		/**
		 * returns a parameterized array type with the given type parameter
		 */
		parameterizeArray : function(parameterTypeObj) {
			return {
				type: 'ArrayType',
				elements: [parameterTypeObj]
			};
		},

		createFunctionType : function(params, result, isConstructor) {
			var functionTypeObj = {
				type: 'FunctionType',
				params: params,
				result: result
			};
			if (isConstructor) {
				functionTypeObj.params = functionTypeObj.params || [];
			    // TODO should we also do 'this'?
				functionTypeObj.params.push({
					type: 'ParameterType',
					name: 'new',
					expression: result
				});
			}

			return functionTypeObj;
		},

		/**
		 * If this is a parameterized array type, then extracts the type,
		 * Otherwise object
		 */
		extractArrayParameterType : function(arrayObj) {
			var elts;
			if (arrayObj.type === 'TypeApplication') {
				if (arrayObj.expression.name === 'Array') {
					elts = arrayObj.applications;
				} else {
					return arrayObj.expression;
				}
			} else if (arrayObj.type === 'ArrayType') {
				elts = arrayObj.elements;
			} else {
				// not an array type
				return arrayObj;
			}

			if (elts.length > 0) {
				return elts[0];
			} else {
				return THE_UNKNOWN_TYPE;
			}
		},

		extractReturnType : function(fnType) {
			return fnType.result || (fnType.type === 'FunctionType' ? this.UNDEFINED_TYPE: fnType);
		},

		// TODO should we just return a typeObj here???
		parseJSDocComment : function(docComment) {
			var result = { };
			result.params = {};
			if (docComment) {
				var commentText = docComment.value;
				if (!commentText) {
					return result;
				}
				try {
					var rawresult = doctrine.parse("/*" + commentText + "*/", {unwrap : true, tags : ['param', 'type', 'return']});
					// transform result into something more manageable
					var rawtags = rawresult.tags;
					if (rawtags) {
						for (var i = 0; i < rawtags.length; i++) {
							switch (rawtags[i].title) {
								case "typedef":
								case "define":
								case "type":
									result.type = rawtags[i].type;
									break;
								case "return":
									result.rturn = rawtags[i].type;
									break;
								case "param":
									// remove square brackets
									var name = rawtags[i].name;
									if (name.charAt(0) === '[' && name.charAt(name.length -1) === ']') {
										name = name.substring(1, name.length-1);
									}
									result.params[name] = rawtags[i].type;
									break;
							}
						}
					}
				} catch (e) {
					scriptedLogger.error(e.message, "CONTENT_ASSIST");
					scriptedLogger.error(e.stack, "CONTENT_ASSIST");
					scriptedLogger.error("Error parsing doc comment:\n" + (docComment && docComment.value),
							"CONTENT_ASSIST");
				}
			}
			return result;
		},


		/**
		 * takes this jsdoc type and recursively splits out all record types into their own type
		 * also converts unknown name types into Objects
		 * @see https://developers.google.com/closure/compiler/docs/js-for-compiler
		 */
		convertJsDocType : function(jsdocType, env, doCombine, depth) {
		    if (typeof depth !== 'number') {
		        depth = 0;
		    }
			if (!jsdocType) {
				return THE_UNKNOWN_TYPE;
			}

			var self = this;
			var name = jsdocType.name;
			var allTypes = env.getAllTypes();
			switch (jsdocType.type) {
				case 'NullableLiteral':
				case 'AllLiteral':
				case 'NullLiteral':
				case 'UndefinedLiteral':
				case 'VoidLiteral':
					return {
						type: jsdocType.type
					};

				case 'UnionType':
					return {
						type: jsdocType.type,
						elements: jsdocType.elements.map(function(elt) {
							return self.convertJsDocType(elt, env, doCombine, depth);
						})
					};

				case 'RestType':
					return {
						type: jsdocType.type,
						expression: self.convertJsDocType(jsdocType.expression, env, doCombine, depth)
					};

				case 'ArrayType':
					return {
						type: jsdocType.type,
						elements: jsdocType.elements.map(function(elt) {
							return self.convertJsDocType(elt, env, doCombine, depth);
						})
					};

				case 'FunctionType':
					var fnType = {
						type: jsdocType.type,
						params: jsdocType.params.map(function(elt) {
							return self.convertJsDocType(elt, env, doCombine, depth);
						})
					};
					if (jsdocType.result) {
						// prevent recursion on functions that return themselves
						fnType.result = depth > 1 && jsdocType.result.type === 'FunctionType' ?
							{ type : 'NameExpression', name : JUST_DOTS } :
							self.convertJsDocType(jsdocType.result, env, doCombine, depth);
					}

					// TODO should remove?  new and this are folded into params
//					if (jsdocType['new']) {
//						// prevent recursion on functions that return themselves
//						fnType['new'] = depth < 2 && jsdocType['new'].type === 'FunctionType' ?
//							self.convertJsDocType(jsdocType['new'], env, doCombine, depth) :
//							{ type : 'NameExpression', name : JUST_DOTS };
//					}
//
//					if (jsdocType['this']) {
//						// prevent recursion on functions that return themselves
//						fnType['this'] = depth < 2 && jsdocType['this'].type === 'FunctionType' ?
//							self.convertJsDocType(jsdocType['this'], env, doCombine, depth) :
//							{ type : 'NameExpression', name : JUST_DOTS };
//					}

					return fnType;

				case 'TypeApplication':
					var typeApp = {
						type: jsdocType.type,
						expression: self.convertJsDocType(jsdocType.expression, env, doCombine, depth),

					};
					if (jsdocType.applications) {
                        typeApp.applications = jsdocType.applications.map(function(elt) {
							return self.convertJsDocType(elt, env, doCombine, depth);
						});
					}
					return typeApp;

				case 'ParameterType':
					return {
						type: jsdocType.type,
						name: name,
						expression: jsdocType.expression ?
							self.convertJsDocType(jsdocType.expression, env, doCombine, depth) :
							null
					};

				case 'NonNullableType':
				case 'OptionalType':
				case 'NullableType':
					return {
						prefix: true,
						type: jsdocType.type,
						expression: self.convertJsDocType(jsdocType.expression, env, doCombine, depth)
					};

				case 'NameExpression':
					if (doCombine && env.isSyntheticName(name)) {
						// Must mush together all properties for this synthetic type
						var origFields = allTypes[name];
						// must combine a record type
						var newFields = [];
						Object.keys(origFields).forEach(function(key) {
							if (key === '$$proto') {
								// maybe should traverse the prototype
								return;
							}
							var prop = origFields[key];
							var fieldType = depth > 0 && (prop.typeObj.type === 'NameExpression' && env.isSyntheticName(prop.typeObj.name)) ?
							     { type : 'NameExpression', name : JUST_DOTS } :
							     self.convertJsDocType(prop.typeObj, env, doCombine, depth+1);
							newFields.push({
								type: 'FieldType',
								key: key,
								value: fieldType
							});
						});


						return {
							type: 'RecordType',
							fields: newFields
						};
					} else {
						if (allTypes[name]) {
							return { type: 'NameExpression', name: name };
						} else {
							var capType = name[0].toUpperCase() + name.substring(1);
							if (allTypes[capType]) {
								return { type: 'NameExpression', name: capType };
							}
						}
					}
					return THE_UNKNOWN_TYPE;

				case 'FieldType':
					return {
						type: jsdocType.type,
						key: jsdocType.key,
						value: self.convertJsDocType(jsdocType.value, env, doCombine, depth)
					};

				case 'RecordType':
					if (doCombine) {
						// when we are combining, do not do anything special for record types
						return {
							type: jsdocType.type,
							params: jsdocType.fields.map(function(elt) {
								return self.convertJsDocType(elt, env, doCombine, depth+1);
							})
						};
					} else {
						// here's where it gets interesting
						// create a synthetic type in the env and then
						// create a property in the env type for each record property
						var fields = { };
						for (var i = 0; i < jsdocType.fields.length; i++) {
							var field = jsdocType.fields[i];
							var convertedField = self.convertJsDocType(field, env, doCombine, depth+1);
							fields[convertedField.key] = convertedField.value;
						}
						// create a new type to store the record
						var obj = env.newFleetingObject();
						for (var prop in fields) {
							if (fields.hasOwnProperty(prop)) {
								// add the variable to the new object, which happens to be the top-level scope
								env.addVariable(prop, obj.name, fields[prop]);
							}
						}
						return obj;
					}
			}
			return THE_UNKNOWN_TYPE;
		},

		createNameType : createNameType,

		createParamType : function(name, typeObj) {
			return {
				type: 'ParameterType',
				name: name,
				expression: typeObj
			};
		},

		convertToSimpleTypeName : function(typeObj) {
			switch (typeObj.type) {
				case 'NullableLiteral':
				case 'AllLiteral':
				case 'NullLiteral':
					return "Object";

				case 'UndefinedLiteral':
				case 'VoidLiteral':
					return "undefined";

				case 'NameExpression':
					return typeObj.name;

				case 'TypeApplication':
				case 'ArrayType':
					return "Array";

				case 'FunctionType':
					return "Function";

				case 'UnionType':
					return typeObj.expressions && typeObj.expressions.length > 0 ?
						this.convertToSimpleTypeName(typeObj.expressions[0]) :
						"Object";

				case 'RecordType':
					return "Object";

				case 'FieldType':
					return this.convertToSimpleTypeName(typeObj.value);

				case 'NonNullableType':
				case 'OptionalType':
				case 'NullableType':
				case 'ParameterType':
					return this.convertToSimpleTypeName(typeObj.expression);
			}
		},

		// type styling
		styleAsProperty : function(prop, useHtml) {
			return useHtml ? '<span style="color: blue;font-weight:bold;">' + prop + '</span>': prop;
		},
		styleAsType : function(type, useHtml) {
			return useHtml ? '<span style="color: black;">' + type + '</span>': type;
		},
		styleAsOther : function(text, useHtml) {
			return useHtml ? '<span style="font-weight:bold; color:purple;">' + text + '</span>': text;
		},


		/**
		 * creates a human readable type name from the name given
		 */
		createReadableType : function(typeObj, env, useFunctionSig, depth, useHtml) {
			if (useFunctionSig) {
				typeObj = this.convertJsDocType(typeObj, env, true);
				if (useHtml) {
					return this.convertToHtml(typeObj, 0);
				}
				var res = doctrine.type.stringify(typeObj, {compact: true});
				res = res.replace(JUST_DOTS_REGEX, "{...}");
				res = res.replace(UNDEFINED_OR_EMPTY_OBJ, "");
				return res;
			} else {
				typeObj = this.extractReturnType(typeObj);
				return this.createReadableType(typeObj, env, true, depth, useHtml);
			}
		},
		convertToHtml : function(typeObj, depth) {
			// typeObj must already be converted to avoid infinite loops
//			typeObj = this.convertJsDocType(typeObj, env, true);
			var self = this;
			var res;
			var parts = [];
			depth = depth || 0;

			switch(typeObj.type) {
				case 'NullableLiteral':
					return this.styleAsType("?", true);
				case 'AllLiteral':
					return this.styleAsType("*", true);
				case 'NullLiteral':
					return this.styleAsType("null", true);
				case 'UndefinedLiteral':
					return this.styleAsType("undefined", true);
				case 'VoidLiteral':
					return this.styleAsType("void", true);

				case 'NameExpression':
					var name = typeObj.name === JUST_DOTS ? "{...}" : typeObj.name;
					return this.styleAsType(name, true);

				case 'UnionType':
					parts = [];
					if (typeObj.expressions) {
						typeObj.expressions.forEach(function(elt) {
							parts.push(self.convertToHtml(elt, depth+1));
						});
					}
					return "( " + parts.join(", ") + " )";



				case 'TypeApplication':
					if (typeObj.applications) {
						typeObj.applications.forEach(function(elt) {
							parts.push(self.convertToHtml(elt, depth));
						});
					}
					var isArray = typeObj.expression.name === 'Array';
					if (!isArray) {
						res = this.convertToHtml(typeObj.expression, depth) + ".<";
					}
					res += parts.join(",");
					if (isArray) {
						res += '[]';
					} else {
						res += ">";
					}
					return res;
				case 'ArrayType':
					if (typeObj.elements) {
						typeObj.elements.forEach(function(elt) {
							parts.push(self.convertToHtml(elt, depth+1));
						});
					}
					return parts.join(", ") + '[]';

				case 'NonNullableType':
					return "!" +  this.convertToHtml(typeObj.expression, depth);
				case 'OptionalType':
					return this.convertToHtml(typeObj.expression, depth) + "=";
				case 'NullableType':
					return "?" +  this.convertToHtml(typeObj.expression, depth);
				case 'RestType':
					return "..." +  this.convertToHtml(typeObj.expression, depth);

				case 'ParameterType':
					return this.styleAsProperty(typeObj.name, true) +
						(typeObj.expression.name === JUST_DOTS ? "" : (":" + this.convertToHtml(typeObj.expression, depth)));

				case 'FunctionType':
					var isCons = false;
					var resType;
					if (typeObj.params) {
						typeObj.params.forEach(function(elt) {
							if (elt.name === 'this') {
								isCons = true;
								resType = elt.expression;
							} else if (elt.name === 'new') {
								isCons = true;
								resType = elt.expression;
							} else {
								parts.push(self.convertToHtml(elt, depth+1));
							}
						});
					}

					if (!resType && typeObj.result) {
						resType = typeObj.result;
					}

					var resText;
					if (resType && resType.type !== 'UndefinedLiteral' && resType.name !== 'undefined') {
						resText = this.convertToHtml(resType, depth+1);
					} else {
						resText = '';
					}
					res = this.styleAsOther(isCons ? 'new ' : 'function', true);
					if (isCons) {
						res += resText;
					}
					res += '(' + parts.join(",") + ')';
					if (!isCons && resText) {
						res += '&rarr;' + resText;
					}

					return res;

				case 'RecordType':
					if (typeObj.fields && typeObj.fields.length > 0) {
						typeObj.fields.forEach(function(elt) {
							parts.push(proposalUtils.repeatChar('&nbsp;&nbsp;', depth+1) + self.convertToHtml(elt, depth+1));
						});
						return '{<br/>' + parts.join(',<br/>') + '<br/>' + proposalUtils.repeatChar('&nbsp;&nbsp;', depth) + '}';
					} else {
						return '{ }';
					}
					break;

				case 'FieldType':
					return this.styleAsProperty(typeObj.key, true) +
						":" + this.convertToHtml(typeObj.value, depth);
			}

		},
		ensureTypeObject: ensureTypeObject,
		OBJECT_TYPE: THE_UNKNOWN_TYPE,
		UNDEFINED_TYPE: createNameType("undefined"),
		NUMBER_TYPE: createNameType("Number"),
		BOOLEAN_TYPE: createNameType("Boolean"),
		STRING_TYPE: createNameType("String"),
		ARRAY_TYPE: createNameType("Array"),
		FUNCTION_TYPE: createNameType("Function")
	};
}