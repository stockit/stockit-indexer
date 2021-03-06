package com.stockit.module.service

import scaldi.Module

/**
 * Created by dmcquill on 3/26/15.
 */
class SolrClientConfigModule extends Module {
    bind [String] identifiedBy 'solr and 'cloudUrl to "solr.deepdishdev.com:8983"

    bind [String] identifiedBy 'solr and 'articlesHttpUrl to "http://solr.deepdishdev.com:8983/solr/articles"

    bind [String] identifiedBy 'solr and 'historicStockHttpUrl to "http://solr.deepdishdev.com:8983/solr/stockHistory"

    bind [String] identifiedBy 'solr and 'stocksHttpUrl to "http://solr.deepdishdev.com:8983/solr/stock"

    bind [String] identifiedBy 'solr and 'articleStockHttpUrl to "http://solr.deepdishdev.com:8983/solr/articleStock"

    bind [Int] identifiedBy 'solr and 'clientTimeout to 30000
    bind [Int] identifiedBy 'solr and 'connectTimeout to 30000
}
