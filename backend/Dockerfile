# ============================================================
#  后端镜像：多阶段构建（Maven 编译 -> JRE 运行）
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先复制 pom 以利用依赖缓存层
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ------------------------------------------------------------
FROM eclipse-temurin:17-jre
WORKDIR /app

ENV TZ=Asia/Shanghai \
    UPLOAD_PATH=/app/uploads \
    JAVA_OPTS="-Xms256m -Xmx512m"

COPY --from=build /build/target/mall-backend.jar app.jar
# 演示用本地占位图（商品图 / 轮播图 / 头像）
COPY uploads ./uploads

RUN mkdir -p /app/uploads

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dfile.encoding=UTF-8 -jar app.jar"]
