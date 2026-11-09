#!/usr/bin/env python3
"""Pleos Connect Virtual Console 조작 도구.

에뮬레이터의 Virtual Console(http://localhost:8088)을 명령줄에서 다룬다.
콘솔은 브라우저 세션 하트비트가 있어야 값을 받으므로, 쓰기 명령은 하트비트를 함께 보낸다.

  python3 tools/vc.py list [검색어]                 조작 가능한 기능 목록
  python3 tools/vc.py state [state_key]             현재 상태(JSON)
  python3 tools/vc.py set <기능id> <값> [--area A]  값 하나 바꾸기
  python3 tools/vc.py run <시나리오.json>           시연 시나리오 실행
  python3 tools/vc.py hb                            하트비트만 계속 보내기(Ctrl+C로 종료)

시나리오 형식(JSON 배열):
  [{"set": "gear-selection", "value": "Drive"},
   {"set": "seat-occupancy", "area": "rear_left", "value": true},
   {"wait": 2},
   {"shot": "뒷좌석_착석"},
   {"say": "화면 설명용 메모(로그에만 남음)"}]
"""
import json
import os
import subprocess
import sys
import threading
import time
import urllib.parse
import urllib.request

BASE = os.environ.get("VC_BASE", "http://localhost:8088")
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ADB = os.path.join(
    os.environ.get("ANDROID_SDK_ROOT", "/opt/homebrew/share/android-commandlinetools"),
    "platform-tools", "adb")
CLIENT = "claude-cli"


def http(method, path, timeout=5):
    req = urllib.request.Request(BASE + path, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, r.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()


def ensure_forward():
    try:
        http("GET", "/vehicle/state", timeout=2)
    except Exception:
        subprocess.run([ADB, "forward", "tcp:8088", "tcp:8088"], check=True, capture_output=True)


def heartbeat_forever(stop):
    while not stop.is_set():
        try:
            http("POST", "/browser/session?client=" + CLIENT, timeout=2)
        except Exception:
            pass
        stop.wait(1)


def start_heartbeat():
    stop = threading.Event()
    threading.Thread(target=heartbeat_forever, args=(stop,), daemon=True).start()
    # 콘솔이 브리지를 활성화할 때까지 잠깐 기다린다
    for _ in range(20):
        _, body = http("GET", "/vehicle/state")
        if json.loads(body).get("browser", {}).get("active"):
            break
        time.sleep(0.25)
    return stop


def catalog():
    return json.loads(http("GET", "/vehicle/catalog")[1])


def to_param(v):
    if isinstance(v, bool):
        return "true" if v else "false"
    return str(v)


def parse_value(raw):
    low = raw.lower()
    if low in ("true", "false"):
        return low == "true"
    try:
        return float(raw) if "." in raw else int(raw)
    except ValueError:
        return raw


def build_request(feature, value, area=None):
    ctl = feature.get("control") or {}
    params = dict(ctl.get("params") or {})
    if "field_by_area" in ctl:
        if area is None:
            raise SystemExit(f"{feature['id']}: --area 필요 ({', '.join(ctl['field_by_area'])})")
        params[ctl["field_by_area"][str(area)]] = value
    else:
        params[ctl.get("field") or "value"] = value
        if ctl.get("area_field"):
            if area is None:
                areas = [a["value"] for a in feature.get("areas", [])]
                raise SystemExit(f"{feature['id']}: --area 필요 ({', '.join(areas)})")
            params[ctl["area_field"]] = area
    query = urllib.parse.urlencode({k: to_param(v) for k, v in params.items()})
    return feature["write_endpoint"] + "?" + query


def set_value(cat, fid, value, area=None):
    feats = {f["id"]: f for f in cat["features"]}
    if fid not in feats:
        raise SystemExit(f"없는 기능: {fid} (vc.py list 로 확인)")
    path = build_request(feats[fid], value, area)
    code, body = http("POST", path)
    ok = 200 <= code < 300
    print(f"{'OK ' if ok else 'ERR'} {fid}{f'[{area}]' if area is not None else ''} = {value}  ({code})")
    if not ok:
        print("   ", body[:300])
    return ok


def shot(name):
    out = os.path.join(ROOT, "out", "shots")
    os.makedirs(out, exist_ok=True)
    path = os.path.join(out, time.strftime("%Y%m%d-%H%M%S") + f"_{name}.png")
    with open(path, "wb") as f:
        subprocess.run([ADB, "exec-out", "screencap", "-p"], stdout=f, check=True)
    print("SHOT", path)


def main(argv):
    if not argv or argv[0] in ("-h", "--help"):
        print(__doc__)
        return
    ensure_forward()
    cmd = argv[0]
    if cmd == "list":
        q = argv[1].lower() if len(argv) > 1 else ""
        for f in catalog()["features"]:
            line = f"{f['id']:32} {f.get('domain_label', ''):14} {f.get('label', '')}"
            if q in line.lower():
                ctl = f.get("control") or {}
                opts = [str(o.get("value")) for o in ctl.get("options", [])]
                rng = f"{ctl.get('min')}~{ctl.get('max')}" if "min" in ctl else ""
                areas = [a["value"] for a in f.get("areas", [])]
                print(line, "|", ",".join(opts) or rng, ("| area: " + ",".join(areas)) if areas else "")
    elif cmd == "state":
        s = json.loads(http("GET", "/vehicle/state")[1])
        print(json.dumps(s.get(argv[1]) if len(argv) > 1 else s, ensure_ascii=False, indent=1))
    elif cmd == "set":
        if len(argv) < 3:
            raise SystemExit("사용법: vc.py set <기능id> <값> [--area A]")
        area = argv[argv.index("--area") + 1] if "--area" in argv else None
        stop = start_heartbeat()
        ok = set_value(catalog(), argv[1], parse_value(argv[2]), area)
        stop.set()
        sys.exit(0 if ok else 1)
    elif cmd == "run":
        steps = json.load(open(argv[1]))
        stop = start_heartbeat()
        cat = catalog()
        for st in steps:
            if "set" in st:
                set_value(cat, st["set"], st["value"], st.get("area"))
            elif "wait" in st:
                time.sleep(st["wait"])
            elif "shot" in st:
                shot(st["shot"])
            elif "say" in st:
                print("--", st["say"])
        stop.set()
    elif cmd == "hb":
        stop = threading.Event()
        print("하트비트 전송 중 (Ctrl+C로 종료)")
        try:
            heartbeat_forever(stop)
        except KeyboardInterrupt:
            pass
    else:
        raise SystemExit(f"모르는 명령: {cmd}")


if __name__ == "__main__":
    main(sys.argv[1:])
