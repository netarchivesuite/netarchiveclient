package dk.statsbiblioteket.netarchiveclient.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

public class NetarchiveUtil {
    
    
    @SuppressWarnings("unchecked")
    public static List<NetarchiveDoc> solrDoc2NetarchiveDoc( SolrDocumentList results){

        List<NetarchiveDoc> docs = new ArrayList<NetarchiveDoc>();

        for ( SolrDocument current : results){
            NetarchiveDoc doc = new NetarchiveDoc();
            doc.setWaybackDate(Long.parseLong((String) current.getFieldValue("wayback_date")));
            doc.setYear(Integer.parseInt((String) current.getFieldValue("crawl_year")));
            doc.setCrawlDate((Date)  current.getFieldValue("crawl_date"));             
            ArrayList<String> titles = (ArrayList<String>)( current.getFieldValue("title") );
            if (titles != null && titles.size() >= 1){
                doc.setTitle(titles.get(0));                
            }            
            else {
                if (titles != null) {
                    titles.size();
                }
            }
    
            doc.setId((String)  current.getFieldValue("id"));       

            doc.setHost((String)  current.getFieldValue("host"));
            doc.setDomain((String)  current.getFieldValue("domain"));
            doc.setPublicSuffix((String)  current.getFieldValue("public_suffix"));
            doc.setContentType((String)  current.getFieldValue("content_type_served")); 

            ArrayList<String> servers= (ArrayList<String>)  current.getFieldValue("server");
            if (servers != null && servers.size() >= 1){
                doc.setServer(servers.get(0));
            }    

            ArrayList<String> linksHosts = (ArrayList<String>)  current.getFieldValue("links_hosts");
            doc.setLinksHost(linksHosts);

            ArrayList<String> contentTexts = (ArrayList<String>)  current.getFieldValue("content");

            if (contentTexts != null && contentTexts.size()>= 1){
                doc.setContentText( contentTexts.get(0));                
            }    
            else {
                if (contentTexts != null) {
                    contentTexts.size();
                }
            }

            Object lengthObj = current.getFieldValue("content_text_length");
            if (lengthObj != null){
                doc.setContentTextLength((Integer) current.getFieldValue("content_text_length"));            
            }

            doc.setContentLanguage((String)  current.getFieldValue("content_language"));            
            doc.setElementsUsed((ArrayList<String>)  current.getFieldValue("elements_used"));
            doc.setLinksDomains((ArrayList<String>)  current.getFieldValue("links_domains"));
            doc.setLinksPublicSuffixes((ArrayList<String>)  current.getFieldValue("links_public_suffixes"));

            docs.add(doc);
        }
        return docs;
    }

}
