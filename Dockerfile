# 1. 베이스 이미지: Amazon Corretto 17 (안정적이고 가벼운 리눅스 버전)
FROM amazoncorretto:17

# 2. 작업 폴더 설정
WORKDIR /app

# 3. 우리가 만든 JAR 파일을 컨테이너 안으로 복사
COPY build/libs/migrationproject-0.0.1-SNAPSHOT.jar app.jar

# 4. 파일 다운로드를 위한 임시 폴더 생성
RUN mkdir -p local-download

# 5. 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]