package ca.cgjennings.apps.arkham;

import ca.cgjennings.apps.arkham.component.GameComponent;
import java.beans.PropertyChangeListener;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbstractGameComponentEditorTest {

    /**
     * Regression test: the {@code pcl} listener installed by the
     * {@code AbstractGameComponentEditor} constructor for
     * {@link StrangeEonsAppWindow#VIEW_BACKDROP_PROPERTY} must be removed for
     * the same property name when the editor is disposed. If the remove call
     * uses a different property name, the listener stays registered on the
     * long-lived {@code AppFrame} singleton, pinning the editor (and its
     * sheets, viewers, and game component) for the life of the application.
     */
    @Test
    void disposeRemovesViewBackdropPropertyChangeListener() {
        AppFrame mockAppFrame = mock(AppFrame.class);
        StrangeEons mockApp = mock(StrangeEons.class);
        // ResourceKit's static initializer dereferences
        // StrangeEons.getApplication().getCommandLineArguments().resfolder.
        // Without this stub the EDT-scheduled createTimer call from the
        // editor constructor triggers a chain of class-init failures.
        CommandLineArguments mockArgs = mock(CommandLineArguments.class);
        when(mockApp.getCommandLineArguments()).thenReturn(mockArgs);

        try (MockedStatic<AppFrame> mockedAppFrame = mockStatic(AppFrame.class);
                MockedStatic<StrangeEons> mockedStrangeEons = mockStatic(StrangeEons.class)) {
            mockedAppFrame.when(AppFrame::getApp).thenReturn(mockAppFrame);
            mockedStrangeEons.when(StrangeEons::getApplication).thenReturn(mockApp);

            TestEditor editor = new TestEditor();

            ArgumentCaptor<PropertyChangeListener> listenerCaptor =
                    ArgumentCaptor.forClass(PropertyChangeListener.class);
            verify(mockAppFrame).addPropertyChangeListener(
                    eq(StrangeEonsAppWindow.VIEW_BACKDROP_PROPERTY),
                    listenerCaptor.capture());
            PropertyChangeListener pcl = listenerCaptor.getValue();

            try {
                editor.dispose();
            } catch (Throwable ignored) {
                // dispose() does extra teardown that may fail outside a real
                // app environment; we only care whether the listener
                // unregistration call was made before any failure.
            }

            verify(mockAppFrame).removePropertyChangeListener(
                    StrangeEonsAppWindow.VIEW_BACKDROP_PROPERTY, pcl);
        }
    }

    private static final class TestEditor extends AbstractGameComponentEditor<GameComponent> {
        @Override
        protected void populateComponentFromDelayedFields() {
        }

        // The base constructor schedules an EDT lambda that calls createTimer
        // by way of Settings.getShared().getInt(...). Outside a real app the
        // settings backing store is empty, which causes Settings.ParseError ->
        // Language.string -> ScriptMonkey.scriptError, a deep init chain that
        // floods stderr with NPEs. Overriding createTimer to a no-op stops the
        // chain at the doorstep.
        @Override
        protected void createTimer() {
        }
    }
}
