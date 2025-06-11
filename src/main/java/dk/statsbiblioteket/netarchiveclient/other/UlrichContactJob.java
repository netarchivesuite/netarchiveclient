package dk.statsbiblioteket.netarchiveclient.other;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.FacetField.Count;

public class UlrichContactJob {

    private static String SOLR_SERVER_URL="http://localhost:52300/solr/ns/";

    /* 
     *   Set up tunnel so the solrServerUrl: 
     *   ssh -L 127.0.0.1:52300:ariel.statsbiblioteket.dk:52300 develro@ariel.statsbiblioteket.dk  
     *
     *   This job extract domains from a list of files having a query on each line.
     * 
     */
    


    public static void main(String[] args) throws Exception{
        try {
             
            File folder = new File("/home/teg/ulrich");
            File[] listOfFiles = folder.listFiles();
            for (File file: listOfFiles) {
                
                BufferedReader br = new BufferedReader(new FileReader(file));
                
                String line;
                while ((line = br.readLine()) != null) { //Only 1 line in the files in this project
                    ArrayList<String> domains = getDomainFacets(line);
                    //System.out.println(file +":"+domains.size());
                    domains.stream().forEach(System.out::println);                  
                }
            
                br.close();
            }
                                
            
        }
        catch(Exception e) {
            e.printStackTrace();
        }

    }

    private static ArrayList<String> getDomainsFromFacet(  FacetField facetField ){
        ArrayList<String> domains = new ArrayList<String>();

        List<Count> domain = facetField.getValues();
        for (Count link: domain) {
            String dom =link.getName();
            domains.add(dom);
        }
        return domains;
    }

    public static  ArrayList<String> getDomainFacets(String query) throws Exception{
        
        SolrClient solrServer= new HttpSolrClient.Builder( SOLR_SERVER_URL).build();
        SolrQuery solrQuery = new SolrQuery();                                 
        solrQuery.setQuery(query);
        solrQuery.setFacet(true);                           
        solrQuery.addFilterQuery("status_code:200"); // already in query thouggh
        solrQuery.add("facet.field", "domain");
        solrQuery.add("facet.limit", "5000");
        solrQuery.set("fl", "id,domain"); //only extract these
        QueryResponse rsp = solrServer.query(solrQuery, METHOD.POST);
        FacetField facetField = rsp.getFacetField("domain");
        ArrayList<String> domains = getDomainsFromFacet(facetField);
        return domains;
        
    }
   

}
