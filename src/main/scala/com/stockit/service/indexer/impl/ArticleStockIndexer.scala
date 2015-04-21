package com.stockit.service.indexer.impl

import com.stockit.service.ticker.TickerDatasource

import collection.JavaConversions.asJavaCollection

import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.lang.{Double, Long}

import com.stockit.model.{StockHistory, Article}
import com.stockit.service.indexer.{IndexerException, Indexer}
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.client.solrj.{SolrQuery, SolrServerException, SolrClient}
import org.apache.solr.common.{SolrDocument, SolrInputDocument}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer

/**
 * Created by dmcquill on 4/17/15.
 */
class ArticleStockIndexer extends Indexer {
    implicit val formats = org.json4s.DefaultFormats

    val logger = LoggerFactory.getLogger(classOf[HistoricStockIndexer])

    var solrClient: SolrClient = null

    var stockHistorySolrClient: SolrClient = null

    var articleSolrClient: SolrClient = null

    val numPerPage = 500

    val dayFormat = new SimpleDateFormat("yyyy-MM-dd")

    var tickerDatasource: TickerDatasource = null

//    private def processArticleQuery(queryTuple: (String, SolrQuery)): (String, Seq[(StockHistory, Article)]) = {
//
//        def innerProcessArticleQuery(query: SolrQuery): Seq[(StockHistory, Article)] = {
//            val response: QueryResponse = articleSolrClient.query(query)
//            val documentList = response.getResults
//            val total = documentList.getNumFound
//
//            var documentsListBuffer: ListBuffer[Article] = ListBuffer[Article]()
//
//            var it = documentList.listIterator
//            while(it.hasNext()) {
//                val solrDocument = it.next()
//
//                (solrDocument.getFieldValue("id"), solrDocument.getFieldValue("title"), solrDocument.getFieldValue("content"), solrDocument.getFieldValue("date")) match {
//                    case (id: String, title: String, content: String, date: Date) => {
//                        documentsListBuffer += Article(id, title, content, date)
//                    }
//                    case _ => {}
//                }
//            }
//
//            val articles = documentsListBuffer.toSeq
//
//            val historyStockArticles: Seq[(StockHistory, Article)] = articles
//                .map { (article: Article) =>
//                    var query = new SolrQuery()
//                    val plusOneDate = new Date(article.date.getTime() + (1000 * 60 * 60 * 24))
//                    val startDay = s"${dayFormat.format(plusOneDate)}T00:00:00Z"
//                    val endDay = s"${dayFormat.format(plusOneDate)}T23:59:59Z"
//
//                    query.setQuery("*:*")
//                    query.setFilterQueries(s"symbol:${"\"" + queryTuple._1 + "\""}", s"date:[$startDay TO $endDay]")
//                    query.setRows(1)
//                    query.setStart(0)
//
//                    val response: QueryResponse = stockHistorySolrClient.query(query)
//                    val documentList = response.getResults
//
//                    if(documentList.getNumFound > 0) {
//                        val solrDocument = documentList.get(0)
//                        Some((solrDocument, article))
//                    } else {
//                        None
//                    }
//
//                }
//                .flatMap { list =>
//                    list.map { (articleSolrDoc: (SolrDocument, Article)) =>
//                        val solrDocument = articleSolrDoc._1
//                        val article = articleSolrDoc._2
//                        (solrDocument.getFieldValue("id"), solrDocument.getFieldValue("symbol"), solrDocument.getFieldValue("date"), solrDocument.getFieldValue("open"), solrDocument.getFieldValue("high"), solrDocument.getFieldValue("low"), solrDocument.getFieldValue("close"), solrDocument.getFieldValue("adjClose"), solrDocument.getFieldValue("volume")
//                        ) match {
//                            case (
//                                id: String, symbol: String, date: Date, open: Double, high: Double, low: Double, close: Double, adjClose: Double, volume: Long
//                            ) => Some((StockHistory(id, symbol, date, open, high, low, close, adjClose, volume), article))
//                            case _ => None
//                        }
//                    }
//                }
//                .flatten.seq
//
//
////            if(query.getStart < total) {
////                query setStart numPerPage + query.getStart
////                historyStockArticles ++ innerProcessArticleQuery(query)
//           // } else
//            historyStockArticles
//        }
//
//        (queryTuple._1, innerProcessArticleQuery {
//            queryTuple._2
//        })
//    }

//    private def indexForTicker(ticker: String, alias: String): Unit = {
//        var query = new SolrQuery
//        query.setParam("wt", "json")
//        query.setParam("indent", "true")
//        query.setQuery(s"content:'$alias'")
//        query.setRows(numPerPage)
//        query.setStart(0)
//
//        val tickerArticles: (String, Seq[(StockHistory, Article)]) = processArticleQuery((ticker, query))
//        val solrDocuments = tickerArticles._2
//            .map { (stockArticle: (StockHistory, Article)) =>
//                val stockHistory = stockArticle._1
//                val article = stockArticle._2
//                val doc: SolrInputDocument = new SolrInputDocument()
//                doc.addField("articleId", article.id)
//                doc.addField("title", article.title)
//                doc.addField("content", article.content)
//                doc.addField("date", article.date)
//
//                doc.addField("stockHistoryId", stockHistory.id)
//                doc.addField("symbol", stockHistory.symbol)
//                doc.addField("historyDate", stockHistory.date)
//                doc.addField("open", stockHistory.open)
//                doc.addField("high", stockHistory.high)
//                doc.addField("low", stockHistory.low)
//                doc.addField("close", stockHistory.close)
//                doc.addField("adjClose", stockHistory.adjClose)
//                doc.addField("volume", stockHistory.volume)
//                doc
//            }
//
//        if(solrDocuments.size > 0) {
//            solrClient.add(solrDocuments)
//        }
//
//        logger.info(s"Found ${solrDocuments.size} for ticker: [$ticker]")
//    }

