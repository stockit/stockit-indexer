package com.stockit.module.service

import org.apache.http.client.HttpClient
import org.apache.solr.client.solrj.impl.LBHttpSolrClient
import scaldi.Module

/**
 * Created by dmcquill on 3/26/15.
 */
class SolrClientLBModule extends Module {
    implicit val dependentModules = new HttpClientModule :: new SolrClientConfigModule

    bind [LBHttpSolrClient] identifiedBy 'solrClient and 'lbSolrClient to new LBHttpSolrClient(
        inject[HttpClient]('httpClient and 'poolingHttpClient),
        inject[String]('solr and 'cloudUrl)
    )
}
