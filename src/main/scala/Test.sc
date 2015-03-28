import java.io.{FileReader, File}
import java.util.Scanner

var file = new File("/Users/dmcquill/dev/Scala/stockit-indexer/tickerSymbols.csv")

var fileReader = new Scanner(new FileReader(file))

var keyedTickers: Map[String, Seq[String]] = Map[String, Seq[String]]()

keyedTickers.contains("test")
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

keyedTickers
