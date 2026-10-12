// ---- Игровая логика (порт Kotlin GameEngine) ----
const STATS = { STR: 'Сила', AGI: 'Ловкость', VIT: 'Выносливость', INT: 'Интеллект' };
const DIFFS = {
  EASY: { title: 'Новичок', mult: 0.7, hint: '70% от нормы' },
  NORMAL: { title: 'Охотник', mult: 1.0, hint: '100% от нормы' },
  HARD: { title: 'Монарх', mult: 1.3, hint: '130% от нормы' }
};
const EX = {
  PUSHUPS: { title: 'Отжимания', unit: 'повт.', stat: 'STR', base: 10, growth: 2, cap: 100, round: 1, dpu: 1.0 },
  SITUPS: { title: 'Пресс', unit: 'повт.', stat: 'VIT', base: 10, growth: 2, cap: 100, round: 1, dpu: 1.0 },
  SQUATS: { title: 'Приседания', unit: 'повт.', stat: 'AGI', base: 10, growth: 2, cap: 100, round: 1, dpu: 1.0 },
  RUN: { title: 'Бег', unit: 'км', stat: 'VIT', base: 1000, growth: 200, cap: 10000, round: 100, dpu: 0.1 },
  PLANK: { title: 'Планка', unit: '', stat: 'INT', base: 30, growth: 5, cap: 300, round: 5, dpu: 0.5, timed: true },
  BURPEES: { title: 'Берпи', unit: 'повт.', stat: 'STR', base: 10, growth: 1, cap: 100, round: 1, dpu: 1.0 }
};
const DAILY = ['PUSHUPS', 'SITUPS', 'SQUATS', 'RUN', 'PLANK'];
const QUICK = { RUN: [500, 1000, 2000], PLANK: [15, 30, 60] };
const quickSteps = t => QUICK[t] || [5, 10, 20];
const RANKS = [
  { letter: 'E', title: 'Слабейший охотник', min: 1 },
  { letter: 'D', title: 'Охотник', min: 10 },
  { letter: 'C', title: 'Опытный охотник', min: 20 },
  { letter: 'B', title: 'Элитный охотник', min: 35 },
  { letter: 'A', title: 'Охотник национального уровня', min: 50 },
  { letter: 'S', title: 'Монарх', min: 70 }
];
const rankOf = level => RANKS.filter(r => level >= r.min).pop();
const SHADOWS = [
  { name: 'Iron', tier: 'Рядовой', bonus: 2 }, { name: 'Tank', tier: 'Рядовой', bonus: 2 },
  { name: 'Fangs', tier: 'Рядовой', bonus: 3 }, { name: 'Tusk', tier: 'Элита', bonus: 3 },
  { name: 'Greed', tier: 'Элита', bonus: 4 }, { name: 'Kaisel', tier: 'Элита', bonus: 5 },
  { name: 'Igris', tier: 'Маршал', bonus: 6 }, { name: 'Bellion', tier: 'Маршал', bonus: 7 },
  { name: 'Beru', tier: 'Маршал', bonus: 8 }
];
const BOSSES = ['Каменный голем', 'Король ледяных медведей', 'Гигантский паук', 'Повелитель гоблинов',
  'Красный страж', 'Тёмный жрец', 'Страж врат', 'Королева муравьёв'];
const QUEST_SIZE = 5, MAX_TOKENS = 2, POINTS_PER_LEVEL = 3, BASE_STAT = 10;

function valueText(t, v) {
  if (t === 'RUN') return (v / 1000).toFixed(1);
  if (t === 'PLANK') return Math.floor(v / 60) + ':' + String(v % 60).padStart(2, '0');
  return String(v);
}
function stepLabel(t, v) {
  if (t === 'RUN') return '+' + valueText(t, v) + ' км';
  if (t === 'PLANK') return '+' + v + 'с';
  return '+' + v;
}

function newState() {
  return {
    accepted: false, name: 'Игрок', level: 1, xp: 0,
    stats: { STR: 10, AGI: 10, VIT: 10, INT: 10 },
    freePoints: 0, streak: 0, bestStreak: 0, totalQuests: 0, restTokens: 1,
    difficulty: 'NORMAL', restSeconds: 60,
    quest: { date: '', targets: {}, progress: {}, completed: false, rested: false, statsAwarded: [] },
    penalty: null, dungeon: null, shadows: [], extractionPity: 0
  };
}

const xpToNext = l => 100 + 40 * (l - 1);
const questXp = l => 60 + 8 * l;
function target(type, level, diff) {
  const t = EX[type];
  const raw = Math.min(t.cap, t.base + t.growth * (level - 1)) * DIFFS[diff].mult;
  return Math.max(t.round, Math.round(raw / t.round) * t.round);
}
const penaltyTarget = (level, missed) => Math.min(150, (10 + level) * Math.min(missed, 3));
const shadowBonusPct = s => s.shadows.reduce((a, n) => a + ((SHADOWS.find(x => x.name === n) || {}).bonus || 0), 0);
const streakBonusPct = s => Math.min(s.streak, 30);
const xpMultiplier = s => 1 + (shadowBonusPct(s) + streakBonusPct(s)) / 100;
const extractionChancePct = s => Math.min(90, 40 + (s.stats.INT ?? BASE_STAT) + s.extractionPity * 10);
const dailyDamage = (level, diff) =>
  DAILY.reduce((a, t) => a + Math.round(target(t, level, diff) * EX[t].dpu), 0);

