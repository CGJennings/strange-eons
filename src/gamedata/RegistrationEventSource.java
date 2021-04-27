package gamedata;

import java.lang.ref.SoftReference;
import java.util.LinkedList;

/**
 * A helper that fires registration events for some kind of game data.
 *
 * @author Chris Jennings <https://cgjennings.ca/contact>
 */
public class RegistrationEventSource<E> {

    void fireRegistrationEvent(Object obj) {
        fire(obj, true);
    }

    void fireUnregistrationEvent(Object obj) {
        fire(obj, false);
    }

    public void addListener(RegistrationListener li) {
        if (li == null) {
            throw new NullPointerException("listener");
        }
        if (listeners == null) {
            listeners = new LinkedList<>();
        }
        boolean removeNulls = false;
        for (SoftReference<RegistrationListener> ref : listeners) {
            RegistrationListener l = ref.get();
            if (l == null) {
                removeNulls = true;
                continue;
            }
            if (l.equals(li)) {
                return;
            }
        }
        listeners.add(new SoftReference<>(li));
        if (removeNulls) {
            removeListener(null);
        }
    }

    public void removeListener(RegistrationListener li) {
        if (listeners == null) {
            return;
        }
        for (int i = listeners.size() - 1; i >= 0; --i) {
            SoftReference<RegistrationListener> ref = listeners.get(i);
            RegistrationListener l = ref.get();
            if (l == null || l.equals(li)) {
                listeners.remove(i);
            }
        }
    }

    private void fire(Object obj, boolean isReg) {
        if (listeners == null) {
            return;
        }
        for (SoftReference<RegistrationListener> ref : listeners) {
            RegistrationListener l = ref.get();
            if (l == null) {
                continue;
            }
            l.gameDataRegistered(obj, isReg);
        }

    }

    private LinkedList<SoftReference<RegistrationListener>> listeners;
}
