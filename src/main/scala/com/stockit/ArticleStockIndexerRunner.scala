package com.stockit

import com.stockit.module.service.IndexerModule
import com.stockit.service.indexer.impl.{ArticleStockIndexer}
import scaldi.Injectable

/**
 * Created by dmcquill on 3/26/15.
 */
object ArticleStockIndexerRunner extends Injectable {
    def main(args: Array[String]): Unit = {

        implicit val appModule = new IndexerModule()

        val historicStockIndexer = inject[ArticleStockIndexer]('indexer and 'articleStock)

        historicStockIndexer.indexAll()
    }
}
