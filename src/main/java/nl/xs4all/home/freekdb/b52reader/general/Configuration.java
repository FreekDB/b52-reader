/*
 * Project: B52 reader (https://github.com/FreekDB/b52-reader).
 * License: Apache version 2 (https://www.apache.org/licenses/LICENSE-2.0).
 */


package nl.xs4all.home.freekdb.b52reader.general;

import java.awt.Frame;
import java.awt.Rectangle;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import nl.xs4all.home.freekdb.b52reader.model.Author;
import nl.xs4all.home.freekdb.b52reader.sources.ArticleSource;
import nl.xs4all.home.freekdb.b52reader.sources.RssArticleSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Configuration functionality (most settings are stored in and read from the configuration file).
 */
public class Configuration {
    /**
     * Logger for this class.
     */
    private static final Logger logger = LogManager.getLogger();

    /**
     * Selected article sources (from configuration file).
     */
    private static List<ArticleSource> selectedArticleSources;

    /**
     * All available article sources (from configuration file).
     */
    private static List<ArticleSource> allArticleSources;

    /**
     * The application window state (normal or maximized; from configuration file).
     */
    private static int frameExtendedState;

    /**
     * The application window position (from configuration file).
     */
    private static Rectangle frameBounds;

    /**
     * Get the selected article sources.
     *
     * @return the selected article sources.
     */
    public static List<ArticleSource> getSelectedArticleSources() {
        if (selectedArticleSources == null) {
            initialize();
        }

        return selectedArticleSources;
    }

    /**
     * Get the application window state (normal or maximized).
     *
     * @return the application window state (normal or maximized).
     */
    public static int getFrameExtendedState() {
        if (selectedArticleSources == null) {
            initialize();
        }

        return frameExtendedState;
    }

    /**
     * Get the application window position.
     *
     * @return the application window position.
     */
    public static Rectangle getFrameBounds() {
        if (selectedArticleSources == null) {
            initialize();
        }

        return frameBounds;
    }

    /**
     * Should the GUI span table or the table with the custom article renderer be used.
     *
     * @return use the GUI span table (true) or the table with the custom article renderer (false).
     */
    public static boolean useSpanTable() {
        return true;
    }

    /**
     * Write the application configuration to file.
     *
     * @param frameExtendedState the application window state (normal or maximized).
     * @param frameBounds        the application window bounds.
     */
    public static void writeConfiguration(int frameExtendedState, Rectangle frameBounds) {
        String sourceIds = selectedArticleSources.stream()
                .map(ArticleSource::getSourceId)
                .collect(Collectors.joining(","));

        String windowConfiguration = (frameExtendedState != Frame.MAXIMIZED_BOTH ? "normal" : "maximized") + ";" +
                                     frameBounds.x + "," + frameBounds.y + "," +
                                     frameBounds.width + "x" + frameBounds.height;

        URL configurationUrl = Configuration.class.getClassLoader().getResource("b52-reader.configuration");

        try {
            if (configurationUrl != null) {
                Properties configuration = new Properties();

                configuration.setProperty("source-ids", sourceIds);

                for (ArticleSource articleSource : allArticleSources) {
                    String parameters = articleSource instanceof RssArticleSource
                            ? getRssParameters((RssArticleSource) articleSource)
                            : articleSource.getClass().getName();

                    configuration.setProperty("source-" + articleSource.getSourceId(), parameters);
                }

                configuration.setProperty("window-configuration", windowConfiguration);

                String header = "Configuration file for the b52-reader (https://github.com/FreekDB/b52-reader).";
                configuration.store(new FileWriter(configurationUrl.getFile()), header);
            }
        } catch (IOException e) {
            logger.error("Exception while reading the configuration file " + configurationUrl, e);
        }
    }

    /**
     * Get the configuration parameters for an RSS article source.
     *
     * @param rssSource the RSS article source.
     * @return the configuration parameters for an RSS article source.
     */
    private static String getRssParameters(RssArticleSource rssSource) {
        return "rss|" + rssSource.getFeedName() + "|" + rssSource.getDefaultAuthor().getName() + "|" +
               rssSource.getFeedUrl() + (rssSource.getCategoryName() != null ? "|" + rssSource.getCategoryName() : "");
    }

