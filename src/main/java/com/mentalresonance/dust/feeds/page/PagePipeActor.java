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

package com.mentalresonance.dust.feeds.page;

import com.mentalresonance.dust.core.actors.ActorBehavior;
import com.mentalresonance.dust.core.actors.PersistentActor;
import com.mentalresonance.dust.core.actors.Props;
import com.mentalresonance.dust.html.msgs.HtmlDocumentMsg;
import com.mentalresonance.dust.http.service.HttpRequestResponseMsg;
import com.mentalresonance.dust.http.service.HttpService;
import com.mentalresonance.dust.http.trait.HttpClientActor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;

/**
 * GET a given page defined in an HtmlDocumentMsg and send that page on.
 */
@Slf4j
public class PagePipeActor extends PersistentActor implements HttpClientActor {

    LinkedHashMap<String, String> headers;

    /**
     * Default headers for http calls
     * @return Props
     */
    public static Props props() {
        return Props.create(PagePipeActor.class, new LinkedHashMap<String, String>());
    }

    /**
     * Customer headers for http calls
     * @param headers to use in http calls
     * @return Props
     */
    public static Props props(LinkedHashMap<String, String> headers) {
        return Props.create(PagePipeActor.class, headers);
    }

    /**
     * Constructor
     * @param headers for http calls
     */
    public PagePipeActor(LinkedHashMap<String, String> headers) {
        this.headers = headers;
    }

    protected ActorBehavior createBehavior() {
        return message -> {
            switch(message) {
                case HtmlDocumentMsg msg -> {
                    Request req = HttpService.buildGetRequest(msg.getSource(), headers);
                    request(new HttpRequestResponseMsg(self, req, msg));
                }
                case HttpRequestResponseMsg msg -> {
                    try {
                        if (null != msg.response) {
                            HtmlDocumentMsg hdm = (HtmlDocumentMsg)msg.tag;

                            hdm.setHtml(msg.response.body().string());
                            parent.tell(msg.tag, self);
                        }
                        else if (null != msg.exception) {
                            log.error("%s Error: %s".formatted(self.path, msg.exception.getMessage()));
                        }
                    }
                    catch (Exception e) {
                        log.error("%s Error: %s".formatted(self.path, e.getMessage()));
                        if (null != msg.response)
                            msg.response.close();
                    }
                    finally {
                        if (null != msg.response)
                            msg.response.close();
                    }
                }
                default -> super.createBehavior().onMessage(message);
            }
        };
    }

    /**
     * Want flat directory structure so map urls to unique filenames by taking path and changing / -> -
     * @param url
     * @return
     */
    private String urlToFilename(String url) throws URISyntaxException, MalformedURLException {
        return new URI(url).toURL().getPath().substring(1).replaceAll("/", "-");
    }
}
