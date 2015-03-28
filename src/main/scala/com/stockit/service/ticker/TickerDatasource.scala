package com.stockit.service.ticker

import java.io.{FileReader, File}
import java.util.Scanner

/**
 * Created by dmcquill on 3/27/15.
 */
class TickerDatasource {

    var dataStore: Map[String, Seq[String]] = null

    private def initDatastore: Unit = {
        val file = new File("./tickerSymbols.csv")

        var fileReader = new Scanner(new FileReader(file))

        var keyedTickers: Map[String, Seq[String]] = Map[String, Seq[String]]()

        while(fileReader.hasNext()) {
            val line = fileReader.nextLine
            val lineParts = line.split(",")
            val ticker: String = lineParts(0)
            val companyName: String = lineParts(1)

            if(keyedTickers.contains(ticker) == false) {
                keyedTickers += ticker -> Seq[String]()
            }

            var aliases = keyedTickers(ticker)

            if(aliases.contains(ticker) == false) {
                aliases = aliases :+ ticker
            }

            if(aliases.contains(companyName) == false) {
                aliases = aliases :+ companyName
            }

            keyedTickers += (ticker -> aliases)
        }

        fileReader.close()

        dataStore = keyedTickers
    }

    def getTickers: Seq[String] = {
        if(dataStore == null) {
            initDatastore
        }

        dataStore.keys.toSeq
    }

    def getTickerAliases: Map[String, Seq[String]] = {
        if(dataStore == null) {
            initDatastore
        }

        dataStore
    }

}
