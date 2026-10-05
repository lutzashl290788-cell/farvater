#!/usr/bin/env python3
# собирает update.json для проверки обновлений в приложении
# аргументы: версия, репозиторий owner/name, тег, файл с изменениями, папка с APK
import datetime
import hashlib
import json
import os
import re
import sys

version, repo, tag, notes_file, dist = sys.argv[1:6]
major, minor, patch = (int(x) for x in version.split("."))

notes, critical = [], False
if os.path.exists(notes_file):
    for line in open(notes_file, encoding="utf-8"):
        line = line.strip()
        # строка #critical помечает срочное исправление
        if line.lower() == "#critical":
            critical = True
        elif line.startswith(("- ", "• ")):
            notes.append(line[2:].strip())

assets = {}
for name in sorted(os.listdir(dist)):
    m = re.fullmatch(rf"Farvater-{re.escape(version)}-(.+)\.apk", name)
    if not m:
        continue
    path = os.path.join(dist, name)
    digest = hashlib.sha256(open(path, "rb").read()).hexdigest()
    assets[m.group(1)] = {
        "url": f"https://github.com/{repo}/releases/download/{tag}/{name}",
        "sha256": digest,
        "size": os.path.getsize(path),
    }

print(json.dumps({
    "versionCode": major * 10000 + minor * 100 + patch,
    "versionName": version,
    "published": datetime.date.today().strftime("%d.%m.%Y"),
    "critical": critical,
    "notes": notes,
    "assets": assets,
}, ensure_ascii=False, indent=2))
