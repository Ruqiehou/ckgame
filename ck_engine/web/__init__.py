"""WebUI：基于标准库 http.server 的浏览器图形界面。

无需地图，聚焦信息面板与操作：玩家资金/属性、统治者与角色列表、
事件、玩法、决策、领地、军事、政治等。启动方式：

    python -m ck_engine.web.server [--port 8000] [--scenario 1066]

对外导出 CKWebServer 供嵌入式集成（如 Jupyter / 自有主程序）。
"""

from __future__ import annotations

from typing import Any

__all__ = ["CKWebServer"]


def __getattr__(name: str) -> Any:
    # 惰性导入：避免 python -m ck_engine.web.server 时的循环导入警告
    if name == "CKWebServer":
        from ck_engine.web.server import CKWebServer

        return CKWebServer
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")
