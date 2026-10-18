# 后端项目 Docker 化实验操作指南

> 目标：把 Spring Boot 后端项目用 git 打包上传，再在 Linux 虚拟机上用 Docker 构建镜像、运行容器并验证。
> 实验重点：Dockerfile 指令（FROM/RUN/COPY/ADD/WORKDIR）、进阶指令（ENV/EXPOSE/VOLUME）、CMD 与 ENTRYPOINT 区别、多阶段构建。

---

## 0. 项目现状

- 项目：校园实训任务管理系统（CTTS）后端
- 技术栈：Spring Boot 4.1.1 + JDK 17 + Maven + MyBatis-Plus
- 打包产物：`target/demo-0.0.1-SNAPSHOT.jar`
- 说明：当前接口用的是**内存模拟数据**，不依赖 MySQL，所以容器内**不需要数据库也能正常启动并访问**。

---

## 一、本地把项目打包为 git 并上传

在项目根目录 `demo` 下打开终端（Windows 可用 Git Bash / PowerShell）。

```bash
# 1. 初始化 git 仓库（首次需要配置身份，可跳过若已配置）
git config --global user.name "你的名字"
git config --global user.email "你的邮箱"

# 2. 初始化仓库并提交
git init
git add .
git commit -m "校园实训任务管理系统后端"

# 3. 关联远程仓库（先在 GitHub / Gitee 网页上新建一个空仓库）
git remote add origin <你的仓库地址>
git branch -M main
git push -u origin main
```

> 说明：项目自带的 `.gitignore` 已经忽略了 `target/`，所以只上传源码，不传 jar——jar 会在虚拟机里由 Docker 现场构建。

---

## 二、虚拟机安装 Docker 并克隆代码

登录 Linux 虚拟机（Ubuntu/Debian 为例）。

```bash
# 1. 安装 Docker（任选一种）
# 方式 A：官方一键脚本
curl -fsSL https://get.docker.com | bash
# 方式 B：apt 安装
sudo apt-get update
sudo apt-get install -y docker.io git
sudo systemctl enable --now docker

# 2. 验证 Docker
docker version
docker run hello-world

# 3. 克隆代码
git clone <你的仓库地址>
cd demo
```

---

## 三、编写 Dockerfile（核心）

在项目根目录新建 `Dockerfile`（内容已放入本项目）：

```dockerfile
# ============ 第一阶段：构建 ============
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ============ 第二阶段：运行 ============
FROM eclipse-temurin:17-jre
ENV TZ=Asia/Shanghai
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8080
VOLUME /tmp
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
CMD ["--server.port=8080"]
```

### 指令含义对照（实验目的 1、2）

| 指令 | 作用 | 示例 |
|------|------|------|
| `FROM` | 指定基础镜像；`AS` 命名阶段 | `FROM maven:... AS builder` |
| `WORKDIR` | 设置工作目录（不存在自动创建） | `WORKDIR /build` |
| `RUN` | 构建时执行命令（常用于安装/编译） | `RUN mvn -q clean package` |
| `COPY` | 复制本地文件到镜像 | `COPY src ./src` |
| `ADD` | COPY 增强版：可自动解压 tar、支持远程 URL | `ADD xxx.tar.gz /app` |
| `ENV` | 设置环境变量，运行时可读取 | `ENV TZ=Asia/Shanghai` |
| `EXPOSE` | 声明容器监听端口（文档作用） | `EXPOSE 8080` |
| `VOLUME` | 声明数据卷，数据持久化 | `VOLUME /tmp` |

### CMD 与 ENTRYPOINT 的区别（实验目的 2）

| | `ENTRYPOINT` | `CMD` |
|--|--|--|
| 定位 | 容器启动时**固定**执行的命令 | 提供**默认参数**或默认命令 |
| 能否被覆盖 | 需 `--entrypoint` 才能覆盖 | 会被 `docker run` 末尾参数直接覆盖 |
| 组合效果 | `ENTRYPOINT ["java","-jar","app.jar"]` | `CMD ["--server.port=8080"]` |

示例：
- `docker run ctts` → 实际执行 `java -jar app.jar --server.port=8080`
- `docker run ctts --server.port=9090` → 覆盖 CMD，执行 `... --server.port=9090`

### COPY 与 ADD 的区别

- `COPY`：只做文件复制，行为简单，**优先使用**。
- `ADD`：额外支持**自动解压 tar 包**、从**远程 URL** 下载文件；不需要这两个特性时一律用 COPY。

---

## 四、.dockerignore

新建 `.dockerignore`（已放入本项目），排除不需要进入构建上下文的文件，减小上下文、加快构建：

```
target/
.git/
.idea/
*.iml
*.md
```

---

## 五、构建镜像（多阶段构建，实验目的 3）

```bash
# 在项目根目录执行（注意最后的点 .）
docker build -t ctts-backend:1.0 .

# 查看构建出的镜像
docker images
```

### 多阶段构建的作用

- **第一阶段**（`maven:...` 镜像）体积约 700MB，负责编译打包。
- **第二阶段**（`eclipse-temurin:17-jre`）只保留运行需要的 JRE，**不包含 Maven、JDK 和源码**，镜像体积可降到约 250MB 左右。
- 用 `COPY --from=builder ...` 把第一阶段的 jar 取到第二阶段，最终镜像只含 JRE + jar，**大幅减小体积**。

---

## 六、运行容器并验证（实验目的 3）

```bash
# 1. 后台运行容器，把宿主机 8080 映射到容器 8080
docker run -d --name ctts -p 8080:8080 ctts-backend:1.0

# 2. 查看容器状态
docker ps

# 3. 查看启动日志（看到 Started DemoApplication 即成功）
docker logs -f ctts

# 4. 验证接口
curl http://localhost:8080/api/projects
curl http://localhost:8080/api/projects/1
curl "http://localhost:8080/api/projects/page?page=1&size=2"
```

预期返回 JSON（项目列表 / 单条项目 / 分页结果）。

### 其它常用命令

```bash
docker stop ctts          # 停止容器
docker start ctts         # 再次启动
docker rm -f ctts         # 删除容器
docker rmi ctts-backend:1.0   # 删除镜像
docker exec -it ctts sh   # 进入容器
```

---

## 七、注意事项

1. **MySQL 连接问题**：当前 `application.yml` 里写的是 `localhost:3306`。容器内的 `localhost` 指向容器自身，不是宿主机。由于本项目接口用内存模拟数据、暂不连库，所以不影响验证。若后续要连宿主机 MySQL，把地址改为 `host.docker.internal`（Mac/Win）或宿主机 IP / 容器网络名即可。
2. **构建需要联网**：`docker build` 会拉取基础镜像、`mvn` 会下载依赖，确保虚拟机能访问外网。
3. **端口冲突**：宿主机 8080 被占用时，改成 `-p 9090:8080`，再用 `http://localhost:9090` 访问。
4. **时区/中文**：已通过 `ENV TZ=Asia/Shanghai` 设置时区，避免时间偏差。

---

## 附：完整操作流程图

```
本地 Windows                远程仓库                 Linux 虚拟机
   git init  ──────────────►  GitHub/Gitee  ◄──────  git clone
   git add/commit/push                                  │
                                                        ▼
                                              docker build -t ctts-backend .
                                                        │
                                                        ▼
                                              docker run -d -p 8080:8080 ...
                                                        │
                                                        ▼
                                              curl 验证 /api/projects
```
