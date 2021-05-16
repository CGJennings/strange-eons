package ca.cgjennings.ui;

import java.io.File;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/**
 * A simple audio clip player intended to play audio cues as part of a user
 * interface. The player is intentionally transparent with respect to failure;
 * if a clip fails to load or the system does not have audio support, the player
 * will not throw an exception. This prevents surprise failures on rare systems
 * that do not have audio support, but it also means that these audio cues
 * should never be the only means of providing feedback.
 *
 * <p>
 * New clip players are constructed by providing a reference to the audio file
 * that contains the clip to play. This is either a {@code File} or a
 * {@code URL}. The constructor also specifies whether a clip should be
 * looped. An unlooped clip will play once each time {@link #play()} is called.
 * A looped clip will continue to play, repeating as necessary, until it is
 * explicitly stopped.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 2.1
 */
public class ClipPlayer {

    private boolean looped;
    private Clip clip;

    /**
     * Create a {@code ClipPlayer} for an audio clip stored in a file. If
     * the clip cannot be read from the file, the player will do nothing when
     * {@link #play()} is called and {@link #isClipValid()} will return
     * {@code false}.
     *
     * @param audioClip the location of the clip to load
     * @param looped whether to play the clip in a loop
     */
    public ClipPlayer(File audioClip, boolean looped) {
        try {
            init(AudioSystem.getAudioInputStream(audioClip), looped);
        } catch (Throwable t) {
            Logger.getGlobal().log(Level.WARNING, "failed to create stream " + audioClip, t);
            clip = null;
        }
    }

    /**
     * Create a {@code ClipPlayer} for a an audio clip stored at a URL. If
     * the clip cannot be read, the player will do nothing when {@link #play()}
     * is called and {@link #isClipValid()} will return {@code false}.
     *
     * @param audioClip the location of the clip to load
     * @param looped whether to play the clip in a loop
     */
    public ClipPlayer(URL audioClip, boolean looped) {
        try {
            init(AudioSystem.getAudioInputStream(audioClip), looped);
        } catch (Throwable t) {
            t.printStackTrace();
            clip = null;
        }
    }

    private void init(AudioInputStream audio, boolean looped) {
        this.looped = true;
        try {
            clip = AudioSystem.getClip();
            clip.open(audio);
            if (looped) {
                clip.setLoopPoints(0, -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            clip = null;
        }
        this.looped = looped;
    }

    /**
     * Play the audio clip. If set to loop, the clip will continue to play until
     * {@link #stop()} is called. If the clip is invalid, nothing happens. If
     * the clip is already playing, it will be restarted from the beginning.
     */
    public void play() {
        if (clip == null) {
            return;
        }
        clip.stop();
        clip.setFramePosition(0);
        if (looped) {
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        } else {
            clip.start();
        }
    }

    /**
     * If the clip is currently playing, stop playing it. If the clip is not
     * playing or is invalid, nothing happens.
     */
    public void stop() {
        if (clip == null) {
            return;
        }
        clip.stop();
    }

    /**
     * Close the clip player, freeing the associated audio system resources.
     * Subsequent calls to {@link #play()} will have no effect.
     */
    public void close() {
        stop();
        if (clip != null) {
            clip.close();
        }
        clip = null;
    }

    /**
     * Returns {@code true} if the clip was loaded successfully and the
     * system is able to play it.
     *
     * @return {@code true} if playing the clip will produce audio
     */
    public boolean isClipValid() {
        return clip != null;
    }
}
