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

package com.mentalresonance.dust.feeds.msgs;

import com.mentalresonance.dust.core.actors.ActorRef;
import com.mentalresonance.dust.html.msgs.DocumentMsg;
import lombok.Getter;
import lombok.Setter;

/**
 * Non-html document. In this case field type should be the http media type and the content is in
 * the rawContent byte array
 */
public class RawDocumentMsg extends DocumentMsg {

    @Getter
    @Setter
    private byte[] rawContent;

    /**
     * Constructor
     * @param sender of message
     */
    public RawDocumentMsg(ActorRef sender) {
        super(sender);
    }

    @Override
    public String getContent() {
        return null;
    }

}
