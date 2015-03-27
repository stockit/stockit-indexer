package com.stockit.module.service

import com.stockit.util.HttpClientFactory
import org.apache.http.client.HttpClient
import org.apache.http.conn.HttpClientConnectionManager
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import scaldi.Module

/**
 * Created by dmcquill on 3/26/15.
 */
class HttpClientModule extends Module {
    bind [HttpClientConnectionManager] identifiedBy 'httpConnectionManager and 'poolingCM to new PoolingHttpClientConnectionManager() initWith {
        _.setDefaultMaxPerRoute(10)
    } initWith {
        _.setMaxTotal(10)
    }

    bind [HttpClientFactory] identifiedBy 'httpClientFactory to new HttpClientFactory

    bind [HttpClient] identifiedBy 'httpClient and 'poolingHttpClient to
        inject[HttpClientFactory]('httpClientFactory).getHttpClient(inject[HttpClientConnectionManager]('httpConnectionManager and 'poolingCM))
}
