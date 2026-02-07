package com.example.migration.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class BlobService {

    private final BlobServiceClient blobServiceClient;
    
    @Value("${spring.cloud.azure.storage.blob.container-name}")
    private String containerName;

    // 생성자 주입 (Spring Boot가 알아서 클라이언트를 만들어 넣어줍니다)
    public BlobService(BlobServiceClient blobServiceClient) {
        this.blobServiceClient = blobServiceClient;
    }

   public void uploadFile(File file) {
        try {
            // 1. 컨테이너 클라이언트 생성
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            
            // ▼ [추가] 컨테이너가 없으면 만듭니다. (이게 있어야 에러 안 남!)
            if (!containerClient.exists()) {
                containerClient.create();
                System.out.println(">>> [초기화] '" + containerName + "' 컨테이너 생성 완료");
            }

            // 2. 파일명으로 블롭 클라이언트 생성
            BlobClient blobClient = containerClient.getBlobClient(file.getName());

            // 3. 업로드 실행 (overwrite: true)
            blobClient.uploadFromFile(file.getAbsolutePath(), true);
            
            System.out.println(">>> [Azurite 업로드 성공] " + file.getName() + " -> 완료!");
            
            // 4. (선택) 로컬 파일 삭제는 테스트니까 일단 주석 처리
            // file.delete(); 

        } catch (Exception e) {
            System.err.println(">>> [업로드 실패] " + e.getMessage());
            e.printStackTrace();
        }
    }

}
