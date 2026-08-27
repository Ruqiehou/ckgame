# 开发者指南（C++ 版）

## 环境

- C++17 编译器：MSVC 2019+ / GCC 9+ / Clang 10+
- CMake 3.16+

## 构建与运行

```bash
# 配置
cmake -S . -B build

# 构建
cmake --build build --config Release

# 运行（MSVC）
build\Release\ckgame.exe

# 运行（GCC/Clang）
./build/ckgame
```

## 编译选项

- MSVC：`/utf-8 /W4`（UTF-8 源码 + 高警告级别）
- GCC/Clang：`-Wall -Wextra`

## 代码规范

- 命名空间 `ckgame`，子命名空间按模块划分
- 头文件使用 `#pragma once`
- 常量使用 `constexpr` / `inline constexpr`
- 枚举使用 `enum class`
- 核心类型遵循其他版本的值语义设计（如 `GameDate` 不可变值类型）

## 添加新功能

1. 在 `src/` 对应目录创建头文件与实现
2. 在 `CMakeLists.txt` 中添加 `.cpp` 源文件
3. 后续在 `GameAPI` 中添加 action 处理、`GameTUI` 中添加菜单项
4. 重新构建验证

## 目录约定

- 存档目录：`saves/`（项目根目录）
- 场景数据：`data/scenarios/`
- 地图布局：`data/map_layouts/`

## 常见问题

**Q: 如何修改游戏平衡参数？**
A: 修改 [balance.h](../../ckgame-cpp/src/core/balance/balance.h) 中的 `constexpr` 常量

**Q: Windows 控制台中文乱码？**
A: 确认使用 MSVC 构建（自动加 `/utf-8`），或终端执行 `chcp 65001`

**Q: 何时引入 JSON 库？**
A: 实现存档系统时建议引入 nlohmann/json（见数据格式文档）