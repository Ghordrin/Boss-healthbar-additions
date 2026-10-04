// Compares the plugin's hand-kept boss data with the OSRS Wiki and the latest RuneLite API.
// Usage: node .github/scripts/check-boss-data.js <runelite-api.jar> [--update] [--out report.md]
// Needs Node 18+ and javap (JDK) on the PATH or in JAVA_HOME.
'use strict';

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { execFileSync } = require('child_process');

const ROOT = path.resolve(__dirname, '..', '..');
const SRC = path.join(ROOT, 'src/main/java/com/ghordrin/bosshealthbar');
const STATE_FILE = path.join(ROOT, '.github/boss-data/drain-pages.json');
const WIKI_API = 'https://oldschool.runescape.wiki/api.php';
const USER_AGENT = 'boss-health-bar-additions data check (github.com/Ghordrin/Boss-healthbar-additions)';

// Boss icons that aren't a single boss we draw a bar for.
const IGNORED_SPRITES = new Set([
	'TEMPOROSS', 'WINTERTODT', 'CHAMBERS_OF_XERIC', 'CHAMBERS_OF_XERIC_CHALLENGE_MODE', 'THEATRE_OF_BLOOD',
	'TOMBS_OF_AMASCUT', 'TOMBS_OF_AMASCUT_EXPERT',
]);
// Bosses with a wiki weakness that we leave out on purpose.
const IGNORED_WEAKNESS_PAGES = new Set(['maggot king']);
// Forms in our tables that the wiki's infoboxes don't list.
const UNLISTED_IDS = new Set(['TOA_KEPHRI_BOSS_WEAK', 'NIGHTMARE_CHALLENGE_INITIAL']);
// Besides the boss page and its /Strategies page, where the drain limit is written down.
const RAID_STRATEGIES = 'Tombs of Amascut/Strategies';
const EXTRA_DRAIN_SOURCES = {
	'Akkha': [RAID_STRATEGIES], 'Ba-Ba': [RAID_STRATEGIES], 'Kephri': [RAID_STRATEGIES], 'Zebak': [RAID_STRATEGIES],
	"Tumeken's Warden": [RAID_STRATEGIES], "Elidinis' Warden": [RAID_STRATEGIES],
};

const args = process.argv.slice(2);
const jar = args.find(a => !a.startsWith('--') && args[args.indexOf(a) - 1] !== '--out');
const update = args.includes('--update');
const outIndex = args.indexOf('--out');
const outFile = outIndex >= 0 ? args[outIndex + 1] : null;
if (!jar)
{
	console.error('Usage: node check-boss-data.js <runelite-api.jar> [--update] [--out report.md]');
	process.exit(2);
}

function javapConstants(className)
{
	const javap = process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin', 'javap') : 'javap';
	const out = execFileSync(javap, ['-cp', jar, '-constants', className], { encoding: 'utf8', maxBuffer: 64 << 20 });
	const constants = {};
	for (const m of out.matchAll(/static final int (\w+) = (-?\d+);/g))
	{
		constants[m[1]] = Number(m[2]);
	}
	return constants;
}

async function wiki(params)
{
	const url = WIKI_API + '?' + new URLSearchParams({ format: 'json', ...params });
	const res = await fetch(url, { headers: { 'User-Agent': USER_AGENT } });
	if (!res.ok)
	{
		throw new Error(`Wiki request failed: ${res.status} ${url}`);
	}
	return res.json();
}

async function monsterRows()
{
	const rows = [];
	for (let offset = 0; ; offset += 5000)
	{
		const query = "bucket('infobox_monster').select('page_name','id','elemental_weakness','elemental_weakness_percent')"
			+ `.limit(5000).offset(${offset}).run()`;
		const json = await wiki({ action: 'bucket', query });
		if (json.error)
		{
			throw new Error('Wiki bucket query failed: ' + json.error);
		}
		rows.push(...json.bucket);
		if (json.bucket.length < 5000)
		{
			return rows;
		}
	}
}

function rowIds(row)
{
	return [].concat(row.id || []).join(',').split(',').map(s => Number(s.trim())).filter(Boolean);
}

const basePage = name => name.split('#')[0].toLowerCase();
const read = file => fs.readFileSync(path.join(SRC, file), 'utf8');

function parseBossStats(npcIds)
{
	const src = read('BossStats.java');
	const weakness = new Map();
	const drainCap = new Map();
	const resolve = list => list.split(',').map(s => s.trim()).filter(Boolean).map(s => {
		const name = s.replace('NpcID.', '');
		if (!(name in npcIds))
		{
			throw new Error(`BossStats uses NpcID.${name}, which the RuneLite API no longer has`);
		}
		return npcIds[name];
	});
	for (const m of src.matchAll(/\.weakness\(Element\.(\w+), (\d+), ([^)]*)\)/g))
	{
		for (const id of resolve(m[3]))
		{
			weakness.set(id, { element: m[1][0] + m[1].slice(1).toLowerCase(), percent: Number(m[2]) });
		}
	}
	for (const m of src.matchAll(/\.drainCap\((\d+), ([^)]*)\)/g))
	{
		for (const id of resolve(m[2]))
		{
			drainCap.set(id, Number(m[1]));
		}
	}
	return { weakness, drainCap };
}

