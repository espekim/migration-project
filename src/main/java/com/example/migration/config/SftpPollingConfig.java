package com.example.migration.config;


import org.apache.sshd.sftp.client.SftpClient; // JSch 대신 이거 씁니다 (Apache MINA)
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizingMessageSource;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizer;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory; // 다시 이거 씁니다
import org.springframework.messaging.MessageHandler;

import java.io.File;
import com.example.migration.service.BlobService;

@Configuration
public class SftpPollingConfig {
	
	private final BlobService blobService;
	
	@Value("${spring.integration.sftp.host:localhost}")
    private String sftpHost;

    @Value("${spring.integration.sftp.port:2222}")
    private int sftpPort;

    @Value("${spring.integration.sftp.user:user}")
    private String sftpUser;

    @Value("${spring.integration.sftp.password:pass}")
    private String sftpPassword;

    // 생성자 주입
    public SftpPollingConfig(BlobService blobService) {
        this.blobService = blobService;
    }


    // 1. SFTP 접속 정보 (Spring Boot 3부터는 Default가 Apache MINA 기반임)
    @Bean
    public SessionFactory<SftpClient.DirEntry> sftpSessionFactory() {
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory();
        factory.setHost(sftpHost);
        factory.setPort(sftpPort);
        factory.setUser(sftpUser);
        factory.setPassword(sftpPassword);
        factory.setAllowUnknownKeys(true);
        
        // 제네릭 타입을 <SftpClient.DirEntry>로 명시
        return new CachingSessionFactory<>(factory);
    }

    // 2. 동기화 설정
    @Bean
    public SftpInboundFileSynchronizer sftpInboundFileSynchronizer() {
        // 이제 타입이 SftpClient.DirEntry로 딱 맞아서 오류가 안 납니다.
        SftpInboundFileSynchronizer synchronizer = new SftpInboundFileSynchronizer(sftpSessionFactory());
        synchronizer.setDeleteRemoteFiles(false);
        synchronizer.setRemoteDirectory("upload");
        return synchronizer;
    }

    // 3. 폴링 트리거 (1초 간격)
    @Bean
    @InboundChannelAdapter(channel = "sftpChannel", poller = @Poller(fixedDelay = "1000"))
    public MessageSource<File> sftpMessageSource() {
        SftpInboundFileSynchronizingMessageSource source =
                new SftpInboundFileSynchronizingMessageSource(sftpInboundFileSynchronizer());
        
        source.setLocalDirectory(new File("local-download"));
        source.setAutoCreateLocalDirectory(true);
        return source;
    }

    // 4. 처리기 (로그 출력)
    @Bean
    @ServiceActivator(inputChannel = "sftpChannel")
    public MessageHandler handler() {
        return message -> {
            File file = (File) message.getPayload();
            System.out.println("=========================================");
            System.out.println(">>> [Apache MINA] 파일 감지 성공: " + file.getName());
            //System.out.println(">>> [경로] " + file.getAbsolutePath());
            
            blobService.uploadFile(file);
            System.out.println("=========================================");
        };
    }
}