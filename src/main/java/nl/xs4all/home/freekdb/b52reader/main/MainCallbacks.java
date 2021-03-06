/*
 * Project: B52 reader (https://github.com/FreekDB/b52-reader).
 * License: Apache version 2 (https://www.apache.org/licenses/LICENSE-2.0).
 */


package nl.xs4all.home.freekdb.b52reader.main;

import java.awt.Rectangle;

/**
 * A callback interface for methods called from the GUI and implemented by the main application class (B52Reader).
 *
 * @author <a href="mailto:fdbdbr@gmail.com">Freek de Bruijn</a>
 */
public interface MainCallbacks {
    /**
     * Handle shutdown of the application.
     *
     * @param frameExtendedState the state of the frame (normal or maximized).
     * @param frameBounds        the bounds of the frame.
     * @return whether the shutdown was done successfully.
     */
    boolean shutdownApplication(int frameExtendedState, Rectangle frameBounds);
}
