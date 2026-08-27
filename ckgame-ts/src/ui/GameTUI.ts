import * as readline from 'node:readline';
import { GameAPI } from './GameAPI.js';

export class GameTUI {
  private api: GameAPI;
  private rl: readline.Interface;

  private static readonly SEASONS: Record<string, string> = { SPRING: '\u6625', SUMMER: '\u590f', AUTUMN: '\u79cb', WINTER: '\u51ac' };
  private static readonly TERRAINS: Record<string, string> = { PLAINS: '\u5e73\u539f', HILLS: '\u4e18\u9675', MOUNTAINS: '\u5c71\u5730', FOREST: '\u68ee\u6797', DESERT: '\u6c99\u6f20', WETLAND: '\u6e7f\u5730', FARMLAND: '\u519c\u7530', COASTAL: '\u6cbf\u6d77' };

  constructor() {
    this.api = new GameAPI();
    this.rl = readline.createInterface({ input: process.stdin, output: process.stdout });
  }

  async run(): Promise<void> {
    console.log('====================================================');
    console.log('  CK \u98ce\u683c\u5927\u6218\u7565\u5f15\u64ce -- \u547d\u4ee4\u884c\u6587\u5b57\u5bf9\u8bdd\u6a21\u5f0f');
    console.log('  \u5341\u5b57\u519b\u4e4b\u738b TypeScript \u7248  1066 \u5e74\u8bfa\u66fc\u5f81\u670d\u6a21\u62df\u5668');
    console.log('====================================================');
    const snap = this.api.snapshot();
    const p = this.playerMap(snap);
    console.log('  \u5f53\u524d\u73a9\u5bb6: ' + this.str(p, 'name', '?'));
    console.log('  \u8f93\u5165 0 \u6216 quit \u9000\u51fa\uff0c\u8f93\u5165 help \u67e5\u770b\u5e2e\u52a9');
    await this.pause();

    while (true) {
      this.handleEvents();
      this.render('\u4e3b\u83dc\u5355');
      this.showMainMenu();
      const cmd = (await this.input('> ')).toLowerCase().trim();
      if (cmd === '') continue;
      if (cmd === '0' || cmd === 'quit' || cmd === 'exit') { this.api.action({ action: 'save' }); console.log('  \u5df2\u81ea\u52a8\u5b58\u6863\u3002\u518d\u89c1\uff01'); break; }
      switch (cmd) {
        case 'help': this.showHelp(); break;
        case '1': this.showStatus(); break;
        case '2': this.showCharDetail(); break;
        case '3': this.showCounties(); break;
        case '4': this.showCountyDetail(); break;
        case '5': this.showArmies(); break;
        case '6': this.showWars(); break;
        case '7': this.showClaims(); break;
        case '8': this.showTreaties(); break;
        case '9': this.showRulers(); break;
        case '10': this.showSieges(); break;
        case '11': this.showLog(); break;
        case '12': this.showMap(); break;
        case 'a': await this.actionRaise(); break;
        case 'b': await this.actionMove(); break;
        case 'c': await this.actionDisband(); break;
        case 'd': await this.actionWar(); break;
        case 'e': await this.actionPeace(); break;
        case 'f': await this.actionImprove(); break;
        case 'g': this.actionFeast(); break;
        case 'h': await this.actionBuild(); break;
        case 'i': await this.actionDevelop(); break;
        case 'j': await this.actionClaim(); break;
        case 'k': await this.actionGrant(); break;
        case 'l': this.actionKnights(); break;
        case 'm': await this.menuCouncil(); break;
        case 'n': await this.menuSchemes(); break;
        case 'o': await this.menuDiplomacy(); break;
        case 'p': await this.menuLaws(); break;
        case 'q': await this.menuSaveLoad(); break;
        case 'r': await this.actionAdvance(); break;
        case 's': await this.actionSwitchPlayer(); break;
        case 't': this.toggleCheat(); break;
        default: console.log('  \u672a\u77e5\u6307\u4ee4\uff0c\u8f93\u5165 help \u67e5\u770b\u5e2e\u52a9'); await this.pause(); break;
      }
    }
    this.rl.close();
  }

