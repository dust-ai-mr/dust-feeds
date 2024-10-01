/*
 *   Copyright 2024 Alan Littleford
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

package com.mentalresonance.dust.feeds.crawler;

import com.mentalresonance.dust.core.actors.Actor;
import com.mentalresonance.dust.core.actors.ActorBehavior;
import com.mentalresonance.dust.core.actors.ActorRef;
import com.mentalresonance.dust.core.actors.Props;
import com.mentalresonance.dust.core.actors.lib.ThrottlingRelayActor;
import com.mentalresonance.dust.core.msgs.Terminated;
import com.mentalresonance.dust.core.system.exceptions.ActorInstantiationException;
import com.mentalresonance.dust.html.msgs.HtmlDocumentMsg;
import com.mentalresonance.dust.http.service.HttpService;
import com.mentalresonance.dust.http.trait.HttpClientActor;
import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Crawl a site, sending information back to the requester
 */
@Setter
@Getter
@Slf4j
public class SiteCrawlerPipeActor extends Actor implements HttpClientActor {

	private ActorRef throttler;
	@Getter
	private URL url;
	private int numActiveCrawlers = 0;
	private BaseRobotRules rules;
	private ActorRef originalSender;
	private Boolean haveCheckedRobots = false;

	public static final String ROOT = "root";
	public static final String PAGE = "page";
	/**
	 * Maps url -> map contain page specific data for the page. This enables us to detect when we might be going to
	 * re-crawl a page and prevent it, as well as being the sum total of what we know about the site.
	 */
	private Map<String, Map> pages = new LinkedHashMap<>();
	/**
	 * List of [regex, type]. The regexs are applied in order to href in links and if we have a match that link is followed.
	 * If that successfully returns a page the page is given the specified type.
	 */
	private List<List<String>> hrefFilters;
	/**
	 * List of [regex, type]. The regexs are applied in order to the anchor text of a link and if we have a match
	 * that link is followed. If that successfully returns a page the page is given the specified type.
	 * This test is applied <b>after</b> hrefFilters if hrefFilters fails
	 */
	private List<List<String>> anchorFilters;

	/**
	 * Props
	 * @param hrefFilters filters for hrefs
	 * @param anchorFilters filters for anchors
	 * @return the Props
	 */
	public static Props props(List<List<String>> hrefFilters, List<List<String>> anchorFilters) {
		return Props.create(SiteCrawlerPipeActor.class, hrefFilters, anchorFilters);
	}
	/**
	 * Props
	 * @param hrefFilters filters for hrefs
	 * @return the Props
	 */
	public static Props props(List<List<String>> hrefFilters) {
		return SiteCrawlerPipeActor.props(hrefFilters, new ArrayList<>());
	}
	/**
	 * Props -- use null filters
	 * @return the Props
	 */
	public static Props props() {
		String[] wild = {".*", PAGE};
		return SiteCrawlerPipeActor.props( Collections.singletonList(Arrays.asList(wild)),new ArrayList<>());
	}

	public SiteCrawlerPipeActor(List<List<String>> hrefFilters, List<List<String>> anchorFilters) {
		this.hrefFilters = hrefFilters;
		this.anchorFilters = anchorFilters;
	}

	@Override
	public void preStart() throws ActorInstantiationException {
		// One throttler per site - hit it no more than 1 / sec
		throttler = actorOf(ThrottlingRelayActor.props(1000L), "throttler");
	}

