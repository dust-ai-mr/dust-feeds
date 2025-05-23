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

package com.mentalresonance.dust.feeds.searxng.msgs;

import java.io.Serializable;
import java.util.LinkedHashMap;

/**
 * A SearxNG search request
 */
public class SearxNGRequestMsg implements Serializable {

    /**
     * The query
     */
    public String q;
    /**
     * Optional categories (see SearXNG engine documentation)
     */
    public String categories = null;
    /**
     * Optional engines (see SearXNG engine documentation)
     */
    public String engines = null;
    /**
     * Optional time_range (see SearXNG engine documentation)
     */
    public String time_range = null;
    /**
     * Format of response -- default is json
     */
    public String format = "json";

    /**
     * Convert this message to a map
     * @return the map
     */
    public LinkedHashMap<String, String> serialize() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();

        map.put("q", q);
        map.put("categories", categories);
        map.put("format", format);
        map.put("engines", engines);
        map.put("time_range", time_range);

        return map;
    }

    /**
     * Constructor
     */
    public SearxNGRequestMsg() {}
}
