"""WebUI HTTP 服务：基于标准库 http.server 的无地图信息面板。

路由：
    GET  /                  -> index.html
    GET  /static/<file>     -> ck_engine/web/static/ 下的静态资源
    GET  /api/snapshot      -> 完整游戏状态 JSON（GameAPI.snapshot()）
    GET  /api/scenarios     -> 可选场景列表
    POST /api/action        -> 执行操作（body: {"action": ..., ...}），返回新快照

启动方式：
    python -m ck_engine.web.server [--host 127.0.0.1] [--port 8000] [--scenario 1066]

对外导出 CKWebServer 供嵌入式集成（如 Jupyter / 自有主程序）。
"""

from __future__ import annotations

import argparse
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any, Dict, List, Tuple
from urllib.parse import unquote, urlparse

from ck_engine.game.scenario_loader import list_scenarios
from ck_engine.ui.api import GameAPI

STATIC_DIR = Path(__file__).resolve().parent / "static"

CONTENT_TYPES = {
    ".html": "text/html; charset=utf-8",
    ".css": "text/css; charset=utf-8",
    ".js": "application/javascript; charset=utf-8",
    ".json": "application/json; charset=utf-8",
    ".svg": "image/svg+xml",
    ".png": "image/png",
    ".jpg": "image/jpeg",
    ".ico": "image/x-icon",
}


def _json_safe(obj: Any) -> Any:
    """把快照/场景数据中的非 JSON 类型（元组、Path、集合）递归转为可序列化结构。"""
    if isinstance(obj, dict):
        out: Dict[str, Any] = {}
        for k, v in obj.items():
            if isinstance(k, tuple):
                key = ",".join(str(x) for x in k)
            else:
                key = str(k)
            out[key] = _json_safe(v)
        return out
    if isinstance(obj, (list, tuple, set)):
        return [_json_safe(x) for x in obj]
    if isinstance(obj, Path):
        return str(obj)
    return obj


def _dump(obj: Any) -> str:
    return json.dumps(_json_safe(obj), ensure_ascii=False)


class CKWebServer(ThreadingHTTPServer):
    """CK 游戏 WebUI 服务器：持有单个 GameAPI，提供信息查询与操作接口。"""

    daemon_threads = True
    allow_reuse_address = True

    def __init__(
        self,
        host: str = "127.0.0.1",
        port: int = 8000,
        scenario: str | None = None,
    ) -> None:
        self.api = GameAPI(scenario)
        self.scenario_id = self.api.sim.scenario_id
        super().__init__((host, port), _Handler)

    @property
    def url(self) -> str:
        host, port = self.server_address[:2]
        return f"http://{host}:{port}"

    def serve_forever_in_thread(self) -> None:
        """嵌入式场景：在后台线程启动服务。"""
        import threading

        threading.Thread(target=self.serve_forever, daemon=True).start()


class _Handler(BaseHTTPRequestHandler):
    server_version = "CKWebServer/1.0"

    def log_message(self, fmt: str, *args: Any) -> None:  # 安静访问日志
        pass

    # ---------- 响应辅助 ----------
    def _send_bytes(self, body: bytes, status: int, ctype: str) -> None:
        self.send_response(status)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def _send_json(self, data: Any, status: int = 200) -> None:
        body = _dump(data).encode("utf-8")
        self._send_bytes(body, status, "application/json; charset=utf-8")

    def _send_text(self, text: str, status: int = 200, ctype: str = "text/plain; charset=utf-8") -> None:
        self._send_bytes(text.encode("utf-8"), status, ctype)

    def _send_file(self, path: Path) -> None:
        try:
            data = path.read_bytes()
        except OSError:
            self._send_text("404 Not Found", 404)
            return
        self._send_bytes(data, 200, CONTENT_TYPES.get(path.suffix.lower(), "application/octet-stream"))

    # ---------- 路由 ----------
    def do_GET(self) -> None:
        parsed = urlparse(self.path)
        path = unquote(parsed.path)
        if path == "/" or path == "/index.html":
            self._send_file(STATIC_DIR / "index.html")
        elif path.startswith("/static/"):
            self._serve_static(path[len("/static/"):])
        elif path == "/api/snapshot":
            self._send_json(self.server.api.snapshot())
        elif path == "/api/scenarios":
            self._send_json(list_scenarios())
        else:
            self._send_text("404 Not Found", 404)

    def _serve_static(self, rel: str) -> None:
        if not rel or ".." in rel or rel.startswith("/") or "\\" in rel:
            self._send_text("403 Forbidden", 403)
            return
        self._send_file(STATIC_DIR / rel)

    def do_POST(self) -> None:
        parsed = urlparse(self.path)
        if unquote(parsed.path) != "/api/action":
            self._send_text("404 Not Found", 404)
            return
        try:
            length = int(self.headers.get("Content-Length", 0) or 0)
            payload = json.loads(self.rfile.read(length).decode("utf-8") or "{}")
        except (ValueError, UnicodeDecodeError):
            self._send_json({"error": "请求体不是合法 JSON"}, 400)
            return
        if not isinstance(payload, dict):
            self._send_json({"error": "请求体必须是 JSON 对象"}, 400)
            return
        snapshot = self.server.api.action(payload)
        self._send_json(snapshot)


def main() -> None:
    parser = argparse.ArgumentParser(description="CK 游戏 WebUI（无地图信息面板）")
    parser.add_argument("--host", default="127.0.0.1", help="监听地址（默认 127.0.0.1）")
    parser.add_argument("--port", type=int, default=8000, help="监听端口（默认 8000）")
    parser.add_argument("--scenario", default=None, help="场景 id，如 1066/867/1200/1453（默认 1066）")
    args = parser.parse_args()

    server = CKWebServer(args.host, args.port, args.scenario)
    print(f"CK 游戏 WebUI 已启动：{server.url}  （场景 {server.scenario_id}）")
    print("按 Ctrl+C 停止。")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\n已停止。")
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
