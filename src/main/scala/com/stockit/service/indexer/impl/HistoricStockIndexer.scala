package com.stockit.service.indexer.impl

import com.stockit.service.ticker.TickerDatasource
import com.stockit.util.DateIntervalUtil
import org.json4s.JValue
import org.json4s.JsonAST.JValue

import collection.JavaConversions.asJavaCollection

import java.io.IOException
import java.util
import java.util.{Collection, Date}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import com.stockit.StockitApp
import com.stockit.service.indexer.{Indexer, IndexerException}
import com.stockit.table.{Article, Articles}
import com.stockit.yql.StockClient
import org.apache.solr.client.solrj.{SolrClient, SolrServerException}
import org.apache.solr.common.SolrInputDocument
import org.json4s
import org.slf4j.LoggerFactory

import org.json4s.{JValue, native}

import slick.driver.MySQLDriver.simple._

/**
 * Created by dmcquill on 3/23/15.
 */
class HistoricStockIndexer extends Indexer {

    implicit val formats = org.json4s.DefaultFormats

    val logger = LoggerFactory.getLogger(classOf[HistoricStockIndexer])

    var solrClient: SolrClient = null

    val stockClient = new StockClient
    val tickerDatasource = new TickerDatasource

    val dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd")
    val dateIntervalUtil = new DateIntervalUtil()

    val articles = TableQuery[Articles]

    val doubleAttributes = ("Open"->"open")::("High"->"high")::("Low"->"low")::("Close"->"close")::("Adj_Close"->"adjClose")::Nil
    val longAttributes = ("Volume"->"volume")::Nil
    val dateAttributes = ("Date"->"date")::Nil
    val stringAttributes = ("Symbol"->"symbol")::Nil

    val maxBatchSize = 200

    private def indexTickers(tickers: Seq[String]): Unit = {
        logger.info("Re-indexing historic stocks from yql")
        val db = StockitApp.stockitDatabase

        val session = db.createSession()

        var maxDate: Date = articles.map({ _.date }).max.run(session).getOrElse(null)
        var minDate: Date = articles.map({ _.date }).min.run(session).getOrElse(null)

        if(maxDate != null && minDate != null) {
            logger.info(s"total date interval: [ ${dateFormatter.format(minDate)}, ${dateFormatter.format(maxDate)} ]")

            val numIntervals: Int = Math.ceil(( maxDate.getTime() - minDate.getTime() ).asInstanceOf[Double] / DateIntervalUtil.MILLIS_IN_YEAR).asInstanceOf[Int]
            var partitions: Seq[(Date, Date)] = dateIntervalUtil.partitionDateRange(minDate, maxDate, numIntervals)

            val documents = partitions
                .flatMap { partition =>
                logger.info(s"processing date subinterval: [ ${dateFormatter.format(partition._1)}, ${dateFormatter.format(partition._2)} ]")
                var results = stockClient.priceDataBatch(tickers.toList, dateFormatter.format(partition._1), dateFormatter.format(partition._2))
                logger.info(s"done processing date subinterval: [ ${dateFormatter.format(partition._1)}, ${dateFormatter.format(partition._2)} ]")
                results
            }
                .flatMap { response: String =>
                val responseJSON: JValue = native.parseJson(response)
                val quotes: JValue = responseJSON \ "query" \ "results" \ "quote"
                quotes.children.map({ quote: json4s.JValue =>
                    try {
                        extractDocument(quote)
                    } catch {
                        case e: Exception => null
                    }
                })
            }
                .filter({ _ != null })
            solrClient.add(documents)
        }

        try {
            solrClient.commit
            solrClient.optimize
        } catch {
            case sse: SolrServerException =>
                throw new IndexerException("Error clearing historic_stocks from index", sse)
            case ioe: IOException =>
                throw new IndexerException("Error clearing historic_stocks from index", ioe)
        }
        logger.info("Re-indexed historic_stocks from yql")
    }

    def indexAll(): Unit = {
        logger.info("Clearing the historic stocks index")
//        deleteAll()
        logger.info("Cleared the historic stocks index")

        val allTickers = tickerDatasource.getTickers

        val batchSize = maxBatchSize

        var currentBatch = 1

        val groupedStockTickers = allTickers
            .grouped(batchSize)
            .toList
            .map({ _.toSeq })

        val totalBatches = groupedStockTickers.size
        groupedStockTickers
            .foreach { batch: Seq[String] =>
                logger.info(s"started stock ticker batch: [$currentBatch] of [$totalBatches]")
                indexTickers(batch.toSeq)
                currentBatch += 1
                logger.info(s"finished stock ticker batch: [$currentBatch] of [$totalBatches]")
            }
    }

    def extractDocument(quote: json4s.JValue) : SolrInputDocument = {
        var doc: SolrInputDocument = new SolrInputDocument()
        for (attribute <- doubleAttributes) {
            var value = (quote \ attribute._1).extract[String]
            doc.addField(attribute._2, value.toDouble)
        }
        for (attribute <- longAttributes) {
            var value = (quote \ attribute._1).extract[String]
            doc.addField(attribute._2, value.toLong)
        }
        for (attribute <- dateAttributes) {
            var value = (quote \ attribute._1).extract[String]
            doc.addField(attribute._2, dateFormatter.parse(value))
        }
        for (attribute <- stringAttributes) {
            var value = (quote \ attribute._1).extract[String]
            doc.addField(attribute._2, value)
        }
        doc
    }

    def deleteAll(): Unit = clearIndexByQuery("*:*")

    def clearIndexByQuery(query: String): Unit = {
        try {
            solrClient.deleteByQuery(query)
            solrClient.commit
        } catch {
            case sse: SolrServerException =>
                throw new IndexerException("Error clearing historic_stocks from index", sse)
            case ioe: IOException =>
                throw new IndexerException("Error clearing historic_stocks from index", ioe)
        }
    }

}
