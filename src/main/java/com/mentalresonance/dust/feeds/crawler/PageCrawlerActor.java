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
import com.mentalresonance.dust.html.services.HtmlService;
import com.mentalresonance.dust.http.service.HttpRequestResponseMsg;
import com.mentalresonance.dust.http.service.HttpService;
import com.mentalresonance.dust.http.trait.HttpClientActor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.Serializable;

/**
 * Process the web page at a given URL returning page and all on-site links to the parent.
 * Then dies.
 */
@Slf4j
public class PageCrawlerActor extends Actor implements HttpClientActor {

	/**
	 * Props
	 * @param throttler nullable Throttler
	 * @return Props
	 */
	public static Props props(ActorRef throttler) {
		return Props.create(PageCrawlerActor.class, throttler);
	}

	/**
	 * Constructor
	 * @param throttler nullable throttler ref
	 */
	public PageCrawlerActor(ActorRef throttler) {
		this.throttler = throttler;
	}

	@Override
	protected void preStart() {
		dieIn(10 * 60000L);// Can get q'd up by throttler
	}

	@Override
	protected void postStop() {
		cancelDeadMansHandle();
	}

	@Override
	protected ActorBehavior createBehavior() {

		return (Serializable message) -> {

			switch(message) {

				case PageCrawlMsg msg:
					url = msg.getUrl();
					type = msg.getType();
					parent.tell(new CanProcessPageMsg(url), self);
					break;

				case CanProcessPageMsg msg:
					if (msg.getPermission()) {
						HttpRequestResponseMsg rrm = new HttpRequestResponseMsg(
								self,
								HttpService.buildGetRequest(url)
						);
						throttler.tell(rrm, self);
					}
					else {
						// log.info("{} already crawled or blocked by robots.txt", url);
						stopSelf();
					}
					break;

				case HttpRequestResponseMsg rrm:
					if (rrm.isProxied())
					{
						rrm.setProxied(false);
						request(rrm);  // Do http request which will come back to me ..
						// Should not take longer than 60 secs to download and process the page (sites throttle bots)
					}
					else if (null != rrm.response) { // .. and I will end up here
						if (rrm.response.isSuccessful())
							self.tell(new ProcessPageMsg(rrm.response.body().string()), self);
						else {
							log.warn("Request to {} failed", url);
							stopSelf();
						}
					}
					else {
						log.error("No response from {} .. stopping", url);
						stopSelf();
					}
					break;

				case ProcessPageMsg msg:
					try {
						Document doc = Jsoup.parse(msg.html);
						parent.tell(new PageMsg(
								url,
								msg.html,
								type,
								HtmlService.links(doc, url, true, true)
						), null);
					}
					catch (Exception e) {
						e.printStackTrace();
						throw(e);
					}
					stopSelf();
					break;

				default: log.warn("Got message {}", message);
			}
		};
	}

    @Setter
    @Getter
    private String url;
	final private ActorRef throttler;
	private String type;

	private static class ProcessPageMsg implements Serializable {
		@Getter
		private String html;

		public ProcessPageMsg(String html) {
			this.html = html;
		}
	}
}
