import com.stockit.service.indexer.Test
val testRange = 1 to 150
val maxBatchSize = 7
val numBatches = Math.ceil( testRange.size.toDouble / maxBatchSize ).toInt
var i = 2
var test = maxBatchSize * (i - 1) + (if(i > 0) 1 else 0 )
testRange.grouped(7).toList.map({ _(0)})
100 % 10
Test.TEST



