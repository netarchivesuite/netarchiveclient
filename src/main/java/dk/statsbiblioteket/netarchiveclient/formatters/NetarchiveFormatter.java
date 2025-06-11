package dk.statsbiblioteket.netarchiveclient.formatters;

import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.Iterator;

public abstract class NetarchiveFormatter {
    
    private final Logger log = LoggerFactory.getLogger(NetarchiveFormatter.class);
    
    protected final PrintWriter out;
    
    public NetarchiveFormatter(PrintWriter out) {
        this.out = out;
    }
    
    public abstract int printResults(Iterator<SolrDocument> iterator);
    
    protected void logCount(Logger log, int count) {
        this.log.trace("Outputting record nr {}", count);
        if (count % 1000 == 0){
            this.log.info("Outputting record nr {}", count);
        }
    }
    
}
