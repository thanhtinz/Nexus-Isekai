#!/bin/bash
# setup-vps.sh — Cài môi trường trên VPS Ubuntu 22.04 mới
# Chạy lần đầu: chmod +x scripts/setup-vps.sh && sudo ./scripts/setup-vps.sh

set -e

echo "=== Nexus Isekai VPS Setup ==="

# Update
apt update && apt upgrade -y

# Java 17
apt install -y openjdk-17-jdk
echo "Java: $(java -version 2>&1 | head -1)"

# Maven
apt install -y maven

# Node.js 20
curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
apt install -y nodejs
echo "Node: $(node -v)"

# MySQL 8
apt install -y mysql-server
systemctl start mysql && systemctl enable mysql

# Nginx
apt install -y nginx
systemctl enable nginx

# Certbot
apt install -y certbot python3-certbot-nginx

# UFW Firewall
ufw --force enable
ufw allow ssh
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow 7777/tcp    # Game server
echo "Firewall configured"

# Log directory
mkdir -p /var/log/nexus
echo "Log dir: /var/log/nexus"

echo ""
echo "=== VPS Setup Complete! ==="
echo "Next: Configure application.properties then run deploy.sh"
