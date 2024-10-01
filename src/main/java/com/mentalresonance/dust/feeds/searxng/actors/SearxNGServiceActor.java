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

package com.mentalresonance.dust.feeds.searxng.actors;

import com.google.gson.Gson;
import com.mentalresonance.dust.core.actors.Actor;
import com.mentalresonance.dust.core.actors.ActorBehavior;
import com.mentalresonance.dust.core.actors.ActorRef;
import com.mentalresonance.dust.core.actors.Props;
import com.mentalresonance.dust.feeds.searxng.msgs.SearxNGRequestMsg;
import com.mentalresonance.dust.feeds.searxng.msgs.SearxNGResponseMsg;
import com.mentalresonance.dust.http.service.HttpRequestResponseMsg;
import com.mentalresonance.dust.http.service.HttpService;
import com.mentalresonance.dust.http.trait.HttpClientActor;

/**
 * Request / response service actor for the SearxNG meta search engine
 * https://docs.searxng.org/
 */
public class SearxNGServiceActor extends Actor implements HttpClientActor
{
    String url;
    ActorRef from;

    /**
     * Create
     * @param url of SearxNG instance. e.g. https://searx.foss.family/search  or http://localhost:8080/search if you
     * have a local copy of SearXNG installed.
     * @return
     */
    public static Props props(String url) {
        return Props.create(SearxNGServiceActor.class, url);
    }

    public SearxNGServiceActor(String url) {
        this.url = url;
    }

    @Override
    public ActorBehavior createBehavior() {

        return (message) -> {
            switch(message) {
                case SearxNGRequestMsg msg -> {
                    url = HttpService.buildUrl(url, msg.serialize());
                    from = sender;
                    HttpRequestResponseMsg request = new HttpRequestResponseMsg(
                        self,
                        HttpService.buildGetRequest(url)
                    );
                    request(request);
                }
                case HttpRequestResponseMsg msg -> {
                    SearxNGResponseMsg resp = new Gson().fromJson(msg.response.body().string(), SearxNGResponseMsg.class);
                    if (null != from) from.tell(resp, self);
                    stopSelf();
                }
                default -> throw new IllegalStateException("Unexpected value: " + message);
            }
        };
    }
}
