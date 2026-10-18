# ==================== 第一阶段：构建阶段 ====================
# FROM：指定带 Maven + JDK 17 的基础镜像，并命名为 builder
FROM maven:3.9-eclipse-temurin-17 AS builder

# WORKDIR：设置容器内工作目录（不存在则自动创建）
WORKDIR /build

# 先拷贝 pom.xml：只复制依赖描述文件，便于复用 Docker 层缓存
COPY pom.xml ./

# RUN：预下载依赖
RUN mvn -B -q dependency:go-offline

# 再拷贝源码
COPY src ./src

# 打包（跳过测试），生成 target/*.jar
RUN mvn -B -q clean package -DskipTests

# ==================== 第二阶段：运行阶段（只保留 JRE，减小体积） ====================
FROM eclipse-temurin:17-jre

# ENV：设置环境变量（时区）
ENV TZ=Asia/Shanghai

WORKDIR /app

# COPY --from：从上一阶段拷贝构建产物，重命名为 app.jar
COPY --from=builder /build/target/*.jar app.jar

# EXPOSE：声明容器对外提供的端口
EXPOSE 8080

# VOLUME：声明匿名数据卷（存放临时文件）
VOLUME /tmp

# ENTRYPOINT：容器启动时固定执行的命令
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# CMD：给 ENTRYPOINT 提供默认参数，可被 docker run 末尾参数覆盖
CMD ["--server.port=8080"]
