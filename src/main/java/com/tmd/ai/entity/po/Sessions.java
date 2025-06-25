package com.tmd.ai.entity.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sessions {
    private String sessionId;
    private int userId;
    private String content;
    private LocalDateTime createTime;
}
