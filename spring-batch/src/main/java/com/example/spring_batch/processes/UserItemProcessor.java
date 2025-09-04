package com.example.spring_batch.processes;

import com.example.spring_batch.entity.User;
import org.springframework.batch.item.ItemProcessor;

public class UserItemProcessor implements ItemProcessor<User, User> {
    @Override
    public User process(User item) throws Exception {
        item.setFirstName(item.getFirstName().toUpperCase());
        item.setLastName(item.getLastName().toUpperCase());
        return item;
    }
}
