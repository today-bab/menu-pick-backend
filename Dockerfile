# 1) 빌드: 실행 가능한 jar를 만들고 Spring Boot 계층별로 풀어 둔다 (테스트는 CI에서 따로 실행)
FROM eclipse-temurin:21-jdk-noble AS builder
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN --mount=type=cache,target=/root/.gradle ./gradlew dependencies --no-daemon -q > /dev/null
COPY src src
RUN --mount=type=cache,target=/root/.gradle ./gradlew bootJar --no-daemon -q \
	&& cp "$(find build/libs -name '*.jar' ! -name '*-plain.jar')" application.jar \
	&& java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# 2) 실행: 자주 바뀌는 계층(application)을 마지막에 두어 이미지 캐시를 살린다
FROM eclipse-temurin:21-jre-noble
WORKDIR /application
RUN useradd --system --uid 1001 --no-create-home app
COPY --from=builder /workspace/extracted/dependencies/ ./
COPY --from=builder /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/extracted/application/ ./
USER 1001
EXPOSE 8080
# 컨테이너 메모리 한도에 맞춰 힙 크기를 정한다
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "application.jar"]
