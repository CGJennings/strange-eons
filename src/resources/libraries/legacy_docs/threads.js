/*

  threads.js - version 8
  Threaded execution support.


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
 * Basic multithreading support.
 */

/**
 * Thread( [name], functionToRun ) [ctor]
 * Create a new thread that will execute <tt>functionToRun</tt>.
 *
 * <b>Note:</b> Any calls into Strange Eons from the background
 * thread <i>must</i> be made on the event dispatch thread.
 * (Use <tt>Thread.invokeLater()</tt> or <tt>Thread.invokeAndWait()</tt>.)
 *
 * name : an optional name for the thread
 * functionToRun : a function to run in another thread
 */
/**
 * Thread.start()
 * Starts executing <tt>functionToRun</tt> in parallel with the
 * caller. Threads cannot be run multiple times.
 */
/**
 * Thread.alive [readonly]
 * This is <tt>true</tt> if the thread has been started but has
 * not yet finished.
 *
 * <tt>true</tt> if the thread is running
 */
/**
 * Thread.name [readonly]
 * Returns the name of the thread.
 *
 * returns the name used to create this <tt>Thread</tt>
 */
/**
 * Thread.interrupt()
 * Sets the thread's interruption flag.
 */
/**
 * Thread.interrupted [static]
 * This is <tt>true</tt> if the <i>currently running thread</i> has been
 * interrupted. Checking this value clears it to <tt>false</tt> until the
 * next time the thread is interrupted.
 */
/**
 * Thread.join( [msTimeout] )
 * Causes the current thread to wait until this thread ends. If the optional
 * <tt>msTimeout</tt> is given, the current thread will stop waiting after
 * that many milliseconds.
 *
 * msTimeout : the maximum time to wait, in ms (default is to wait forever)
 *
 * returns the value returned by the <tt>Thread</tt>'s ...
 *         <tt>functionToRun</tt>, if the function returns a value and the ...
 *         thread ended before the timeout elapsed
 */
/**
 * Thread.hasReturnValue [readonly]
 * This is <tt>true</tt> if this thread has completed normally (without
 * throwing an uncaught exception). If the
 * thread has not been started or is still alive,
 * then it returns <tt>false</tt>. Note that this property does not
 * indicate whether the function actually returned anything, only that
 * if it did, it is now available to be read.
 */
/**
 * Thread.returnValue [readonly]
 * If the thread has been started and the thread's <tt>functionToRun</tt>
 * has finished executing without throwing an uncaught exception, then
 * this property will store the value returned by the function, if any.
 * If the thread ended due to an uncaught exception, then reading this
 * property will cause the exception to be thrown.
 */

const Thread = function Thread( name, functionToRun ) {
	if( functionToRun === undefined ) {
		functionToRun = name;
		name = null;
	}
	if( name == null ) name = arkham.plugins.ScriptedRunnable.getDefaultThreadName();
	var r = new arkham.plugins.ScriptedRunnable( functionToRun );
	var t = new java.lang.Thread( r, name );
	t.setDaemon( true );
	this.__defineGetter__( "name", function name() {return name;} );
    this.__defineGetter__( "alive", function alive() {return t.isAlive();} );
	this.__defineGetter__( "returnValue", function returnValue() {return r.getReturnValue();} );
	this.__defineGetter__( "hasReturnValue", function returnValue() {return r.hasReturnValue();} );
	this.interrupt = function() {t.interrupt();};
	this.join = function join( waitTime ) {
		if( waitTime == null ) {
			t.join();
		} else {
			t.join( waitTime );
		}
		return this.returnValue;
	};
	this.start = function start() {t.start();};
	this.toString = function toString() {return name;};
};

Thread.interrupted = function interrupted() {
	return java.lang.Thread.interrupted();
};

