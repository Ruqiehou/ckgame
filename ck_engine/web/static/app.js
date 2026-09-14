/* CK 游戏信息面板前端：轮询快照 + 基础操作。无地图，聚焦信息。 */
"use strict";

const $ = (sel) => document.querySelector(sel);
const POLL_MS = 3000;

let state = null; // 最近一次快照
let charMap = {}; // id -> 人物

// ---------- 工具 ----------
function esc(v) {
  return String(v ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function seasonZh(name) {
  return { SPRING: "春", SUMMER: "夏", AUTUMN: "秋", WINTER: "冬" }[name] || name;
}

function genderZh(name) {
  return name === "MALE" ? "男" : name === "FEMALE" ? "女" : name || "";
}

const ATTR_ZH = {
  diplomacy: "外交", martial: "军略", stewardship: "管理",
  intrigue: "谋略", learning: "学识", prowess: "勇武",
};

function attrZhList(attrs) {
  if (!attrs) return "";
  return ["diplomacy", "martial", "stewardship", "intrigue", "learning", "prowess"]
    .map((k) => `${ATTR_ZH[k]} <b>${attrs[k] ?? 0}</b>`)
    .join(" · ");
}

function relationBadges(c) {
  const badges = [];
  if (c.relation_allied) badges.push('<span class="badge allied">同盟</span>');
  if (c.relation_rival) badges.push('<span class="badge rival">宿敌</span>');
  if (c.relation_at_war) badges.push('<span class="badge atwar">交战</span>');
  if (c.relation_marriage) badges.push('<span class="badge marriage">婚约</span>');
  if (c.relation_vassalage) badges.push('<span class="badge vassal">封臣</span>');
  if (c.relation_trade_agreement) badges.push('<span class="badge">商约</span>');
  if (c.relation_intelligence_sharing) badges.push('<span class="badge">情报</span>');
  return badges.join("");
}

function signature(s) {
  if (!s) return "";
  return [
    s.date, s.tick,
    s.player ? `${s.player.gold}|${s.player.income}|${s.player.men}|${s.player.level}|${s.player.xp}` : "",
    s.messages.length, s.pending_events.length, s.pending_ultimatums.length,
    s.characters.length, s.saves.length, s.wars.length, s.armies.length,
    s.decisions ? s.decisions.length : 0,
    s.gameplays && s.gameplays.player ? s.gameplays.player.length : 0,
  ].join("::");
}

// ---------- 通信 ----------
async function fetchJson(url, options) {
  const resp = await fetch(url, options);
  if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
  return resp.json();
}

async function postAction(payload) {
  const snap = await fetchJson("/api/action", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  applySnapshot(snap);
}

async function refresh() {
  const snap = await fetchJson("/api/snapshot");
  applySnapshot(snap);
}

function applySnapshot(snap) {
  if (signature(snap) === signature(state)) return; // 无变化不重绘
  state = snap;
  charMap = {};
  for (const c of snap.characters || []) charMap[c.id] = c;
  renderAll();
}

// ---------- 渲染 ----------
function renderAll() {
  if (!state) return;
  renderMeta();
  renderPlayerCard();
  renderEventsCard();
  renderLogCard();
  renderCharacters();
  renderCounties();
  renderMilitary();
  renderPolitics();
  renderGameplays();
  renderFamily();
  renderPlayerSelect();
  renderLoadSelect();
}

function renderMeta() {
  $("#meta-date").textContent = state.date;
  $("#meta-season").textContent = seasonZh(state.season);
  $("#meta-tick").textContent = state.tick;
  $("#meta-scenario").textContent = state.player ? `${state.player.name} · ${state.player.title || ""}` : "—";
}

function renderPlayerCard() {
  const p = state.player;
  if (!p) { $("#player-card").innerHTML = '<div class="empty">暂无玩家</div>'; return; }
  const stat = (k, v, cls = "") => `<div class="stat"><div class="k">${k}</div><div class="v ${cls}">${v}</div></div>`;
  const pct = p.xp_to_next > 0 ? Math.min(100, Math.round((p.xp / (p.xp + p.xp_to_next)) * 100)) : 100;
  const laws = p.laws || {};
  $("#player-card").innerHTML = `
    <h2>👑 ${esc(p.name)} <span class="count">${esc(p.title || "")} · Lv.${p.level}</span></h2>
    <div class="stat-grid">
      ${stat("金币", p.gold.toLocaleString("zh-CN"), "gold")}
      ${stat("威望", Math.round(p.prestige).toLocaleString("zh-CN"))}
      ${stat("虔诚", Math.round(p.piety).toLocaleString("zh-CN"))}
      ${stat("压力", p.stress, p.stress >= 50 ? "red" : "")}
      ${stat("健康", p.health)}
      ${stat("月收入", p.income.toLocaleString("zh-CN"), "green")}
      ${stat("兵力", p.men.toLocaleString("zh-CN"), "blue")}
      ${stat("战争疲劳", state.player_war_exhaustion ?? "—")}
    </div>
    <div class="attrs">${attrZhList(p.attrs)}</div>
    <div class="xp-bar" title="经验 ${p.xp} / 下一级需 ${p.xp + p.xp_to_next}"><div style="width:${pct}%"></div></div>
    <div style="font-size:12px;color:var(--text-dim);margin-top:8px">
      继承法：<b>${esc(laws.succession || "—")}</b> · 王权：<b>${esc(laws.crown_authority ?? "—")}</b> · 性别法：<b>${esc(laws.gender_law || "—")}</b>
    </div>`;
}

function renderEventsCard() {
  const events = state.pending_events || [];
  const ultra = state.pending_ultimatums || [];
  let html = "";
  for (const e of events) {
    html += `<div class="event">
      <div class="et">📜 ${esc(e.title)}</div>
      <div class="ed">${esc(e.description)}</div>
      <div class="choices">
        ${(e.choices || []).map((c) =>
          `<button data-act="resolve_event" data-event="${e.event_id}" data-choice="${c.id}">${esc(c.text)}</button>`).join("")}
      </div></div>`;
  }
  for (const u of ultra) {
    html += `<div class="ultimatum">
      <div class="ut">⚠ ${esc(u.kind_zh)}最后通牒</div>
      <div class="ed">${esc(u.text)}（${u.members} 名成员）</div>
      <div class="ubtns">
        <button class="primary" data-act="respond_ultimatum" data-faction="${u.faction_id}" data-accept="true">接受</button>
        <button data-act="respond_ultimatum" data-faction="${u.faction_id}" data-accept="false">拒绝</button>
      </div></div>`;
  }
  if (!html) html = '<div class="empty">暂无待处理事件</div>';
  $("#events-card").innerHTML = `<h2>📨 待处理事件 <span class="count">${events.length + ultra.length}</span></h2>${html}`;
}

function renderLogCard() {
  const msgs = state.messages || [];
  const log = state.log || [];
  let html = msgs.slice().reverse().map((m) => `<div class="log-item msg">${esc(m)}</div>`).join("");
  html += log.slice().reverse().map((m) => `<div class="log-item">${esc(m)}</div>`).join("");
  if (!html) html = '<div class="empty">暂无日志</div>';
  $("#log-card").innerHTML = `<h2>📋 日志 <span class="count">${msgs.length + log.length}</span></h2><div class="log-box">${html}</div>`;
}

function renderCharacters() {
  const rows = (state.characters || []).map((c) => {
    const badges = [];
    if (c.id === state.player.id) badges.push('<span class="badge me">玩家</span>');
    badges.push(relationBadges(c));
    const age = c.age >= 0 ? `${c.age}岁` : "—";
    return `<tr class="${c.id === state.player.id ? "row-player" : ""}" data-cid="${c.id}">
      <td>${esc(c.name)}<div class="sub">${esc(c.dynasty_name || "")}</div></td>
      <td>${genderZh(c.gender)} · ${age}</td>
      <td>${esc(c.title || "—")}</td>
      <td>${badges.join("") || "—"}</td>
      <td class="num">${c.opinion_of_player ?? "—"}</td>
      <td class="num">${Math.round(c.gold)}</td>
      <td class="num">${Math.round(c.prestige)}</td>
      <td>${attrZhList(c.attrs)}</td>
    </tr>
    <tr class="char-detail" id="detail-${c.id}" hidden>
      <td colspan="8">
        ${detailOf(c)}
      </td>
    </tr>`;
  }).join("");
  $("#tab-characters").innerHTML = `
    <div class="section-block">
      <input id="char-filter" placeholder="🔍 搜索人物…" style="width:260px;margin-bottom:10px">
    </div>
    <table>
      <thead><tr>
        <th>姓名</th><th>性别/年龄</th><th>头衔</th><th>关系</th>
        <th class="num">好感</th><th class="num">金币</th><th class="num">威望</th><th>属性</th>
      </tr></thead>
      <tbody>${rows || '<tr><td colspan="8" class="empty">暂无人物</td></tr>'}</tbody>
    </table>`;
  // 行点击展开详情
  document.querySelectorAll("#tab-characters tbody tr[data-cid]").forEach((tr) => {
    tr.addEventListener("click", () => {
      const d = document.getElementById("detail-" + tr.dataset.cid);
      if (d) d.hidden = !d.hidden;
    });
  });
  const filter = document.getElementById("char-filter");
  if (filter) {
    filter.addEventListener("input", () => {
      const q = filter.value.trim();
      document.querySelectorAll("#tab-characters tbody tr[data-cid]").forEach((tr) => {
        const c = charMap[Number(tr.dataset.cid)];
        tr.hidden = q && c && !`${c.name}${c.dynasty_name || ""}${c.title || ""}`.includes(q);
        const d = document.getElementById("detail-" + tr.dataset.cid);
        if (d && !tr.hidden && q && c && `${c.name}`.includes(q)) d.hidden = false;
      });
    });
  }
}

function detailOf(c) {
  const spouses = (c.spouse_ids || []).map((id) => (charMap[id] ? charMap[id].name : `#${id}`)).join("、") || "无";
  const titles = (c.held_title_ids || []).length;
  return `<div style="color:var(--text-dim);font-size:12.5px;line-height:1.8">
    <b style="color:var(--text)">${esc(c.name)}</b>（#${c.id}）· 配偶：${esc(spouses)} · 持有头衔数：${titles}
    <br>对你的好感：<b>${c.opinion_of_player ?? "—"}</b> · 你对TA的好感：<b>${c.player_opinion ?? "—"}</b>
    <br>等级 Lv.${c.level}（经验 ${c.xp} / 距下一级 ${c.xp_to_next}）· 六维：${attrZhList(c.attrs)}
  </div>`;
}

function renderCounties() {
  const mine = (state.counties || []).filter((c) => c.is_player);
  const cards = mine.map((c) => {
    const buildings = (c.buildings || []).map((b) =>
      `<span class="badge" title="${esc(b.description || "")}">${esc(b.name)} Lv.${b.level}${b.can_upgrade ? " ↑" : ""}</span>`).join("");
    const devBtn = c.development < c.dev_cap
      ? `<button data-act="develop_county" data-county="${c.id}">开发（10金）</button>` : "";
    return `<div class="county-card">
      <h3>${esc(c.name)} <span>${esc(c.terrain || "")}</span></h3>
      <div class="stat-line"><span>发展度</span><b>${c.development} / ${c.dev_cap}</b></div>
      <div class="stat-line"><span>控制度</span><b>${c.control}</b></div>
      <div class="stat-line"><span>税收</span><b>${c.tax}/月</b></div>
      <div class="stat-line"><span>征召</span><b>${c.levies}</b></div>
      <div class="stat-line"><span>城堡</span><b>Lv.${c.fort}</b></div>
      <div class="stat-line"><span>港口</span><b>${c.has_port ? `Lv.${c.port_level}（${c.port_income}/月）` : "无"}</b></div>
      <div class="buildings">${buildings || '<span class="empty">无建筑</span>'}</div>
      ${devBtn ? `<div class="actions">${devBtn}</div>` : ""}
    </div>`;
  }).join("");
  $("#tab-counties").innerHTML = `
    <div class="section-block"><h3>己方领地（${mine.length}）</h3></div>
    ${cards || '<div class="empty">暂无己方领地</div>'}
    <div class="section-block" style="margin-top:22px"><h3>全部省份（${(state.counties || []).length}）</h3>
    <table><thead><tr><th>省份</th><th>地形</th><th>持有者</th><th class="num">发展</th><th class="num">控制</th><th class="num">税收</th></tr></thead>
    <tbody>${(state.counties || []).map((c) => `
      <tr class="${c.is_player ? "row-player" : ""}">
        <td>${esc(c.name)}</td><td>${esc(c.terrain || "")}</td><td>${esc(c.holder_name || "—")}</td>
        <td class="num">${c.development}</td><td class="num">${c.control}</td><td class="num">${c.tax}</td>
      </tr>`).join("")}</tbody></table></div>`;
}

function renderMilitary() {
  const armies = (state.armies || []).filter((a) => a.is_player);
  const warItems = (state.wars || []).map((w) => {
    const involved = w.involves_player ? "（你参与）" : "";
    const btn = w.active && w.involves_player && w.can_white_peace
      ? `<button data-act="white_peace" data-war="${w.id}">提议白和</button>` : "";
    return `<div class="war-item">
      <div class="wn">${esc(w.name)} <span class="badge ${w.active ? "atwar" : ""}">${w.active ? "进行中" : "已结束"}</span>${involved}</div>
      <div class="wsub">${esc(w.cb || "")} · ${esc(w.attacker)} vs ${esc(w.defender)} · 战况 ${w.warscore} · ${w.months} 个月</div>
      ${btn ? `<div class="wbtns">${btn}</div>` : ""}
    </div>`;
  }).join("");
  $("#tab-military").innerHTML = `
    <div class="section-block"><h3>军团（${armies.length}）</h3>
      ${armies.length ? `<table>
        <thead><tr><th>军团</th><th>位置</th><th class="num">兵力</th><th class="num">士气</th><th class="num">补给</th><th>状态</th></tr></thead>
        <tbody>${armies.map((a) => `
          <tr>
            <td>${esc(a.name)}</td><td>${esc(a.location_name)}</td>
            <td class="num">${a.men}</td><td class="num">${a.morale}</td>
            <td class="num">${a.supply}${a.supply_low ? " ⚠" : ""}</td>
            <td>${esc(a.status)}${a.in_enemy ? ' <span class="badge atwar">敌境</span>' : ""}</td>
          </tr>`).join("")}</tbody></table>` : '<div class="empty">无野战军（可在己方领地征召）</div>'}
    </div>
    <div class="section-block"><h3>战争（${(state.wars || []).length}）</h3>
      ${warItems || '<div class="empty">暂无战争</div>'}
    </div>`;
}

function renderPolitics() {
  const factions = (state.factions || []).map((f) => `
    <tr>
      <td>${esc(f.kind)}${f.ultimatum ? ' <span class="badge atwar">已下最后通牒</span>' : ""}</td>
      <td class="num">${f.members}</td><td class="num">${f.power}</td><td class="num">${f.discontent}</td>
      <td>${f.ultimatum ? "" : `<button data-act="appease_faction" data-faction="${f.id}">安抚（25金）</button>`}</td>
    </tr>`).join("");
  const council = state.player_council;
  const councilRows = council ? council.members.map((m) => `
    <tr><td>${esc(m.position_zh)}</td><td>${esc(m.holder_name)}</td><td>${esc(m.task_zh || "—")}</td></tr>`).join("")
    : '<tr><td colspan="3" class="empty">暂无内阁</td></tr>';
  const treaties = (state.treaties || []).map((t) =>
    `<tr><td>${esc(t.kind_zh)}</td><td>${esc(t.other_name)}</td><td class="num">${t.expires_year}</td></tr>`).join("");
  const schemes = (state.player_schemes || []).map((s) => `
    <tr><td>${esc(s.kind_zh)}</td><td>${esc(s.target_name)}</td><td class="num">${s.progress}%</td><td class="num">${s.secrecy}%</td></tr>`).join("");
  const claims = (state.player_claims || []).map((cl) => `
    <tr><td>${esc(cl.title_name || cl.county_name || "—")}</td><td>${cl.pressed ? "已主张" : "未主张"}</td><td class="num">${cl.strength}</td></tr>`).join("");
  const decisions = (state.decisions || []).map((d) => `
    <tr>
      <td>${esc(d.title)}<div class="sub">${esc(d.category || "")}${d.cost_gold ? ` · ${d.cost_gold}金` : ""}${d.cost_prestige ? ` · ${d.cost_prestige}威望` : ""}</div></td>
      <td>${d.available ? `<button data-act="execute_decision" data-decision="${esc(d.id)}">执行</button>`
        : `<span style="color:var(--red);font-size:12px">${esc(d.reason || "不可执行")}</span>`}</td>
    </tr>`).join("");
  $("#tab-politics").innerHTML = `
    <div class="section-block"><h3>派系（${(state.factions || []).length}）</h3>
      ${factions ? `<table><thead><tr><th>派系</th><th class="num">成员</th><th class="num">实力</th><th class="num">不满</th><th>行动</th></tr></thead><tbody>${factions}</tbody></table>`
        : '<div class="empty">暂无针对你的派系</div>'}
    </div>
    <div class="section-block"><h3>内阁</h3>
      <table><thead><tr><th>职位</th><th>任职者</th><th>任务</th></tr></thead><tbody>${councilRows}</tbody></table>
    </div>
    <div class="section-block"><h3>条约与停战（${(state.treaties || []).length}）</h3>
      ${treaties ? `<table><thead><tr><th>类型</th><th>对象</th><th class="num">到期年</th></tr></thead><tbody>${treaties}</tbody></table>`
        : '<div class="empty">暂无条约</div>'}
    </div>
    <div class="section-block"><h3>阴谋（${(state.player_schemes || []).length}）</h3>
      ${schemes ? `<table><thead><tr><th>类型</th><th>目标</th><th class="num">进度</th><th class="num">保密</th></tr></thead><tbody>${schemes}</tbody></table>`
        : '<div class="empty">无进行中阴谋</div>'}
    </div>
    <div class="section-block"><h3>宣称（${(state.player_claims || []).length}）</h3>
      ${claims ? `<table><thead><tr><th>目标</th><th>状态</th><th class="num">强度</th></tr></thead><tbody>${claims}</tbody></table>`
        : '<div class="empty">暂无宣称</div>'}
    </div>
    <div class="section-block"><h3>重大决策（${(state.decisions || []).length}）</h3>
      ${decisions ? `<table><thead><tr><th>决策</th><th>执行</th></tr></thead><tbody>${decisions}</tbody></table>`
        : '<div class="empty">暂无决策</div>'}
    </div>`;
}

function renderGameplays() {
  const systems = (state.gameplays && state.gameplays.player) || [];
  const html = systems.map((g) => {
    const stateRows = Object.entries(g.state || {}).map(([k, v]) => {
      const zh = { cooldown_months: "冷却(月)", has_physician: "宫廷医师", monthly_salary: "月薪" }[k] || k;
      const vs = typeof v === "boolean" ? (v ? "是" : "否") : v;
      return `<span>${zh}：<b>${vs}</b></span>`;
    }).join("");
    const btns = (g.actions || []).map((a) =>
      `<button data-act="gameplay_action" data-gp="${esc(g.id)}" data-op="${esc(a.op)}" title="${esc(a.desc || "")}">${esc(a.name)}</button>`).join("");
    return `<div class="gp-block">
      <h3>${esc(g.name)}</h3>
      <div class="gpd">${esc(g.description || "")}</div>
      <div class="gp-state">${stateRows || '<span class="empty">—</span>'}</div>
      <div class="gp-actions">${btns}</div>
    </div>`;
  }).join("");
  $("#tab-gameplays").innerHTML = `<div class="section-block"><h3>玩法目录（${systems.length}）</h3></div>${html || '<div class="empty">暂无玩法</div>'}`;
}

function renderFamily() {
  const fam = state.family || {};
  const spouses = (fam.spouses || []).map((s) => `
    <tr><td>${esc(s.name)}</td><td class="num">${s.age}岁</td></tr>`).join("");
  const children = (fam.children || []).map((c) => `
    <tr>
      <td>${esc(c.name)}</td><td>${genderZh(c.gender)}</td><td class="num">${c.age}岁</td>
      <td>${esc(c.status || "—")}</td>
      <td>${esc(c.education_focus_name || "未教育")}${c.education_years ? `（${c.education_years}年）` : ""}</td>
    </tr>`).join("");
  $("#tab-family").innerHTML = `
    <div class="family-row">
      <div class="section-block" style="flex:1;min-width:280px"><h3>配偶（${(fam.spouses || []).length}）</h3>
        <table><thead><tr><th>姓名</th><th class="num">年龄</th></tr></thead>
        <tbody>${spouses || '<tr><td colspan="2" class="empty">未婚</td></tr>'}</tbody></table></div>
      <div class="section-block" style="flex:2;min-width:320px"><h3>子女（${(fam.children || []).length}）</h3>
        <table><thead><tr><th>姓名</th><th>性别</th><th class="num">年龄</th><th>状态</th><th>教育</th></tr></thead>
        <tbody>${children || '<tr><td colspan="5" class="empty">无子女</td></tr>'}</tbody></table></div>
    </div>`;
}

function renderPlayerSelect() {
  const sel = $("#sel-player");
  const cur = state.player ? state.player.id : "";
  sel.innerHTML = (state.playable || []).map((r) =>
    `<option value="${r.id}" ${String(r.id) === String(cur) ? "selected" : ""}>${esc(r.name)} — ${esc(r.title)}</option>`).join("");
}

function renderLoadSelect() {
  const sel = $("#sel-load");
  const cur = sel.value;
  sel.innerHTML = '<option value="">— 读档 —</option>' +
    (state.saves || []).map((s) =>
      `<option value="${esc(s.name)}" ${s.name === cur ? "selected" : ""}>${esc(s.name)}${s.date ? `（${s.date}）` : ""}</option>`).join("");
}

async function loadScenarios() {
  try {
    const list = await fetchJson("/api/scenarios");
    const sel = $("#sel-scenario");
    sel.innerHTML = list.map((s) => `<option value="${esc(s.id)}">${esc(s.name)}</option>`).join("");
  } catch (e) { /* 忽略：默认场景仍可用 */ }
}

// ---------- 事件委托：所有操作按钮 ----------
document.addEventListener("click", async (ev) => {
  const btn = ev.target.closest("button[data-act]");
  if (!btn) return;
  btn.disabled = true;
  try {
    const act = btn.dataset.act;
    if (act === "resolve_event") {
      await postAction({ action: "resolve_event", event_id: Number(btn.dataset.event), choice_id: Number(btn.dataset.choice) });
    } else if (act === "respond_ultimatum") {
      await postAction({ action: "respond_ultimatum", faction_id: Number(btn.dataset.faction), accept: btn.dataset.accept === "true" });
    } else if (act === "develop_county") {
      await postAction({ action: "develop_county", county_id: Number(btn.dataset.county) });
    } else if (act === "white_peace") {
      await postAction({ action: "white_peace", war_id: Number(btn.dataset.war) });
    } else if (act === "appease_faction") {
      await postAction({ action: "appease_faction", faction_id: Number(btn.dataset.faction) });
    } else if (act === "execute_decision") {
      await postAction({ action: "execute_decision", decision_id: btn.dataset.decision });
    } else if (act === "gameplay_action") {
      await postAction({ action: "gameplay_action", gameplay: btn.dataset.gp, op: btn.dataset.op });
    }
  } catch (e) {
    console.error(e);
    alert("操作失败：" + e.message);
  } finally {
    btn.disabled = false;
  }
});

// ---------- 顶栏按钮 ----------
document.querySelectorAll(".tools button[data-days]").forEach((b) => {
  b.addEventListener("click", async () => {
    b.disabled = true;
    try { await postAction({ action: "advance", days: Number(b.dataset.days) }); }
    catch (e) { alert("推进失败：" + e.message); }
    finally { b.disabled = false; }
  });
});

$("#btn-save").addEventListener("click", async () => {
  const name = $("#inp-save").value.trim() || undefined;
  try { await postAction({ action: "save", name }); } catch (e) { alert("存档失败：" + e.message); }
});

$("#btn-load").addEventListener("click", async () => {
  const name = $("#sel-load").value;
  if (!name) return;
  try { await postAction({ action: "load", name }); } catch (e) { alert("读档失败：" + e.message); }
});

$("#btn-newgame").addEventListener("click", async () => {
  const scenario = $("#sel-scenario").value;
  if (!confirm(`确定开始新游戏？当前进度将丢失（场景：${scenario}）`)) return;
  try { await postAction({ action: "new_game", scenario }); } catch (e) { alert("新游戏失败：" + e.message); }
});

$("#btn-setplayer").addEventListener("click", async () => {
  const cid = Number($("#sel-player").value);
  if (!cid) return;
  try { await postAction({ action: "set_player", character_id: cid }); } catch (e) { alert("切换失败：" + e.message); }
});

// ---------- 标签页 ----------
document.querySelectorAll(".tabs .tab").forEach((tab) => {
  tab.addEventListener("click", () => {
    document.querySelectorAll(".tabs .tab").forEach((t) => t.classList.remove("active"));
    document.querySelectorAll(".tabpage").forEach((p) => p.classList.remove("active"));
    tab.classList.add("active");
    document.getElementById("tab-" + tab.dataset.tab).classList.add("active");
  });
});

document.querySelector("header h1").addEventListener("click", () => {
  refresh().catch((e) => console.error(e));
});

// ---------- 启动 ----------
loadScenarios();
refresh().catch((e) => console.error(e));
setInterval(() => { refresh().catch((e) => console.error(e)); }, POLL_MS);