  private showMainMenu(): void {
    console.log();
    console.log('  [1]  \u72b6\u6001\u6982\u89c8      [2]  \u89d2\u8272\u8be6\u60c5      [3]  \u4f2f\u7235\u9886\u5217\u8868');
    console.log('  [4]  \u4f2f\u7235\u9886\u8be6\u60c5    [5]  \u519b\u961f\u5217\u8868      [6]  \u6218\u4e89\u5217\u8868');
    console.log('  [7]  \u5ba3\u79f0\u5217\u8868      [8]  \u6761\u7ea6\u5217\u8868      [9]  \u7edf\u6cbb\u8005\u5217\u8868');
    console.log('  [10] \u56f4\u57ce\u5217\u8868      [11] \u65e5\u5fd7          [12] \u5730\u56fe');
    console.log('  [A]  \u5f81\u53ec\u519b\u961f      [B]  \u79fb\u52a8\u519b\u961f      [C]  \u89e3\u6563\u519b\u961f');
    console.log('  [D]  \u5ba3\u6218          [E]  \u8bae\u548c          [F]  \u6539\u5584\u5173\u7cfb');
    console.log('  [G]  \u4e3e\u529e\u5bb4\u4f1a      [H]  \u5efa\u9020\u5efa\u7b51      [I]  \u53d1\u5c55\u9886\u5730');
    console.log('  [J]  \u4f2a\u9020\u5ba3\u79f0      [K]  \u6388\u4e88\u5934\u8854      [L]  \u62db\u52df\u9a91\u58eb');
    console.log('  [M]  \u5185\u9601\u7ba1\u7406      [N]  \u9634\u8c0b          [O]  \u5916\u4ea4');
    console.log('  [P]  \u6cd5\u5f8b          [Q]  \u5b58\u6863\u7ba1\u7406      [R]  \u63a8\u8fdb\u65f6\u95f4');
    console.log('  [S]  \u5207\u6362\u89d2\u8272      [T]  \u4f5c\u5f0a          [0]  \u9000\u51fa');
  }

  private render(title: string): void {
    console.clear();
    const snap = this.api.snapshot();
    const p = this.playerMap(snap);
    const season = GameTUI.SEASONS[this.str(snap, 'season', '')] ?? '?';
    const date = this.str(snap, 'date', '????-??-??');
    const wars = this.listMap(snap, 'wars');
    let activeWarCount = 0; for (const w of wars) { if (this.bool(w, 'active') && this.bool(w, 'involvesPlayer')) activeWarCount++; }
    const armies = this.listMap(snap, 'armies');
    let pac = 0; let pam = 0; for (const a of armies) { if (this.bool(a, 'isPlayer')) { pac++; pam += this.intV(a, 'men'); } }
    const counties = this.listMap(snap, 'counties');
    let sc = 0; for (const c of counties) { if (c.siege != null) sc++; }
    const we = this.dbl(snap, 'player_war_exhaustion');
    console.log('============================================================');
    if (title) console.log('  [' + title + ']');
    console.log('  \u65e5\u671f: ' + date + ' [' + season + ']  |  \u7edf\u6cbb\u8005: ' + this.str(p, 'name', '?') + ' (' + this.str(p, 'title', '\u65e0') + ')');
    console.log('  \u91d1: ' + Math.floor(this.dbl(p, 'gold')) + '  \u5a01\u671b: ' + Math.floor(this.dbl(p, 'prestige')) + '  \u8654\u8bda: ' + Math.floor(this.dbl(p, 'piety')) + '  \u538b\u529b: ' + this.intV(p, 'stress') + '  \u5065\u5eb7: ' + this.dbl(p, 'health').toFixed(1));
    const attrs = this.mapVal(p, 'attrs');
    if (attrs) console.log('  \u5916\u4ea4:' + this.intV(attrs, 'diplomacy') + ' \u519b\u4e8b:' + this.intV(attrs, 'martial') + ' \u7ba1\u7406:' + this.intV(attrs, 'stewardship') + ' \u8c0b\u7565:' + this.intV(attrs, 'intrigue') + ' \u5b66\u8bc6:' + this.intV(attrs, 'learning') + ' \u52c7\u6b66:' + this.intV(attrs, 'prowess'));
    const parts: string[] = [];
    if (activeWarCount > 0) parts.push('\u6218\u4e89:' + activeWarCount);
    if (pac > 0) parts.push('\u519b\u961f:' + pac + '(' + pam + '\u4eba)');
    if (sc > 0) parts.push('\u56f4\u57ce:' + sc);
    if (we > 0.1) parts.push('\u6218\u4e89\u75b2\u52b3:' + we.toFixed(1));
    if (parts.length > 0) console.log('  [' + parts.join('  ') + ']');
    console.log('============================================================');
  }

