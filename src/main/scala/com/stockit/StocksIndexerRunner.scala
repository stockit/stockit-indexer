package com.stockit

import com.stockit.module.service.IndexerModule
import com.stockit.service.indexer.Indexer
import com.stockit.service.indexer.impl.StockIndexer
import scaldi.Injectable

/**
 * Created by dmcquill on 3/27/15.
 */
object StocksIndexerRunner extends Injectable {

   def main(args: Array[String]): Unit = {

       implicit val appModule = new IndexerModule

       val stocksIndexer = inject[StockIndexer]('indexer and 'stocks)

       stocksIndexer.indexAll()
   }

}
