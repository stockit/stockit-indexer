package com.stockit.yql

import java.net.URL

import org.apache.commons.httpclient.util.URIUtil


/**
 * Created by jmcconnell1 on 3/1/15.
 */
class Client {
  def query(statement: String) = {

    val baseUrl = "https://query.yahooapis.com/v1/public/yql?q="
    val fullUrlStr = baseUrl + URIUtil.encodeQuery(statement, "UTF-8").replace("=", "%3D") +
      "&format=json&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys"

    val fullUrl = new URL(fullUrlStr)
    println(fullUrl)

    val is = fullUrl.openStream()
    val stream = scala.io.Source.fromInputStream(is).mkString
    is.close()

    stream
  }
}
