package com.stockit.module.service

import com.stockit.service.indexer.impl.ArticlesIndexer
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.impl.{HttpSolrClient, LBHttpSolrClient, CloudSolrClient}
import scaldi.Module

/**
 * Created by dmcquill on 3/26/15.
 */
class ArticlesIndexerModule extends Module {

    implicit val dependencies = new SolrClientModule

    bind [ArticlesIndexer] identifiedBy 'indexer and 'articles to new ArticlesIndexer initWith { indexer: ArticlesIndexer =>
        indexer.solrClient = inject[SolrClient]('solrClient and 'httpSolrClient and 'articlesSolrClient)
    }
}
