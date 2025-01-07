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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SearxNG response
 */
public class SearxNGResponseMsg implements Serializable {

    /**
     * The query
     */
    public String query;
    /**
     * The results
     */
    public List<SearxNGResultMsg> results;
    /**
     * Categories (see SearXNG engine documentation)
     */
    public Map<String, String> categories;
    /**
     * Pagination (see SearXNG engine documentation)
     */
    public Map<String, Integer> pagination;
    /**
     * Suggestions (see SearXNG engine documentation)
     */
    public List<String> suggestions;
    /**
     * Metadata (see SearXNG engine documentation)
     */
    public Map<String, Object> metadata;

    /**
     * Constructor
     */
    public SearxNGResponseMsg() {}

    /**
     * Pretty print urls for logging
     * @return results
     */
    public String toString() {
        return
            results
                .stream()
                .map(r -> r.url)
                .collect(Collectors.joining("\n"));
    }

}
