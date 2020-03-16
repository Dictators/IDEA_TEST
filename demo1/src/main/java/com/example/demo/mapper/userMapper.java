package com.example.demo.mapper;

import com.example.demo.Entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface userMapper {
    public List<User> selectALL();
}
