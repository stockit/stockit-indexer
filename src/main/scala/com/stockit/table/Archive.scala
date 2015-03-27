package com.stockit.table

import scala.slick.driver.MySQLDriver.simple._

/**
 * Created by dmcquill on 3/22/15.
 */
case class Archive(id: Option[Int], url: String, siteId: Int)

class Archives(tag: Tag) extends Table[Archive](tag, "st_archive") {

    def id: Column[Int] = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def url: Column[String] = column[String]("url")
    def siteId: Column[Int] = column[Int]("site_id")

    def * = (id.?, url, siteId ) <> (Archive.tupled, Archive.unapply)

    def site = foreignKey("site_link_fk", siteId, TableQuery[Sites])(_.id)
}

