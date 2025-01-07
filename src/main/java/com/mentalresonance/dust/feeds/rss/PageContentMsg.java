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
import com.mentalresonance.dust.feeds.msgs.RawDocumentMsg;
import com.mentalresonance.dust.html.msgs.HtmlDocumentMsg;
import com.mentalresonance.dust.http.service.HttpRequestResponseMsg;
import okhttp3.Request;

import java.io.IOException;

/**
 * Request for actual web page
 */
public class PageContentMsg extends HttpRequestResponseMsg {

    /**
     * The content
     */
    public RssContentMsg rcm; // The content

    /**
     * Constructor
     * @param requester sender of this request
     * @param request okhttp3 Request
     */
    public PageContentMsg(ActorRef requester, Request request) {
        super(requester, request);
    }

    /**
     * Create HtmlDocumentMsg - note this can only be done <b>once</b> since it uses the response body which is
     * consumed.
     * @return HtmlDocumentMsg
     * @throws IOException if error getting body of response
     */
    public HtmlDocumentMsg toHtmlDocumentMsg() throws IOException
    {
        HtmlDocumentMsg doc = new HtmlDocumentMsg(getSender());

        assert response.body() != null;

        doc.setHtml(response.body().string());
        doc.setContentTs(rcm.published.getTime());
        doc.setAuthor(rcm.author);
        doc.setTitle(rcm.title);
        doc.setSource(rcm.link);
        doc.setCreatedTs(System.currentTimeMillis());
        return doc;
    }

    /**
     * Create RawDocumentMsg - note this can only be done <b>once</b> since it uses the response body which is
     * consumed.
     * @return RawDocumentMsg
     * @param contentType type of content
     * @throws IOException if error getting body of response
     */
    public RawDocumentMsg toRawDocumentMsg(String contentType) throws IOException {
        RawDocumentMsg doc = new RawDocumentMsg(getSender());

        assert response.body() != null;

        doc.setRawContent(response.body().bytes());

        doc.setContentTs(rcm.published.getTime());
        doc.setAuthor(rcm.author);
        doc.setTitle(rcm.title);
        doc.setSource(rcm.link);
        doc.setType(contentType);
        doc.setCreatedTs(System.currentTimeMillis());
        return doc;
    }
}
