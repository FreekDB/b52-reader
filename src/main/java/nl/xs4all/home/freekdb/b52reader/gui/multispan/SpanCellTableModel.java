/*
 * Project: B52 reader (https://github.com/FreekDB/b52-reader).
 * License: unknown.
 *
 * This class is based on the MultiSpanCellTableExample class and related classes from Nobuo Tamemasa.
 * Original Java package: jp.gr.java_conf.tame.swing.table
 *
 * In 2008 Andrew Thompson wrote (https://groups.google.com/forum/#!topic/comp.lang.java.gui/WicMvhk1DdM):
 *     "I spent a lot of time researching the possible licensing of the tame Swing codes,
 *      but Nobuo Tamemasa (the author) has proved to be untraceable, and never specified a
 *      license at the time of posting the codes to public forums.  :-("
 *
 * It is also referred to here: http://stackoverflow.com/a/21977825/1694043
 */


package nl.xs4all.home.freekdb.b52reader.gui.multispan;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.function.Predicate;

import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

import nl.xs4all.home.freekdb.b52reader.datamodel.Article;
import nl.xs4all.home.freekdb.b52reader.general.Configuration;
import nl.xs4all.home.freekdb.b52reader.general.Constants;

// todo: Base the ArticleSpanTableModel/SpanCellTableModel on AbstractTableModel (like the ArticlesTableModel)?

// todo: Contents should be adjusted -> change dataVector or let getValueAt use the list of articles.

/**
 * @version 1.0 11/22/98
 */
public class SpanCellTableModel extends DefaultTableModel {
    private final transient Configuration configuration;

    private List<Class<?>> columnClasses;

    private transient TableSpans tableSpans;
    private transient List<Article> articles;

    public SpanCellTableModel(List<Article> articles, int columnCount, Configuration configuration) {
        this.articles = articles;
        this.configuration = configuration;

        int rowCount = 2 * articles.size();

        @SuppressWarnings("squid:S1149")
        Vector names = new Vector(columnCount);

        names.setSize(columnCount);
        setColumnIdentifiers(names);

        dataVector = new Vector();
        setRowCount(rowCount);

        tableSpans = new DefaultTableSpans(rowCount, columnCount);
    }

    public TableSpans getTableSpans() {
        return tableSpans;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnClasses.get(columnIndex);
    }

    public void setColumnsAndData(List<String> columnNames, List<Class<?>> columnClasses, List<Article> articles,
                                  Predicate<Article> isFetched) {
        // Code modified to prevent stack overflow. See http://stackoverflow.com/a/21977825/1694043 for more information.
        // setColumnIdentifiers(columnNames)
        this.columnIdentifiers = listToVector(columnNames);
        this.columnClasses = columnClasses;
        this.articles = articles;

        @SuppressWarnings("squid:S1149")
        Vector<Vector<Object>> newDataVector = new Vector<>();

        if (articles != null) {
            articles.forEach(article -> {
                newDataVector.add(listToVector(Arrays.asList(
                    isFetched.test(article) ? configuration.getFetchedValue() : "",
                    article.isStarred() ? Constants.STARRED_ICON : Constants.UNSTARRED_ICON,
                    article.isRead() ? "" : "unread",
                    article.getTitle(),
                    article.getAuthor(),
                    article.getDateTime() != null ? Constants.DATE_TIME_FORMAT_LONGER.format(article.getDateTime()) : ""
                )));

                newDataVector.add(listToVector(Arrays.asList("", "", "", article.getText())));
            });
        }

        dataVector = newDataVector;

        tableSpans = new DefaultTableSpans(dataVector.size(), columnIdentifiers != null ? columnIdentifiers.size() : 0);

        newRowsAdded(new TableModelEvent(this, 0, getRowCount() - 1,
                                         TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
    }

    /**
     * Convert a list to a vector.
     *
     * @param list the list to convert.
     * @param <T>  the type of the list items.
     * @return the vector with the same items as are in the list.
     */
    @SuppressWarnings("squid:S1149")
    private <T> Vector<T> listToVector(List<T> list) {
        return list != null ? new Vector<>(list) : null;
    }

    @Override
    public void addColumn(Object columnName, Vector columnData) {
        if (columnName == null) {
            throw new IllegalArgumentException("addColumn() - null parameter");
        }

        //noinspection unchecked
        columnIdentifiers.addElement(columnName);

        int index = 0;
        Enumeration enumeration = dataVector.elements();

        while (enumeration.hasMoreElements()) {
            Object value = (columnData != null && index < columnData.size()) ? columnData.elementAt(index) : null;

            //noinspection unchecked
            ((Vector) enumeration.nextElement()).addElement(value);

            index++;
        }

        tableSpans.addColumn();

        fireTableStructureChanged();
    }

    @Override
    public void addRow(Vector rowData) {
        @SuppressWarnings("squid:S1149")
        Vector newData;

        if (rowData == null) {
            newData = new Vector(getColumnCount());
        } else {
            rowData.setSize(getColumnCount());
            newData = rowData;
        }

        //noinspection unchecked
        dataVector.addElement(newData);
        //noinspection unchecked
        dataVector.addElement(new Vector(getColumnCount()));

        tableSpans.addRow();
        tableSpans.addRow();

        newRowsAdded(new TableModelEvent(this, getRowCount() - 2, getRowCount() - 1,
                                         TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
    }

    @Override
    public void insertRow(int rowIndex, Vector rowData) {
        @SuppressWarnings("squid:S1149")
        Vector newData;

        if (rowData == null) {
            newData = new Vector(getColumnCount());
        } else {
            rowData.setSize(getColumnCount());
            newData = rowData;
        }

        //noinspection unchecked
        dataVector.insertElementAt(newData, rowIndex);
        //noinspection unchecked
        dataVector.insertElementAt(new Vector(getColumnCount()), rowIndex + 1);

        tableSpans.insertRow(rowIndex);
        tableSpans.insertRow(rowIndex + 1);

        newRowsAdded(new TableModelEvent(this, rowIndex, rowIndex + 1,
                                         TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
    }

    Article getArticle(int rowIndex) {
        return (articles != null && rowIndex >= 0 && rowIndex < articles.size())
            ? articles.get(rowIndex)
            : null;
    }
}
