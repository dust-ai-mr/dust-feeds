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

package com.mentalresonance.dust.feeds.email;

import jakarta.mail.Address;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Encapsulate a mail message. Sadly Jakarta's MimeMessage is not serializable and so is not suitable
 * for Actory stuff ... Address, thankfully, is.
 */
@Getter
public class MailMsg implements Serializable {
	@Setter
	String content = null, contentType = null, subject;

	Address[] sender;

	List<Attachment> attachments = new LinkedList<>();

	@Setter
	Serializable data; // Convenience

	public MailMsg(String subject, String content, Address[] sender) {
		this.subject = subject;
		this.content = content;
		this.sender = sender;
	}

	public MailMsg(String subject, Address[] sender) {
		this.subject = subject;
		this.sender = sender;
	}

	public void addAttachment(Attachment attachment) {
		attachments.add(attachment);
	}

	@Override
	public String toString() {
		return "from:%s subject: %s\ncontent:%s".formatted(sender[0].toString(), subject ,content);
	}
}
