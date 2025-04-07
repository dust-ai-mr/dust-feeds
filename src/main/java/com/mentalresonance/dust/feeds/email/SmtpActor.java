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
import com.mentalresonance.dust.core.actors.Props;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Very basic SMTP Actor. to/subject/body only for now
 * Todo: use ImapActor's MailMsg
 */
@Slf4j
public class SmtpActor extends Actor {

	Properties properties = new Properties();
	String userName, password, host;

	public static Props props(Map<String, String> smtpConfig) {
		return Props.create(SmtpActor.class, smtpConfig);
	}

	public SmtpActor(Map<String, String> smtpConfig) {
		host = smtpConfig.get("host");
		properties.put("mail.smtp.host", host);
		properties.put("mail.smtp.port", smtpConfig.get("port"));
		properties.put("mail.smtp.ssl.enable", smtpConfig.get("ssl"));
		properties.put("mail.smtp.auth", smtpConfig.get("auth"));
		userName = smtpConfig.get("user");
		password = smtpConfig.get("password");
	}

	@Override
	public ActorBehavior createBehavior() {
		return (Serializable message) -> {
            if (Objects.requireNonNull(message) instanceof MailMsg msg) {
                sendMail(msg);
            } else {
                super.createBehavior().onMessage(message);
            }
		};
	}

	void sendMail(MailMsg msg) {
		try {
			Session session = Session.getInstance(properties, new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(userName, password);
				}
			});
			// Create a default MimeMessage object
			MimeMessage message = new MimeMessage(session);

			// Set the RFC 822 "From" header field using the
			// value of the InternetAddress.getLocalAddress method
			message.setFrom(new InternetAddress(userName));

			// Add the given addresses to the specified recipient type
			message.addRecipient(Message.RecipientType.TO, msg.sender[0]);

			// Set the "Subject" header field
			message.setSubject(msg.subject);

			// Sets the given String as this part's content and type
			message.setContent(msg.content, msg.contentType);

			// Send message
			Transport.send(message);
		} catch (MessagingException mex) {
			mex.printStackTrace();
		}
	}
}
