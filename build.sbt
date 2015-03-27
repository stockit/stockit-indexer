name := "stockit-indexer"

version := "1.0"

scalaVersion := "2.11.2"

resolvers += "Restlet Repository" at "http://maven.restlet.org"

resolvers += "Apache Repository" at "https://repo.maven.apache.org/maven2"

libraryDependencies ++= Seq(
    "org.scaldi" % "scaldi_2.11" % "0.5.4",
    "mysql" % "mysql-connector-java" % "5.1.12",
    "com.typesafe" % "config" % "1.2.1",
    "org.scala-lang.modules" % "scala-xml_2.11" % "1.0.3",
    "com.typesafe.slick" %% "slick" % "2.1.0",
    "org.apache.httpcomponents" % "httpcore" % "4.3",
    "org.apache.httpcomponents" % "httpclient" % "4.3",
    "org.apache.solr" % "solr-solrj" % "5.0.0",
    "org.apache.solr" % "solr-core" % "5.0.0",
    "org.slf4j" % "slf4j-api" % "1.6.4",
    "org.slf4j" % "slf4j-simple" % "1.6.4",
    "mysql" % "mysql-connector-java" % "5.1.12",
    "com.typesafe" % "config" % "1.2.1"
)

resolvers += "Secured Central Repository" at "https://repo1.maven.org/maven2"

externalResolvers := Resolver.withDefaultResolvers(resolvers.value, mavenCentral = false)
