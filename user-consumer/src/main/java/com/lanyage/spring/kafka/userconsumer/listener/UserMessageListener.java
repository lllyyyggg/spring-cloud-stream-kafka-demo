package com.lanyage.spring.kafka.userconsumer.listener;

import com.lanyage.spring.kafka.userconsumer.domain.User;
import com.lanyage.spring.kafka.userconsumer.msg.UserMessage;
import com.lanyage.spring.kafka.userconsumer.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;

@EnableBinding(Sink.class)
public class UserMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(UserMessageListener.class);

    @Autowired
    private UserService userService;

    @StreamListener(Sink.INPUT)
    public void onUserMessage(UserMessage userMessage) {
        logger.info("==>>>>>>    {}   <<<<<<==", userMessage);
        logger.info("==>>>>>>    {}   <<<<<<==", userMessage.getAction().equals(UserMessage.MA_UPDATE));
        if (UserMessage.MA_UPDATE.equals(userMessage.getAction())) {
            logger.info("==> 收到商品的变更消息，商品货号为{} <==", userMessage.getItemCode());
        } else if (UserMessage.MA_DELETE.equals(userMessage.getAction())) {
            logger.info("==> 收到商品删除消息, 所要删除商品货号为: {} <==", userMessage.getItemCode());
        } else {
            logger.info("==> 收到未知商品的消息:{} <==", userMessage);
        }
    }
}
