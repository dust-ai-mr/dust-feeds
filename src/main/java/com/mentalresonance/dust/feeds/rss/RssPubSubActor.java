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

import com.mentalresonance.dust.core.actors.ActorRef;
import com.mentalresonance.dust.core.actors.Props;
import com.mentalresonance.dust.core.actors.lib.PubSubActor;
import com.mentalresonance.dust.core.msgs.StartMsg;
import com.mentalresonance.dust.core.system.exceptions.ActorInstantiationException;

/**
 * A PubSub Actor who manages a child RSSFeedPipeActor (i.e. I am a single stage pipe).
 * When I get a page from the feed send it to all my subscribers.
 */
public class RssPubSubActor extends PubSubActor {

    ActorRef throttler;
    Long intervalMS;
    String url;
    boolean returnContent;

    /**
     * Props
     * @param throttler optional throttler
     * @return Props
     */
    public static Props props(String url, Long intervalMS, ActorRef throttler, boolean returnContent) {
        return Props.create(RssPubSubActor.class, url, intervalMS, throttler, returnContent);
    }

    public static Props props(String url, Long intervalMS) {
        return Props.create(RssPubSubActor.class, url, intervalMS, null, true);
    }

    /**
     * Constructor
     * @param throttler optional
     */
    public RssPubSubActor(String url, Long intervalMS, ActorRef throttler, boolean returnContent) {
        this.url = url;
        this.intervalMS = intervalMS;
        this.throttler = throttler;
        this.returnContent = returnContent;
    }

    @Override
    protected void preStart() throws ActorInstantiationException {
        actorOf(RssFeedPipeActor.props(url, intervalMS, throttler, returnContent), "rss-feed").tell(new StartMsg(), self);
    }
}