    def indexAll(): Unit = {
        logger.info("Clearing the stock article index")
        deleteAll()
        logger.info("Cleared the stock article index")

        logger.info("Re-indexing stock articles")

        var current = 0

        // TODO: new index process
        // outer article in articles
        //    max( score(ticker) in tickers )
        //       store article with ticker and stock history
        val articleQuery = new SolrQuery()
        articleQuery.setQuery("*:*")
        articleQuery.setFields("id")
        articleQuery.setStart(0)
        articleQuery.setRows(500)

        val response = articleSolrClient.query(articleQuery)
        val documentList = response.getResults

        val ids = new ListBuffer[String]()
        val it = documentList.listIterator
        while(it.hasNext) {
            val document = it.next()
            document.getFieldValue("id") match {
                case (id: String) => ids += id
                case _ => None
            }
        }


        val articleStocks = ids.zipWithIndex.map { (indexedId: (String, Int)) =>
            val id = indexedId._1

            var tickersProcessed = 0
            val tickerAliases = tickerDatasource.getTickerAliasesWithoutTickerName

            val tickerScores = tickerAliases.zipWithIndex.par
                .map { (indexedTickerAlias: ((String, Seq[String]), Int)) =>
                    val tickerAlias = indexedTickerAlias._1

                    if (tickerAlias._2.size == 0) {
                        tickersProcessed += 1
                        logger.info(s"processed ticker [$tickersProcessed] of [${tickerAliases.size}]")
                        None
                    } else {
                        val scoreQuery = new SolrQuery()
                        scoreQuery.setQuery(s"content:${"\"" + tickerAlias._2(0) + "\""}")
                        scoreQuery.setFilterQueries(s"id:${"\"" + id + "\""}")
                        scoreQuery.setFields("id,score")
                        scoreQuery setSort("score", SolrQuery.ORDER.desc)

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
                            case (score: String) => Some((tickerQuery._1, score.toDouble))
                            case _ => None
                        }
                    }
                }
                .flatten

            logger.info(s"processed [${indexedId._2}] of [${ids.length}], found [${tickerScores.size}]")

            if(tickerScores.isEmpty) None else Some(tickerScores.maxBy(_._2))
        }.flatten

        println(articleStocks)


//        tickerDatasource.getTickerAliasesWithoutTickerName.zipWithIndex.par.foreach({ (alias: ((String, Seq[String]), Int)) =>
//            try {
//                indexForTicker(alias._1._1, alias._1._2(0))
//
//                solrClient.commit
//                solrClient.optimize
//            } catch {
//                case sse: SolrServerException =>
//                    logger.error("Unexpected io exception", sse)
////                    throw new IndexerException("Error indexing stock articles", sse)
//                case ioe: IOException =>
//                    logger.error("Unexpected io exception", ioe)
////                    throw new IndexerException("Error indexing stock articles", ioe)
//                case e: Exception =>
//                    logger.error("Unexpected io exception", e)
//
//            }
//            current += 1
//            logger.info(s"Processed stock data for ticker: [${alias._1._1}] completed: [$current of ${tickerDatasource.getTickers.length}]")
//        })

        logger.info("Re-indexed stock articles")

        try {
            solrClient.commit
            solrClient.optimize
        } catch {
            case sse: SolrServerException =>
                throw new IndexerException("Error indexing stock articles", sse)
            case ioe: IOException =>
                throw new IndexerException("Error indexing stock articles", ioe)
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
