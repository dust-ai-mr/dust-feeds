/*
 *   Copyright 2024-2025 Alan Littleford
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.mentalresonance.dust.feeds.rss;

import com.mentalresonance.dust.core.actors.*;
import com.mentalresonance.dust.feeds.msgs.RawDocumentMsg;
import com.mentalresonance.dust.feeds.msgs.UpdateUrlMsg;
import com.mentalresonance.dust.html.msgs.HtmlDocumentMsg;
import com.mentalresonance.dust.http.service.HttpRequestResponseMsg;
import com.mentalresonance.dust.http.service.HttpService;
import com.mentalresonance.dust.http.trait.HttpClientActor;
import com.mentalresonance.dust.core.msgs.SnapshotMsg;
import com.mentalresonance.dust.core.msgs.StartMsg;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndLink;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.mentalresonance.dust.core.msgs.PauseMsg;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


/**
 * Periodically visit RSS feed given by URL and access recent documents. Produces HtmlDocumentMsgs or RssContentMsgs
 * (depending on Props) which it sends to its parent pipeline.
 *
 * <b>Needs to receive a StartMsg to start.</b>
 */
@Slf4j
public class RssFeedPipeActor extends PersistentActor implements HttpClientActor {

    /**
     * Throttler
     */
    protected ActorRef throttler;
    /**
     * url of feed
     */
    protected String url;
    /**
     * time (ms) between visits
     */
    protected Long intervalMS;
    /**
     * Headers to use in Http calls
     */
    protected LinkedHashMap<String, String> headers = null;
    /**
     * Scheduling
     */
    protected Cancellable pump = null;

    /**
     * Persistent state
     */
    protected RssFeedstate rssFeedstate;

    /**
     * By default we return the content indicated by teh feed (i.e. we retrieve the content
     * of the RSS links). Otherwise we will return an RssContentMsg which has meta info and the link
     * and possibly a summary
     */
    @Setter
    protected boolean returnContent;

    /**
     * State - last visit ts
     */
    public static class RssFeedstate implements Serializable {
        /**
         * State - last visit ts
         */
        public Long lastTs = 0L;

        /**
         * Constructor
         */
        public RssFeedstate() {}
    }

    @Override
    protected Class<RssFeedstate> getSnapshotClass() { return RssFeedstate.class; }

    /**
     * Props
     * @param url of feed
     * @param interval (in ms) between visits
     * @return Props
     */
    public static Props props(String url, Long interval) {
        return Props.create(RssFeedPipeActor.class, url, interval, null, true);
    }

    public static Props props(String url, Long interval, boolean returnContent) {
        return Props.create(RssFeedPipeActor.class, url, interval, null, returnContent);
    }
    public static Props props(String url, Long intervalMS, ActorRef throttler) {
        return Props.create(RssFeedPipeActor.class, url, intervalMS, throttler, true);
    }
    public static Props props(String url, Long intervalMS, ActorRef throttler, Boolean returnContent) {
        return Props.create(RssFeedPipeActor.class, url, intervalMS, throttler, returnContent);
    }
    /**
     * Constructor
     * @param url of feed
     * @param intervalMS (in ms) between visits
     * @param throttler nullable throttler
     * @param returnContent if false send parent an {@link RssContentMsg} which describes the linked content, else GET the linked
     *                      content and send parent an {@link HtmlDocumentMsg}
     */
    public RssFeedPipeActor(String url, Long intervalMS, ActorRef throttler, Boolean returnContent) {
        this.url = url;
        this.throttler = throttler;
        this.intervalMS = intervalMS;
        this.returnContent = returnContent == null || returnContent;
    }
    /**
     * Contructor
     * @param url of feed
     * @param intervalMS (in ms) between visits
     * @param throttler nullable throttler
     * @param userAgent to use in http calls
     * @param returnContent if false send parent an {@link RssContentMsg} which describes the linked content, else GET the linked
     *                      content and send parent an {@link HtmlDocumentMsg}
     */
    public RssFeedPipeActor(String url, Long intervalMS, ActorRef throttler, String userAgent, Boolean returnContent) {
        this.url = url;
        this.throttler = throttler;
        this.intervalMS = intervalMS;
        this.returnContent = returnContent == null || returnContent;
        headers = new LinkedHashMap<>();
        headers.put("User-Agent", userAgent);

    }

    @Override
    protected void preStart() {
        saveSnapshot(rssFeedstate);
    }

    /**
     * If I am stopped then I will delete my data - otherwise save it.
     */
    @Override
    protected void postStop() {
        if (isInShutdown()) {
            saveSnapshot(rssFeedstate);
        } else
            deleteSnapshot();
        if (null != pump)
            pump.cancel();
    }

