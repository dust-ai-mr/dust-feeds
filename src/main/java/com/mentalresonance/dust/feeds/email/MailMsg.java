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
