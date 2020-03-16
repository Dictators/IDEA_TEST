package com.example.demo.controller;

import com.example.demo.Entity.User;
import com.example.demo.mapper.userMapper;
import com.example.demo.service.userService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class controller {

    @Autowired
    private com.example.demo.mapper.userMapper userMapper;

    @RequestMapping("/demo")
    @ResponseBody
    public List<User> userSelect(){

        List<User> userlist= userMapper.selectALL();
        return userlist;
    }
}