    @Override
    protected ActorBehavior recoveryBehavior() {
        return message -> {
            switch(message) {
                case SnapshotMsg msg -> {
                    rssFeedstate = null != msg.getSnapshot() ? (RssFeedstate) msg.getSnapshot() : new RssFeedstate();
                    become(createBehavior());
                }
                default  -> {
                    log.error("%s received unhandled message %s in recovery".formatted(self.path, message));
                }
            }
        };
    }

    @Override
    protected ActorBehavior createBehavior() {
        return message -> {
            switch(message) {
                case StartMsg start -> {
                    if (null == headers)
                        request(url);
                    else
                        request(url, headers);
                    pump = scheduleIn(start, intervalMS);
                }
                case PauseMsg ignored -> {
                    if (null != pump)
                        pump.cancel();
                }
                case UpdateUrlMsg msg -> {
                    url = msg.getUrl();
                }
                // Get or return RSS content ..
                case RssContentMsg msg -> {
                    if (! returnContent) {
                        parent.tell(msg, self);
                    }
                    else if (null != msg.link) {
                        PageContentMsg pcm = new PageContentMsg(
                            self,
                            (null == headers) ?
                                HttpService.buildGetRequest(msg.link) :
                                HttpService.buildGetRequest(msg.link, headers)
                        );
                        pcm.rcm = msg;
                        log.trace("Getting page at {}", pcm.request.url());
                        if (null != throttler)
                            throttler.tell(pcm, self);
                        else
                            request(pcm);
                    }
                }
                case PageContentMsg pcm -> {
                    if (pcm.isProxied()) { // Request granted from throttler - so do it
                        pcm.setProxied(false);
                        request(pcm);
                    }
                    else { // Response
                        if (null != pcm.response) {
                            try {
                                log.trace("Got page at {}", pcm.request.url());
                                String contentType = pcm.response.header("content-type");
                                if (null == contentType) contentType = "text/html";
                                if (contentType.contains("html")) {
                                    HtmlDocumentMsg htmlDocumentMsg = pcm.toHtmlDocumentMsg();
                                    htmlDocumentMsg = updateDocument(htmlDocumentMsg);
                                    parent.tell(htmlDocumentMsg, self);
                                }
                                else {
                                    RawDocumentMsg rawDocumentMsg = pcm.toRawDocumentMsg(contentType);
                                    rawDocumentMsg = updateRawDocument(rawDocumentMsg);
                                    parent.tell(rawDocumentMsg, self);
                                }
                            } catch (Exception e) {
                                log.error("Could not get document from page: %s".formatted(url));
                            }
                        }
                        else if (null != pcm.exception) {
                            log.error("Request to page %s failed: %s".formatted(pcm.request.url(), pcm.exception.getMessage()));
                        }
                    }
                }
                // Get the RSS page - note PageContentMsg subclasses HttpRequestResponseMsg so don't shadow it
                case HttpRequestResponseMsg msg -> {
                    if (null != msg.response && msg.response.isSuccessful()) {
                        processRSS(msg.response);
                    } else if (null != msg.exception) {
                        log.error("RSS call to %s failed - %s".formatted(url, msg.exception.getMessage()));
                    } else
                        log.error("RSS call to %s failed - %d".formatted(url, msg.response.code()));
                }
                default -> {
                    super.createBehavior().onMessage(message);
                }
            }
        };
    }

    /**
     * To be overridden -- allow a subclass to modify the document (e.g. add a tag) before it is passed on.
     * @param htmlDocumentMsg the original document
     * @return the unmodified document in this case
     */
    protected HtmlDocumentMsg updateDocument(HtmlDocumentMsg htmlDocumentMsg) {
        return htmlDocumentMsg;
    }
    /**
     * To be overridden -- allow a subclass to modify the document (e.g. add a tag) before it is passed on.
     * @param rawDocumentMsg the original document
     * @return the unmodified document in this case
     */
    protected RawDocumentMsg updateRawDocument(RawDocumentMsg rawDocumentMsg) {
        return rawDocumentMsg;
    }

