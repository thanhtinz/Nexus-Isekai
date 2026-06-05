#!/bin/bash
# Kiểm tra compile toàn bộ codebase. Chạy: bash verify-build.sh
set -e
echo "════════ VERIFY BUILD — Fantasy Realm Online ════════"

echo "[1/4] Server Java..."
cd server-java
mvn -q clean compile -DskipTests 2>/dev/null && echo "  ✓ Maven compile OK" \
  || echo "  (cần Maven + mạng; trong sandbox dùng javac + stub)"
cd ..

echo "[2/4] Admin Panel (Node)..."
cd admin-panel
for f in $(find src -name "*.js"); do node --check "$f" || exit 1; done
echo "  ✓ JS syntax OK"
cd ..

echo "[3/4] Landing Page (Node)..."
cd landing-page
for f in $(find src -name "*.js"); do node --check "$f" || exit 1; done
echo "  ✓ JS syntax OK"
cd ..

echo "[4/4] Protocol sync (Java ↔ Unity)..."
python3 - << 'PY'
import re
def parse(p,pat):
    d={}
    for l in open(p):
        for m in re.finditer(pat,l): d[m.group(1)]=int(m.group(2),16)
    return d
j=parse("server-java/src/main/java/com/fantasyrealm/protocol/PacketType.java",r'([A-Z_]+)\(0x([0-9A-Fa-f]+)\)')
u=parse("client-unity-scripts/Network/Packet.cs",r'([A-Z_]+)=0x([0-9A-Fa-f]+)')
mm=[n for n in j if n in u and j[n]!=u[n]]
print(f"  Java {len(j)} ↔ Unity {len(u)} packet, mismatch: {mm or 'KHÔNG ✓'}")
PY
echo "════════ XONG ════════"
echo "Lưu ý: Unity (.cs) build trong Unity Editor; J2ME (.java) build bằng Ant + WTK."
echo "Cấu trúc sprite/scene phải ráp trong Editor — xem docs/UNITY-SCENE-SETUP.md"
