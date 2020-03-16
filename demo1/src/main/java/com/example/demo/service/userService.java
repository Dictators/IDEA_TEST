package com.example.demo.service;

import com.example.demo.Entity.User;
import com.example.demo.mapper.userMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class userService {
    @Autowired
    private userMapper userMapper;

    public List<User> select(){
        return   userMapper.selectALL();
    }
}
