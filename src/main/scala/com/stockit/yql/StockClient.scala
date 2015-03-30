package com.stockit.yql

import com.stockit.util.ConcurrentReducer
import org.slf4j.LoggerFactory

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by jmcconnell1 on 3/17/15.
 */
class StockClient {
    val logger = LoggerFactory.getLogger(classOf[StockClient])
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

        var concurrentReducer = new ConcurrentReducer()
        concurrentReducer.threads = 8
        var numProcessed = 0

        def increment(): Unit = {
            numProcessed += 1
            if((numProcessed % 40 == 0 && numProcessed != 0) || numProcessed >= symbols.size - 1) {
                logger.info(s"Processed symbol [$numProcessed] of [${symbols.size}]")
            }
        }

        def incrementFailure(): Unit = {
            numProcessed += 1
            logger.error(s"Failed to process symbol [$numProcessed] of [${symbols.size}]")
        }

        var futureCalls = Future.sequence(concurrentReducer.map[String, String](symbols, fn = { symbol: String =>
            try {
                val result = priceData(symbol.replaceAll("&", "%26"), startDate, endDate, auditor)
                increment()
                result
            } catch {
                case e: Exception => {
                    incrementFailure()
                    null
                }
            }
        }))

        val result = Await.result(futureCalls, Duration.Inf)
        result.flatten.filter({_ != null}).toList
    }
}