const pad2 = n => String(n).padStart(2, '0');
const dateStr = d => d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate());
const epochDay = s => { const [y, m, d] = s.split('-').map(Number); return Math.round(Date.UTC(y, m - 1, d) / 86400000); };
const fromEpochDay = n => { const d = new Date(n * 86400000); return d.getUTCFullYear() + '-' + pad2(d.getUTCMonth() + 1) + '-' + pad2(d.getUTCDate()); };
function weekStart(todayS) {
  const e = epochDay(todayS);
  const dow = (e + 3) % 7; // 1970-01-01 = четверг; понедельник = 0
  return fromEpochDay(e - dow);
}

function ctx(s) { return { s: structuredClone(s), events: [] }; }

function grantXp(c, base) {
  if (base <= 0) return 0;
  let amount = Math.round(base * xpMultiplier(c.s));
  if (c.s.penalty) amount = Math.floor(amount / 2);
  let xp = c.s.xp + amount, level = c.s.level, points = c.s.freePoints;
  const oldRank = rankOf(level);
  while (xp >= xpToNext(level)) {
    xp -= xpToNext(level); level++; points += POINTS_PER_LEVEL;
    c.events.push({ type: 'LevelUp', level, points: POINTS_PER_LEVEL });
  }
  Object.assign(c.s, { xp, level, freePoints: points });
  if (rankOf(level) !== oldRank) c.events.push({ type: 'RankUp', rank: rankOf(level) });
  return amount;
}

function completeQuest(c) {
  const streak = c.s.streak + 1;
  c.s.quest.completed = true;
  c.s.streak = streak;
  c.s.bestStreak = Math.max(c.s.bestStreak, streak);
  c.s.totalQuests++;
  const idx = c.events.length;
  const bonus = grantXp(c, Math.floor(questXp(c.s.level) / 2));
  c.events.splice(idx, 0, { type: 'QuestComplete', xp: bonus, streak });
  if (streak % 7 === 0 && c.s.restTokens < MAX_TOKENS) {
    c.s.restTokens++;
    c.events.push({ type: 'RestTokenEarned' });
  }
}

function applyPenalty(c, missed) {
  const loss = Math.min(c.s.xp, Math.floor(xpToNext(c.s.level) / 4) * Math.min(missed, 4));
  const add = penaltyTarget(c.s.level, missed);
  const prev = c.s.penalty;
  const penalty = prev ? { target: Math.min(300, prev.target + add), progress: prev.progress } : { target: add, progress: 0 };
  c.s.xp -= loss; c.s.streak = 0; c.s.penalty = penalty;
  c.events.push({ type: 'PenaltyApplied', missed, xpLost: loss, burpees: penalty.target - penalty.progress });
}

function damageBoss(c, type, amount, rnd) {
  const d = c.s.dungeon;
  if (!d || d.defeated) return;
  const total = d.damage + Math.round(amount * EX[type].dpu);
  if (total < d.maxHp) { d.damage = total; return; }
  d.damage = d.maxHp; d.defeated = true;
  const gained = grantXp(c, questXp(c.s.level) * 3);
  c.events.push({ type: 'BossDefeated', name: d.bossName, xp: gained });
  const next = SHADOWS.find(x => !c.s.shadows.includes(x.name));
  if (next) {
    const chance = extractionChancePct(c.s);
    if (Math.floor(rnd() * 100) < chance) {
      c.s.shadows.push(next.name); c.s.extractionPity = 0; d.extraction = next.name;
      c.events.push({ type: 'ShadowExtracted', shadow: next });
    } else {
      c.s.extractionPity++; d.extraction = 'FAILED';
      c.events.push({ type: 'ExtractionFailed', chance });
    }
  }
}

function newQuest(s, date) {
  const targets = {};
  DAILY.forEach(t => { targets[t] = target(t, s.level, s.difficulty); });
  return { date, targets, progress: {}, completed: false, rested: false, statsAwarded: [] };
}

function accept(state, name) {
  const s = structuredClone(state);
  s.accepted = true; s.name = (name || '').trim() || 'Игрок';
  return s;
}

function refresh(state, todayS, rnd = Math.random) {
  if (!state.accepted) return { state, events: [] };
  const c = ctx(state);
  const q = c.s.quest;
  if (q.date !== todayS) {
    if (q.date) {
      const gap = epochDay(todayS) - epochDay(q.date);
      if (gap < 0) return { state, events: [] };
      const lastOk = q.completed || q.rested;
      const missed = (lastOk ? 0 : 1) + (gap - 1);
      if (missed > 0) applyPenalty(c, missed);
    }
    c.s.quest = newQuest(c.s, todayS);
  }
  const ws = weekStart(todayS);
  if (!c.s.dungeon || c.s.dungeon.weekStart !== ws) {
    const level = c.s.level;
    const idx = ((epochDay(ws) / 7 | 0) % BOSSES.length + BOSSES.length) % BOSSES.length;
    c.s.dungeon = {
      weekStart: ws, bossName: BOSSES[idx], rank: rankOf(level).letter,
      maxHp: dailyDamage(level, c.s.difficulty) * 5, damage: 0, defeated: false, extraction: null
    };
  }
  return { state: c.s, events: c.events };
}

