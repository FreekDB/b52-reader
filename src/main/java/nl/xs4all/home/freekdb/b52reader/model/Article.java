/*
 * Project: B52 reader (https://github.com/FreekDB/b52-reader).
 * License: Apache version 2 (https://www.apache.org/licenses/LICENSE-2.0).
 */


package nl.xs4all.home.freekdb.b52reader.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import nl.xs4all.home.freekdb.b52reader.utilities.Utilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Article {
    /**
     * Logger for this class.
     */
    private static final Logger logger = LogManager.getLogger();

    private int id;
    private String url;
    private String sourceId;
    private Author author;
    private String title;
    private String normalizedTitle;
    private long titleWordCount;
    private Date dateTime;
    private String text;
    private long textWordCount;
    private int likes;
    private boolean starred;
    private boolean read;
    private boolean archived;

    public Article(int id, String url, String sourceId, Author author, String title, Date dateTime, String text, int likes) {
        this.id = id;
        this.url = url;
        this.sourceId = sourceId;
        this.author = author;
        this.title = title;
        this.normalizedTitle = Utilities.normalize(title);
        this.titleWordCount = Utilities.calculateWordCount(title);
        this.dateTime = dateTime;
        this.text = text;
        this.textWordCount = Utilities.calculateWordCount(text);
        this.likes = likes;
    }

    public static Article createArticleFromDatabase(ResultSet resultSet, List<Author> authors) {
        Article article = null;

        try {
            int id = resultSet.getInt("id");
            String url = resultSet.getString("url");
            String sourceId = resultSet.getString("source_id");

            int authorId = resultSet.getInt("author_id");
            Author author = authors.stream()
                    .filter(anAuthor -> anAuthor.getId() == authorId)
                    .findFirst()
                    .orElse(null);

            String title = resultSet.getString("title");
            Date dateTime = resultSet.getTimestamp("date_time");
            String text = resultSet.getString("text");
            int likes = resultSet.getInt("likes");

            article = new Article(id, url, sourceId, author, title, dateTime, text, likes);

            article.setRead(resultSet.getBoolean("read"));
            article.setStarred(resultSet.getBoolean("starred"));
            article.setArchived(resultSet.getBoolean("archived"));
        } catch (SQLException e) {
            logger.error("Exception while creating an article from a database record.", e);
        }

        return article;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    @SuppressWarnings("unused")
    public void setUrl(String url) {
        this.url = url;
    }

    public String getSourceId() {
        return sourceId;
    }

    public Author getAuthor() {
        return author;
    }

    public String getTitle() {
        return title;
    }

    public String getNormalizedTitle() {
        return normalizedTitle;
    }

    public Date getDateTime() {
        return dateTime;
    }

    public void setDateTime(Date dateTime) {
        this.dateTime = dateTime;
    }

    public String getText() {
        return text;
    }

    long getWordCount() {
        return titleWordCount + textWordCount;
    }

    public int getLikes() {
        return likes;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public boolean isStarred() {
        return starred;
    }

    public void setStarred(boolean starred) {
        this.starred = starred;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    @Override
    public String toString() {
        return "[" + id + "] Article \"" + title + "\" by " + author;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }

        Article other = (Article) obj;

        return Objects.equals(id, other.id) &&
               Objects.equals(url, other.url) &&
               Objects.equals(sourceId, other.sourceId) &&
               Objects.equals(author, other.author) &&
               Objects.equals(title, other.title) &&
               Objects.equals(dateTime, other.dateTime) &&
               Objects.equals(text, other.text) &&
               Objects.equals(likes, other.likes) &&
               Objects.equals(read, other.read) &&
               Objects.equals(starred, other.starred) &&
               Objects.equals(archived, other.archived);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, url, sourceId, author, title, dateTime, text, likes, read, starred, archived);
    }
}
