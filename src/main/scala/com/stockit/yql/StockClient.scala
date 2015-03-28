package com.stockit.yql

import java.util.Date

/**
 * Created by jmcconnell1 on 3/17/15.
 */
object StockClient {
  val client = new Client
  val queryString = "SELECT * FROM yahoo.finance.historicaldata " +
    "WHERE symbol = \"%s\" AND startDate = \"%s\" AND endDate = \"%s\""

  def defaultAudit(message: String) = { }

  def priceData(symbol: String, startDate: String, endDate: String, auditor: (String => Any) = defaultAudit) : String = {
    val queryStatement = String.format(queryString, symbol, startDate, endDate)
    auditor(queryStatement)
    client.query(queryStatement)
  }

  def priceDataBatch(symbols: List[String], startDate: String, endDate: String, auditor: (String => Any) = defaultAudit) : List[String] = {
    symbols.map(A => priceData(A, startDate, endDate, auditor))
  }
}
