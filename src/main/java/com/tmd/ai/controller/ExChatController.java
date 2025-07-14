package com.tmd.ai.controller;

import com.tmd.ai.service.AI;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.tmd.ai.constants.SystemConstants.CHAT_SYSTEM_PROMPT;

@RestController
@Api("聊天接口")
@RequestMapping("/ai")
@Slf4j
@CrossOrigin
public class ExChatController {
    @RequestMapping("/chat")
    private String handleQuestion(String question,String chatId) throws Exception {
        AI ai = new AI();
        return ai.chat(question,chatId,CHAT_SYSTEM_PROMPT);
    }

}