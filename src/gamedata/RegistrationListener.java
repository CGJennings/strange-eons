package gamedata;

import java.util.EventListener;

/**
 * An interface implemented by objects that wish to be informed when new game
 * data is registered or unregistered.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 * @since 3.00
 */
public interface RegistrationListener extends EventListener {

    /**
     * This method is called when the relevant type of data is being registered
     * or unregistered.
     *
     * @param instance the specific object being registered or unregistered
     * @param isRegistration {@code true} for registration,
     * {@code false} for unregistration
     */
    void gameDataRegistered(Object instance, boolean isRegistration);
}