    /**
     * Process the XML from the feed
     * @param response - contains XML with feed content
     */
    protected void processRSS(Response response)
    {
        SyndFeed feed;
        String body;

        try {
            body = response.body().string();
            feed = new SyndFeedInput().build(
                    new XmlReader(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)))
            );
        }
        catch (Exception e) {
            log.error("Processing RSS for %s: %s".formatted(url, e.getMessage()));
            return;
        }
        finally {
            response.close();
        }

        final Long[] latestPublished = {0L};

        /**
         * Find 'new' content (since last visit)
         */
        List<SyndEntry> entries = feed
                .getEntries()
                .stream()
                .filter(entry -> {
                    /**
                     * Filter by more recent than we last saw and then adjust that time at the end.
                     * Try item dates first. If these don't exist try global published info
                     */
                    Date published = null != entry.getPublishedDate() ? entry.getPublishedDate() : feed.getPublishedDate();
                    // rome only gets dublin code date so we have to try harder
                    if (published == null) {
                        published = pubDate(body, entry.getLink());
                    }
                    if (null != published && published.getTime() > rssFeedstate.lastTs) {
                        if (published.getTime() > latestPublished[0]) latestPublished[0] = published.getTime();
                        // Ensure we have some published date
                        entry.setPublishedDate(published);
                        return true;
                    } else
                        return false;
                })
                .toList();

        if (latestPublished[0] > 0) {
            rssFeedstate.lastTs = latestPublished[0];
            saveSnapshot(rssFeedstate);
        }
        log.info("Processing %d new entries from RSS feed %s".formatted(entries.size(), url));

        /**
         * May have a list of contents *or* list of links, or a single link -- handle all.
         */

        // Ensure we are not creating duplicates
        LinkedHashMap<String, Boolean> links = new LinkedHashMap<>();

        for (SyndEntry entry: entries)
        {
            for(SyndContent content :entry.getContents().stream().filter(c -> c.getType() == "html").toList())
            {
                RssContentMsg rssContentMsg = new RssContentMsg();
                rssContentMsg.title = entry.getTitle();
                rssContentMsg.link = entry.getLink();
                rssContentMsg.author = entry.getAuthor();
                rssContentMsg.published = entry.getPublishedDate();
                rssContentMsg.content = content.getValue();

                if ((null != rssContentMsg.link) && !links.containsKey(rssContentMsg.link)) {
                    links.put(rssContentMsg.link, true);
                    self.tell(rssContentMsg, self);
                } else if (null == rssContentMsg.link) {
                    self.tell(rssContentMsg, self);
                }
            }
            for (SyndLink sl: entry.getLinks())
            {
                RssContentMsg rssContentMsg = new RssContentMsg();
                rssContentMsg.title = null != sl.getTitle() ? sl.getTitle() : entry.getTitle();
                rssContentMsg.link = sl.getHref();
                rssContentMsg.author = entry.getAuthor();
                rssContentMsg.published = entry.getPublishedDate();
                if (! links.containsKey(rssContentMsg.link)) {
                    links.put(rssContentMsg.link, true);
                    self.tell(rssContentMsg, self);
                }
            }
            if (null != entry.getLink()) {
                RssContentMsg rssContentMsg = new RssContentMsg();
                rssContentMsg.title = entry.getTitle();
                rssContentMsg.link = entry.getLink();
                rssContentMsg.author = entry.getAuthor();
                rssContentMsg.published = entry.getPublishedDate();
                if (! links.containsKey(rssContentMsg.link)) {
                    links.put(rssContentMsg.link, true);
                    self.tell(rssContentMsg, self);
                }
            }
        }
    }

    private Date pubDate(String feed, String entryUrl) {
        Date date = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(feed.getBytes(StandardCharsets.UTF_8)));

            // Normalize XML structure
            doc.getDocumentElement().normalize();

            Element channel = (Element) doc.getElementsByTagName("channel").item(0);

            String pubDateStr = getTextContent(channel, "pubDate");
            String lastBuildDateStr = getTextContent(channel, "lastBuildDate");

            SimpleDateFormat rfc822 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.US);

            Date pubDate = pubDateStr != null ? rfc822.parse(pubDateStr) : null;
            Date lastBuildDate = lastBuildDateStr != null ? rfc822.parse(lastBuildDateStr) : null;

            date = lastBuildDate != null ? lastBuildDate : pubDate;
            if (date == null)
                date = extractDateFromUrl(entryUrl);
        }
        catch (Exception e) {
            log.error("Could not parse RSS feed %s".formatted(feed));
        }
        return date;
    }

    private static String getTextContent(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return null;
    }

    private static final List<DatePattern> datePatterns = List.of(
            new DatePattern("\\b(\\d{4})(\\d{2})(\\d{2})\\b", "yyyyMMdd"),
            new DatePattern("\\b(\\d{4})[-/](\\d{2})[-/](\\d{2})\\b", "yyyy-MM-dd"),
            new DatePattern("\\b(\\d{2})[-/](\\d{2})[-/](\\d{4})\\b", "dd-MM-yyyy")
    );

    private static Date extractDateFromUrl(String url) {
        for (DatePattern pattern : datePatterns) {
            Matcher matcher = pattern.regex.matcher(url);
            if (matcher.find()) {
                String rawDate = matcher.group(0);
                try {
                    return pattern.format.parse(rawDate);
                } catch (ParseException ignored) {
                }
            }
        }
        return null; // No known pattern matched
    }

    private static class DatePattern {
        Pattern regex;
        SimpleDateFormat format;

        DatePattern(String regex, String format) {
            this.regex = Pattern.compile(regex);
            this.format = new SimpleDateFormat(format);
            this.format.setLenient(false);
        }
    }
}
