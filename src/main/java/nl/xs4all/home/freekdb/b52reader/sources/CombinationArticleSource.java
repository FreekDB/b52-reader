/*
 * Project: B52 reader (https://github.com/FreekDB/b52-reader).
 * License: Apache version 2 (https://www.apache.org/licenses/LICENSE-2.0).
 */


package nl.xs4all.home.freekdb.b52reader.sources;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import nl.xs4all.home.freekdb.b52reader.model.Article;
import nl.xs4all.home.freekdb.b52reader.model.Author;

public class CombinationArticleSource implements ArticleSource {
    private final List<ArticleSource> articleSources;
    private final List<Article> articles;

    public CombinationArticleSource(List<ArticleSource> articleSources) {
        this.articleSources = articleSources;
        this.articles = new ArrayList<>();
    }

    @Override
    public List<Article> getArticles(Map<String, Article> previousArticlesMap, Map<String, Author> previousAuthorsMap) {
        articles.clear();

        for (final ArticleSource articleSource : articleSources) {
            articles.addAll(articleSource.getArticles(previousArticlesMap, previousAuthorsMap));
        }

        articles.sort(Comparator.comparing(Article::getDateTime));

        return articles;
    }
}
