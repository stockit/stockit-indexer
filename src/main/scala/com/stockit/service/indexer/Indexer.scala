package com.stockit.service.indexer

/**
 * Created by dmcquill on 3/23/15.
 */
trait Indexer {

    def indexAll(): Unit
    def deleteAll(): Unit

}
