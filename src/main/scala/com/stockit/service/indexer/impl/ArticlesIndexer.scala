package com.stockit.service.indexer.impl

import org.apache.lucene.analysis.synonym.SolrSynonymParser

import collection.JavaConversions.asJavaCollection

import java.io.IOException
import java.util.{Collection, Date}

import com.stockit.StockitApp
import com.stockit.service.indexer.{IndexerException, Indexer}
import com.stockit.table.{Article, Articles}
import org.apache.solr.client.solrj.{SolrClient, SolrServerException}
import org.apache.solr.common.SolrInputDocument
import org.slf4j.LoggerFactory

import slick.driver.MySQLDriver.simple._
/**
 * Created by dmcquill on 3/23/15.
 */
class ArticlesIndexer extends Indexer {

    val logger = LoggerFactory.getLogger(classOf[ArticlesIndexer])

    val articles = TableQuery[Articles]

    var solrClient: SolrClient = null

    object Queries {
        val ALL_ARTICLES = "select sar.id, sar.art_ttl, sar.art_ctnt, sar.art_dt " +
                           "from st_art sar, st_link_article_xref stlax, st_link sl, st_link_archive_xref stlarx " +
                           "where stlax.article_id = sar.id and stlax.link_id = sl.id and stlarx.link_id = sl.id"
    }

    def indexAll(): Unit = {
        logger.info("Clearing the articles index")
        deleteAll()
        logger.info("Cleared the articles index")

        logger.info("Re-indexing articles from db")

        val db = StockitApp.stockitDatabase

        val session = db.createSession()

        val documents: Collection[SolrInputDocument] = articles
            .filterNot({ _.content === "" })
            .run(session)
            .map({ article: Article =>
                var doc: SolrInputDocument = new SolrInputDocument()
                doc.addField("id", article.id.get)
                doc.addField("content", article.content)
                doc.addField("date", new Date(article.date.getTime))
                doc.addField("title", article.title)
                doc
            })

        solrClient.add(documents)

        logger.info("Re-indexed articles from db")

        try {
            solrClient.commit
            solrClient.optimize
        } catch {
            case sse: SolrServerException =>
                throw new IndexerException("Error clearing articles from index", sse)
            case ioe: IOException =>
                throw new IndexerException("Error clearing articles from index", ioe)
        }
    }

    def deleteAll(): Unit = clearIndexByQuery("*:*")

    def clearIndexByQuery(query: String): Unit = {
        try {
            solrClient.deleteByQuery(query)
            solrClient.commit
        } catch {
            case sse: SolrServerException =>
                throw new IndexerException("Error clearing articles from index", sse)
            case ioe: IOException =>
                throw new IndexerException("Error clearing articles from index", ioe)
        }
    }

}
