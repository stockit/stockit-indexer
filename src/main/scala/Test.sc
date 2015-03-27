import com.stockit.module.service.SolrClientModule
import org.apache.solr.client.solrj.SolrClient
import scaldi.{Injectable}
import Injectable._









implicit val injector = new SolrClientModule

//('solrClient and 'cloudSolrClient).identifiers
//injector.getBinding(('solrClient and 'cloudSolrClient).identifiers).get.get

val solrClient = inject[SolrClient]('solrClient and 'cloudSolrClient)