  private showHelp(): void { console.log('\n  \u8f93\u5165\u6570\u5b57\u6216\u5b57\u6bcd\u9009\u62e9\u529f\u80fd\u30020/quit \u9000\u51fa\u3002'); this.pauseSync(); }
  private showStatus(): void { this.render('\u72b6\u6001\u6982\u89c8'); const snap = this.api.snapshot(); const p = this.playerMap(snap); console.log('  \u91d1\u5e01: ' + Math.floor(this.dbl(p, 'gold')) + '\n  \u5a01\u671b: ' + Math.floor(this.dbl(p, 'prestige')) + '\n  \u8654\u8bda: ' + Math.floor(this.dbl(p, 'piety')) + '\n  \u538b\u529b: ' + this.intV(p, 'stress') + '\n  \u5065\u5eb7: ' + this.dbl(p, 'health').toFixed(1) + '\n  \u7b49\u7ea7: ' + this.intV(p, 'level') + ' XP: ' + this.intV(p, 'xp') + '/' + this.intV(p, 'xpToNext') + '\n  \u6708\u6536\u5165: ' + this.dbl(p, 'income').toFixed(1)); const log = this.listStr(snap, 'log'); console.log('\n  -- \u8fd1\u671f\u65e5\u5fd7 --'); for (const l of log.slice(-8)) console.log('  ' + l); this.pauseSync(); }
  private showCharDetail(): void { this.render('\u89d2\u8272\u8be6\u60c5'); const snap = this.api.snapshot(); const chars = this.listMap(snap, 'characters'); console.log('\n  \u9009\u62e9\u89d2\u8272:'); for (let i = 0; i < Math.min(chars.length, 30); i++) { const c = chars[i]; console.log('  ' + (i + 1) + '. ' + this.str(c, 'name', '?') + ' ' + this.intV(c, 'age') + '\u5c81 ' + this.str(c, 'title', '\u65e0')); } console.log('  0. \u8fd4\u56de'); this.pauseSync(); }
  private showCounties(): void { this.render('\u4f2f\u7235\u9886\u5217\u8868'); const snap = this.api.snapshot(); const counties = this.listMap(snap, 'counties'); console.log('  ID  \u540d\u79f0       \u5730\u5f62   \u53d1\u5c55  \u63a7\u5236   \u9886\u4e3b         \u7a0e\u6536    \u5f81\u53ec'); for (const c of counties) { const m = this.bool(c, 'isPlayer') ? '*' : ' '; const tz = GameTUI.TERRAINS[this.str(c, 'terrain', '')] ?? this.str(c, 'terrain', '?'); console.log(m + ' ' + this.intV(c, 'id') + '  ' + this.str(c, 'name', '?').padEnd(10) + ' ' + tz.padEnd(6) + ' ' + String(this.intV(c, 'development')).padEnd(5) + ' ' + this.dbl(c, 'control').toFixed(1).padEnd(7) + ' ' + this.str(c, 'holderName', '\u65e0\u4e3b').padEnd(12) + ' ' + this.dbl(c, 'tax').toFixed(1).padEnd(7) + ' ' + this.intV(c, 'levies')); } this.pauseSync(); }
  private showCountyDetail(): void { this.render('\u4f2f\u7235\u9886\u8be6\u60c5'); const snap = this.api.snapshot(); const counties = this.listMap(snap, 'counties'); for (let i = 0; i < Math.min(counties.length, 30); i++) { const c = counties[i]; console.log('  ' + (i + 1) + '. ' + this.str(c, 'name', '?') + ' (' + this.str(c, 'holderName', '?') + ')'); } console.log('  0. \u8fd4\u56de'); this.pauseSync(); }
  private showArmies(): void { this.render('\u519b\u961f\u5217\u8868'); const snap = this.api.snapshot(); const armies = this.listMap(snap, 'armies'); console.log('  ID  \u540d\u79f0             \u72b6\u6001      \u4f4d\u7f6e       \u5175\u529b   \u8865\u7ed9'); for (const a of armies) { const m = this.bool(a, 'isPlayer') ? '*' : ' '; console.log(m + ' ' + this.intV(a, 'id') + '  ' + this.str(a, 'name', '?').padEnd(16) + ' ' + this.str(a, 'status', '?').padEnd(10) + ' ' + this.str(a, 'locationName', '?').padEnd(10) + ' ' + String(this.intV(a, 'men')).padEnd(6) + ' ' + this.dbl(a, 'supply').toFixed(1)); } this.pauseSync(); }
  private showWars(): void { this.render('\u6218\u4e89\u5217\u8868'); const snap = this.api.snapshot(); const wars = this.listMap(snap, 'wars'); if (wars.length === 0) { console.log('\n  \u5f53\u524d\u65e0\u6218\u4e89'); } else { for (const w of wars) console.log('  [' + (this.bool(w, 'active') ? '\u8fdb\u884c\u4e2d' : '\u5df2\u7ed3\u675f') + '] ' + this.str(w, 'name', '?') + ' | ' + this.str(w, 'attacker', '?') + ' vs ' + this.str(w, 'defender', '?') + ' | \u5206\u6570:' + this.dbl(w, 'warscore').toFixed(0)); } this.pauseSync(); }
  private showClaims(): void { this.render('\u5ba3\u79f0\u5217\u8868'); const claims = this.listMap(this.api.snapshot(), 'player_claims'); if (claims.length === 0) console.log('\n  \u4f60\u6ca1\u6709\u4efb\u4f55\u5ba3\u79f0'); else for (const cl of claims) console.log('  ' + (this.str(cl, 'titleName', '') || this.str(cl, 'countyName', '?')) + ' (\u5f3a\u5ea6:' + this.dbl(cl, 'strength').toFixed(0) + ')'); this.pauseSync(); }
  private showTreaties(): void { this.render('\u6761\u7ea6\u5217\u8868'); const treaties = this.listMap(this.api.snapshot(), 'treaties'); if (treaties.length === 0) console.log('\n  \u6ca1\u6709\u751f\u6548\u7684\u6761\u7ea6'); else for (const t of treaties) console.log('  ' + this.str(t, 'kindZh', this.str(t, 'kind', '?')) + ' <-> ' + this.str(t, 'otherName', '?') + ' (\u81f3 ' + this.str(t, 'expiresYear', '?') + ' \u5e74)'); this.pauseSync(); }
  private showRulers(): void { this.render('\u7edf\u6cbb\u8005\u5217\u8868'); const snap = this.api.snapshot(); const rulers = this.listMap(snap, 'rulers'); console.log('  \u540d\u5b57         \u5934\u8854         \u91d1     \u5a01\u671b    \u519b\u7565  \u6536\u5165    \u5175\u529b'); for (const r of rulers) { const m = this.bool(r, 'isPlayer') ? '*' : ' '; console.log(m + ' ' + this.str(r, 'name', '?').padEnd(11) + ' ' + this.str(r, 'title', '\u65e0').padEnd(12) + ' ' + Math.floor(this.dbl(r, 'gold')).toString().padEnd(6) + ' ' + Math.floor(this.dbl(r, 'prestige')).toString().padEnd(6) + ' ' + this.intV(r, 'martial').toString().padEnd(4) + ' ' + this.dbl(r, 'income').toFixed(1).padEnd(6) + ' ' + this.intV(r, 'men')); } this.pauseSync(); }
  private showSieges(): void { this.render('\u56f4\u57ce\u5217\u8868'); const counties = this.listMap(this.api.snapshot(), 'counties'); const sieged = counties.filter(c => c.siege != null); if (sieged.length === 0) console.log('\n  \u5f53\u524d\u65e0\u56f4\u57ce'); else for (const c of sieged) { const s = this.mapVal(c, 'siege'); if (!s) continue; console.log('  ' + this.str(c, 'name', '?') + ' \u8fdb\u5ea6:' + this.intV(s, 'progress') + '/' + this.intV(s, 'required')); } this.pauseSync(); }
  private showLog(): void { this.render('\u4e8b\u4ef6\u65e5\u5fd7'); const log = this.listStr(this.api.snapshot(), 'log'); if (log.length === 0) console.log('\n  \u6682\u65e0\u65e5\u5fd7'); else { console.log('\n  \u6700\u8fd1 ' + log.length + ' \u6761\u4e8b\u4ef6:'); for (const l of log) console.log('  ' + l); } this.pauseSync(); }
  private showMap(): void { this.render('\u6587\u5b57\u5730\u56fe'); const counties = this.listMap(this.api.snapshot(), 'counties'); console.log('\n  \u4f2f\u7235\u9886\u5217\u8868 (\u5171 ' + counties.length + ' \u4e2a):'); for (const c of counties) { const m = this.bool(c, 'isPlayer') ? '*' : ' '; const nb = (c.neighbors as number[] ?? []).length; console.log(m + ' ' + this.intV(c, 'id') + '. ' + this.str(c, 'name', '?') + ' [' + this.str(c, 'holderName', '?') + '] \u90bb\u63a5:' + nb); } this.pauseSync(); }