    /**
     * Initialize by reading the configuration file and filling the <code>selectedArticleSources</code> and
     * <code>allArticleSources</code> lists.
     */
    private static void initialize() {
        URL configurationUrl = Configuration.class.getClassLoader().getResource("b52-reader.configuration");

        List<String> sourceIds = new ArrayList<>(Arrays.asList("nrc", "test"));
        allArticleSources = new ArrayList<>();

        try {
            Properties configuration = new Properties();

            if (configurationUrl != null) {
                configuration.load(new FileReader(configurationUrl.getFile()));

                String sourceIdsProperty = configuration.getProperty("source-ids", "nrc,test");
                sourceIds.clear();
                sourceIds.addAll(Arrays.asList(sourceIdsProperty.split(",")));

                addConfiguredSources(configuration);

                String windowConfiguration = configuration.getProperty("window-configuration");
                String boundsConfiguration = windowConfiguration.substring(windowConfiguration.indexOf(';') + 1);
                frameExtendedState = windowConfiguration.startsWith("maximized") ? Frame.MAXIMIZED_BOTH : Frame.NORMAL;
                frameBounds = getBoundsFromConfiguration(boundsConfiguration);
            }
        } catch (IOException e) {
            logger.error("Exception while reading the configuration file " + configurationUrl, e);
        }

        selectedArticleSources = allArticleSources.stream()
                .filter(articleSource -> sourceIds.contains(articleSource.getSourceId()))
                .collect(Collectors.toList());
    }

    /**
     * Add the configured article sources to the <code>allArticleSources</code> list.
     *
     * @param configuration the configuration properties.
     */
    private static void addConfiguredSources(Properties configuration) {
        String sourcePrefix = "source-";

        Collections.list(configuration.propertyNames()).forEach(name -> {
            if (name instanceof String) {
                String propertyName = (String) name;

                if (propertyName.startsWith(sourcePrefix) && !propertyName.equals("source-ids")) {
                    String sourceId = propertyName.substring(sourcePrefix.length());
                    String sourceConfiguration = configuration.getProperty(propertyName);

                    ArticleSource articleSource = createArticleSource(sourceId, sourceConfiguration);

                    if (articleSource != null) {
                        allArticleSources.add(articleSource);
                    }
                }
            }
        });
    }

    /**
     * Create an article source object from the source id and the source configuration.
     *
     * @param sourceId            the source id.
     * @param sourceConfiguration the source configuration.
     * @return the new article source object.
     */
    private static ArticleSource createArticleSource(String sourceId, String sourceConfiguration) {
        ArticleSource articleSource = null;

        try {
            Object source = null;

            if (sourceConfiguration.startsWith("rss|")) {
                String[] configurationItems = sourceConfiguration.split("\\|");

                if (configurationItems.length >= 4) {
                    String feedName = configurationItems[1];
                    Author defaultAuthor = ObjectHub.getPersistencyHandler().getOrCreateAuthor(configurationItems[2]);
                    URL feedUrl = new URL(configurationItems[3]);
                    String categoryName = configurationItems.length >= 5 ? configurationItems[4] : null;

                    source = new RssArticleSource(sourceId, feedName, defaultAuthor, feedUrl, categoryName);
                }
            } else {
                Class<?> sourceClass = Class.forName(sourceConfiguration);
                source = sourceClass.getConstructor().newInstance();
            }

            if (source instanceof ArticleSource) {
                articleSource = (ArticleSource) source;
            }
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException |
                InvocationTargetException | MalformedURLException e) {
            logger.error("Exception while initializing article source " + sourceId + ".", e);
        }

        return articleSource;
    }

    /**
     * Create a rectangle with the window bounds from the bounds configuration.
     *
     * @param boundsConfiguration the bounds configuration.
     * @return the rectangle with the window bounds.
     */
    private static Rectangle getBoundsFromConfiguration(String boundsConfiguration) {
        int[] bounds = Arrays.stream(boundsConfiguration.split("[,x]")).mapToInt(Integer::parseInt).toArray();

        return new Rectangle(bounds[0], bounds[1], bounds[2], bounds[3]);
    }
}
