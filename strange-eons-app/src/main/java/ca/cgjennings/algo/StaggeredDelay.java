package ca.cgjennings.algo;

import javax.swing.Timer;

/**
 * Retry an action in the UI thread after a delay, with random staggering
 * to prevent multiple simultaneous 
 */
public final class StaggeredDelay {
    private StaggeredDelay() {}

    public static void then(Runnable action) {
        then(2500, 2500, action);
    }

    
    public static void then(int delayMin, Runnable action) {
        then(delayMin, delayMin/2, action);        
    }
    
    public static void then(int delayMin, int staggerLimit, Runnable action) {
        delayMin = Math.max(250, delayMin);
        staggerLimit = Math.max(100, staggerLimit);
        int delay = delayMin + (int) (Math.random() * staggerLimit);
        delay = delay / 10 * 10;
        Timer timer = new Timer(delay, (e) -> action.run());
        timer.setRepeats(false);
        timer.start();        
    }
}