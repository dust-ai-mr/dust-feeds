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
import com.mentalresonance.dust.http.service.HttpRequestResponseMsg;
import com.mentalresonance.dust.http.service.HttpService;
import com.mentalresonance.dust.http.trait.HttpClientActor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * GET a given page and store at a given location or delete a file so obtained.
 */
@Slf4j
public class PersistingPagePipeActor extends PersistentActor implements HttpClientActor {

    LinkedHashMap<String, String> headers;

    /**
     * Message to persist a page
     */
    public static class PersistPageMsg implements Serializable {
        /**
         * url of page
         */
        public String url;
        /**
         * directory containing file
         */
        public String directory;
        /**
         * filename of page
         */
        public String fileName;

        /**
         * Construct
         * @param url of page to persist
         * @param directory to persist to
         */
        public PersistPageMsg(String url, String directory) {
            this.url = url;
            this.directory = directory;
        }
    }

    /**
     * Message to Delete a persisted file
     */
    public static class DeletePageMsg implements Serializable {
        /**
         * url of page
         */
        public String url;
        /**
         * directory containing file
         */
        public String directory;
        /**
         * filename of page
         */
        public String fileName;

        /**
         * Constructor
         * @param url page that was persisted
         * @param directory where persisted file to be deleted is
         */
        public DeletePageMsg(String url, String directory) {
            this.url = url;
            this.directory = directory;
        }
    }

    /**
     * Default headers for http calls
     * @return Props
     */
    public static Props props() {
        return Props.create(PersistingPagePipeActor.class, new LinkedHashMap<String, String>());
    }

    /**
     * Custom headers for http calls
     * @param headers for heep calls
     * @return Props
     */
    public static Props props(LinkedHashMap<String, String> headers) {
        return Props.create(PersistingPagePipeActor.class, headers);
    }

    /**
     * Constructor
     * @param headers for http calls
     */
    public PersistingPagePipeActor(LinkedHashMap<String, String> headers) {
        this.headers = headers;
    }

    protected ActorBehavior createBehavior() {
        return message -> {
            switch(message) {
                case PersistPageMsg msg -> {
                    Request req = HttpService.buildGetRequest(msg.url, headers);
                    request(new HttpRequestResponseMsg(self, req, msg));
                }
                case HttpRequestResponseMsg msg -> {
                    try {
                        if (null != msg.response) {
                            InputStream bytes = msg.response.body().byteStream();
                            PersistPageMsg ppm = (PersistPageMsg)msg.tag;
                            String fileName = urlToFilename(ppm.url);
                            FileOutputStream file = new FileOutputStream("%s/%s".formatted(ppm.directory, fileName));

                            ((PersistPageMsg)msg.tag).fileName = fileName;
                            IOUtils.copy(bytes, file);
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
                }
                case DeletePageMsg msg -> {
                    File dest = new File("%s/%s".formatted(msg.directory, urlToFilename(msg.url)));
                    dest.delete();
                    parent.tell(msg, self);
                }
                default -> super.createBehavior().onMessage(message);
            }
        };
    }

    /**
     * Want flat directory structure so map urls to unique filenames by taking path and changing / -> -
     * @param address url
     * @return filename
     */
    private String urlToFilename(String address) throws URISyntaxException, MalformedURLException, UnsupportedEncodingException {
        URL url = new URL(address);

        if (!Objects.equals(url.getPath(), ""))
            return url.getPath().substring(1).replaceAll("/", "-");
        else
            return URLEncoder.encode(address, StandardCharsets.UTF_8);
    }
}
