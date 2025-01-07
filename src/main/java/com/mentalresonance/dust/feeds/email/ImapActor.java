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

import com.mentalresonance.dust.core.actors.Actor;
import com.mentalresonance.dust.core.actors.ActorBehavior;
import com.mentalresonance.dust.core.actors.ActorRef;
import com.mentalresonance.dust.core.actors.Props;
import com.mentalresonance.dust.core.msgs.StartMsg;
import jakarta.mail.*;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.search.FlagTerm;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Create an Imap client for a single email inbox.
 */
@Slf4j
public class ImapActor extends Actor {

	Properties properties = new Properties();
	String userName, password, host;
	ActorRef clientRef;
	Long scheduleIn;

	/**
	 * Create
	 * @param imapConfig contains host:, user: password; port:, ssl: true/false, scheduleIn: ms to schedule
	 * @return Props
	 */
	public static Props props(Map<String, String> imapConfig) {
		return Props.create(ImapActor.class, imapConfig);
	}

	/**
	 * Construct
	 * @param imapConfig see props
	 */
	public ImapActor(Map<String, String> imapConfig) {
		host = imapConfig.get("host");
		userName = imapConfig.get("user");
		password = imapConfig.get("password");
		properties.put("mail.store.protocol", "imap");
		properties.put("mail.imap.host", host);
		properties.put("mail.imap.port", imapConfig.get("port"));
		properties.put("mail.imap.ssl.enable", imapConfig.get("ssl"));
		// Ensure schedulein is a String in config source !
		scheduleIn = Long.parseLong(imapConfig.get("schedulein"));
	}


	@Override
	public ActorBehavior createBehavior() {
		return (Serializable message) -> {
            if (Objects.requireNonNull(message) instanceof StartMsg) {
                clientRef = (ActorRef)((StartMsg)message).getMsg();
                scheduleIn(message, scheduleIn);
                getMail();
            } else {
                super.createBehavior().onMessage(message);
            }
		};
	}

	private void getMail() {
		try {
			Session session = Session.getDefaultInstance(properties);
			Store store = session.getStore();
			store.connect(host, userName, password);

			Folder inbox = store.getFolder("INBOX");
			inbox.open(Folder.READ_WRITE);

			Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
			if (messages.length > 0)
				log.info("Number of unread messages: {}", messages.length);

			// Just reading the first message as an example
			for (Message message : messages) {
				String subject = message.getSubject();
				Address[] from = message.getFrom();
				Object content = message.getContent();
				MailMsg mailmsg = new MailMsg(subject, from);

				if (content instanceof Multipart) { // else a String
					Multipart multipart = (Multipart) content;
					for (int i = 0; i < multipart.getCount(); i++) {
						BodyPart bodyPart = multipart.getBodyPart(i);
						if (bodyPart instanceof MimeBodyPart) {
							MimeBodyPart mimeBodyPart = (MimeBodyPart) bodyPart;
							if (Part.ATTACHMENT.equalsIgnoreCase(mimeBodyPart.getDisposition())) {
								String filename = mimeBodyPart.getFileName();
								log.info("Attachment: %s".formatted(filename));
								byte[] bytes = mimeBodyPart.getInputStream().readAllBytes();
								mailmsg.addAttachment(new Attachment(filename, mimeBodyPart.getContentType(), bytes));
							} else {
								mailmsg.setContentType(mimeBodyPart.getContentType());
								mailmsg.setContent(mimeBodyPart.getContent().toString());
							}
						}
					}
				} else {
					mailmsg.setContentType(message.getContentType());
					mailmsg.setContent(message.getContent().toString());
				}
				message.setFlag(Flags.Flag.SEEN, true);
				clientRef.tell(mailmsg, self);
			}
			inbox.close(false);
			store.close();
		}
		catch (MessagingException e) {
			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
