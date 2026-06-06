package com.co.lowcode.sso.config;


import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SmtpMailSender {

	@Autowired
	JavaMailSender javaMailSender;
	@Value("${spring.mail.username}")
	private String from;
	
	public void send(String to, String subject, String body) throws MessagingException{
		MimeMessage message = javaMailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message,true);//true indica que el mensaje es multipart
		helper.setTo(to);
		helper.setFrom(from);
		helper.setSubject(subject );
		helper.setText(body,true);//true indica que es un cuerpo en html
		javaMailSender.send(message);
	}
}
