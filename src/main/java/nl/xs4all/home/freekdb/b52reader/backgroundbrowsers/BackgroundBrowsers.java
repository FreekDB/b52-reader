/*
 * Project: B52 reader (https://github.com/FreekDB/b52-reader).
 * License: Apache version 2 (https://www.apache.org/licenses/LICENSE-2.0).
 */


package nl.xs4all.home.freekdb.b52reader.backgroundbrowsers;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import chrriis.dj.nativeswing.swtimpl.NSPanelComponent;
import chrriis.dj.nativeswing.swtimpl.components.JWebBrowser;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserAdapter;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserNavigationEvent;

/**
 * This class allows invisible browsers to be running in the background to get html content of a specific url.
 */
public class BackgroundBrowsers {
    /**
     * Map of URLs to {@link JWebBrowser} objects.
     */
    private static final Map<String, JWebBrowser> URL_TO_WEB_BROWSER = new HashMap<>();

    /**
     * Map of URLs to html content.
     */
    private static final Map<String, String> URL_TO_HTML_CONTENT = new HashMap<>();

    /**
     * Logger for this class.
     */
    private static final Logger logger = LogManager.getLogger(BackgroundBrowsers.class);

    /**
     * The (invisible) panel to which the browsers can be added (to allow them to work).
     */
    private JPanel backgroundBrowsersPanel;

    /**
     * List to keep track of all {@link JWebBrowser} objects, to enable cleanup of any forgotten browsers.
     */
    private List<JWebBrowser> webBrowsers;

    /**
     * Construct a {@link BackgroundBrowsers} object, which can handle multiple browsers.
     *
     * @param backgroundBrowsersPanel the (invisible) panel to which the browsers can be added (to allow them to work).
     */
    public BackgroundBrowsers(JPanel backgroundBrowsersPanel) {
        this.backgroundBrowsersPanel = backgroundBrowsersPanel;
        this.webBrowsers = new ArrayList<>();
    }

    /**
     * Get the html content for the specified url. A default timeout of 10 seconds is used.
     *
     * @param url the url for which the html content should be retrieved.
     * @return the html content that was retrieved or null.
     */
    public String getHtmlContent(String url) {
        return getHtmlContent(url, 10000);
    }

    /**
     * Get the html content for the specified url. The specified maximum wait time in milliseconds is used.
     *
     * @param url the url for which the html content should be retrieved.
     * @param maxWaitTimeMs the maximum amount of time to wait (in milliseconds).
     * @return the html content that was retrieved or null.
     */
    @SuppressWarnings({"WeakerAccess", "SameParameterValue"})
    public String getHtmlContent(final String url, final int maxWaitTimeMs) {
        try {
            logger.debug("Launching a background browser for url " + url);

            URL_TO_HTML_CONTENT.remove(url);

            SwingUtilities.invokeAndWait(() -> launchBackgroundBrowser(url));

            logger.debug("Waiting for html content...");

            boolean done = false;
            int waitCount = 0;
            int maxWaitCount = maxWaitTimeMs / 100;
            while (!done && waitCount < maxWaitCount) {
                //noinspection BusyWait
                Thread.sleep(100);

                if (URL_TO_HTML_CONTENT.containsKey(url)) {
                    // Some systems provide some kind of intermediate "in progress" html content.
                    if (URL_TO_HTML_CONTENT.get(url).contains("Working...")) {
                        logger.debug("Working...");
                        if (waitCount % 10 == 0) {
                            logger.trace("Refresh html content.");
    
                            SwingUtilities.invokeAndWait(() ->
                                    URL_TO_HTML_CONTENT.put(url, URL_TO_WEB_BROWSER.get(url).getHTMLContent()));
                            
                            logger.trace("Html content: " + URL_TO_HTML_CONTENT.get(url).substring(0, 100));
                        }
                    } else {
                        done = true;
                    }
                }
                waitCount++;
            }

            closeBackgroundBrowser(url);
        } catch (InterruptedException | InvocationTargetException e) {
            logger.error("Exception while getting html content with a background browser.", e);
        }

        return URL_TO_HTML_CONTENT.get(url);
    }

    /**
     * Launch a background browser and add a listener that puts the html content in the <code>URL_TO_HTML_CONTENT</code>.
     *
     * @param url the url for which the html content should be retrieved.
     */
    private void launchBackgroundBrowser(String url) {
        JWebBrowser webBrowser = new JWebBrowser();

        webBrowser.setMenuBarVisible(false);
        webBrowser.setButtonBarVisible(false);
        webBrowser.setLocationBarVisible(false);

        webBrowser.addWebBrowserListener(new WebBrowserAdapter() {
            @Override
            public void locationChanged(WebBrowserNavigationEvent webBrowserNavigationEvent) {
                super.locationChanged(webBrowserNavigationEvent);

                URL_TO_HTML_CONTENT.put(url, webBrowser.getHTMLContent());
            }
        });

        URL_TO_WEB_BROWSER.put(url, webBrowser);
        webBrowsers.add(webBrowser);
        backgroundBrowsersPanel.add(webBrowser);

        webBrowser.navigate(url);
    }

    /**
     * Close the background browser that was launched for the specified url.
     *
     * @param url the url for which the html content should be retrieved.
     */
    private void closeBackgroundBrowser(String url) {
        logger.debug("Closing the background browser for url " + url);

        JWebBrowser webBrowser = URL_TO_WEB_BROWSER.get(url);

        if (webBrowser != null) {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    webBrowser.disposeNativePeer();

                    URL_TO_WEB_BROWSER.remove(url);
                    webBrowsers.remove(webBrowser);
                    backgroundBrowsersPanel.remove(webBrowser);
                });
            } catch (InterruptedException | InvocationTargetException e) {
                logger.error("Exception while closing a background browser.", e);
            }
        } else {
            logger.error("Error closing background browser: no browser found for url {}", url);
        }
    }

    /**
     * Close all background browsers that for some reason have not been closed yet.
     */
    public void closeAllBackgroundBrowsers() {
        logger.debug("Closing all background browsers.");

        try {
            SwingUtilities.invokeAndWait(() -> {
                webBrowsers.forEach(NSPanelComponent::disposeNativePeer);

                URL_TO_WEB_BROWSER.clear();
                URL_TO_HTML_CONTENT.clear();
                webBrowsers.clear();
                backgroundBrowsersPanel.removeAll();
            });
        } catch (InterruptedException | InvocationTargetException e) {
            logger.error("Exception while closing all background browsers.", e);
        }
    }
}
