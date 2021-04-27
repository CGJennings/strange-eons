// Polyfill logger object for debugging
let scriptedLogger = {
	SHOW_CALLER : false,
	INFO : true,
	DEBUG : true,
	WARN : true,
	ERROR : true,
	info : function(msg, category) {
		if( this.INFO ) {
			println( 'Info: ' + msg );
		}
	},
	debug : function(msg, category) {
		if( this.DEBUG ) {
			println( 'Debug: ' + msg );
		}
	},
	warn : function(msg, category) {
		if( this.WARN ) {
			println( 'Warn: ' + msg );
		}
	},
	error : function(msg, category) {
		if( this.ERROR ) {
			println( 'Error: ' + msg );
		}
	},
	isEnabled : function(catName) {
		return true;
	}
};

// Polyfill for window.console
let console = {
	error : println
};


function getLibrary( lib ) {
	return arkham.plugins.ScriptMonkey.getLibrary( lib );
}

useLibrary('res://libraries/completion/doctrine.js');
useLibrary('res://libraries/completion/esprima.js');
useLibrary('res://libraries/completion/esprimaVisitor.js');
useLibrary('res://libraries/completion/types.js');
useLibrary('res://libraries/completion/proposalUtils.js');
useLibrary('res://libraries/completion/contentAssist.js');

let theTypes = types( proposalUtils, scriptedLogger );
let contentAssistProvider = new contentAssistProvider( esprimaVisitor(), theTypes, proposalUtils );
let assistant = new contentAssistProvider( null, {} );
		
// Convert a proposal into a simple object array easily digested on the Java side
function convertProposal( p ) {
	return [
		p.proposal,
		p.description,
		java.lang.Integer.valueOf( p.escapePosition ? p.escapePosition : -1 ),
		p.style
	];
}

// Convert an array of proposals at once
function convertProposals( ps ) {
	if( ps == null ) {
		return [];
	} else {
		return ps.map( convertProposal );
	}
}


// Create a content assist context object from a list of possible parameters
function createContext( prefix, selStart, selEnd ) {
	let selection = null;
	if( selStart ) {
		selection = {
			start: selStart,
			end: selEnd ? selEnd : selStart
		};
	}
	let ctx = {	
		prefix: prefix ? prefix : '',
		selection: selection,
		inferredOnly: true
	};
	return ctx;
}

//
// Implementations for interface methods
//

function getProposals( fileText, prefix, offset, selStart, selEnd ) {
	try {
		let ctx = createContext( prefix, selStart, selEnd );
		let props = assistant.computeProposals( fileText, offset, ctx );
		if( (!props || props.length === 0) && prefix.length > 0 ) {
			ctx.inferredOnly = false;
			props = assistant.computeProposals( fileText, offset, ctx );
		}
		return convertProposals( props );
	} catch( ex ) {
		Error.handleUncaught( ex );
	}
	return null;
}	