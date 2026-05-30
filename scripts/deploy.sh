#!/bin/bash
set -e
echo "=== Nexus Isekai Deploy ==="

echo "[1/3] Build server..."
cd server && mvn clean package -DskipTests -q && cd ..

echo "[2/3] Build webshop..."
cd webshop && npm ci --silent && npm run build && cd ..

echo "[3/3] Restart service..."
if systemctl is-active --quiet nexus-server 2>/dev/null; then
    sudo systemctl restart nexus-server
    sleep 2
    systemctl is-active --quiet nexus-server && echo "Service OK" || echo "Check: journalctl -u nexus-server"
else
    echo "Service not running. Start: sudo systemctl start nexus-server"
fi

echo ""
echo "Done! Webshop: http://$(curl -4s ifconfig.me 2>/dev/null || echo YOUR_VPS_IP)"
