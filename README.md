# NetarchiveClient


## NetarchiveClient v.3.1.1 has been released and can be downloaded here:
https://github.com/netarchivesuite/netarchiveclient/releases/tag/v.3.1.1

## About NetarchiveClient

Netarchiveclient is used to extract very large scale texts fields from a Solr index created by the [Warc-indexer](https://github.com/ukwa/webarchive-discovery). With the [SolrWayback](https://github.com/netarchivesuite/solrwayback) GUI it is possible to extract millions of records.
But if the extraction is for 100M+ records and from a Solr Cloud installation with multiple shards/collections and NetarchiveClient can also
distribute the extraction to the indiviual shards in parallel.

As an example 30B+ documents was extracted from 170 Solr servers in 4 months querying 30 Solr servers in parallel at a time.


Building:
The jar file will be under the /target folder


Usage:

Extract tool for custom field extraction from the netarchive solr-index. The results will be streamed so there is no  limit for the size of the exctact. The buffer size need to be defined.
It can query the whole collection at once by setting shard to 'ns'. This can be used if the size of the extract is less than 10M rows. For large extracts it is required to extract the text from each shard seperatly, and
this can also be concurrent for each shard without performance issues.

Program arguments:

The program must have the following 7 arguments as program arguments:

1) Solr collection url.  This can be "http://localhost:52300/solr/" or  "https://netarkiv-search.statsbiblioteket.dk/solr/"

2) shard. The value can be 'ns' which means the whole collection, querying all shards. Or a single shard as ns1 or ns99 etc.

3) date start in the format yyyymmdd  Example: 19900101 which will return all material since first harvest is from 1998

4) date end in the format yyyymmdd Example:  20990101 which will return all.,

5) batchsize, value from 1K to 10K.  If content field is extracted batchsize should be set to only 1K since the amount of IO can give EOM on the solr-nodes.

6) The field list to be extracted comma seperated. Example: title, subject, description, comments, author, url

7) The query. The fields must be defined implicit. Example text:"query text.." og domain:"abc.dk"). Quotes " in query can be escaped with \"

8) Format (optional):   -f=JSON   Possible options are JSON or CVS. CVS is default if not specified

9) Deduplication (optional):   -d=url   . Will only show 1 result for each value of the field used for deduplication. Limitation are fields must be sortable. (url, hash etc.).  Also for multivalue fields deduplication will not work.

Example :

Example command with a query to all shards (ns) and almost every field defined. 4GB heap space and buffersize of 1000:
localhost:53200 has been ssh tunnel forwarded to the solr master server.

```
java -Xmx8G \
    -cp netarchiveclient-3.0.0-jar-with-dependencies.jar \
    dk.statsbiblioteket.netarchiveclient.NetarchiveExtractorMain \
    "http://localhost:52300/solr/"\
    ns 19900101 20200101 1000 \
    "title, subject, description, comments, author, url, url_norm, content_type, last_modified, last_modified_year, links, content, content_text_length, url_type, content_length, content_encoding, content_language, content_type_norm, wayback_date, crawl_date, crawl_dates, crawl_year, crawl_years, crawl_year_month, crawl_year_month_day, host, domain, public_suffix, links_hosts, links_domains, links_public_suffixes, hash, publication_date, publication_year, content_type_version, content_type_full, content_type_tika, content_type_droid, content_type_served, content_type_ext, server, generator, ssdeep_hash_bs_3, ssdeep_hash_bs_6"\
     "text:\"corona OR covid\""
```



Example using direct url to the solr server.

```
java -Xmx8G \
    -cp netarchiveclient-3.0.0-jar-with-dependencies.jar \
    dk.statsbiblioteket.netarchiveclient.NetarchiveExtractorMain  \
    "https://solr-url.com/solr/" \
    ns 19900101 20200101 1000 \
    "title, subject, description, comments, author, url, url_norm, content_type, last_modified, last_modified_year, links, content, content_text_length, url_type, content_length, content_encoding, content_language, content_type_norm, wayback_date, crawl_date, crawl_dates, crawl_year, crawl_years, crawl_year_month, crawl_year_month_day, host, domain, public_suffix, links_hosts, links_domains, links_public_suffixes, hash, publication_date, publication_year, content_type_version, content_type_full, content_type_tika, content_type_droid, content_type_served, content_type_ext, server, generator, ssdeep_hash_bs_3, ssdeep_hash_bs_6"\
     "text:\"corona OR covid\""
```



When performing multiple shard-oriented extractions, consider port-forwarding each Solr instance separately and

specify the collection as par of the Solr URL. This avoids a round-trip through the master Solr (`ns`).

In the sample below, a port-forward from `juliet:52387` to `localhost` has beeen established.

Note that the shard (the parameter directly after the Solr URL) is empty

```
java -Xmx8G \
    -cp netarchiveclient-3.1.1-jar-with-dependencies.jar \
    dk.statsbiblioteket.netarchiveclient.NetarchiveExtactorMain  \
    "http://localhost:52387/solr/ns87"\
    "" 19900101 20400101 1000 \
    "title, subject, description, comments, author, url, url_norm"\
     "text:\"corona OR covid\" AND crawl_year:2020"
```




Notice how you escape the quote " character in the query.

Data-format:

CSV:

Field seperator is :  ,  (comma)

Multi value field seperator is 'tab'.

All fields start and ends with a "

Empty fields will just be : ""

JSON:

Multi value field seperator is 'tab'.



Troubleshooting

If the Solr Cloud stops responding (this can be checked by issuing a search through SolrWayback), it is probably due to an Out Of memory error. This can happen if the batchsize is too high or if too many concurrent extractions are started.

Lower the batchsize or increase the memory on the Solr servers. Most often it is the Solr master server in charge of merging the queries that
need more memory.

