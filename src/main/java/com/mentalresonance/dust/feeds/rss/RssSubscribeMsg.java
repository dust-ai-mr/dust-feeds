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

import com.mentalresonance.dust.core.msgs.PubSubMsg;
import com.mentalresonance.dust.html.msgs.DocumentMsg;
import lombok.Getter;

import java.io.Serializable;

/**
 * (Un) subscribe from a subscribable RSS feed
 */
@Getter
public class RssSubscribeMsg extends PubSubMsg {

    String url;

    /**
     * Constructor
     * @param subscribe if true subscribe else unsubscribe
     * @param url of feed
     */
    public RssSubscribeMsg(Boolean subscribe, String url) {
        super(DocumentMsg.class, subscribe);
        this.url = url;
    }
}
