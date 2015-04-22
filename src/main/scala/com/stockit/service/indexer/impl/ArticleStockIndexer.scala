package com.stockit.service.indexer.impl
import collection.JavaConversions.asJavaCollection
import com.stockit.service.ticker.TickerDatasource

import java.io.IOException
import java.text.SimpleDateFormat
import java.util.{Date,Collection}
import java.lang.{Double,Long}

import com.stockit.model.{StockHistory, Article}
import com.stockit.service.indexer.{IndexerException, Indexer}
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.client.solrj.{SolrQuery, SolrServerException, SolrClient}
import org.apache.solr.common.{SolrDocument, SolrInputDocument}
import org.slf4j.LoggerFactory

/**
 * Created by dmcquill on 4/17/15.
 */
class ArticleStockIndexer extends Indexer {
    implicit val formats = org.json4s.DefaultFormats

    val logger = LoggerFactory.getLogger(classOf[ArticleStockIndexer])

    var solrClient: SolrClient = null

    var stockHistorySolrClient: SolrClient = null

    var articleSolrClient: SolrClient = null

    val rowsPerIteration = 1

    val dayFormat = new SimpleDateFormat("yyyy-MM-dd")

    var tickerDatasource: TickerDatasource = null

    def articlesForTicker(ticker: String, alias: Seq[String]): Seq[(String, Double)] = {
        val query = new SolrQuery
        query.setQuery(s"content:${"\"" + ticker + "\""}")
        query.setFields("id", "score")
        query.setRows(1000)

        try {
            val solrResponse = articleSolrClient.query(query)
            val documentList = solrResponse.getResults

            var articles: Seq[(String, Double)] = Seq[(String, Double)]()

            var it = documentList.listIterator
            while(it.hasNext) {
                val solrDoc = it.next

                (solrDoc.getFieldValue("id"), solrDoc.getFieldValue("score")) match {
                    case (id: String, score: java.lang.Float) => {
                        val castedScore: Double = score.doubleValue
                        articles = articles :+ (id, castedScore)
                    }
                }
            }

            articles

        } catch {
            case e: Exception => {
                e.printStackTrace()
                Seq[(String, Double)]()
            }
        }
    }

    def articleById(id: String): Option[Article] = {
        var query = new SolrQuery
        query.setQuery(s"id:${"\"" + id + "\""}")

        val response = articleSolrClient.query(query)
        val docList = response.getResults

        if(docList.getNumFound > 0) {
            val doc = docList.get(0)
            (doc.getFieldValue("id"), doc.getFieldValue("content"), doc.getFieldValue("date"), doc.getFieldValue("title")) match {
                case (id: String, content: String, date: Date, title: String) => Some(Article(id, title, content, date))
                case _ => None
            }
        } else None
    }

    def mapArticleStock(scoredStockArticle: (String, Article, Double)): Option[(SolrDocument, Article)] = {
        val ticker = scoredStockArticle._1
        val article = scoredStockArticle._2
        val score = scoredStockArticle._3

        var query = new SolrQuery
        val plusOneDate = new Date(article.date.getTime() + (1000 * 60 * 60 * 24))
        val startDay = s"${dayFormat.format(plusOneDate)}T00:00:00Z"
        val endDay = s"${dayFormat.format(plusOneDate)}T23:59:59Z"

        query.setQuery("*:*")
        query.setFilterQueries(s"symbol:${"\"" + ticker + "\""}", s"date:[$startDay TO $endDay]")
        query.setRows(1)
        query.setStart(0)

        try {
            val response: QueryResponse = stockHistorySolrClient.query(query)
            val documentList = response.getResults

            if(documentList.getNumFound > 0) {
                val solrDocument = documentList.get(0)
                Some((solrDocument, article))
            } else {
                None
            }
        } catch {
            case e: Exception => {
                None
            }
        }
    }

