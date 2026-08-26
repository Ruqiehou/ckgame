package com.ckgame.politics;

/**
 * 两个统治者之间的外交关系标志。
 * 对应 Python 的 RelationFlags dataclass。
 */
public final class DiplomacyFlags {
    public boolean allied = false;
    public boolean atWar = false;
    public boolean nonAggression = false;
    public boolean rival = false;
    public boolean marriagePact = false;
    public boolean vassalage = false;
    public boolean tradeAgreement = false;
    public boolean intelligenceSharing = false;

    /** 当前关系是否阻止宣战。 */
    public boolean blocksWar() {
        return allied || nonAggression || marriagePact || vassalage;
    }
}
