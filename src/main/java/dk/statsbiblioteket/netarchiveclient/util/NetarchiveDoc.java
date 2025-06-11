package dk.statsbiblioteket.netarchiveclient.util;

import java.util.ArrayList;
import java.util.Date;

public class NetarchiveDoc {
    long waybackDate;
    int year;
    Date crawlDate;
    String id;
    String url;
    String host;
    String domain;
    String publicSuffix;
    String server;
    String contentText;
    int contentTextLength;
    String contentType;
    String title;
    String contentLanguage;    
    ArrayList<String> elementsUsed;
    ArrayList<String> linksDomains;
    ArrayList<String> linksHost;
    ArrayList<String> linksPublicSuffixes;

    public long getWaybackDate() {
        return waybackDate;
    }
    public void setWaybackDate(long waybackDate) {
        this.waybackDate = waybackDate;
    }
    public int getYear() {
        return year;
    }
    public void setYear(int year) {
        this.year = year;
    }
    public Date getCrawlDate() {
        return crawlDate;
    }
    public void setCrawlDate(Date crawlDate) {
        this.crawlDate = crawlDate;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public String getHost() {
        return host;
    }
    public void setHost(String host) {
        this.host = host;
    }
    public String getDomain() {
        return domain;
    }
    public void setDomain(String domain) {
        this.domain = domain;
    }
    public String getPublicSuffix() {
        return publicSuffix;
    }
    public void setPublicSuffix(String publicSuffix) {
        this.publicSuffix = publicSuffix;
    }
    public String getServer() {
        return server;
    }
    public void setServer(String server) {
        this.server = server;
    }



    public String getContentLanguage() {
        return contentLanguage;
    }
    public void setContentLanguage(String contentLanguage) {
        this.contentLanguage = contentLanguage;
    }
    public ArrayList<String> getElementsUsed() {
        return elementsUsed;
    }
    public void setElementsUsed(ArrayList<String> elementsUsed) {
        this.elementsUsed = elementsUsed;
    }
    public ArrayList<String> getLinksDomains() {
        return linksDomains;
    }
    public void setLinksDomains(ArrayList<String> linksDomains) {
        this.linksDomains = linksDomains;
    }
    public ArrayList<String> getLinksPublicSuffixes() {
        return linksPublicSuffixes;
    }
    public void setLinksPublicSuffixes(ArrayList<String> linksPublicSuffixes) {
        this.linksPublicSuffixes = linksPublicSuffixes;
    }
    public int getContentTextLength() {
        return contentTextLength;
    }
    public void setContentTextLength(int contentTextLength) {
        this.contentTextLength = contentTextLength;
    }
    public String getContentType() {
        return contentType;
    }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getContentText() {
        return contentText;
    }
    public void setContentText(String contentText) {
        this.contentText = contentText;
    }
    public ArrayList<String> getLinksHost() {
        return linksHost;
    }
    public void setLinksHost(ArrayList<String> linksHost) {
        this.linksHost = linksHost;
    }

}
