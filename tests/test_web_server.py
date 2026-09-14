"""WebUI 服务器端到端测试：HTTP 路由 / 快照 / 操作。"""

import json
import threading
import unittest
import urllib.request

from ck_engine.web.server import CKWebServer


class WebServerTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.server = CKWebServer("127.0.0.1", 0, "1066")
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()
        host, port = cls.server.server_address[:2]
        cls.base = f"http://{host}:{port}"

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()

    def _get(self, path: str):
        try:
            with urllib.request.urlopen(self.base + path, timeout=15) as resp:
                return resp.status, resp.headers.get("Content-Type", ""), resp.read()
        except urllib.error.HTTPError as e:
            return e.code, e.headers.get("Content-Type", ""), e.read()

    def _post(self, payload: dict):
        req = urllib.request.Request(
            self.base + "/api/action",
            data=json.dumps(payload).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        with urllib.request.urlopen(req, timeout=15) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))

    def test_index_page(self):
        status, ctype, body = self._get("/")
        self.assertEqual(status, 200)
        self.assertIn("text/html", ctype)
        self.assertIn(b"CK", body)
        self.assertIn(b"app.js", body)

    def test_static_asset(self):
        status, ctype, body = self._get("/static/app.js")
        self.assertEqual(status, 200)
        self.assertIn("javascript", ctype)
        self.assertIn(b"postAction", body)

    def test_static_path_traversal_blocked(self):
        status, _, _ = self._get("/static/../server.py")
        self.assertEqual(status, 403)

    def test_snapshot_json(self):
        status, ctype, body = self._get("/api/snapshot")
        self.assertEqual(status, 200)
        self.assertIn("application/json", ctype)
        snap = json.loads(body.decode("utf-8"))  # 元组等必须可序列化
        for key in ("date", "player", "characters", "counties", "messages"):
            self.assertIn(key, snap)
        self.assertTrue(snap["characters"])

    def test_action_advance(self):
        before = json.loads(self._get("/api/snapshot")[2].decode("utf-8"))
        _, after = self._post({"action": "advance", "days": 1})
        self.assertNotEqual(after["date"], before["date"])

    def test_action_unknown_safe(self):
        status, snap = self._post({"action": "no_such_action"})
        self.assertEqual(status, 200)
        self.assertIn("player", snap)

    def test_save_load_roundtrip(self):
        _, before = self._post({"action": "advance", "days": 5})
        status, _ = self._post({"action": "save", "name": "web_test"})
        self.assertEqual(status, 200)
        status, after = self._post({"action": "load", "name": "web_test"})
        self.assertEqual(status, 200)
        self.assertEqual(after["date"], before["date"])


if __name__ == "__main__":
    unittest.main()
