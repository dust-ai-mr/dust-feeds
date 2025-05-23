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

package com.mentalresonance.dust.feeds.crawler;

import com.mentalresonance.dust.core.actors.ActorRef;
import com.mentalresonance.dust.core.msgs.ProxyMsg;
import lombok.Getter;
import lombok.Setter;

/**
 * Request to crawl the page
 */
@Getter
@Setter
public class PageCrawlMsg extends ProxyMsg {
	/**
	 * Constructor
	 * @param sender of request
	 * @param url of page
	 * @param type of page ('root' or 'page')
	 */
	public PageCrawlMsg(ActorRef sender, String url, String type) {
		super(sender);
		this.url = url;
		this.type = type;
	}

	private String url;
	private String type;
}
