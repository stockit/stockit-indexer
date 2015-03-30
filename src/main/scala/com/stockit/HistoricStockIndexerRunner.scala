package com.stockit

import com.stockit.module.service.{IndexerModule}
import com.stockit.service.indexer.impl.{HistoricStockIndexer}
import com.stockit.service.ticker.TickerDatasource
import scaldi.Injectable

/**
 * Created by dmcquill on 3/26/15.
 */
object HistoricStockIndexerRunner extends Injectable {
    def main(args: Array[String]): Unit = {

        implicit val appModule = new IndexerModule()

        val historicStockIndexer = inject[HistoricStockIndexer]('indexer and 'historicStock)

        historicStockIndexer.indexAll()
    }
}
