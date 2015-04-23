package com.stockit

import java.io.{FileWriter, BufferedWriter, File}

import com.stockit.service.ticker.TickerDatasource

/**
 * Created by dmcquill on 4/15/15.
 */
object StopwordsGenerator {

    def main(args: Array[String]): Unit = {
        val tickerDatasource = new TickerDatasource

        val file = new File("ticker_stopwords.txt")

        if( file.exists ) {
            file.delete
        }

        val fileWriter = new BufferedWriter(new FileWriter(file))

        tickerDatasource.getStopwords.foreach { (stopword: String) =>
            fileWriter.write(stopword)
            fileWriter.newLine()
        }

        fileWriter.close()
    }

}