	@Override
	public ActorBehavior createBehavior() {
		return (Serializable message) -> {
			switch(message) {

				/*
				 * Request to crawl a site or page. We fire up a PageCrawlerActor to do the job.
				 * He will send us a CanProcessPageMsg before doing anything.
				 * If we are on the root page check robots.txt if we haven't already done so. We do not use the throttler for this
				 * since it is only done once.
				 */
				case PageCrawlMsg msg:
					ActorRef crawler = actorOf(PageCrawlerActor.props(throttler));

					if (Objects.equals(msg.getType(), "root")) {
						url = new URI(msg.getUrl()).toURL();
						originalSender = sender;

						if (! haveCheckedRobots) {
							String robots = String.join("/", Arrays.copyOfRange(msg.getUrl().split("/"), 0, 3)) + "/robots.txt";

							Response robotsResponse = HttpService.doRequest(HttpService.buildGetRequest(robots));
							SimpleRobotRulesParser parser = new SimpleRobotRulesParser();
							rules = parser.parseContent(
									msg.getUrl(),
									robotsResponse.body().string().getBytes(),
									StandardCharsets.UTF_8.name(),
									List.of()
							);
							haveCheckedRobots = true;
						}
					}
					crawler.tell(message, self);
					watch(crawler);
					break;

				/*
				 * All page processors ask us if they can go ahead since a page should only be done once or it
				 * may be blocked by robots.txt
				 * We also normalize the url here ...
				 */
				case CanProcessPageMsg msg:
					String surl = msg.getUrl();
					boolean canCrawl = rules.isAllowed(surl);
					String normalizedUrl = normalizeUrl(surl);

					// Haven't seen it before and is allowed by rules
					msg.setPermission(pages.get(normalizedUrl) == null && canCrawl);

					if (msg.getPermission()) {
						pages.put(normalizedUrl, Map.of("started",  true));
					}
					sender.tell(msg, self);
					break;

				/*
				  From PageCrawlerActor - the contents of the page he crawled. Send the page to the original site
				  crawl requester then process on-site links
				 */
				case PageMsg msg:
					if (!msg.getContent().isEmpty()) {
						HtmlDocumentMsg page = new HtmlDocumentMsg(self);
						page.setSource(msg.getUrl());
						page.setHtml(msg.getContent());
						page.setType(msg.getType());
						originalSender.tell(page, self);
					} else
						log.warn("No content in {}", msg.getUrl());

					msg.getLinks().forEach(l ->  {	// [url, text] url is to this site still
						String linkUrl = l.getFirst();
						if (!Objects.equals(linkUrl, "#")) {
							try {
								String link = normalizeUrl(linkUrl); // Get normalized path
								String clz = classify(link, l.get(1));
								if (null != clz) {
									// Give time for other messages to come in e.g. CanProcessPageMsgs
									scheduleIn(new PageCrawlMsg(self, linkUrl, clz), 500L);
									++numActiveCrawlers;
								}
							} catch (Exception e) {
								log.error("Error in {}", l, e);
							}
						}
					});
					break;

				case Terminated ignored:
					if (0 == --numActiveCrawlers) { // Done
						log.info("Finished crawling site {}", url);
						context.stop(self);
					}
					//else if (0 == numActiveCrawlers % 100)
					//	log.info("ActiveCrawlers = {}", numActiveCrawlers);
					break;

				default: log.warn("Got message {}", message);
			}
		};
	}

	/**
	 * Attempt to determine 'class' of link - should we follow it and if so is it a root or page on the other end
	 * Our pattern matching is <b>Case Insensitive</b>
	 *
	 * @param surl - target of link
	 * @param anchor - anchor test of link
	 * @return PAGE or ROOT
	 */
	private String classify(String surl, final String anchor) throws MalformedURLException {
		{
			final String[] clz = {null};
			String path = (surl.startsWith("http")) ? new URL(surl).getPath() : surl;

			hrefFilters.stream().anyMatch(  f -> {
				Pattern pattern = Pattern.compile(f.getFirst(), Pattern.CASE_INSENSITIVE);
				Matcher matcher = pattern.matcher(path);
				if (matcher.find()) {
					clz[0] = f.get(1);
					return true;
				} else
					return false;
			});
			// If href doesn't let us follow how about the Anchor text
			if (clz[0] == null) {
				anchorFilters.stream().anyMatch(  f -> {
					Pattern pattern = Pattern.compile(f.getFirst(), Pattern.CASE_INSENSITIVE);
					Matcher matcher = pattern.matcher(anchor);
					if (matcher.find()) {
						clz[0] = f.get(1);
						return true;
					} else
						return false;
				});
			}
			return clz[0];
		}
	}

	/**
	 * Normalize url so we don't revisit. This is used as the key in pages[:]. Note we have
	 * to keep the original url to actually access.
	 *
	 * @param url
	 * @return normalized url
	 */
	private static String normalizeUrl(String url) {
		url = url.toLowerCase();

		if (url.startsWith("http://"))
			url = "https://" + url.substring(7);

		url = url.replace("https://www.", "https://");

		// Ignore fragment links
		int fragmentIndex = url.indexOf("#");
		if (fragmentIndex >= 0) url = url.substring(0, fragmentIndex);

		if (!url.endsWith("/")) url = url + "/";
		if (url.startsWith("www.")) url = url.substring(4);

		return url;
	}
}
