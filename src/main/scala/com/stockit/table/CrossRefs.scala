package com.stockit.table

import scala.slick.driver.MySQLDriver.simple._

class LinkArchive(tag: Tag) extends Table[(Int, Int)](tag, "st_link_archive_xref") {
    def linkId = column[Int]("link_id")
    def archiveId = column[Int]("archive_id")

    def * = (linkId, archiveId)

    def link = foreignKey("link_archive_link_fk", linkId, TableQuery[Links])(_.id)
    def archive = foreignKey("link_archive_archive_fk", archiveId, TableQuery[Archives])(_.id)
}

class LinkArticle(tag: Tag) extends Table[(Int, Int)](tag, "st_link_article_xref") {
    def linkId = column[Int]("link_id")
    def articleId = column[Int]("article_id")

    def * = (linkId, articleId)

    def link = foreignKey("link_article_link_fk", linkId, TableQuery[Links])(_.id)
    def article = foreignKey("link_article_article_fk", articleId, TableQuery[Articles])(_.id)
}