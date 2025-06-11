package dk.statsbiblioteket.netarchiveclient.formatters;

import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class CsvFormatter extends NetarchiveFormatter {
    private static final Logger log = LoggerFactory.getLogger(CsvFormatter.class);
    
    private static final String FIELD_SEPARATOR = ",";
    private static final String MULTIVALUE_SEPARATOR = "\t";
    private final String fieldList;
    
    public CsvFormatter(PrintWriter out, String fieldList) {
        super(out);
        this.fieldList = fieldList;
    }
    
    @Override
    public int printResults(Iterator<SolrDocument> results) {
        String fieldList = this.fieldList;
        boolean fieldListPrinted = false;
        int count = 0;
        while (results.hasNext()) {
            SolrDocument next = results.next();
            fieldList = getFieldList(fieldList, next);
            fieldListPrinted = printFieldList(fieldList, fieldListPrinted);
            logCount(log, count);
            String csv = toCSV(next, fieldList);
            out.println(csv);
            count++;
        }
        return count;
    }
    
    private boolean printFieldList(String fieldList, boolean fieldListPrinted) {
        if (!fieldListPrinted) {
            out.println(Arrays.stream(fieldList.split(FIELD_SEPARATOR))
                              .map(field -> escapeQuotes(field))
                              .collect(
                                      Collectors.joining(FIELD_SEPARATOR)));
            fieldListPrinted = true;
        }
        return fieldListPrinted;
    }
    
    private String getFieldList(String fieldList, SolrDocument next) {
        if (fieldList == null || fieldList.isEmpty() || fieldList.equals("*")) {
            fieldList = next.entrySet().stream()
                            .map(entry -> entry.getKey())
                            .sorted()
                            .collect(Collectors.joining(FIELD_SEPARATOR));
        }
        return fieldList;
    }
    
    
    protected static String toCSV(SolrDocument doc, String fieldList) {
        StringBuilder result = new StringBuilder();
        
        
        for (String field : fieldList.split(FIELD_SEPARATOR)) {
            Object field_value = doc.getFieldValue(field.trim());
            if (field_value != null) { //if null, just output a tab
                
                if (field_value instanceof List) { //if multivalued
                    field_value = String.join(MULTIVALUE_SEPARATOR, (List<String>) field_value);
                }
                String escaped = escapeQuotes(field_value.toString());
                result.append(escaped);
            } else {
                result.append(escapeQuotes(""));
            }
            result.append(FIELD_SEPARATOR);
        }
        
        
        //Remove last tab
        result.delete(result.length() - FIELD_SEPARATOR.length(), result.length());
        
        return result.toString();
        
    }
    
    
    //Sets " around the expression and replaces " with "". (CSV format)
    private static String escapeQuotes(String text) {
        if (text == null) {
            return "";
        }
        return "\"" + text.replaceAll("\"", "\"\"") + "\"";
    }
    
    
}
