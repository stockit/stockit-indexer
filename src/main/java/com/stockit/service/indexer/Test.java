//package com.stockit.service.indexer;
//
//import org.apache.solr.client.solrj.SolrServerException;
//import org.apache.solr.client.solrj.impl.CloudSolrServer;
//import org.apache.solr.common.SolrException;
//import org.apache.solr.common.SolrInputDocument;
//
//import java.io.IOException;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//
///**
// * Created by dmcquill on 3/26/15.
// */
//public class Test {
//
//    private String allStoresAndProductsQuery;
//
//    private String allProductsForStoreQuery;
//
//    private String productForAllStoresQuery;
//
//    private String productForStoreQuery;
//
//    private JdbcTemplate jdbcTemplate;
//
//    private CategoryTreeLoader categoryTreeLoader;
//
//    private PriceTierLoader priceTierLoader;
//
//    private ProductGroupLoader productGroupLoader;
//
//    private ProductRatingLoader productReviewLoader;
//
//    private EcomStoreInformationLoader ecomStoreInfoLoader;
//
//    private PeapodProductToCategoryLoaderDelegate peapodProdCatLoader;
//
//    private CloudSolrServer solrServer;
//
//    private AliasService aliasService;
//
//    private SingleRootCatService singleRootCatService;
//
//    public void indexAll() {
//
//        logger.info("Clearing the product index.");
//        this.removeAll();
//        logger.error("Cleared the product index.");
//
//        this.getCategoryTreeLoader().reInit();
//        this.getEcomStoreInfoLoader().reInit();
//        this.getProductRatingLoader().reInit();
//
//        logger.info("Reindexing all products for all stores");
//        ProductRowHandlerCallback callback = new ProductRowHandlerCallback(
//                this.getPeapodProdCatLoader().getProductCategoryMap());
//
//        this.getJdbcTemplate().query(this.getAllStoresAndProductsQuery(),
//                new Object[]{}, callback);
//
//        callback.indexDocuments(true);
//        logger.info("Reindexed all products for all stores");
//
//        try {
//            this.getSolrServer().commit();
//            this.getSolrServer().optimize();
//            aliasService.toggle(CoreGroup.PRODUCTS);
//        } catch (SolrServerException ex) {
//            this.logger.warn("Error attempting to optimize product index.",
//                    ex);
//        } catch (IOException ex) {
//            this.logger.warn("Error attempting to optimize product index.",
//                    ex);
//        }
//    }
//
//    public void indexDocuments(boolean indexRemainder) {
//
//        if (this.documents.size() == 0) { return; }
//
//        if (this.documents.size() > 999 ||
//                (indexRemainder && this.documents.size() > 0)) {
//
//            try {
//                ProductIndexer.this.getSolrServer().add(this.documents);
//
//                // With Solr 4 CloudSolrServer setup - do not commit here, let solrconfig.xml
//                //  configurations handle committing data in batches.
//                //ProductIndexer.this.getSolrServer().commit();
//            } catch (SolrServerException ex) {
//                ProductIndexer.this.logger.warn(
//                        "Error attempting to add items to the solr index", ex);
//                throw new IndexerException("Error attempting to add items to the solr index", ex);
//            } catch (IOException ex) {
//                ProductIndexer.this.logger.warn(
//                        "Error attempting to add items to the solr index", ex);
//                throw new IndexerException(
//                        "Error attempting to add items to the solr index", ex);
//            } catch (SolrException ex) {
//                ProductIndexer.this.logger.warn(
//                        "Error attempting to add items to the solr index", ex);
//                throw new IndexerException(
//                        "Error attempting to add items to the solr index", ex);
//            }
//            this.documents.clear();
//        }
//    }
//
//    private SolrInputDocument createProductDocument(ResultSet rs)
//            throws SQLException {
//
//        int nId = rs.getInt("id");
//        int nStoreId = rs.getInt("store_id");
//
//        String additionalSearchText = StringUtils.trimToNull(rs.getString("additionalSearchText"));
//        String sDescription = StringUtils.trimToEmpty(rs.getString("description"));
//        int nBrandId = rs.getInt("brand");
//        String sBrandTx = StringUtils.trimToEmpty(rs.getString("brand_tx"));
//        String sUpc = rs.getString("upc");
//        String activeCd = StringUtils.trimToEmpty(rs.getString("actv_cd"));
//        String consumerCategory = StringUtils.trimToEmpty(rs.getString("consumerCat"));
//        int consumerCategoryId = rs.getInt("consumerCatId");
//        String size = StringUtils.trimToEmpty(rs.getString("size"));
//
//        String sCatId = this.podIdToCategoryMap.get(rs.getString("id"));
//
//        int nCatId = sCatId == null ? 0 : Integer.parseInt(sCatId);
//        int quantitySold = rs.getInt("purch_qty");
//
//        SolrInputDocument doc = new SolrInputDocument();
//        doc.addField("id", nStoreId + "-" + nId);
//        doc.addField("product_id", nId);
//        doc.addField("store_id", nStoreId);
//        doc.addField("description", sDescription);
//        doc.addField("brand_id", nBrandId);
//        doc.addField("brand_tx", nBrandId  + ":" + sBrandTx);
//        doc.addField("brand_search_tx", sBrandTx);
//        doc.addField("qtySold", quantitySold);
//        doc.addField("activeCd", activeCd);
//        doc.addField("consumerCategory", consumerCategory);
//        doc.addField("consumerCategoryId", consumerCategory + ":" + consumerCategoryId);
//        doc.addField("consumCatId", consumerCategoryId);
//        doc.addField("size", size);
//        doc.addField("prod_cat_id", nCatId);
//
//        if (additionalSearchText != null) {
//            doc.addField("additionalSearchText", additionalSearchText);
//        }
//
//        this.addUpcs(doc, sUpc);
//        this.addProductCategories(doc, nCatId);
//        this.addItemSearchPhrases(doc, nCatId);
//
//        EcomStoreInfo storeInfo = ProductIndexer.this.getEcomStoreInfoLoader().getStore(nStoreId);
//
//        this.addReviewFlags(doc, storeInfo, nCatId, nId);
//        this.addCategoryPaths(doc, nCatId);
//        singleRootCatService.process(doc, consumerCategoryId);
//        this.addNutritionalIndicators(doc, rs);
//        this.addPriceZones(nId, nStoreId, doc);
//        this.addNewArrivalFlag(rs, doc);
//        this.addProductGroups(nId, nStoreId, doc);
//
//        return doc;
//    }
//
//
//}
