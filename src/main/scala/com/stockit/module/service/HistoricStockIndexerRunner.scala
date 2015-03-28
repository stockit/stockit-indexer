package com.stockit.module.service

import com.stockit.service.indexer.impl.{HistoricStockIndexer, ArticlesIndexer}
import org.apache.solr.client.solrj.SolrClient
import scaldi.Module

/**
 * Created by dmcquill on 3/26/15.
 */
class HistoricStockIndexerModule extends Module {

    implicit val dependencies = new SolrClientModule

    bind [HistoricStockIndexer] identifiedBy 'indexer and 'historicStock to new HistoricStockIndexer initWith {
      indexer: HistoricStockIndexer => indexer.solrClient = inject[SolrClient]('solrClient and 'httpSolrClient and 'historicStockSolrClient)
    }
}
