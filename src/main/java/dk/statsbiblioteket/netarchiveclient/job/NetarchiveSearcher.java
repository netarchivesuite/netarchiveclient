package dk.statsbiblioteket.netarchiveclient.job;

import dk.statsbiblioteket.netarchiveclient.util.AutochainingIterator;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.Callable;


public class NetarchiveSearcher implements Callable<Iterator<SolrDocument>> {
    
    private static final Logger log = LoggerFactory.getLogger(NetarchiveSearcher.class);
    
    //TODO should these be configurable?
    /**
     * How long should we sleep when solr reports error, before trying again
     */
    public static final int SLEEP_MILLIS_ON_ERROR = 60000;
    
    /**
     * How many times should we try solr before giving up. Note, EACH request can be tried this many times.
     */
    public static final int NUM_ATTEMPTS_ON_SOLR = 3;
    
    private final HttpSolrClient solrClient;
    private final Date day1;
    private final Date day2;
    private final int batchSize;
    private final String query;
    private final String collectionInstance;
    private final String fieldList;
    private final String startCursor;
    private final String sort; // Defaults to {@code crawl_date asc, id asc} is empty

    private long totalReceived = 0;

    public NetarchiveSearcher(HttpSolrClient solrClient,
                              Date day1, Date day2, int batchSize,
                              String query, String collectionInstance,
                              String fieldList, String startCursor, String sort) {
        
        this.solrClient = solrClient;
        this.day1 = day1;
        this.day2 = day2;
        this.batchSize = batchSize;
        this.query = query;
        this.collectionInstance = collectionInstance;
        this.fieldList = fieldList;
        this.startCursor = startCursor;
        this.sort = sort;
    }
    
    
    @Override
    public Iterator<SolrDocument> call() {
        
        Iterator<SolrDocument> results = getDocs(solrClient, day1, day2, batchSize, query,
                                                 collectionInstance, fieldList, startCursor, sort);
        return results;
    }
    
    protected Iterator<SolrDocument> getDocs(
            SolrClient masterServer, Date dateFrom, Date dateTo, int maxRows,
            String orgQuery, String collectionInstance, String fieldList, String startCursor, String sort) {
        
        return new AutochainingIterator<String, SolrDocument>(cursorMark -> {
            try {
                cursorMark = Optional.ofNullable(cursorMark).orElse(startCursor);
                return getDocsPart(masterServer, dateFrom, dateTo, maxRows, orgQuery, collectionInstance,
                                   fieldList, cursorMark, sort);
            } catch (IOException | SolrServerException e) {
                throw new RuntimeException(e);
            }
        });
        
        
    }
    
    private AutochainingIterator.IteratorOffset<String, Iterator<SolrDocument>> getDocsPart(
            SolrClient masterServer, Date dateFrom, Date dateTo, int maxRows,
            String orgQuery, String collectionInstance, String fieldList, String cursorMark, String sort)
            throws IOException, SolrServerException {
        long startTimeNS = System.nanoTime();
        // Base query
        SolrQuery solrQuery = new SolrQuery();
        
        //endpoint not included
        solrQuery.setQuery("crawl_date:[" + getSolrTimeStamp(dateFrom) + " TO " + getSolrTimeStamp(dateTo) + "}");
        if (sort.isEmpty()) {
            solrQuery.setSorts(Arrays.asList(SolrQuery.SortClause.create("crawl_date", ORDER.asc),
                                             SolrQuery.SortClause.create("id", ORDER.asc)));
        } else {
            solrQuery.set(CommonParams.SORT, sort);
        }

        if (collectionInstance != null) {
            // for some reason the collection parameter does not work.
            solrQuery.add("shards", "http://localhost:52300/solr/" + collectionInstance + "/");
        }
        solrQuery.add("facet", "false");
        
        solrQuery.add("fl", fieldList);
        log.info("Retrieving for cursorMark '{}'", cursorMark);
        solrQuery.add("cursorMark", cursorMark);
        
        solrQuery.setFilterQueries(orgQuery); //important. filter can be reused
        
        
        // Get hitCount
        solrQuery.setRows(maxRows);
        SolrRequest<QueryResponse> queryResponseSolrRequest = querySolrOptionalAuth(solrQuery);
        QueryResponse rsp = querySolr(masterServer, queryResponseSolrRequest, NUM_ATTEMPTS_ON_SOLR);

        SolrDocumentList results = rsp.getResults();
        totalReceived += results.size();
        long recordsPerSec = results.size()*1_000_000_000L / (System.nanoTime()-startTimeNS);

        // log.debug("res " + results.size() + ", nano= " + (System.nanoTime()-startTimeNS) + ", nano/1M " + (System.nanoTime()-startTimeNS)/1000000000L);
        long etaSec = results.getNumFound() == totalReceived ? 0 :
                (results.getNumFound()-totalReceived)/(recordsPerSec == 0 ? 1 : recordsPerSec);
        String eta = etaSec > 7200 ? (etaSec / 3600) + " hours" :
                etaSec > 120 ? (etaSec / 60) + " minutes" :
                        etaSec + " seconds";
        cursorMark = rsp.getNextCursorMark();
        log.info("Retrieved next batch (of {}) out of about {} hits(counting deduplication hits) . Current speed: {} records/sec, ETA: {}",
                results.size(), results.getNumFound(), recordsPerSec, eta);
        
        return AutochainingIterator.IteratorOffset.of(cursorMark, results.iterator());
        
    }
    
    private static QueryResponse querySolr(SolrClient masterServer,
                                           SolrRequest<QueryResponse> queryResponseSolrRequest,
                                           int remainingAttempts)
            throws SolrServerException, IOException {
        try {
            QueryResponse rsp = queryResponseSolrRequest.process(masterServer);
            return rsp;
        } catch (IOException | SolrException e) {
            if (remainingAttempts > 0) {
                log.warn("Encountered solr problem {}, so sleeping and trying again", e.getMessage(), e);
                try {
                    Thread.sleep(SLEEP_MILLIS_ON_ERROR);
                } catch (InterruptedException interruptedException) {
                    log.warn("Interrupted sleep, why you do this?", interruptedException);
                }
                return querySolr(masterServer, queryResponseSolrRequest, remainingAttempts - 1);
            } else {
                throw e;
            }
        }
    }
    
    
    private static String getSolrTimeStamp(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); //not thread safe, so create new
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date) + "Z";
    }
    
    
    private static SolrRequest<QueryResponse> querySolrOptionalAuth(SolrQuery solrQuery) {
        SolrRequest<QueryResponse> req = new QueryRequest(solrQuery);
        return req;
    }
    
}


