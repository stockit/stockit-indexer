package com.stockit.service.indexer.impl

import org.json4s.JValue
import org.json4s.JsonAST.JValue

import collection.JavaConversions.asJavaCollection

import java.io.IOException
import java.util
import java.util.{Collection, Date}

import com.stockit.StockitApp
import com.stockit.service.indexer.{Indexer, IndexerException}
import com.stockit.table.{Article, Articles}
import com.stockit.yql.StockClient
import org.apache.solr.client.solrj.{SolrClient, SolrServerException}
import org.apache.solr.common.SolrInputDocument
import org.json4s
import org.slf4j.LoggerFactory

import org.json4s.{JValue, native}

/**
 * Created by dmcquill on 3/23/15.
 */
class HistoricStockIndexer extends Indexer {

    implicit val formats = org.json4s.DefaultFormats

    val logger = LoggerFactory.getLogger(classOf[HistoricStockIndexer])

    var solrClient: SolrClient = null

    val stockClient = StockClient

    val dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd")

    val numAttributes = "Open"::"High"::"Low"::"Close"::"Volume"::"Adj_Close"::Nil
    val dateAttributes = "Date"::Nil
    val stringAttributes = "Symbol"::Nil

    def indexAll(): Unit = {
        logger.info("Clearing the historic stocks index")
        deleteAll()
        logger.info("Cleared the historic stocks index")

        logger.info("Re-indexing historic stocks from db")

        val documents: Collection[SolrInputDocument] = stockClient.priceDataBatch(List(), "", "")
            .flatMap({ response: String =>
            val responseJSON: JValue = native.parseJson(response)
            val quotes: JValue = responseJSON \ "query" \ "results" \ "quote"
            quotes.children.map({ quote: json4s.JValue =>
                extractDocument(quote)
            })
        })

        solrClient.add(documents)

        logger.info("Re-indexed historic_stocks from db")

        try {
            solrClient.commit
            solrClient.optimize
        } catch {
            case sse: SolrServerException =>
                throw new IndexerException("Error clearing historic_stocks from index", sse)
            case ioe: IOException =>
                throw new IndexerException("Error clearing historic_stocks from index", ioe)
        }
    }

    def extractDocument(quote: json4s.JValue) : SolrInputDocument = {
        var doc: SolrInputDocument = new SolrInputDocument()
        for (attribute <- numAttributes) {
            var value = (quote \ attribute).extract[String]
            doc.addField(attribute, value.toDouble)
        }
        for (attribute <- dateAttributes) {
            var value = (quote \ attribute).extract[String]
            doc.addField(attribute, dateFormatter.parse(value))
        }
        for (attribute <- stringAttributes) {
            var value = (quote \ attribute).extract[String]
            doc.addField(attribute, value)
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
