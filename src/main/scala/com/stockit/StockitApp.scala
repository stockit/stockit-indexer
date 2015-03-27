package com.stockit

import com.typesafe.config.ConfigFactory

import scala.slick.jdbc.JdbcBackend._

/**
 * Created by dmcquill on 3/26/15.
 */
object StockitApp extends App {

    lazy val stockitDBName = "mysql-stockit"
    lazy val databaseConfig = ConfigFactory.load("database")

    def stockitDatabase: Database = {
        Database.forConfig(
            stockitDBName,
            config = databaseConfig
        )
    }
}