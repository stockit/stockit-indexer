package com.stockit.service.indexer.impl

import com.stockit.service.ticker.TickerDatasource

import collection.JavaConversions.asJavaCollection

import java.io.{IOException}
import java.util.{Collection}

import com.stockit.service.indexer.IndexerException
import org.apache.solr.client.solrj.{SolrServerException, SolrClient}
import org.apache.solr.common.SolrInputDocument
import org.slf4j.LoggerFactory

/**
 * Created by dmcquill on 3/27/15.
 */
class StockIndexer {
    val logger = LoggerFactory.getLogger(classOf[ArticlesIndexer])

    var solrClient: SolrClient = null

    def indexAll(): Unit = {
        logger.info("Clearing the stocks index")
        deleteAll()
        logger.info("Cleared the stocks index")

        logger.info("Re-indexing stocks from csv")

        val tickerDatasource = new TickerDatasource

        val keyedTickers = tickerDatasource.getTickerAliases

        val documents: Collection[SolrInputDocument] = keyedTickers.map({ (mapping) =>
            var doc: SolrInputDocument = new SolrInputDocument()
            doc.addField("id", mapping._1)
            doc.addField("stockTicker", mapping._1)

            mapping._2 foreach {
                doc.addField("aliases", _)
            }

            doc
        })

        solrClient.add(documents)

        logger.info("Re-indexed stocks from csv")

        try {
            solrClient.commit
            solrClient.optimize
        } catch {
            case sse: SolrServerException =>
                throw new IndexerException("Error saving stocks to index", sse)
            case ioe: IOException =>
                throw new IndexerException("Error saving stocks to index", ioe)
        }
    }

    def deleteAll(): Unit = clearIndexByQuery("*:*")

    def clearIndexByQuery(query: String): Unit = {
        try {
            solrClient.deleteByQuery(query)
            solrClient.commit
        } catch {
            case sse: SolrServerException =>
                throw new IndexerException("Error clearing stocks from index", sse)
            case ioe: IOException =>
                throw new IndexerException("Error clearing stocks from index", ioe)
        }
    }
}
