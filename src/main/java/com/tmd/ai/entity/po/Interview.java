package com.tmd.ai.entity.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Interview {
    private int interviewId;
    private int userId;
    private String judge;
    private LocalDateTime createTime;
}
