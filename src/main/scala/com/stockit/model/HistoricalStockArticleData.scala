package com.stockit.model

/**
 * Created by dmcquill on 4/14/15.
 */
// TODO: stock history model
case class HistoricalStockArticleData(stockHistory: StockHistory, articles: Seq[Article], idf: Double)
