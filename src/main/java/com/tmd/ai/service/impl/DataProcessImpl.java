package com.tmd.ai.service.impl;

import com.tmd.ai.entity.po.Sessions;
import com.tmd.ai.mapper.DataProcessMapper;
import com.tmd.ai.service.DataProcess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DataProcessImpl implements DataProcess {
    
    @Autowired
    private DataProcessMapper dataProcessMapper;
    
    @Override
    public void addSession(String body, int userId) {


        dataProcessMapper.addSession();
    }
}
