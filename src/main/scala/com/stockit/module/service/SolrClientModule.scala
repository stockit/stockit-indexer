package com.stockit.module.service

import org.apache.http.client.HttpClient
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.impl.{HttpSolrClient, LBHttpSolrClient, CloudSolrClient}
import scaldi.Module

/**
 * Created by dmcquill on 3/26/15.
 */
class SolrClientModule extends Module {
    implicit val dependentModules = new HttpClientModule :: new SolrClientLBModule :: new SolrClientConfigModule

    bind [SolrClient] identifiedBy 'solrClient and 'cloudSolrClient to new CloudSolrClient(inject[String]('solr and 'cloudUrl), inject[LBHttpSolrClient]('solrClient and 'lbSolrClient))

    bind [SolrClient] identifiedBy 'solrClient and 'httpSolrClient and 'articlesSolrClient to new HttpSolrClient(inject[String]('solr and 'articlesHttpUrl), inject[HttpClient]('httpClient and 'poolingHttpClient)) //(inject[String]('solr and 'cloudUrl), inject[LBHttpSolrClient]('solrClient and 'lbSolrClient))
}