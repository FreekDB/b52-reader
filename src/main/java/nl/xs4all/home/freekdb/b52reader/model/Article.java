/*
 * Project: B52 reader (https://github.com/FreekDB/b52-reader).
 * License: Apache version 2 (https://www.apache.org/licenses/LICENSE-2.0).
 */


package nl.xs4all.home.freekdb.b52reader.model;

import java.util.Date;

import nl.xs4all.home.freekdb.b52reader.utilities.Utilities;

public class Article {
    private String id;
    private String url;
    private Author author;
    private String title;
    private String normalizedTitle;
    private long titleWordCount;
    private Date dateTime;
    private String text;
    private long textWordCount;
    private boolean read;
    private boolean starred;
    private boolean archived;

    public Article(String id, String url, Author author, String title, Date dateTime, String text) {
        this.id = id;
        this.url = url;
        this.author = author;
        this.title = title;
        this.normalizedTitle = Utilities.normalize(title);
        this.titleWordCount = Utilities.calculateWordCount(title);
        this.dateTime = dateTime;
        this.text = text;
        this.textWordCount = Utilities.calculateWordCount(text);
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
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

    long getWordCount() {
        return titleWordCount + textWordCount;
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
}
