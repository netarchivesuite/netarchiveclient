package dk.statsbiblioteket.netarchiveclient.connector;

import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrJConnector {

    public static String SERVER_ARIEL_MASTER_NS_TUNNEL_LOCALHOST = "http://localhost:52300/solr/ns/";
    public static String SERVER_ARIEL_MASTER_NO_COLLECTION_TUNNEL_LOCALHOST = "http://localhost:52300/solr/";    
    public static String SERVER_ARIEL_MASTER_VIA_APACHE_MOUNT = "https://netarkiv-search.statsbiblioteket.dk/solr/ns/";
    
    public static String SERVER_ARIEL_MASTER = "http://ariel:52300/solr/";
    public static String SERVER_ROSALIND_MASTER = "http://rosalind:52300/solr/";
    public static String SERVER_LOCALHOST_8983 = "http://localhost:8983/solr";
        
    private HttpSolrClient solrClient;
    
    
    private static final Logger log = LoggerFactory.getLogger(SolrJConnector.class);

    public SolrJConnector(String serverUrl){        
        try{
            //Silent all the debugs log from HTTP Client (used by SolrJ)
            System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
            System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
            System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "ERROR"); 
            System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "ERROR"); 
            System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "ERROR");        
            java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.OFF); 
            java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.OFF);
            
            solrClient = new HttpSolrClient.Builder(serverUrl).build();             
            solrClient.setRequestWriter(new BinaryRequestWriter()); //To avoid http error code 413/414, due to monster URI. (and it is faster)              
        }
        catch(Exception e){
            System.out.println("Unable to connect to:"+ serverUrl);
            e.printStackTrace();
            log.error("Unable to connect to to:"+serverUrl,e);            
        }
    }

    public  HttpSolrClient getSolrServer() {
        return solrClient;
    }

}
