package com.stockit.table

import scala.slick.driver.MySQLDriver.simple._

/**
 * Created by dmcquill on 3/19/15.
 */
case class Link(id: Option[Int], url: String)

class Links(tag: Tag) extends Table[Link](tag, "st_link") {

    def id: Column[Int] = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def url: Column[String] = column[String]("url")

    def * = (id.?, url) <> (Link.tupled, Link.unapply)
}

