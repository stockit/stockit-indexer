package com.stockit.service.indexer.impl

import collection.JavaConversions.asJavaCollection

import java.io.{FileReader, File, IOException}
import java.util.{Scanner, Date, Collection}

import com.stockit.StockitApp
import com.stockit.service.indexer.IndexerException
import com.stockit.table.{Article, Articles}
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

        var file = new File("./tickerSymbols.csv")

        var fileReader = new Scanner(new FileReader(file))

        var keyedTickers: Map[String, Seq[String]] = Map[String, Seq[String]]()

        while(fileReader.hasNext()) {
            val line = fileReader.nextLine
            val lineParts = line.split(",")
            val ticker: String = lineParts(0)
            val companyName: String = lineParts(1)

            if(keyedTickers.contains(ticker) == false) {
                keyedTickers += ticker -> Seq[String]()
            }

            var aliases = keyedTickers(ticker)

            if(aliases.contains(ticker) == false) {
                aliases = aliases :+ ticker
            }

            if(aliases.contains(companyName) == false) {
                aliases = aliases :+ companyName
            }

            keyedTickers += (ticker -> aliases)
        }

        fileReader.close()

        val documents: Collection[SolrInputDocument] = keyedTickers.map({ (mapping) =>
            var doc: SolrInputDocument = new SolrInputDocument()
            doc.addField("id", mapping._1)
            doc.addField("stockTicker", mapping._1)

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