function parseKnownBosses()
{
	const src = read('KnownBosses.java');
	const sprites = new Set([...src.matchAll(/IconBoss25x25\.(\w+)/g)].map(m => m[1]));
	const names = new Set([...src.matchAll(/"([^"]+)"/g)].map(m => m[1].toLowerCase()));
	return { sprites, names };
}

// The sentences of a page that talk about lowering defence, so unrelated edits don't count as a change.
function drainSentences(wikitext)
{
	return wikitext
		.replace(/<ref[^>]*>[\s\S]*?<\/ref>/g, '')
		.split(/(?<=[.!?])\s+|\n+/)
		.filter(s => /defen[cs]e/i.test(s) && /(drain|lower|reduc|cap|minimum)/i.test(s))
		.map(s => s.trim());
}

// Null when the page doesn't exist.
async function pageWikitext(title)
{
	const json = await wiki({ action: 'parse', page: title, prop: 'wikitext', redirects: '1', formatversion: '2' });
	if (json.error)
	{
		if (json.error.code === 'missingtitle')
		{
			return null;
		}
		throw new Error(`Could not read wiki page ${title}: ${json.error.info}`);
	}
	return json.parse.wikitext;
}

const wikitextCache = new Map();
async function drainSources(page)
{
	const sources = [];
	for (const title of [page, page + '/Strategies', ...(EXTRA_DRAIN_SOURCES[page] || [])])
	{
		if (!wikitextCache.has(title))
		{
			wikitextCache.set(title, await pageWikitext(title));
		}
		const text = wikitextCache.get(title);
		if (text !== null)
		{
			sources.push({ title, sentences: drainSentences(text) });
		}
	}
	return sources;
}

const pageLink = title => `[${title}](https://oldschool.runescape.wiki/w/${encodeURIComponent(title.replace(/ /g, '_')).replace(/%2F/g, '/')})`;

async function main()
{
	const npcIds = javapConstants('net.runelite.api.gameval.NpcID');
	const spriteIds = javapConstants('net.runelite.api.gameval.SpriteID$IconBoss25x25');
	const { weakness, drainCap } = parseBossStats(npcIds);
	const known = parseKnownBosses();
	const rows = await monsterRows();
	const npcName = id => Object.keys(npcIds).find(k => npcIds[k] === id) || '?';

	const rowsById = new Map();
	for (const row of rows)
	{
		for (const id of rowIds(row))
		{
			if (!rowsById.has(id))
			{
				rowsById.set(id, []);
			}
			rowsById.get(id).push(row);
		}
	}
	const sections = [];
	const section = (title, lines) => lines.length && sections.push(`### ${title}\n\n${lines.map(l => '- ' + l).join('\n')}`);
	const fmt = r => `${r.page_name}: ${r.elemental_weakness || 'none'} ${r.elemental_weakness_percent || ''}`.trim();

	// 1. Our weakness values against the wiki.
	const weaknessDiffs = [];
	for (const [id, ours] of weakness)
	{
		const wikiRows = rowsById.get(id);
		if (!wikiRows)
		{
			weaknessDiffs.push(`\`NpcID.${npcName(id)}\` (${id}): no wiki monster has this ID any more`);
		}
		else if (!wikiRows.some(r => (r.elemental_weakness || '').toLowerCase() === ours.element.toLowerCase()
			&& Number(r.elemental_weakness_percent) === ours.percent))
		{
			weaknessDiffs.push(`\`NpcID.${npcName(id)}\` (${id}): we have ${ours.element} +${ours.percent}%, the wiki has ${wikiRows.map(fmt).join('; ')}`);
		}
	}
	section('Weakness values that differ from the wiki', weaknessDiffs);

	// 2. Wiki weaknesses on bosses we know, for IDs our table doesn't have (new forms, or a boss with no entry yet).
	const missingWeakness = [];
	for (const row of rows)
	{
		const page = basePage(row.page_name);
		const version = (row.page_name.split('#')[1] || '').toLowerCase();
		if (!row.elemental_weakness || row.elemental_weakness === 'None' || IGNORED_WEAKNESS_PAGES.has(page)
			|| !(known.names.has(page) || known.names.has(version)))
		{
			continue;
		}
		const missing = rowIds(row).filter(id => !weakness.has(id));
		if (missing.length)
		{
			missingWeakness.push(`${fmt(row)}% for ${missing.map(id => `\`NpcID.${npcName(id)}\` (${id})`).join(', ')}`);
		}
	}
	section('Wiki weaknesses missing from BossStats', missingWeakness);

	// 3. Boss icons in the RuneLite API that KnownBosses doesn't use: usually a newly released boss.
	const newSprites = Object.keys(spriteIds)
		.filter(name => !/^_\d+$/.test(name) && !known.sprites.has(name) && !IGNORED_SPRITES.has(name))
		.map(name => `\`SpriteID.IconBoss25x25.${name}\`: add the boss to KnownBosses (and BossStats, KillCounts if needed), or to IGNORED_SPRITES in this script`);
	section('New boss icons in the RuneLite API', newSprites);

	// 4. Drain limits come from article text, so flag pages whose sentences about lowering defence changed.
	const drainPages = new Map();
	const unlisted = [];
	for (const [id, cap] of drainCap)
	{
		const row = (rowsById.get(id) || [])[0];
		if (!row)
		{
			if (!UNLISTED_IDS.has(npcName(id)))
			{
				unlisted.push(`\`NpcID.${npcName(id)}\` (${id}): no wiki monster has this ID`);
			}
			continue;
		}
		const page = row.page_name.split('#')[0];
		if (!drainPages.has(page))
		{
			drainPages.set(page, new Set());
		}
		drainPages.get(page).add(cap);
	}
	const state = fs.existsSync(STATE_FILE) ? JSON.parse(fs.readFileSync(STATE_FILE, 'utf8')) : {};
	const newState = {};
	const drainChanges = [];
	for (const [page, caps] of [...drainPages].sort())
	{
		const sources = await drainSources(page);
		const text = sources.map(s => s.title + '\n' + s.sentences.join('\n')).join('\n');
		const hash = crypto.createHash('sha256').update(text).digest('hex').slice(0, 16);
		newState[page] = hash;
		if (state[page] !== hash)
		{
			const capText = [...caps].map(c => c ? '-' + c : 'no drain').join(', ');
			const quotes = sources.filter(s => s.sentences.length)
				.map(s => `  ${pageLink(s.title)}:\n` + s.sentences.map(line => `  > ${line}`).join('\n'));
			drainChanges.push(`**${page}** (we show ${capText})` + (state[page] ? '' : ', not reviewed before')
				+ (quotes.length ? '\n' + quotes.join('\n') : '\n  No text about lowering defence found'));
		}
	}
	section('Drain limit IDs the wiki doesn\'t list', unlisted);
	section('Drain limit pages whose text about lowering defence changed (re-check the value, then run with --update)', drainChanges);

	// 5. docs/bosses.md against the code.
	const docs = fs.readFileSync(path.join(ROOT, 'docs/bosses.md'), 'utf8');
	const docNames = new Set();
	const docProblems = [];
	for (const line of docs.split('\n'))
	{
		const cells = line.split('|').map(c => c.trim());
		if (cells.length < 5 || cells[1] === 'Boss' || cells[1].startsWith('---'))
		{
			continue;
		}
		const names = cells[1].split(' / ').map(n => n.toLowerCase());
		names.forEach(n => docNames.add(n));
		const ids = rows.filter(r => names.includes(basePage(r.page_name))).flatMap(rowIds);
		const ourWeakness = new Set(ids.filter(id => weakness.has(id)).map(id => `${weakness.get(id).element} +${weakness.get(id).percent}%`));
		const ourCaps = new Set(ids.filter(id => drainCap.has(id)).map(id => drainCap.get(id) ? '-' + drainCap.get(id) : 'no drain'));
		for (const w of ourWeakness)
		{
			const [element, percent] = w.split(' ');
			if (!cells[2].includes(percent) || !cells[2].includes(element))
			{
				docProblems.push(`${cells[1]}: the code has ${w}, the docs say "${cells[2]}"`);
			}
		}
		for (const c of ourCaps)
		{
			if (!cells[3].includes(c))
			{
				docProblems.push(`${cells[1]}: the code has a drain limit of ${c}, the docs say "${cells[3]}"`);
			}
		}
	}
	for (const name of known.names)
	{
		if (!docNames.has(name))
		{
			docProblems.push(`"${name}" is in KnownBosses but not in docs/bosses.md`);
		}
	}
	section('docs/bosses.md out of date', docProblems);

	if (update)
	{
		fs.mkdirSync(path.dirname(STATE_FILE), { recursive: true });
		fs.writeFileSync(STATE_FILE, JSON.stringify(newState, null, '\t') + '\n');
		console.log(`Saved the reviewed drain limit pages to ${path.relative(ROOT, STATE_FILE)}`);
	}

	const report = sections.join('\n\n');
	if (outFile)
	{
		fs.writeFileSync(outFile, report);
	}
	console.log(report || 'Everything matches.');
}

main().catch(e => {
	console.error(e.stack || e);
	process.exit(1);
});