  private handleEvents(): void {
    const snap = this.api.snapshot();
    const events = this.listMap(snap, 'pending_events');
    for (const ev of events) {
      console.log('\n  \u4e8b\u4ef6: ' + this.str(ev, 'title', '?'));
      console.log('  ' + this.str(ev, 'description', ''));
      const choices = this.listMap(ev, 'choices');
      for (let i = 0; i < choices.length; i++) console.log('  ' + (i + 1) + '. ' + this.str(choices[i], 'text', '?'));
    }
  }

  private async actionRaise(): Promise<void> { this.render('\u5f81\u53ec\u519b\u961f'); const snap = this.api.snapshot(); const counties = this.listMap(snap, 'counties').filter(c => this.bool(c, 'isPlayer')); if (counties.length === 0) { console.log('\n  \u6ca1\u6709\u53ef\u5f81\u53ec\u7684\u9886\u5730'); await this.pause(); return; } for (let i = 0; i < counties.length; i++) console.log('  ' + (i + 1) + '. ' + this.str(counties[i], 'name', '?')); console.log('  0. \u8fd4\u56de'); const ch = await this.inputInt('\u9009\u62e9', 0); if (ch <= 0 || ch > counties.length) return; const res = this.api.action({ action: 'raise_army', county_id: this.intV(counties[ch - 1], 'id') }); this.showResult(res); await this.pause(); }
  private async actionMove(): Promise<void> { this.render('\u79fb\u52a8\u519b\u961f'); const armies = this.listMap(this.api.snapshot(), 'armies').filter(a => this.bool(a, 'isPlayer')); if (armies.length === 0) { console.log('\n  \u6ca1\u6709\u519b\u56e2'); await this.pause(); return; } for (let i = 0; i < armies.length; i++) console.log('  ' + (i + 1) + '. ' + this.str(armies[i], 'name', '?') + ' @ ' + this.str(armies[i], 'locationName', '?')); console.log('  0. \u8fd4\u56de'); const ach = await this.inputInt('\u9009\u62e9\u519b\u56e2', 0); if (ach <= 0 || ach > armies.length) return; const counties = this.listMap(this.api.snapshot(), 'counties'); for (let i = 0; i < Math.min(counties.length, 30); i++) console.log('  ' + (i + 1) + '. ' + this.str(counties[i], 'name', '?')); console.log('  0. \u8fd4\u56de'); const dch = await this.inputInt('\u76ee\u6807\u7701\u4efd', 0); if (dch <= 0 || dch > counties.length) return; const res = this.api.action({ action: 'move_army', army_id: this.intV(armies[ach - 1], 'id'), county_id: this.intV(counties[dch - 1], 'id') }); this.showResult(res); await this.pause(); }
  private async actionDisband(): Promise<void> { const armies = this.listMap(this.api.snapshot(), 'armies').filter(a => this.bool(a, 'isPlayer')); if (armies.length === 0) { console.log('\n  \u6ca1\u6709\u519b\u56e2'); await this.pause(); return; } for (let i = 0; i < armies.length; i++) console.log('  ' + (i + 1) + '. ' + this.str(armies[i], 'name', '?')); console.log('  0. \u8fd4\u56de'); const ch = await this.inputInt('\u9009\u62e9', 0); if (ch <= 0 || ch > armies.length) return; const res = this.api.action({ action: 'disband_army', army_id: this.intV(armies[ch - 1], 'id') }); this.showResult(res); await this.pause(); }
  private async actionWar(): Promise<void> { this.render('\u5ba3\u6218'); const chars = this.listMap(this.api.snapshot(), 'characters').filter(c => this.intV(c, 'id') !== this.api.playerId && this.bool(c, 'is_alive')); for (let i = 0; i < Math.min(chars.length, 30); i++) console.log('  ' + (i + 1) + '. ' + this.str(chars[i], 'name', '?')); console.log('  0. \u8fd4\u56de'); const ch = await this.inputInt('\u9009\u62e9\u76ee\u6807', 0); if (ch <= 0 || ch > chars.length) return; const res = this.api.action({ action: 'declare_war', target_id: this.intV(chars[ch - 1], 'id') }); this.showResult(res); await this.pause(); }
  private async actionPeace(): Promise<void> { const wars = this.listMap(this.api.snapshot(), 'wars').filter(w => this.bool(w, 'active') && this.bool(w, 'involvesPlayer')); if (wars.length === 0) { console.log('\n  \u6ca1\u6709\u53ef\u8bae\u548c\u7684\u6218\u4e89'); await this.pause(); return; } for (let i = 0; i < wars.length; i++) console.log('  ' + (i + 1) + '. ' + this.str(wars[i], 'name', '?')); console.log('  0. \u8fd4\u56de'); const ch = await this.inputInt('\u9009\u62e9', 0); if (ch <= 0 || ch > wars.length) return; const res = this.api.action({ action: 'white_peace', war_id: this.intV(wars[ch - 1], 'id') }); this.showResult(res); await this.pause(); }
  private async actionImprove(): Promise<void> { const chars = this.listMap(this.api.snapshot(), 'characters').filter(c => this.intV(c, 'id') !== this.api.playerId); for (let i = 0; i < Math.min(chars.length, 20); i++) console.log('  ' + (i + 1) + '. ' + this.str(chars[i], 'name', '?')); console.log('  0. \u8fd4\u56de'); const ch = await this.inputInt('\u9009\u62e9', 0); if (ch <= 0 || ch > chars.length) return; const res = this.api.action({ action: 'improve_relations', target_id: this.intV(chars[ch - 1], 'id') }); this.showResult(res); await this.pause(); }
  private actionFeast(): void { const res = this.api.action({ action: 'hold_feast' }); this.showResult(res); this.pauseSync(); }
  private async actionBuild(): Promise<void> { console.log('\n  \u5efa\u7b51\u529f\u80fd\u8be6\u89c1\u53bf\u57ce\u8be6\u60c5\u4e2d\u64cd\u4f5c'); await this.pause(); }
  private async actionDevelop(): Promise<void> { const counties = this.listMap(this.api.snapshot(), 'counties').filter(c => this.bool(c, 'isPlayer')); for (let i = 0; i < counties.length; i++) console.log('  ' + (i + 1) + '. ' + this.str(counties[i], 'name', '?') + ' (\u53d1\u5c55:' + this.intV(counties[i], 'development') + ')'); console.log('  0. \u8fd4\u56de'); const ch = await this.inputInt('\u9009\u62e9', 0); if (ch <= 0 || ch > counties.length) return; const res = this.api.action({ action: 'develop_county', county_id: this.intV(counties[ch - 1], 'id') }); this.showResult(res); await this.pause(); }
  private async actionClaim(): Promise<void> { const counties = this.listMap(this.api.snapshot(), 'counties').filter(c => !this.bool(c, 'isPlayer')); for (let i = 0; i < Math.min(counties.length, 20); i++) console.log('  ' + (i + 1) + '. ' + this.str(counties[i], 'name', '?')); console.log('  0. \u8fd4\u56de'); const ch = await this.inputInt('\u9009\u62e9', 0); if (ch <= 0 || ch > counties.length) return; const res = this.api.action({ action: 'fabricate_claim', county_id: this.intV(counties[ch - 1], 'id') }); this.showResult(res); await this.pause(); }
  private async actionGrant(): Promise<void> { console.log('\n  \u6388\u4e88\u5934\u8854\u529f\u80fd'); await this.pause(); }
  private actionKnights(): void { const res = this.api.action({ action: 'recruit_knights' }); this.showResult(res); this.pauseSync(); }
  private async actionAdvance(): Promise<void> { const days = await this.inputInt('\u63a8\u8fdb\u5929\u6570 (1-365)', 30); const res = this.api.action({ action: 'advance', days: Math.max(1, Math.min(365, days)) }); this.showResult(res); await this.pause(); }
  private async actionSwitchPlayer(): Promise<void> { const playable = this.listMap(this.api.snapshot(), 'playable'); for (let i = 0; i < playable.length; i++) console.log('  ' + (i + 1) + '. ' + this.str(playable[i], 'name', '?') + ' (' + this.str(playable[i], 'title', '\u65e0') + ')'); console.log('  0. \u8fd4\u56de'); const ch = await this.inputInt('\u9009\u62e9', 0); if (ch <= 0 || ch > playable.length) return; this.api.action({ action: 'set_player', character_id: this.intV(playable[ch - 1], 'id') }); console.log('\n  \u5df2\u5207\u6362'); await this.pause(); }
  private toggleCheat(): void { const res = this.api.action({ action: 'toggle_cheat' }); this.showResult(res); this.pauseSync(); }

