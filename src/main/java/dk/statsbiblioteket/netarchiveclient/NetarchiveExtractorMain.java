package dk.statsbiblioteket.netarchiveclient;

import dk.statsbiblioteket.netarchiveclient.connector.SolrJConnector;
import dk.statsbiblioteket.netarchiveclient.formatters.JsonFormatter;
import dk.statsbiblioteket.netarchiveclient.formatters.NetarchiveFormatter;
import dk.statsbiblioteket.netarchiveclient.formatters.CsvFormatter;
import dk.statsbiblioteket.netarchiveclient.job.NetarchiveSearcher;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.concurrent.Callable;


/* Extract tool for custom field extraction from the netarchive solr-index. The results will be stream so there is no
 * limit for the size of the extract. The buffer size need to be defined.
 * It can query the whole collection at once by setting shard  to 'ns'. This can be used if the size of the
 * extract is less than 10M rows. For large extracts it is required to extract the text from each shard seperatly, and
 * this can also be concurrent for each shards without performance issues.
 *
 * If the text field is extract a general rule is 4GB heap space for a buffer size of 5000. If the text field is not
 * extracted a buffersize of 10000 is acceptable for 4GB heap space. Performance improves almost linear with
 * buffersize so allocating as much heapspace as possible and higher buffersize will improve performance.
 *
 * This tool require a local SSH tunnel to the Solr-index on localhost:52300.
 * etc. the solr-index can be contacted on: http://localhost:52300/solr/#/ns/query
 *
 * Example command with a query to all shards (ns) and almost every field defined. 4GB heap space and buffersize of 5000.
 * java -Xmx8G -cp netarchiveclient-3.1.0-jar-with-dependencies.jar dk.statsbiblioteket.netarchiveclient.job.NetarchiveExtactor "http://localhost:52300/solr/" ns 19900101 20200101 1000 "title, subject, description, comments, author, url, url_norm, content_type, last_modified, last_modified_year, links, content, content_text_length, url_type, content_length, content_encoding, content_language, content_type_norm, wayback_date, crawl_date, crawl_dates, crawl_year, crawl_years, crawl_year_month, crawl_year_month_day, host, domain, public_suffix, links_hosts, links_domains, links_public_suffixes, hash, publication_date, publication_year, content_type_version, content_type_full, content_type_tika, content_type_droid, content_type_served, content_type_ext, server, generator, ssdeep_hash_bs_3, ssdeep_hash_bs_6"  "text:\"thomas egense\""
 * The dataformat is a CSV file, where multivalue field uses TAB as seperator.
 * 
 * Documentation on sbprojects: https://sbprojects.statsbiblioteket.dk/display/YAK/netarchiveclient
 * 
 * if {@code --deduplicateField} is specified, export order will be {@code deduplicateField asc},
 * else it will be {@code crawl_date asc}. Can only use deduplication on sortable fields (docValues). Does not work on multivalue fields, there may be deduplications.
 */

//https://picocli.info/#_introduction
@CommandLine.Command()
public class NetarchiveExtractorMain implements Callable<Integer> {
    
    
    private static final Logger log = LoggerFactory.getLogger(NetarchiveExtractorMain.class);
    
    //protected static String fieldList = null;
    //"title, subject, description, comments, author, url, url_norm, content_type, last_modified, last_modified_year, links, content, content_text_length, url_type, content_length, content_encoding, content_language, content_type_norm, wayback_date, crawl_date, crawl_dates, crawl_year, crawl_years, crawl_year_month, crawl_year_month_day, host, domain, public_suffix, links_hosts, links_domains, links_public_suffixes, hash, publication_date, publication_year, content_type_version, content_type_full, content_type_tika, content_type_droid, content_type_served, content_type_ext, server, generator, ssdeep_hash_bs_3, ssdeep_hash_bs_6";
    
    
    @CommandLine.Parameters(index = "0", defaultValue = "http://localhost:52300/solr/#/")
    private String solrMasterUrl;
    
    @CommandLine.Parameters(index = "1", defaultValue = "ns")
    private String shard;
    
    
    @CommandLine.Parameters(index = "2")
    private Date yyyymmdd1;
    
    
    @CommandLine.Parameters(index = "3")
    private Date yyyymmdd2;
    
    
    @CommandLine.Parameters(index = "4")
    private Integer batchSize;
    
    
    @CommandLine.Parameters(index = "5")
    private String fieldList;
    
    
    @CommandLine.Parameters(index = "6")
    private String query;
    
    @CommandLine.Option(names = {"-f", "--format"}, required = false, type = OutputFormatEnum.class,
                        description = "Valid values: ${COMPLETION-CANDIDATES}", defaultValue = "CSV")
    private OutputFormatEnum format;
    
    @CommandLine.Option(names = {"--cursorMark"}, required = false, type = String.class, defaultValue = "*")
    private String cursorMark;
    
    @CommandLine.Option(names = {"-d", "--deduplicateField"}, required = false, type = String.class,
                        description = "Valid values: ${COMPLETION-CANDIDATES}", defaultValue = "")
    private String deduplicateField;


    public enum OutputFormatEnum {
        CSV, JSON
    }
    
