# 开发者指南（Rust 版）

## 环境

- Rust 1.70+（推荐使用 [rustup](https://rustup.rs/) 安装）
- Cargo（Rust 包管理器，随 Rust 安装）

## 运行

```bash
# 构建
cargo build --release

# 运行
cargo run --release

# 运行演示
cargo run

# 单元测试
cargo test

# 检查（不编译）
cargo check
```

## 项目结构

```
ckgame-rust/
  Cargo.toml          - 项目清单与依赖
  src/
    main.rs           - 主入口
    core/             - 核心模块
    ui/               - 用户界面
  data/               - 游戏数据
  saves/              - 存档目录
```

## 添加新功能

1. 在对应模块（`core/` / `ui/`）创建新的 `.rs` 文件
2. 在父级 `mod.rs` 中声明模块：`pub mod new_module;`
3. 在 `Cargo.toml` 中添加必要的依赖
4. 运行 `cargo check` 验证

## 常用命令

```bash
# 更新依赖
cargo update

# 清理构建
cargo clean

# 生成文档
cargo doc --open

# 格式化代码
cargo fmt

# 检查代码风格
cargo clippy
```

## 存档位置

- 存档目录：`saves/`（项目根目录）
- 自动存档：`saves/autosave.json`

## 常见问题

**Q: 如何修改游戏平衡参数？**
A: 修改 [balance.rs](../../ckgame-rust/src/core/balance.rs) 中的 `pub const` 常量

**Q: Windows 控制台中文乱码？**
A: 在 PowerShell 中执行 `chcp 65001`，或在 Windows Terminal 中设置 UTF-8

**Q: 如何添加新的特性？**
A: 在 [traits.rs](../../ckgame-rust/src/core/traits.rs) 的 `ALL_TRAITS` 静态切片中添加元素

**Q: 日期计算依赖 chrono？**
A: 是的，`GameDate` 封装了 `chrono::NaiveDate` 进行日期运算与闰年判断

**Q: 何时使用 `Rc<RefCell<>>` vs `Arc<RwLock<>>`？**
A: 单线程共享使用 `Rc<RefCell<>>`；多线程共享使用 `Arc<RwLock<>>`