    def indexAll(): Unit = {
        logger.info("Clearing the stock article index")
        deleteAll()
        logger.info("Cleared the stock article index")

        logger.info("Re-indexing stock articles")

        val tickerAliases = tickerDatasource.getTickerAliasesWithoutTickerName

        var numProcessed = 0
        val tickerArticleScores = tickerAliases.par
            .map { (tickerAlias: (String, Seq[String])) =>
                val scoredArticles = articlesForTicker(tickerAlias._1, tickerAlias._2).map((result: (String, Double)) => (tickerAlias._1, result._1, result._2))
                numProcessed += 1
                logger.info(s"processed [$numProcessed] of [${tickerAliases.size}}], found: [${scoredArticles.size}]")
                scoredArticles
            }.flatten

        logger.info("Finished processing tickers")

        val articleIds = tickerArticleScores.map(_._2).toSet

        numProcessed = 0

        val scoredIdMap = articleIds.map { (id: String) =>
            val filteredTickerScores = tickerArticleScores filter (_._2 == id)
            val mappedResult = (id, filteredTickerScores.toList.sortBy(_._3).map((tickerScore: (String, String, Double)) => (tickerScore._1, tickerScore._3)).seq)

            numProcessed += 1
            logger.info(s"processed article id [$numProcessed] of [${articleIds.size}}]")

            mappedResult
        } filterNot (_._2.isEmpty) map{ (tuple: (String, Seq[(String, Double)])) =>
            (tuple._1, tuple._2.head)
        }

        numProcessed = 0

        val articles = scoredIdMap.map { (idTuple: (String, (String, Double))) =>
            val article = articleById(idTuple._1)

            numProcessed += 1
            logger.info(s"processed article id [$numProcessed] of [${scoredIdMap.size}}]")

            article
        }.flatten

        logger.info(s"Retrieved relevant articles, found: [${articles.size}]")

        numProcessed = 0

        val historyStockArticles = articles
            .map { (article: Article) =>
                val foundScoredId = scoredIdMap.filter(_._1 == article.id).head

                val articleStock = mapArticleStock(foundScoredId._2._1, article, foundScoredId._2._2)

                numProcessed += 1
                logger.info(s"processed article stock history [$numProcessed] of [${articles.size}}]")

                articleStock
            }
            .flatten
            .map { ( articleDocument: (SolrDocument, Article)) =>
                val doc = articleDocument._1
                (doc.getFieldValue("id"), doc.getFieldValue("symbol"), doc.getFieldValue("date"), doc.getFieldValue("open"), doc.getFieldValue("high"), doc.getFieldValue("low"), doc.getFieldValue("close"), doc.getFieldValue("adjClose"), doc.getFieldValue("volume")) match {
                    case (id: String, symbol: String, date: Date, open: Double, high: Double, low: Double, close: Double, adjClose: Double, volume: Long) => {
                        Some(StockHistory(id, symbol, date, open, high, low, close, adjClose, volume), articleDocument._2)
                    }
                    case _ => None
                }
            }
            .flatten

        logger.info(s"Retrieved stock history for articles, found: [${historyStockArticles.size}]")

        val solrDocuments = historyStockArticles
            .map { (stockArticle: (StockHistory, Article)) =>
                val stockHistory = stockArticle._1
                val article = stockArticle._2
                val doc: SolrInputDocument = new SolrInputDocument()
                doc.addField("articleId", article.id)
                doc.addField("title", article.title)
                doc.addField("content", article.content)
                doc.addField("date", article.date)

                doc.addField("stockHistoryId", stockHistory.id)
                doc.addField("symbol", stockHistory.symbol)
                doc.addField("historyDate", stockHistory.date)
                doc.addField("open", stockHistory.open)
                doc.addField("high", stockHistory.high)
                doc.addField("low", stockHistory.low)
                doc.addField("close", stockHistory.close)
                doc.addField("adjClose", stockHistory.adjClose)
                doc.addField("volume", stockHistory.volume)
                doc
            }.seq.toSeq

        if(solrDocuments.size > 0) {
            solrClient.add(solrDocuments)
        }

        logger.info("Re-indexed stock articles")

        try {
            solrClient.commit
            solrClient.optimize
        } catch {
            case sse: SolrServerException =>
                logger.error("Error indexing stock articles", sse);
            case ioe: IOException =>
                logger.error("Error indexing stock articles", ioe);
            case e: Exception =>
                logger.error("Error indexing stock articles", e);
        }
    }

    def deleteAll(): Unit = clearIndexByQuery("*:*")

    def clearIndexByQuery(query: String): Unit = {
        try {
            solrClient.deleteByQuery(query)
            solrClient.commit
        } catch {
            case sse: SolrServerException =>
                throw new IndexerException("Error clearing stock articles from index", sse)
            case ioe: IOException =>
                throw new IndexerException("Error clearing stock articles from index", ioe)
        }
    }
}
