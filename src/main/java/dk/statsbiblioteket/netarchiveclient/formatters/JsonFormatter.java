package dk.statsbiblioteket.netarchiveclient.formatters;

import dk.statsbiblioteket.netarchiveclient.util.JSON;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.Iterator;

public class JsonFormatter extends NetarchiveFormatter{
    
    private final Logger log = LoggerFactory.getLogger(JsonFormatter.class);
    
    public JsonFormatter(PrintWriter out) {
        super(out);
    }
    
    @Override
    public int printResults(Iterator<SolrDocument> results) {
        int count = 0;
        while (results.hasNext()) {
            SolrDocument next = results.next();
            String json = JSON.toJson(next, false);
            logCount(log, count);
            out.println(json);
            count++;
        }
        return count;
    }
    
  
    
}
