package com.tmd.ai.controller;

import com.tmd.ai.constants.SystemConstants;
import com.tmd.ai.entity.vo.PDFVO;
import com.tmd.ai.entity.vo.Result;
import com.tmd.ai.repository.ChatHistoryRepository;
import com.tmd.ai.repository.FileRepository;
import com.tmd.ai.service.AI;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;

import static com.tmd.ai.constants.SystemConstants.PDF_SYSTEM_PROMPT;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/pdf")
public class InterviewController {

    private final ChatClient chatClient;

    private final ChatHistoryRepository chatHistoryRepository;

    private final FileRepository fileRepository;

    public static String pdfContent;

    @PostMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> chat(@RequestBody PDFVO pdfVO){
        String prompt = pdfVO.getPrompt();
        String chatId = pdfVO.getChatId();
        //1.保存会话ID
        chatHistoryRepository.save("interview",chatId);
        //2.请求模型
        return chatClient.prompt()
                .system(PDF_SYSTEM_PROMPT)
                .user(prompt)
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY,chatId))
                .stream()
                .content();
    }
    public String chatSync(String prompt, String chatId) throws Exception {
        // 1.保存会话ID
        chatHistoryRepository.save("interview", chatId);
        // 2.请求模型，取消流式，直接获取完整字符串
//        return chatClient.prompt()
//                .system(SystemConstants.PDF_SYSTEM_PROMPT)
//                .user(prompt)
//                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
//                .call()
//                .content();
        AI ai = new AI();
        String input = ai.chat(prompt, chatId,PDF_SYSTEM_PROMPT);
        // 去掉开头的 ```json
        if (input.startsWith("```json")) {
            input = input.substring("```json".length());
        }

        // 去掉结尾的 ```
        if (input.endsWith("```")) {
            input = input.substring(0, input.length() - 3);
        }
        return input;
    }

    /**
     * 文件上传
     */
    @RequestMapping("/upload/{chatId}")
    public Result uploadPdf(@PathVariable String chatId, @RequestParam("file") MultipartFile file) {
        try {
            // 1. 校验文件是否为PDF格式
            if (!Objects.equals(file.getContentType(), "application/pdf")) {
                return Result.error("只能上传PDF文件！");
            }
            // 2.保存文件
            boolean success = fileRepository.save(chatId, file.getResource());
            if (!success) {
                return Result.error("保存文件失败！");
            }
            // 3.写入pdfContent
            pdfContent = this.writeToVectorStore(file.getResource());
            return Result.success(chatSync("以下为简历内容" + pdfContent, chatId));
        } catch (Exception e) {
            return Result.error("上传文件失败！");
        }
    }


    private String writeToVectorStore(Resource resource) {
        // 1.创建PDF的读取器
        PagePdfDocumentReader reader = new PagePdfDocumentReader(
                resource, // 文件源
                PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults())
                        .withPagesPerDocument(1) // 每1页PDF作为一个Document
                        .build()
        );
        // 2.读取PDF文档，拆分为Document
        List<Document> documents = reader.read();

        return documents.toString();
    }
}