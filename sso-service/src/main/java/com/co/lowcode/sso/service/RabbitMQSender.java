package com.co.lowcode.sso.service;

import java.util.Map;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class RabbitMQSender {
	
	@Autowired
	private AmqpTemplate rabbitTemplate;
	
	@Value("${sso.rabbitmq.exchange}")
	private String exchange;
	
	@Value("${sso.rabbitmq.routingkey}")
	private String routingkey;	
	
	public void send(Map<String, Object> msg) {
		rabbitTemplate.convertAndSend(exchange, routingkey, msg);
		System.out.println("Send msg = " + msg);
	    
	}
}