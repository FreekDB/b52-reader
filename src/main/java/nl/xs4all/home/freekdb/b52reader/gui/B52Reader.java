/*
 * Project: B52 reader (https://github.com/FreekDB/b52-reader).
 * License: Apache version 2 (https://www.apache.org/licenses/LICENSE-2.0).
 */


package nl.xs4all.home.freekdb.b52reader.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import nl.xs4all.home.freekdb.b52reader.general.Constants;
import nl.xs4all.home.freekdb.b52reader.general.EmbeddedBrowserType;
import nl.xs4all.home.freekdb.b52reader.gui.djnativeswing.JWebBrowserPanel;
import nl.xs4all.home.freekdb.b52reader.model.Article;
import nl.xs4all.home.freekdb.b52reader.model.Author;
import nl.xs4all.home.freekdb.b52reader.model.database.PersistencyHandler;
import nl.xs4all.home.freekdb.b52reader.sources.ArticleSource;
import nl.xs4all.home.freekdb.b52reader.sources.CombinationArticleSource;
import nl.xs4all.home.freekdb.b52reader.sources.RssArticleSource;
import nl.xs4all.home.freekdb.b52reader.sources.nrc.NrcScienceArticleSource;
import nl.xs4all.home.freekdb.b52reader.sources.testdata.TestDataArticleSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import chrriis.dj.nativeswing.swtimpl.NativeInterface;

// todo: Fix SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
//       mvn dependency:tree
//       com.rometools:rome:jar:1.7.2:compile -> org.jdom:jdom2:jar:2.0.6:compile -> org.slf4j:slf4j-api:jar:1.7.16:compile

// todo: Solve exception during exit (PersistencyHandler.updateExistingArticles): "Exception while updating authors in the database."
// todo: Embedded browser (JWebBrowser) does not resize when application window is resized after initial view?
public class B52Reader {
    private static final String APPLICATION_NAME_AND_VERSION = "B52 reader 0.0.6";
    private static final int BACKGROUND_BROWSER_MAX_COUNT = 6;

    private static final Logger logger = LogManager.getLogger(B52Reader.class);

    private PersistencyHandler persistencyHandler;
    private List<Article> currentArticles;
    private List<Article> filteredArticles;
    private Article selectedArticle;
    private int backgroundArticleIndex;
    private int backgroundBrowserCount;

    private JFrame frame;
    private JTextField filterTextField;
    private JTable table;
    private ArticlesTableModel tableModel;
    private ManyBrowsersPanel manyBrowsersPanel;

    public static void main(String[] arguments) {
        if (Constants.EMBEDDED_BROWSER_TYPE == EmbeddedBrowserType.EMBEDDED_BROWSER_DJ_NATIVE_SWING) {
            NativeInterface.open();
        }

        SwingUtilities.invokeLater(() -> new B52Reader().createAndShowApplication());

        if (Constants.EMBEDDED_BROWSER_TYPE == EmbeddedBrowserType.EMBEDDED_BROWSER_DJ_NATIVE_SWING) {
            NativeInterface.runEventPump();
        }
    }

