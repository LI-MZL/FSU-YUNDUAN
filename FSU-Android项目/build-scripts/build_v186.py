#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Build V186 APK — v186 深度审计+死代码清理
  P0 结构: renderUpdateLog未闭合导致7个函数被嵌套(已移至顶层)
  P0 init(): startSCPing(fp)未定义fp(改S.fp)
  P1 goOffline: 补stopAlarmTimers/cancelVPNReconnect
  P1 fullStateReset: 修复_httpServerPeriodicTmr泄漏
  P1 自检: 不存在的vpnReconnect改scheduleVPNReconnect
  P1 隐式全局4个显式声明
  P2 删除15个死函数+2个死变量+重复键
  P2 OTA指令静默失败改为明确不支持
流程: 重打包(替换html x2,剥离旧签名) → zipalign → apksigner(v1+v2+v3)
"""
import os, sys, zipfile, subprocess, tempfile

BASE_APK = "/workspace/FSU_v185.apk"
NEW_HTML = "/workspace/云端FSU_v1.0_完整版_v113_source_index.html"
OUT_APK  = "/workspace/FSU_v186.apk"
TOOLS    = "/workspace/.build_tools"
KEY_PEM  = "/workspace/.build_keys/fsu_key.pem"
CERT_PEM = "/workspace/.build_keys/fsu_cert.pem"

for f in [BASE_APK, NEW_HTML, KEY_PEM, CERT_PEM]:
    if not os.path.exists(f):
        print("ERROR: 文件不存在: " + f); sys.exit(1)

env = dict(os.environ)
env["LD_LIBRARY_PATH"] = os.path.join(TOOLS, "lib64")

def run(cmd):
    print("$", " ".join(cmd))
    r = subprocess.run(cmd, env=env, capture_output=True, text=True)
    if r.stdout.strip(): print(r.stdout.strip())
    if r.stderr.strip(): print(r.stderr.strip())
    if r.returncode != 0:
        print("ERROR: 命令失败(%d)" % r.returncode); sys.exit(1)
    return r

with open(NEW_HTML, "rb") as f: new_html = f.read()
print("新HTML: %d bytes" % len(new_html))
assert b"V2.186.00.00" in new_html, "版本号缺失"

REPLACE = {
    "assets/public/index.html": new_html,
    "org/apache/cordova/allowlist/index.html": new_html,
}

def is_sig(name):
    if not name.startswith("META-INF/"): return False
    if name == "META-INF/MANIFEST.MF": return True
    return name.endswith((".SF", ".RSA", ".DSA", ".EC"))

work = tempfile.mkdtemp(prefix="fsu186_")
unsigned = os.path.join(work, "unsigned.apk")
cnt, replaced = 0, []
with zipfile.ZipFile(BASE_APK, 'r') as zin, zipfile.ZipFile(unsigned, 'w') as zout:
    for info in zin.infolist():
        name = info.filename
        if is_sig(name): continue
        data = REPLACE.get(name, zin.read(name))
        if name in REPLACE: replaced.append(name)
        ni = zipfile.ZipInfo(name, date_time=info.date_time)
        ni.compress_type = info.compress_type
        ni.external_attr = info.external_attr
        ni.create_system = 0
        zout.writestr(ni, data)
        cnt += 1
print("条目:", cnt, "替换:", replaced)
assert len(replaced) == 2

aligned = os.path.join(work, "aligned.apk")
run([os.path.join(TOOLS, "zipalign"), "-f", "-p", "4", unsigned, aligned])
run([os.path.join(TOOLS, "zipalign"), "-c", "-p", "4", aligned])

pk8 = os.path.join(work, "fsu_key.pk8")
run(["openssl", "pkcs8", "-topk8", "-nocrypt", "-in", KEY_PEM, "-outform", "DER", "-out", pk8])
run([os.path.join(TOOLS, "apksigner"), "sign",
     "--key", pk8, "--cert", CERT_PEM,
     "--v1-signing-enabled", "true", "--v2-signing-enabled", "true",
     "--v3-signing-enabled", "true", "--out", OUT_APK, aligned])
run([os.path.join(TOOLS, "apksigner"), "verify", "--print-certs", OUT_APK])

print("=" * 60)
print("V186 APK: %s (%.2f MB)" % (OUT_APK, os.path.getsize(OUT_APK) / 1048576.0))
