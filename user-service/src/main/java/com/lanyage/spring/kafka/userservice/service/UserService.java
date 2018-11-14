package com.lanyage.spring.kafka.userservice.service;

import com.lanyage.spring.kafka.userservice.domain.User;
import com.lanyage.spring.kafka.userservice.msg.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private Source source;
    private List<User> users;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public UserService() {
        this.users = this.create();
    }

    public List<User> findAll() {
        return this.users;
    }

//    public User findOne(Integer itemCode) {
//        for (User user : this.users) {
//            if(user.getId() == itemCode) {
//                return user;
//            }
//        }
//        return null;
//    }

    public User save(User userDTO) {
        for (User user : this.users) {
            if (user.getId() == userDTO.getId()) {
                user.setName(userDTO.getName());
                break;
            }
        }
        this.users.add(userDTO);
        this.sendMsg(UserMessage.MA_UPDATE, userDTO.getId());
        return userDTO;
    }

    private void sendMsg(String maUpdate, Integer id) {
        UserMessage userMessage = new UserMessage(maUpdate, id);
        logger.info("==> 发送商品消息:{} <==", userMessage);
        //发送消息
        this.source.output().send(MessageBuilder.withPayload(userMessage).build());
    }


    List<User> create() {
        List<User> users = new ArrayList<>();
        users.add(new User(1, "兰亚戈"));
        users.add(new User(2, "戴梦晓"));
        return users;
    }
}
