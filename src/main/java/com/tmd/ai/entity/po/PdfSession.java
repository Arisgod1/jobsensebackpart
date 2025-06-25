package com.tmd.ai.entity.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PdfSession {
    private String pdfId;
    private String userId;
    private String sessionId;
}
