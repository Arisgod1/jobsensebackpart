package com.tmd.ai.controller;

import com.tmd.ai.constants.SystemConstants;
import com.tmd.ai.entity.vo.PDFVO;
import com.tmd.ai.entity.vo.Result;
import com.tmd.ai.repository.ChatHistoryRepository;
import com.tmd.ai.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/pdf")
public class InterviewController {

    private final ChatClient chatClient;

    private final ChatHistoryRepository chatHistoryRepository;

    private final FileRepository fileRepository;

    private String pdfContent;

    @PostMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> chat(@RequestBody PDFVO pdfVO){
        String prompt = pdfVO.getPrompt();
        String chatId = pdfVO.getChatId();
        //1.保存会话ID
        chatHistoryRepository.save("interview",chatId);
        //2.请求模型
        return chatClient.prompt()
                .system(SystemConstants.PDF_SYSTEM_PROMPT)
                .user(prompt)
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY,chatId))
                .stream()
                .content();
    }
    public String chatSync(String prompt, String chatId) {
        // 1.保存会话ID
        chatHistoryRepository.save("interview", chatId);
        // 2.请求模型，取消流式，直接获取完整字符串
        return chatClient.prompt()
                .system(SystemConstants.PDF_SYSTEM_PROMPT)
                .user(prompt)
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
                .call()
                .content();
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
            this.pdfContent = this.writeToVectorStore(file.getResource());
            return Result.success(chatSync("以下为简历内容" + pdfContent, chatId));
        } catch (Exception e) {
            return Result.error("上传文件失败！");
        }
    }

    /**
     * 文件下载
     */
    @GetMapping("/file/{chatId}")
    public ResponseEntity<Resource> download(@PathVariable("chatId") String chatId){
        // 1.读取文件
        Resource resource = fileRepository.getFile(chatId);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        // 2.文件名编码，写入响应头
        String filename = URLEncoder.encode(Objects.requireNonNull(resource.getFilename()), StandardCharsets.UTF_8);
        // 3.返回文件
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(resource);
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