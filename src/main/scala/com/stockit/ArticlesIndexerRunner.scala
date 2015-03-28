package com.stockit

import com.stockit.module.service.IndexerModule
import com.stockit.service.indexer.impl.ArticlesIndexer
import scaldi.Injectable

/**
 * Created by dmcquill on 3/26/15.
 */
object ArticlesIndexerRunner extends Injectable {
    def main(args: Array[String]): Unit = {

        implicit val appModule = new IndexerModule()

        val articlesIndexer = inject[ArticlesIndexer]('indexer and 'articles)

        articlesIndexer.indexAll()

    }
}
