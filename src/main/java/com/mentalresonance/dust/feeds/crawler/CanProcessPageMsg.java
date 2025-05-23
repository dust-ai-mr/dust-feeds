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

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Does the sender have permission to crawl this URL
 */
@Getter
@Setter
public class CanProcessPageMsg implements Serializable {

	/**
	 * The url
	 */
	private String url;
	/**
	 * Permission - true grants
	 */
	private Boolean permission;

	/**
	 * Constructor
	 * @param url of page
	 */
	public CanProcessPageMsg(String url) {
		this.url = url;
	}
}