/**
 * Thread.busyWindow( task, [title], [canCancel] ) [static]
 * Runs a lengthy task in the background while providing feedback to the user.
 * When this function is called, the user interface will be blocked by a
 * status window while the function <tt>task</tt> executes. This function will
 * be passed a single argument, an object with the following properties:
 *
 * maximumProgress : sets the integer number of "steps" needed to complete the task;
 *     the definition of a step is left up to the task function
 * currentProgress : sets the highest completed step to a value between 0 and ...
 *     <tt>maximumProgress</tt> (inclusive)
 * title : sets the main title of the feedback display
 * status : sets the text of a smaller text description at the bottom of the ...
 *     feedback display
 * cancelled : gets a boolean value that is <tt>true</tt> if the user has clicked ...
 *     the cancel button
 *
 * The feedback window displays a progress bar that will indicate that the task's
 * completedness is proportional to <tt>currentProgress/maximumProgress</tt>.
 * If no maximum is set, the progress bar will indicate that an indeterminate
 * amount of time is required by the task. If <tt>canCancel</tt> is <tt>true</tt>
 * then the feedback window will include a Cancel button. If used, the task should
 * periodically check if this button has been pressed using the <tt>cancelled</tt>
 * property, and quit the task as soon as possible if it is <tt>true</tt>.
 * (This will always be <tt>false</tt> if the Cancel button was not requested.)
 *
 * task : a function to be run in the background
 * title : the initial title to use for the feedback display
 * canCancel : if <tt>true</tt>, the feedback display will include a Cancel button
 */
Thread.busyWindow = function busyWindowContext() {
	function wrap( t ) {
		var isCancelled = false;
		var callback = {
			set maximumProgress( max ) {
				arkham.BusyDialog.maximumProgress( max );
			},
			set currentProgress( cur ) {
				arkham.BusyDialog.currentProgress( cur, 50 );
			},
			set title( text ) {
				arkham.BusyDialog.titleText( text );
			},
			set status( text ) {
				arkham.BusyDialog.statusText( text, 50 );
			},
			get cancelled() {
				return isCancelled;
			}
		};
		var wrappedTask = function task() {
			try {
				task.retVal = t( callback );
			} catch( ex ) {
				Error.handleUncaught( ex );
			}
		};
		wrappedTask.retVal = null;
		wrappedTask.createCancelAction = function createCancelAction() {
			return new java.awt.event.ActionListener() {
				actionPerformed: function actionPerformed( evt ) {
					isCancelled = true;
				}
			};
		};
		return wrappedTask;
	}

	return function busyWindow( task, message, canCancel ) {
		useLibrary.__threadassert();
		if( !message ) {
			message = string("busy-script");
		}
		var action = wrap( task );
		var cancelAction = canCancel ? action.createCancelAction() : null;
		new arkham.BusyDialog( null, message, action, cancelAction );
		return action.retVal;
	};
}();

/**
 * Thread.run( functionToRun ) [static]
 * Starts a new thread.
 * This is a convenience that creates a new <tt>Thread</tt>
 * and immediately <tt>start()</tt>s it. The new <tt>Thread</tt>
 * is returned.
 *
 * returns the new <tt>Thread</tt>
 */
Thread.run = function run( task ) {
	var t = new Thread( task );
	t.start();
	return t;
};

/**
 * Thread.invokeLater( functionToRun ) [static]
 * Run a task on the event dispatch thread without waiting for it to return.
 * For example:
 * <pre>
 * Thread.invokeLater(
 *     function() {
 *         println( "I will be called later." );
 *     }
 * );
 * println( "I will be called now." );
 * </pre>
 */
Thread.invokeLater = function invokeLater( task ) {
    java.awt.EventQueue.invokeLater( function invokeLaterTask() {
	        try {
	            task();
	        } catch( ex ) {
	            Error.handleUncaught( ex );
	        }
	    }
    );
};

/**
 * Thread.invokeAndWait( functionToRun ) [static]
 * Run a task on the event dispatch thread, waiting for it to return.
 * If the current thread is the event dispatch thread, then it will
 * run immediately. Otherwise, the current thread will be paused
 * until the event dispatch thread is able to execute the function.
 *
 * Returns the result returned by <tt>functionToRun</tt>,
 * or <tt>undefined</tt> if the task throws an exception
 */
Thread.invokeAndWait = function invokeAndWait( task ) {
    var returnValue = undefined;
    try {
        if( java.awt.EventQueue.isDispatchThread() ) {
            return task();
        } else {
            java.awt.EventQueue.invokeAndWait( function invokeLaterTask() {
                try {
                    returnValue = task();
                } catch( ex ) {
                    Error.handleUncaught( ex );
                }
            });
        }
    } catch( ex ) {
        Error.handleUncaught( ex );
    }
    return returnValue;
};

/**
 * Thread.Lock : java.util.concurrent.locks.ReentrantLock [ctor] [static]
 * This is a convenient reference to the class
 * <a href='javadoc:java/util/concurrent/locks/ReentrantLock.html'>...
 * java.util.concurrent.locks.ReentrantLock</a> that can be used for synchronization purposes.
 */
Thread.Lock = java.util.concurrent.locks.ReentrantLock;
