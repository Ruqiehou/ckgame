package com.ckgame.politics;

import com.ckgame.core.Gender;

/** 继承人候选项：对应 Python 的元组 (id, gender, birth_ordinal, alive)。 */
public record HeirCandidate(int id, Gender gender, int birthOrdinal, boolean alive) {}
