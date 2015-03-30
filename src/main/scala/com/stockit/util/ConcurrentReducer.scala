package com.stockit.util

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by dmcquill on 3/22/15.
 */
class ConcurrentReducer {

    var threads = 1

    def map[A, B](list: List[A], fn: ( A => B )): Seq[Future[Seq[B]]] = {
        val numPerClient = Math.ceil(list.size.asInstanceOf[Double] / threads).asInstanceOf[Int]
        ( 1 to threads ) map { thread =>
            Future {
                val clientStart = (thread - 1 ) * numPerClient
                val clientEnd = clientStart + numPerClient

                list slice(clientStart, clientEnd) map { item: A =>
                    fn(item)
                }
            }
        }
    }

}
