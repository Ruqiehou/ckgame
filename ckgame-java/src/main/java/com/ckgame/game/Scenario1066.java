package com.ckgame.game;

import com.ckgame.world.World;

/**
 * 1066 场景：数据在 data/scenarios/1066.json，代码只负责加载。
 * 对应 Python 的 ck_engine.game.scenario.Scenario1066。
 */
public final class Scenario1066 {

    private Scenario1066() {}

    /** 使用默认数据（classpath 资源）构建 1066 场景。 */
    public static World build() {
        return ScenarioLoader.loadScenario();
    }

    /** 使用指定文件系统路径构建场景。 */
    public static World build(String path) {
        return ScenarioLoader.loadScenario(path);
    }
}
