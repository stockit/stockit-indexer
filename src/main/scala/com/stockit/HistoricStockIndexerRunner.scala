package com.stockit

import com.stockit.module.service.{HistoricStockIndexerModule, ArticlesIndexerModule}
import com.stockit.service.indexer.impl.{HistoricStockIndexer, ArticlesIndexer}
import scaldi.Injectable

/**
 * Created by dmcquill on 3/26/15.
 */
object HistoricStockIndexerRunner extends Injectable {
    def main(args: Array[String]): Unit = {

        implicit val appModule = new HistoricStockIndexerModule()

        val historicStockIndexer = inject[HistoricStockIndexer]('indexer and 'historicStock)

        historicStockIndexer.indexAll()
    }
}
