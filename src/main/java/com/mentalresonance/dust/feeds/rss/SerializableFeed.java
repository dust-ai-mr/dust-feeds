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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.feed.synd.SyndContentImpl;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SerializableFeed implements Serializable {
    private static final long serialVersionUID = 1L;

    // Getters & setters
    private String title;
    private String link;
    private String description;
    private String feedType = "rss_2.0"; // default
    private List<SerializableFeedEntry> entries = new ArrayList<>();

    @Setter
    @Getter
    public static class SerializableFeedEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        // Getters & setters
        private String title;
        private String link;
        private String description;
        private Date publishedDate;

        public SerializableFeedEntry(String title, String link, String description, Date publishedDate) {
            this.title = title;
            this.link = link;
            this.description = description;
            this.publishedDate = publishedDate;
        }

    }

    /** Convert this serializable object into a SyndFeed */
    public SyndFeed toSyndFeed() {
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType(feedType);
        feed.setTitle(title);
        feed.setLink(link);
        feed.setDescription(description);

        List<SyndEntry> syndEntries = new ArrayList<>();
        for (SerializableFeedEntry sfe : entries) {
            SyndEntry entry = new SyndEntryImpl();
            entry.setTitle(sfe.getTitle());
            entry.setLink(sfe.getLink());
            entry.setPublishedDate(sfe.getPublishedDate());

            SyndContentImpl content = new SyndContentImpl();
            content.setType("text/plain");
            content.setValue(sfe.getDescription());
            entry.setDescription(content);

            syndEntries.add(entry);
        }
        feed.setEntries(syndEntries);
        return feed;
    }
}