    @Override
    public Integer call() throws Exception {
        //if (args.length != 7) {
        //    System.err.println(
        //            "Syntax is solrUrl shard(ns 'all' or single shard ns1 - ns100 ) yyyymmdd1 yyyymmdd2 batchsize(1000 to 100000) fieldList(comma seperatored) query");
        //    System.err.println(
        //            "Examples for solrUrl: \"http://localhost:52300/solr/\"  if you have local tunnel to ariel (developer only)");
        //    System.err.println(
        //            "Examples for solrUrl: \"https://netarkiv-search.statsbiblioteket.dk/solr/\" for netarkut user miaplacidus");
        //    System.exit(1);
        //}
        try (PrintWriter consoleOut = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));) {

            // For backwards compatibility we accept Solr URLs that does not specify the Solr core to call
            if (solrMasterUrl.endsWith("solr/")) {
                solrMasterUrl = solrMasterUrl + "ns/";
            } else if (solrMasterUrl.endsWith("solr")) {
                solrMasterUrl = solrMasterUrl + "/ns/";
            }

            if (!shard.isEmpty() && !shard.startsWith("ns")) {
                throw new RuntimeException("shard can be empty (all results from the given Solr URL), ns (all shards)" +
                        " or ns1, ns2... etc. It was '"+ shard + "'");
            }
            String collectionInstance = null; //will be overwritten if only a single shard id queried
            
            
            //Only query a single shard? See if shard is followed by a number.
            if (shard.length() > 2) {
                try {
                    Integer.parseInt(shard.substring(2)); //everything after ns
                    collectionInstance = shard;
                } catch (Exception e) {
                    throw new RuntimeException(
                            "syntax for shard not ns or ns followed by number. 'ns'(means all) or ns10 (shard 10)", e);
                }
            }
            
            
            if (batchSize < 1000 || batchSize > 1000000) {
                throw new RuntimeException(
                        "Batch size should be between 1000 and 1000000. For text extaction a value of 5000 requires at least 4GB heap space");
            }
            
            
            fieldList = fieldList.replaceAll("\\s", ""); //solr does not allow spaces in list
            String originalFieldList = fieldList;
            if (!deduplicateField.isEmpty() && !fieldList.contains(deduplicateField)) {
                fieldList = fieldList.isEmpty() ? deduplicateField : fieldList + "," + deduplicateField;
            }
            if (fieldList.isEmpty()) {
                throw new RuntimeException("You must provide a field list. If you want all fields, use '*'");
            }
            log.info("solr request server: '{}'", solrMasterUrl);
            log.info("shard: '{}'", shard);
            log.info("dateStart: " + yyyymmdd1);
            log.info("dateEnd: " + yyyymmdd2);
            
            log.info("fields: '{}'", fieldList);
            log.info("deduplicateField: '{}'", deduplicateField);
            log.info("query: '{}'", query);
            if (collectionInstance == null) {
                log.info("Query will be sent to '{}' without limiting to sub-collections", solrMasterUrl);
            } else {
                log.info("Query will only be sent to collection: '{}'", collectionInstance);
            }
            
            SolrJConnector masterConnector = new SolrJConnector(solrMasterUrl);
            HttpSolrClient solrClient = masterConnector.getSolrServer();
            
            String sort = deduplicateField.isEmpty() ? "" : deduplicateField + " asc, crawl_date asc, id asc";
            Iterator<SolrDocument> results = new NetarchiveSearcher(
                    solrClient, (yyyymmdd1), (yyyymmdd2), batchSize, query,
                    collectionInstance, fieldList, cursorMark, sort).call();
            if (!deduplicateField.isEmpty()) {
                results = new DeduplicatingInterator(results, deduplicateField);
            }

            NetarchiveFormatter output;
            switch (format) {
                default:
                case CSV:
                    output = new CsvFormatter(consoleOut, originalFieldList);
                    break;
                case JSON:
                    // TODO: Make the JsonFormatter use originalFieldList
                    output = new JsonFormatter(consoleOut);
                    break;
            }
            int count = output.printResults(results);
            
            
            log.info("Finished job with {} hits for parameters: " +
                            "shardid: '{}', start: {}, end: {} query: '{}' deduplicateField: '{}'",
                    count, shard, yyyymmdd1, yyyymmdd2, query, deduplicateField);
            
            return 0;
        }
    }

    private static class DeduplicatingInterator implements Iterator<SolrDocument> {
        private final Iterator<SolrDocument> inner;
        private final String deduplicateField;
        private SolrDocument next = null;
        private Object lastValue = null;

        public DeduplicatingInterator(Iterator<SolrDocument> inner, String deduplicateField) {
            this.inner = inner;
            this.deduplicateField = deduplicateField;
            
        }

        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            }
            while (inner.hasNext()) {
                SolrDocument candidate = inner.next();
                if (lastValue != null && lastValue.equals(candidate.getFieldValue(deduplicateField))) {
                    continue; // Skip duplicate
                }
                next = candidate;
                lastValue = next.getFieldValue(deduplicateField);
                break;
            }
            return next != null;
        }

        @Override
        public SolrDocument next() {
            if (!hasNext()) {
                throw new IllegalStateException("No more SolrDocuments");
            }
            SolrDocument deliver = next;
            next = null;
            return deliver;
        }

    }

    public static void main(String... args) {
        CommandLine app = new CommandLine(new NetarchiveExtractorMain());
        app.registerConverter(Date.class, s -> parseYYYYMMDD(s));
        int exitCode = app.execute(args);
        
        
        System.exit(exitCode);
    }
    
    
    protected static Date parseYYYYMMDD(String yyyymmdd) throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date d = df.parse(yyyymmdd);
        return d;
    }
    
}