  // Sub-menus
  private async menuCouncil(): Promise<void> { console.log('\n  \u5185\u9601\u7ba1\u7406'); const snap = this.api.snapshot(); const council = this.mapVal(snap, 'player_council'); if (!council) { console.log('  \u6682\u65e0\u5185\u9601'); await this.pause(); return; } console.log('  1. \u4efb\u547d\u5b98\u5458  2. \u5206\u914d\u4efb\u52a1  0. \u8fd4\u56de'); const cmd = (await this.input('\u9009\u62e9')).trim(); if (cmd === '1') { console.log('\n  \u4efb\u547d\u5b98\u5458\u529f\u80fd'); await this.pause(); } else if (cmd === '2') { console.log('\n  \u5206\u914d\u4efb\u52a1\u529f\u80fd'); await this.pause(); } }
  private async menuSchemes(): Promise<void> { console.log('\n  \u9634\u8c0b\u6d3b\u52a8'); const schemes = this.listMap(this.api.snapshot(), 'player_schemes'); if (schemes.length === 0) console.log('\n  \u6ca1\u6709\u8fdb\u884c\u4e2d\u7684\u9634\u8c0b'); else for (const s of schemes) console.log('  ' + this.str(s, 'kindZh', '?') + ' -> ' + this.str(s, 'targetName', '?') + ' \u8fdb\u5ea6:' + this.dbl(s, 'progress').toFixed(0) + '%'); console.log('\n  1. \u53d1\u8d77\u9634\u8c0b  0. \u8fd4\u56de'); const cmd = (await this.input('\u9009\u62e9')).trim(); if (cmd === '1') { const chars = this.listMap(this.api.snapshot(), 'characters').filter(c => this.intV(c, 'id') !== this.api.playerId && this.bool(c, 'is_alive')); for (let i = 0; i < Math.min(chars.length, 20); i++) console.log('  ' + (i + 1) + '. ' + this.str(chars[i], 'name', '?')); console.log('  0. \u8fd4\u56de'); const tch = await this.inputInt('\u76ee\u6807', 0); if (tch <= 0 || tch > chars.length) return; console.log('  1.\u8c0b\u6740 2.\u62c9\u62e2 3.\u4f2a\u9020\u628a\u67c4'); const kch = await this.inputInt('\u7c7b\u578b', 1); const kinds = ['MURDER', 'SWAY', 'FABRICATE_HOOK']; const res = this.api.action({ action: 'start_scheme', scheme_kind: kinds[kch - 1] ?? 'MURDER', target_id: this.intV(chars[tch - 1], 'id') }); this.showResult(res); await this.pause(); } }
  private async menuDiplomacy(): Promise<void> { const chars = this.listMap(this.api.snapshot(), 'characters').filter(c => this.intV(c, 'id') !== this.api.playerId && this.bool(c, 'is_alive')); for (let i = 0; i < Math.min(chars.length, 30); i++) console.log('  ' + (i + 1) + '. ' + this.str(chars[i], 'name', '?')); console.log('  0. \u8fd4\u56de'); const ch = await this.inputInt('\u9009\u62e9\u89d2\u8272', 0); if (ch <= 0 || ch > chars.length) return; const tid = this.intV(chars[ch - 1], 'id'); console.log('\n  1.\u7ed3\u76df 2.\u4e92\u4e0d\u4fb5\u72af 3.\u9644\u5eb8 4.\u8d38\u6613 5.\u60c5\u62a5 6.\u8054\u59fb 7.\u8d60\u793c 8.\u5bbf\u654c'); const act = (await this.input('\u9009\u62e9')).trim(); const actions = ['form_alliance', 'form_non_aggression', 'form_vassalage', 'form_trade_agreement', 'form_intelligence_sharing', 'arrange_marriage', 'send_gift', 'set_rival']; const payload: any = { target_id: tid }; if (act === '7') { payload.action = 'send_gift'; payload.amount = await this.inputDouble('\u91d1\u989d', 50); } else { const idx = parseInt(act) - 1; if (idx >= 0 && idx < actions.length) payload.action = actions[idx]; else return; } const res = this.api.action(payload); this.showResult(res); await this.pause(); }
  private async menuLaws(): Promise<void> { console.log('\n  \u6cd5\u5f8b\u7ba1\u7406'); const snap = this.api.snapshot(); const p = this.playerMap(snap); const laws = this.mapVal(p, 'laws'); console.log('  \u7ee7\u627f\u6cd5: ' + this.str(laws ?? {}, 'succession', '\u65e0')); console.log('  \u738b\u6743: ' + this.str(laws ?? {}, 'crown_authority', '\u65e0')); console.log('  \u6027\u522b\u6cd5: ' + this.str(laws ?? {}, 'gender_law', '\u65e0')); console.log('\n  1.\u66f4\u6539\u7ee7\u627f\u6cd5 2.\u66f4\u6539\u738b\u6743 3.\u66f4\u6539\u6027\u522b\u6cd5 0.\u8fd4\u56de'); const cmd = (await this.input('\u9009\u62e9')).trim(); if (cmd === '1') { console.log('  1.PRIMOGENITURE 2.ULTIMOGENITURE 3.ELECTIVE'); const ch = await this.inputInt('\u9009\u62e9', 0); if (ch > 0) { const laws = ['PRIMOGENITURE', 'ULTIMOGENITURE', 'ELECTIVE']; this.api.action({ action: 'set_succession_law', law: laws[ch - 1] }); } } else if (cmd === '2') { const ch = await this.inputInt('\u738b\u6743\u7b49\u7ea7(0-2)', 0); this.api.action({ action: 'set_crown_authority', level: ch }); } else if (cmd === '3') { console.log('  1.AGNATIC 2.COGNATIC 3.ABSOLUTE'); const ch = await this.inputInt('\u9009\u62e9', 0); if (ch > 0) { const laws = ['AGNATIC', 'AGNATIC_COGNATIC', 'ABSOLUTE_COGNATIC']; this.api.action({ action: 'set_gender_law', law: laws[ch - 1] }); } } await this.pause(); }
  private async menuSaveLoad(): Promise<void> { console.log('\n  1.\u4fdd\u5b58 2.\u8bfb\u53d6 3.\u5220\u9664 4.\u65b0\u6e38\u620f 0.\u8fd4\u56de'); const cmd = (await this.input('\u9009\u62e9')).trim(); if (cmd === '1') { this.api.action({ action: 'save' }); console.log('\n  \u5df2\u5b58\u6863'); } else if (cmd === '2') { this.api.action({ action: 'load' }); console.log('\n  \u5df2\u8bfb\u6863'); } else if (cmd === '3') { const name = (await this.input('\u5b58\u6863\u540d')).trim(); this.api.action({ action: 'delete_save', name }); } else if (cmd === '4') { this.api.action({ action: 'new_game' }); console.log('\n  \u65b0\u5c40\u5f00\u59cb'); } await this.pause(); }