    private void createAndShowApplication() {
        initializeDatabase();

        currentArticles = getArticles(Arrays.asList("nrc", "test"));
        filteredArticles = currentArticles;

        frame = new JFrame(APPLICATION_NAME_AND_VERSION);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setBounds(0, 0, 1920, 1080);
        //frame.setBounds(0, 0, 1024, 768);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(createFilterPanel(), BorderLayout.NORTH);

        manyBrowsersPanel = new ManyBrowsersPanel();

        JTable table = createTable(currentArticles);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(10000, 200));
        northPanel.add(scrollPane, BorderLayout.CENTER);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                manyBrowsersPanel.disposeAllBrowsers();
                saveDataAndCloseDatabase();
            }
        });

        frame.getContentPane().add(northPanel, BorderLayout.NORTH);
        frame.getContentPane().add(manyBrowsersPanel, BorderLayout.CENTER);
        frame.setVisible(true);

        // Start a background timer to initialize and load some browsers in the background.
        backgroundBrowserCount = 0;
        backgroundArticleIndex = 1;
        Timer backgroundTasksTimer = new Timer(2000, actionEvent -> handleBackgroundTasks());
        backgroundTasksTimer.start();
    }

    private List<Article> getArticles(List<String> articleSourceIds) {
        List<Article> articles = new ArrayList<>();

        try {
            List<ArticleSource> articleSources = new ArrayList<>();

            if (articleSourceIds.contains("nrc")) {
                articleSources.add(new NrcScienceArticleSource());
            }

            if (articleSourceIds.contains("acm")) {
                articleSources.add(new RssArticleSource("ACM Software",
                                                        new URL("https://cacm.acm.org/browse-by-subject/software.rss"),
                                                        new Author(4, "ACM")));
            }

            if (articleSourceIds.contains("verge")) {
                articleSources.add(new RssArticleSource("The Verge",
                                                        new URL("https://www.theverge.com/rss/index.xml"),
                                                        new Author(5, "The Verge")));
            }

            if (articleSourceIds.contains("test")) {
                articleSources.add(new TestDataArticleSource());
            }

            Map<String, Article> storedArticlesMap = persistencyHandler.getStoredArticlesMap();
            Map<String, Author> storedAuthorsMap = persistencyHandler.getStoredAuthorsMap();

            articles = new CombinationArticleSource(articleSources).getArticles(storedArticlesMap, storedAuthorsMap);
        } catch (MalformedURLException e) {
            logger.error("Exception while getting articles.", e);
        }

        return articles;
    }

    private void handleBackgroundTasks() {
        if (backgroundBrowserCount < BACKGROUND_BROWSER_MAX_COUNT && backgroundArticleIndex < currentArticles.size()) {
            logger.debug("Started with background tasks.");

            String url = currentArticles.get(backgroundArticleIndex).getUrl();
            if (!manyBrowsersPanel.hasBrowserForUrl(url)) {
                logger.debug("Background: prepare browser " + (backgroundBrowserCount + 1) + " for " + url);
                manyBrowsersPanel.showBrowser(url, false);
                backgroundBrowserCount++;
            }
            backgroundArticleIndex++;

            logger.debug("Finished with background tasks.");
        }
    }

    private void initializeDatabase() {
        persistencyHandler = new PersistencyHandler();

        if (persistencyHandler.initializeDatabaseConnection()) {
            persistencyHandler.createTablesIfNeeded();
            persistencyHandler.readAuthorsAndArticles();
        }
    }

    private JPanel createFilterPanel() {
        JPanel filterPanel = new JPanel();
        filterPanel.add(new JLabel("Filter:"));

        filterTextField = new JTextField("", 64);

        filterTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                filterAndShowArticles();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                filterAndShowArticles();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                filterAndShowArticles();
            }
        });

        filterPanel.add(filterTextField);

        return filterPanel;
    }

    private void filterAndShowArticles() {
        Article previousSelectedArticle = selectedArticle;

        filteredArticles = currentArticles.stream()
                .filter(filterTextField != null ? new ArticleFilter(filterTextField.getText()) : article -> true)
                .filter(article -> !article.isArchived())
                .collect(Collectors.toList());

        tableModel.setArticles(filteredArticles);

        frame.setTitle(APPLICATION_NAME_AND_VERSION + " - " + (filteredArticles.size() > 0 ? "1" : "0")
                       + "/" + filteredArticles.size());

        if (filteredArticles.size() > 0) {
            boolean selectFirstArticle = true;
            if (previousSelectedArticle != null) {
                int previousIndex = filteredArticles.indexOf(previousSelectedArticle);
                if (previousIndex != -1) {
                    //selectArticle(previousSelectedArticle, previousIndex);
                    table.getSelectionModel().setSelectionInterval(previousIndex, previousIndex);
                    selectFirstArticle = false;
                }
            }
            if (selectFirstArticle && table != null) {
                table.getSelectionModel().setSelectionInterval(0, 0);
            }
        }
    }

    private JTable createTable(List<Article> articles) {
        ArticleTableCellRenderer.setDefaultBackgroundColor(frame.getBackground());

        tableModel = new ArticlesTableModel(articles);

        table = new JTable(tableModel);
        table.setDefaultRenderer(Article.class, new ArticleTableCellRenderer());
        table.setRowHeight(42);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().setSelectionInterval(0, 0);

        // todo: table.addKeyListener(new KeyboardShortcutHandler(this));

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                int selectedArticleIndex = table.getSelectedRow();
                Article selectedArticle = selectedArticleIndex != -1 ? filteredArticles.get(selectedArticleIndex) : null;

                if (selectedArticle != null) {
                    boolean updateArticleList = false;

                    // todo: Get rid of these magic numbers (36 and 60) below.
                    if (mouseEvent.getX() < 36) {
                        selectedArticle.setStarred(!selectedArticle.isStarred());
                        updateArticleList = true;
                    } else if (mouseEvent.getX() < 60) {
                        selectedArticle.setRead(!selectedArticle.isRead());
                        updateArticleList = true;
                    }

                    if (updateArticleList) {
                        // todo: Keep selection and scroll location if possible.
                        filterAndShowArticles();
                    }
                }
            }
        });

        table.getSelectionModel().addListSelectionListener(listSelectionEvent -> {
            int selectedArticleIndex = table.getSelectedRow();

            if (selectedArticleIndex >= 0 && !listSelectionEvent.getValueIsAdjusting()) {
                Article selectedArticle = filteredArticles.get(selectedArticleIndex);
                selectArticle(selectedArticle, selectedArticleIndex);
            }
        });

        if (tableModel.getRowCount() > 0)
            selectArticle(filteredArticles.get(0), 0);

        return table;
    }

    private void selectArticle(Article article, int articleIndex) {
        String articleCounterAndSize = (articleIndex + 1) + "/" + filteredArticles.size();
        frame.setTitle(APPLICATION_NAME_AND_VERSION + " - " + articleCounterAndSize);

        selectedArticle = article;

        manyBrowsersPanel.showBrowser(selectedArticle.getUrl(), true);
    }

    // todo: Merge/move this method into the ManyBrowsersPanel class to support different types of embedded browsers.
    @SuppressWarnings("unused")
    private JPanel createNewBrowserPanel(Article article) {
        JPanel newBrowserPanel;

        switch (Constants.EMBEDDED_BROWSER_TYPE) {
            case EMBEDDED_BROWSER_DJ_NATIVE_SWING:
                // Use the JWebBrowser class from the DJ Native Swing library.
                // todo: cache some embedded browsers (and don't use too much memory).
                newBrowserPanel = new JWebBrowserPanel(article.getUrl());
                break;

            case EMBEDDED_BROWSER_PLACEHOLDER:
            default:
                // Placeholder for an embedded browser.
                newBrowserPanel = new JPanel();
                newBrowserPanel.add(new JLabel(article.getUrl()));
                break;
        }

        return newBrowserPanel;
    }

    private void saveDataAndCloseDatabase() {
        persistencyHandler.saveAuthorsAndArticles(currentArticles);

        if (persistencyHandler.closeDatabaseConnection()) {
            logger.debug("Closed the database connection.");
        }
    }
}
