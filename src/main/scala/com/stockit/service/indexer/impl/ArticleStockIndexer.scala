package com.stockit.service.indexer.impl
import collection.JavaConversions.asJavaCollection
import com.stockit.service.ticker.TickerDatasource

import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
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

    val logger = LoggerFactory.getLogger(classOf[HistoricStockIndexer])

    var solrClient: SolrClient = null

    var stockHistorySolrClient: SolrClient = null

    var articleSolrClient: SolrClient = null

    val rowsPerIteration = 1

    val dayFormat = new SimpleDateFormat("yyyy-MM-dd")

    var tickerDatasource: TickerDatasource = null

    def processArticle(article: Article): Option[(String, Article, Double)] = {
        val id = article.id

        var tickersProcessed = 0
        val tickerAliases = tickerDatasource.getTickerAliasesWithoutTickerName

        val tickerScores = tickerAliases.zipWithIndex.par
            .map { (indexedTickerAlias: ((String, Seq[String]), Int)) =>
                val tickerAlias = indexedTickerAlias._1

                if (tickerAlias._2.size == 0) {
                    tickersProcessed += 1
                    if (tickersProcessed % 1000 == 0 || tickerAliases.size - tickersProcessed < 10) {
                        logger.info(s"processed ticker [$tickersProcessed] of [${tickerAliases.size}]")
                    }
                    None
                } else {
                    val scoreQuery = new SolrQuery()
                    scoreQuery.setQuery(s"content:${"\"" + tickerAlias._2(0) + "\""}")
                    scoreQuery.setFilterQueries(s"id:${"\"" + id + "\""}")
                    scoreQuery.setFields("id,score")
                    scoreQuery.setSort("score", SolrQuery.ORDER.desc)

                    try {
                        val solrResponse = articleSolrClient.query(scoreQuery)
                        tickersProcessed += 1

                        if (tickersProcessed % 1000 == 0 || tickerAliases.size - tickersProcessed < 10) {
                            logger.info(s"processed ticker [$tickersProcessed] of [${tickerAliases.size}]")
                        }
                        Some((tickerAlias._1, solrResponse))
                    } catch {
                        case e: Exception => {
                            tickersProcessed += 1
                            if (tickersProcessed % 1000 == 0 || tickerAliases.size - tickersProcessed < 10) {
                                logger.info(s"processed ticker [$tickersProcessed] of [${tickerAliases.size}] in error")
                            }
                            None
                        }
                    }
                }
            }
            .flatten
            .map { (tickerQuery: (String, QueryResponse)) =>
                val documentList = tickerQuery._2.getResults

                if (documentList.getNumFound == 0) {
                    None
                } else {
                    val doc = documentList.get(0)

                    doc.getFieldValue("score") match {
                        case (score: java.lang.Float) => {
                            Some((tickerQuery._1, score.toDouble))
                        }
                    }
                }
            }
            .flatten

        if(tickerScores.isEmpty) {
            None
        } else {
            val max = tickerScores.maxBy(_._2)
            Some((max._1, article, max._2))
        }
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


        def processQuery(query: SolrQuery): Unit = {

            def processInnerQuery(articleQuery: SolrQuery): Unit = {
                val response = articleSolrClient.query(articleQuery)

                val documentList = response.getResults

                var docList: Seq[Article] = Seq[Article]()
                val it = documentList.listIterator
                while(it.hasNext) {
                    val document = it.next()
                    (document.getFieldValue("id"),document.getFieldValue("title"),document.getFieldValue("content"),document.getFieldValue("date")) match {
                        case (id: String, title: String, content: String, date: Date) => {
                            docList = docList :+ Article(id, title, content, date)
                        }
                    }
                }

                val articleStocks: Seq[(String, Article, Double)] = docList.zipWithIndex.map { (indexedId: (Article, Int)) =>
                    val result = processArticle(indexedId._1)

                    logger.info(s"processed [${indexedId._2}] of [${docList.size}]")

                    result
                }.flatten

                val historyStockArticles: Seq[(StockHistory, Article)] = articleStocks
                    .map(mapArticleStock)
                    .flatMap { list =>
                    list.map { (articleSolrDoc: (SolrDocument, Article)) =>
                        val solrDocument = articleSolrDoc._1
                        val article = articleSolrDoc._2
                        (solrDocument.getFieldValue("id"), solrDocument.getFieldValue("symbol"), solrDocument.getFieldValue("date"), solrDocument.getFieldValue("open"), solrDocument.getFieldValue("high"), solrDocument.getFieldValue("low"), solrDocument.getFieldValue("close"), solrDocument.getFieldValue("adjClose"), solrDocument.getFieldValue("volume")
                            ) match {
                            case (
                                id: String, symbol: String, date: Date, open: Double, high: Double, low: Double, close: Double, adjClose: Double, volume: Long
                                ) => Some((StockHistory(id, symbol, date, open, high, low, close, adjClose, volume), article))
                            case _ => None
                        }
                    }
                }
                    .flatten.seq

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
                }

                if(solrDocuments.size > 0) {
                    solrClient.add(solrDocuments)
                }

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

                if(articleQuery.getStart < response.getResults.getNumFound) {
                    query.setStart(query.getStart + rowsPerIteration)

                    logger.info(s"completed [${query.getStart + query.getRows}] of [${response.getResults.getNumFound}]")

                    processInnerQuery(query)
                }
            }

            processInnerQuery(query)
        }

        val articleQuery = new SolrQuery()
        articleQuery.setQuery("*:*")
        articleQuery.setStart(0)
        articleQuery.setRows(rowsPerIteration)

        processQuery(articleQuery)

        logger.info("Re-indexed stock articles")
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
