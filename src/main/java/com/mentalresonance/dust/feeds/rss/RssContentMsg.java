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

package com.mentalresonance.dust.feeds.rss;


import java.io.Serializable;
import java.util.Date;

/**
 * One page gleaned from RSS
 */
public class RssContentMsg implements Serializable {
    /**
     * Date published
     */
    public Date published;
    /**
     * Content author
     */
    public String author = null;
    /**
     * Content title
     */
    public String title = null;
    /**
     * Content
     */
    public String content;
    /**
     * Link to source content
     */
    public String link;

    /**
     * Constructor
     */
    public RssContentMsg() {}

    @Override
    public String toString() {
        return "%s (%s) %s".formatted(title, author, link);
    }
}
