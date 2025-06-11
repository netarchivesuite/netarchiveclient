package dk.statsbiblioteket.netarchiveclient.job;

import dk.statsbiblioteket.netarchiveclient.NetarchiveExtractorMain;

public class NetarchiveExtactorInvoker {
    
    public static void main(String... args) throws Exception {
        
        NetarchiveExtractorMain.main("http://localhost:52300/solr/",
                                     "ns",
                                     "20050101",
                                     "20201212",
                                     "1000",
                                     "title, subject, description, comments, author, url, url_norm, content_type, last_modified, last_modified_year, links, content, content_text_length, url_type, content_length, content_encoding, content_language, content_type_norm, wayback_date, crawl_date, crawl_dates, crawl_year, crawl_years, crawl_year_month, crawl_year_month_day, host, domain, public_suffix, links_hosts, links_domains, links_public_suffixes, hash, publication_date, publication_year, content_type_version, content_type_full, content_type_tika, content_type_droid, content_type_served, content_type_ext, server, generator, ssdeep_hash_bs_3, ssdeep_hash_bs_6",
                                     "text:\"Bruger\"",
                                     "-f",
                                     "CSV");
    }
}