#! /bin/sh

WORD_DIR=$(cd $(dirname $0); pwd)
SERVICE_NAME="wvp"

# 检查是否为 root 用户
if [ "$(id -u)" -ne 0 ]; then
  echo "提示: 建议使用 root 用户执行此脚本，否则可能权限不足！"
  read -p "继续？(y/n) " -n 1 -r
  if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    exit 1
  fi
  echo
fi

# 当前目录直接搜索（不含子目录）
jar_files=(*.jar)

if [ ${#jar_files[@]} -eq 0 ]; then
  echo "当前目录无 JAR 文件！"
  exit 1
fi

# 遍历结果
for jar in "${jar_files[@]}"; do
  echo "找到 JAR 文件: $jar"
done

# 写文件
# 生成 Systemd 服务文件内容
SERVICE_FILE="/etc/systemd/system/${SERVICE_NAME}.service"
cat << EOF | sudo tee "$SERVICE_FILE" > /dev/null
[Unit]
Description=${SERVICE_NAME}
After=syslog.target

[Service]
User=$USER
WorkingDirectory=${WORD_DIR}
ExecStart=java -jar ${jar_files}
SuccessExitStatus=143
Restart=on-failure
RestartSec=10s
Environment=SPRING_PROFILES_ACTIVE=prod

[Install]
WantedBy=multi-user.target
EOF

# 重载 Systemd 并启动服务
sudo systemctl daemon-reload
sudo systemctl enable "$SERVICE_NAME"
sudo systemctl start "$SERVICE_NAME"

# 验证服务状态
echo "服务已安装！执行以下命令查看状态:"
echo "sudo systemctl status $SERVICE_NAME"
