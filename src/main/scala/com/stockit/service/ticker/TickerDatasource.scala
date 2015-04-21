package com.stockit.service.ticker

import java.io.{FileReader, File}
import java.util.Scanner

/**
 * Created by dmcquill on 3/27/15.
 */
class TickerDatasource {

    var dataStore: Map[String, Seq[String]] = null

    private def initDatastore: Unit = {
        val file = new File("./tickerSymbols-full.csv")

        var fileReader = new Scanner(new FileReader(file))

        var keyedTickers: Map[String, Seq[String]] = Map[String, Seq[String]]()

        while(fileReader.hasNext()) {
            val line = fileReader.nextLine
            val lineParts = line.split(",")
            val ticker: String = lineParts(0)
            val companyName: String = lineParts(1).replaceAll("\"", "").replaceAll("""(?m)\s+$""", "").replaceAll("[^A-Za-z0-9\\s]", "").trim()

            if(keyedTickers.contains(ticker) == false) {
                keyedTickers += ticker -> Seq[String]()
            }

            var aliases = keyedTickers(ticker)

            if(aliases.contains(ticker) == false) {
                aliases = aliases :+ ticker
            }

            if(aliases.contains(companyName) == false && companyName != null && companyName != "" ) {
                aliases = aliases :+ companyName
            }

            if(aliases.length > 0) {
                keyedTickers += (ticker.trim -> aliases)
            }
        }

        fileReader.close()

        dataStore = keyedTickers
    }

    def getStopwords: Seq[String] = {

        val allAliases = getTickerAliases
            .map { (mapping: (String, Seq[String])) =>
            mapping._2.filter { _ != mapping._1 }
        }
            .flatten
            .map { (alias: String) =>
            alias.split("\\s+")
        }
            .flatten
            .foldLeft(Map[String,Int]()) { (map: Map[String, Int], aliasPart: String) =>
            map + (aliasPart.toLowerCase -> (map.getOrElse(aliasPart.toLowerCase, 0) + 1))
        }
            .seq

        val max: Double = allAliases.maxBy( _._2 )._2.toDouble
        val threshold = 0.01

        allAliases.toList.filter((map: (String, Int)) => map._2.toDouble / max > threshold).sortBy(_._2).map(_._1)
    }

    def getSynonyms: Seq[(String, Seq[String])] = {
        val stopwords = getStopwords

        val aliases: Seq[(String, Seq[String])] = getTickerAliases.map({ (mapping: (String, Seq[String])) =>
            (mapping._1.toLowerCase, mapping._2.map {
                _.split("\\s+").map(_.toLowerCase).map(_.replaceAll("[^0-9a-zA-z]", "").trim).diff(stopwords).filter(_ != null).filter(_ != "").mkString(" ").trim
            }.filter(_ != null).filter(_ != ""))
        }).seq.toSeq

        aliases
    }

    def getTickers: Seq[String] = {
        getTickerAliases.keys.toSeq
    }

    def getTickerAliases: Map[String, Seq[String]] = {
        if(dataStore == null) {
            initDatastore
        }

        dataStore
    }

    def getTickerAliasesWithoutTickerName: Map[String, Seq[String]] = {
        getTickerAliases.map { (mapItem: (String, Seq[String])) =>
            (mapItem._1, mapItem._2.filter(_ != mapItem._1))
        }
    }

}
