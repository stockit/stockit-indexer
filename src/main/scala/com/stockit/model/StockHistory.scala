package com.stockit.model

import java.util.Date

/**
 * Created by dmcquill on 4/14/15.
 */
case class StockHistory(id: String, symbol: String, date: Date, open: Double, high: Double, low: Double, close: Double, adjClose: Double, volume: Long)
