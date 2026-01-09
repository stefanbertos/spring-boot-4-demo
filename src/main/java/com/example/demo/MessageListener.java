package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageListener {

    @JmsListener(destination = "${ibm.mq.queue-name}")
    public void receiveMessage(String message) {
        log.info("Received message from queue: {}", message);
    }
}
