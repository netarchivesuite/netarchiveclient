package dk.statsbiblioteket.netarchiveclient.other;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.TreeSet;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;

public class ExtractLinksForDomains {

    

    /*
     * The class takes a file with domain names as input
     * For each domain name it will extract all in and out-going links and save in graph/Gehpi format for each domain.
     * The extract is done for each year in the interval define
     * 
     * 
     * Set up tunnel so the solrServerUrl is found.
     * 
     * Variables:
     *   int startYear=2004;
     *   int endYear=2021;
     *     String solrServerUrl="http://localhost:52300/solr/ns/";
     *     String fileName = "/home/teg/Desktop/domain_extract.txt"; The list of domain names. one pr line
     *     String outFolder="/home/teg/domain_links/"; Where to save. Year folders must be created in this folder first
     * 
     */
    
      
    //query 1, outgoing
    //domain:"adgency.dk" AND crawl_year:2021
    //facet=true&facet.field=links_domains&facet.limit=1000

    //query2, ingoing      
    //links_domains:adgency.dk AND crawl_year:2021 
    //facet=true&facet.field=domains&facet.limit=1000

    public static void main(String[] args) throws Exception{
        try {

            String solrServerUrl="http://localhost:52300/solr/ns/"; 

            SolrClient solrServer= new HttpSolrClient.Builder(solrServerUrl).build();
            String fileName = "/home/teg/Desktop/domain_extract.txt";
            String outFolder="/home/teg/domain_links/";

            BufferedReader br = new BufferedReader(new FileReader(fileName));
            ArrayList<String> domainList= new ArrayList<String> (); 
            String line;
            while ((line = br.readLine()) != null) {
                domainList.add(line);
            }        
            br.close();


            int startYear=2004;
            int endYear=2021;

            for (int year =startYear;year<=endYear;year++) {
                for (String domain : domainList) {
                    System.out.println("year:"+ year+" domain:"+domain);

                    //First outgoing from domain
                    SolrQuery solrQuery = new SolrQuery();                
                    String query = "domain:"+domain+ " AND crawl_year:"+year;                 
                    solrQuery.setQuery(query);
                    solrQuery.setFacet(true);                           
                    solrQuery.addFilterQuery("status_code:200");
                    solrQuery.add("facet.field", "links_domains");
                    solrQuery.add("facet.limit", "5000");

                    QueryResponse rsp = solrServer.query(solrQuery, METHOD.POST);
                    FacetField facetField = rsp.getFacetField("links_domains");
                    ArrayList<String> domainsOutList = getDomainsFromFacet(facetField);
                    domainsOutList.remove(domain); //Do not include itself
                    StringBuilder domainOutBuilder= new StringBuilder();          
                    if (domainsOutList.size() > 1) { //only write if there is a link
                      String result = String.join(",", domainsOutList);
                      domainOutBuilder.append(domain+",");
                      domainOutBuilder.append(result);
                      domainOutBuilder.append("\n");
                    }
                    
                    //Then ingoing to domain
                    solrQuery = new SolrQuery();
                    query = "links_domains:"+domain+" AND crawl_year:"+year;                 
                    solrQuery.setQuery(query);
                    solrQuery.addFilterQuery("status_code:200");
                    solrQuery.setFacet(true);                                
                    solrQuery.add("facet.field", "domain");
                    solrQuery.add("facet.limit", "5000");
                    rsp = solrServer.query(solrQuery, METHOD.POST);
                    facetField = rsp.getFacetField("domain");
                    ArrayList<String> domainsInList = getDomainsFromFacet(facetField);
                    
                    domainsInList.remove(domain); //Do not include itself
                    StringBuilder domainInBuilder= new StringBuilder();
                    for (String domainIn: domainsInList) {
                        domainInBuilder.append(domainIn+","+domain);
                        domainInBuilder.append("\n");
                    }

                    //links_domains:adgency.dk AND crawl_year:2021 
                    //facet=true&facet.field=links_domains&facet.limit=1000

                    String allLinks=domainOutBuilder.toString()+domainInBuilder.toString();
                    
                    //Write the file only if it contains more than domain name
                    if (allLinks.length() > domain.length()+3 ){

                        BufferedWriter writer = new BufferedWriter(new FileWriter(outFolder+"/"+year+"/"+domain+".csv"));
                        writer.write(allLinks);                
                        writer.close();
                        System.out.println("Linkgraph saved for domain:"+domain);
                    }

                }
            }

        }
        catch(Exception e) {
            e.printStackTrace();
        }

    }

    private static ArrayList<String> getDomainsFromFacet(  FacetField facetField ){
        ArrayList<String> domains = new ArrayList<String>();

        List<Count> links_domain_facet_list = facetField.getValues();
        for (Count link: links_domain_facet_list) {
            String dom =link.getName();
            domains.add(dom);
        }
        return domains;
    }

    
   /*  Extract all domains in the corpus
 * 
 *       TreeSet<String> uniqueDomains = new TreeSet<String>(); 
        Scanner scanner = new Scanner(new File("/home/teg/domain_links/all_2004_to_2021.csv"));
        while (scanner.hasNextLine()) {
         String line = scanner.nextLine();
         String[] tokens = line.split(",");
         for (String token : tokens) {
           uniqueDomains.add(token.trim());      
         }                  
        }
        scanner.close();
        
    
        
        StringBuilder allLinksBuilder = new StringBuilder();
        for (String domain: uniqueDomains) {
            allLinksBuilder.append(domain +"\n");            
        }
        
        
   
        BufferedWriter writer = new BufferedWriter(new FileWriter("/home/teg/domain_links/unique_domains.txt"));
        writer.write(allLinksBuilder.toString());                
        writer.close();
 */


}
