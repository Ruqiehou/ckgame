"""CK 游戏启动入口。

用法:
    python main.py [场景id]
    python main.py 867     # 直接进入维京场景
    python main.py         # 交互式选择场景（默认 1066）
"""

from __future__ import annotations

from ck_engine.game.tui import main


if __name__ == "__main__":
    main()
