package com.stockit.util

import org.apache.http.client.HttpClient
import org.apache.http.conn.HttpClientConnectionManager
import org.apache.http.impl.client.HttpClients

/**
 * Created by dmcquill on 3/25/15.
 */
class HttpClientFactory {

    def getHttpClient(cm: HttpClientConnectionManager): HttpClient = {
        return HttpClients.custom.setConnectionManager(cm).build
    }

}