function logProgress(state, type, amount, todayS, rnd = Math.random) {
  const q = state.quest;
  const tgt = q.targets[type];
  if (!state.accepted || !(amount > 0) || q.date !== todayS || q.rested || tgt == null) return { state, events: [] };
  const c = ctx(state);
  const old = q.progress[type] || 0;
  const nw = old + amount;
  const share = Math.floor(questXp(state.level) / QUEST_SIZE);
  const gain = Math.floor(share * Math.min(nw, tgt) / tgt) - Math.floor(share * Math.min(old, tgt) / tgt);
  c.s.quest.progress[type] = nw;
  grantXp(c, gain);
  if (nw >= tgt && !c.s.quest.statsAwarded.includes(type)) {
    const st = EX[type].stat;
    c.s.stats[st] = (c.s.stats[st] ?? BASE_STAT) + 1;
    c.s.quest.statsAwarded.push(type);
  }
  const cq = c.s.quest;
  if (!cq.completed && Object.entries(cq.targets).every(([t, v]) => (cq.progress[t] || 0) >= v)) completeQuest(c);
  damageBoss(c, type, amount, rnd);
  return { state: c.s, events: c.events };
}

function logPenalty(state, amount) {
  const p = state.penalty;
  if (!p || !(amount > 0)) return { state, events: [] };
  const s = structuredClone(state);
  const progress = p.progress + amount;
  if (progress >= p.target) { s.penalty = null; return { state: s, events: [{ type: 'PenaltyCleared' }] }; }
  s.penalty.progress = progress;
  return { state: s, events: [] };
}

function useRestDay(state, todayS) {
  const q = state.quest;
  if (state.restTokens <= 0 || q.date !== todayS || q.completed || q.rested) return state;
  const s = structuredClone(state);
  s.restTokens--; s.quest.rested = true;
  return s;
}

function allocate(state, stat) {
  if (state.freePoints <= 0) return state;
  const s = structuredClone(state);
  s.freePoints--; s.stats[stat] = (s.stats[stat] ?? BASE_STAT) + 1;
  return s;
}

const EVENT_TITLE = {
  QuestComplete: 'КВЕСТ ВЫПОЛНЕН', LevelUp: 'УРОВЕНЬ ПОВЫШЕН', RankUp: 'РАНГ ПОВЫШЕН',
  PenaltyApplied: 'ШТРАФНАЯ ЗОНА', PenaltyCleared: 'ШТРАФ СНЯТ', BossDefeated: 'ПОДЗЕМЕЛЬЕ ЗАЧИЩЕНО',
  ShadowExtracted: 'ИЗВЛЕЧЕНИЕ ТЕНИ', ExtractionFailed: 'ИЗВЛЕЧЕНИЕ ПРОВАЛЕНО', RestTokenEarned: 'ДЕНЬ ОТДЫХА'
};
function eventText(e) {
  switch (e.type) {
    case 'QuestComplete': return `Ежедневный квест завершён.\nБонус: +${e.xp} XP\nСерия: ${e.streak} дн.`;
    case 'LevelUp': return `Вы достигли уровня ${e.level}.\nПолучено свободных очков: ${e.points}`;
    case 'RankUp': return `Ваш ранг: ${e.rank.letter} — ${e.rank.title}.`;
    case 'PenaltyApplied': return `Квест не выполнен (пропущено дней: ${e.missed}).\nСерия сброшена, потеряно ${e.xpLost} XP.\nВыживите в штрафной зоне: ${e.burpees} берпи.\nПока штраф активен, опыт уменьшен вдвое.`;
    case 'PenaltyCleared': return 'Вы выжили в штрафной зоне.\nОпыт снова начисляется полностью.';
    case 'BossDefeated': return `Босс «${e.name}» повержен.\nНаграда: +${e.xp} XP`;
    case 'ShadowExtracted': return `«${e.shadow.name}» (${e.shadow.tier}) присягает вам на верность.\nБонус к опыту: +${e.shadow.bonus}%\nВстань.`;
    case 'ExtractionFailed': return `Тень рассеялась. Шанс был ${e.chance}%.\nСледующая попытка будет увереннее.`;
    case 'RestTokenEarned': return 'Серия достигла недели. Вы получили жетон дня отдыха.';
  }
  return '';
}

if (typeof module !== 'undefined') module.exports = {
  newState, accept, refresh, logProgress, logPenalty, useRestDay, allocate, target, xpToNext, questXp,
  weekStart, DAILY, EX, SHADOWS
};
