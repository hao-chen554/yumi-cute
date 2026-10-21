package com.yumi.cute.controller;

import com.yumi.cute.common.Result;
import com.yumi.cute.common.UserContext;
import com.yumi.cute.service.FileService;
import com.yumi.cute.util.FileSigner;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
public class FileController {

    private final FileService fileService;
    private final FileSigner fileSigner;

    public FileController(FileService fileService, FileSigner fileSigner) {
        this.fileService = fileService;
        this.fileSigner = fileSigner;
    }

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        Long userId = UserContext.getUserId();
        // 落盘和登记属主是裸地址，返回给前端的是带短时签名的地址
        return Result.success(fileSigner.sign(fileService.upload(userId, file)));
    }
}