  // I/O helpers
  private input(prompt: string): Promise<string> { return new Promise(resolve => { this.rl.question('  ' + prompt + ' > ', ans => resolve(ans)); }); }
  private async inputInt(prompt: string, fallback: number): Promise<number> { const s = (await this.input(prompt)).trim(); const v = parseInt(s); return isNaN(v) ? fallback : v; }
  private async inputDouble(prompt: string, fallback: number): Promise<number> { const s = (await this.input(prompt)).trim(); const v = parseFloat(s); return isNaN(v) ? fallback : v; }
  private pause(): Promise<void> { return new Promise(resolve => { this.rl.question('\n  \u6309\u56de\u8f66\u7ee7\u7eed...', () => resolve()); }); }
  private pauseSync(): void { console.log('\n  \u6309\u56de\u8f66\u7ee7\u7eed...'); }
  private showResult(res: any): void { const msgs = res.messages as string[]; if (msgs && msgs.length > 0) console.log('  ' + msgs[msgs.length - 1]); }

  // Type-safe accessors
  private playerMap(snap: any): any { return snap.player ?? {}; }
  private mapVal(m: any, key: string): any { return m?.[key] ?? null; }
  private listMap(m: any, key: string): any[] { const v = m?.[key]; return Array.isArray(v) ? v.filter(x => x && typeof x === 'object') : []; }
  private listStr(m: any, key: string): string[] { const v = m?.[key]; return Array.isArray(v) ? v.map(String) : []; }
  private str(m: any, key: string, def: string): string { const v = m?.[key]; return v != null ? String(v) : def; }
  private intV(m: any, key: string): number { const v = m?.[key]; return typeof v === 'number' ? v : 0; }
  private dbl(m: any, key: string): number { const v = m?.[key]; return typeof v === 'number' ? v : 0; }
  private bool(m: any, key: string): boolean { return m?.[key] === true; }
}
