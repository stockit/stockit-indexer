package com.stockit

import java.io.{FileWriter, BufferedWriter, File}

import com.stockit.service.ticker.TickerDatasource

/**
 * Created by dmcquill on 4/14/15.
 */
object SynonymsGenerator {

    def main(args: Array[String]): Unit = {
        val tickerDatasource = new TickerDatasource

        val file = new File("ticker_synonyms.txt")

        if( file.exists ) {
            file.delete
        }

        val fileWriter = new BufferedWriter(new FileWriter(file))

        tickerDatasource.getSynonyms.foreach { (mapping: (String, Seq[String])) =>
            var textMapping = s"${mapping._1} => ${ mapping._2.mkString(", ") }"
            fileWriter.write(textMapping)
            fileWriter.newLine
        }

        fileWriter.close()
    }

}
