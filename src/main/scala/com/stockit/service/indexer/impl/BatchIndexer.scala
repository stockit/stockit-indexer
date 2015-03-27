package com.stockit.service.indexer.impl

import com.stockit.service.indexer.Indexer

/**
 * Created by dmcquill on 3/23/15.
 */
class BatchIndexer(indexers: List[Indexer]) extends Indexer {

    def indexAll(): Unit = {
        indexers foreach {
            _.indexAll()
        }
    }

    def deleteAll(): Unit = {
        indexers foreach {
            _.deleteAll()
        }
    }

}
