let config = {};
let currentPage = 'home';

const MENU = [
    { id: 'home', label: 'Home', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9,22 9,12 15,12 15,22"/></svg>' },
    { id: 'profiles', label: 'Profili', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M2.97 12.92A2 2 0 0 0 2 14.63v3.24a2 2 0 0 0 .97 1.71l3 1.8a2 2 0 0 0 2.06 0L12 19v-5.5l-5-3-4.03 2.42Z"/><path d="m7 16.5-4.74-2.85"/><path d="m7 16.5 5-3"/><path d="M7 16.5v5.17"/><path d="M12 13.5V19l3.97 2.38a2 2 0 0 0 2.06 0l3-1.8a2 2 0 0 0 .97-1.71v-3.24a2 2 0 0 0-.97-1.71L17 10.5l-5 3Z"/><path d="m17 16.5-5-3"/><path d="m17 16.5 4.74-2.85"/><path d="M17 16.5v5.17"/><path d="M7.97 4.42A2 2 0 0 0 7 6.13v4.37l5 3 5-3V6.13a2 2 0 0 0-.97-1.71l-3-1.8a2 2 0 0 0-2.06 0l-3 1.8Z"/><path d="M12 8 7.26 5.15"/><path d="m12 8 4.74-2.85"/><path d="M12 13.5V8"/></svg>' },
    { id: 'mods', label: 'Mod', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><path d="M3.27 6.96 12 12.01l8.73-5.05"/><path d="M12 22.08V12"/></svg>' },
    { id: 'versions', label: 'Versione', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>' },
    { id: 'settings', label: 'Impostazioni', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>' }
];

// ===== Helpers =====
function $(id) { return document.getElementById(id); }
function esc(s) { return String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])); }
let gameRunning = false;
let relaunching = false;

function toast(msg, type = 'info') {
    const icons = {
        success: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><path d="m9 11 3 3L22 4"/></svg>',
        error: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>'
    };
    const el = document.createElement('div');
    el.className = `toast ${type}`;
    el.innerHTML = `${icons[type] || ''}<span></span>`;
    el.querySelector('span').textContent = String(msg ?? '');
    $('toasts').appendChild(el);
    setTimeout(() => { el.style.opacity = '0'; el.style.transition = 'opacity 0.15s'; setTimeout(() => el.remove(), 150); }, 3500);
}

function errMsg(e) { return typeof e === 'string' ? e : e?.message || 'Errore imprevisto'; }

// ===== Theme (Light/Dark con animazione Liquid Glass) =====
function applyTheme(theme, animate) {
    const root = document.documentElement;
    if (animate) {
        const btn = $('theme-toggle');
        const r = btn ? btn.getBoundingClientRect() : { left: innerWidth / 2, top: 44, width: 42, height: 42 };
        const cx = r.left + r.width / 2, cy = r.top + r.height / 2;
        const maxR = Math.hypot(Math.max(cx, innerWidth - cx), Math.max(cy, innerHeight - cy));
        const reveal = document.createElement('div');
        reveal.className = 'theme-reveal';
        reveal.style.setProperty('--x', cx + 'px');
        reveal.style.setProperty('--y', cy + 'px');
        reveal.style.setProperty('--r', (maxR * 2) + 'px');
        document.body.appendChild(reveal);
        requestAnimationFrame(() => requestAnimationFrame(() => reveal.classList.add('grow')));
        root.classList.add('theme-transition');
        root.dataset.theme = theme;
        setTimeout(() => { reveal.remove(); root.classList.remove('theme-transition'); }, 920);
    } else {
        root.dataset.theme = theme;
    }
    try { localStorage.setItem('rc-theme', theme); } catch (e) {}
}

function toggleTheme() {
    const next = document.documentElement.dataset.theme === 'light' ? 'dark' : 'light';
    applyTheme(next, true);
}

function initTheme() {
    let saved = 'dark';
    try { saved = localStorage.getItem('rc-theme') || 'dark'; } catch (e) {}
    applyTheme(saved, false);
}

// ===== Luce speculare che segue il mouse (Liquid Glass) =====
document.addEventListener('mousemove', (e) => {
    const root = document.documentElement;
    root.style.setProperty('--mx', e.clientX + 'px');
    root.style.setProperty('--my', e.clientY + 'px');
}, { passive: true });

// ===== Ripple Material Expressive =====
document.addEventListener('pointerdown', (e) => {
    const el = e.target.closest('.btn, .chip, .menu-item, .dd-item, .tab, .toggle, .win-btn, .theme-toggle');
    if (!el) return;
    const rect = el.getBoundingClientRect();
    const d = Math.max(rect.width, rect.height);
    const ink = document.createElement('span');
    ink.className = 'ripple-ink';
    ink.style.width = ink.style.height = d + 'px';
    ink.style.left = (e.clientX - rect.left - d / 2) + 'px';
    ink.style.top = (e.clientY - rect.top - d / 2) + 'px';
    el.appendChild(ink);
    setTimeout(() => ink.remove(), 620);
}, { passive: true });

// ===== Minecraft Skin Avatar (CSS 3D) =====
const skinCache = new Map();

function imageDataUrl(src) {
    return new Promise((resolve) => {
        const img = new Image();
        img.onload = () => resolve(img);
        img.onerror = () => resolve(null);
        img.src = src;
    });
}

async function loadSkinTexture(uuid) {
    const key = uuid ? String(uuid).replace(/-/g, '') : 'offline';
    if (skinCache.has(key)) return skinCache.get(key);
    let dataUrl = null;
    if (uuid) {
        try {
            const r = await window.electronAPI.getSkin({ uuid });
            if (r && r.dataUrl) dataUrl = r.dataUrl;
        } catch (e) { dataUrl = null; }
    }
    const img = dataUrl ? await imageDataUrl(dataUrl) : null;
    const src = img || await imageDataUrl('steve.png');
    const norm = src ? Avatar.normalizeSkin(src) : null;
    skinCache.set(key, norm);
    return norm;
}

async function refreshAvatar(el, uuid, faceSize) {
    if (!el) return;
    const norm = await loadSkinTexture(uuid);
    if (!el.isConnected) return;
    el.innerHTML = '';
    el.appendChild(norm && norm.tex ? Avatar.makeHeadCube(norm.tex, faceSize) : document.createTextNode('?'));
}

async function refreshSkinPanel() {
    const fig = $('skin-figure');
    if (!fig) return;
    const acc = getActive();
    const uuid = acc && acc.type === 'microsoft' ? acc.uuid : null;
    fig.innerHTML = '';
    // Premium accounts render from their real UUID; offline accounts render as
    // MHF_Steve through the same API (no internal CSS models).
    const renderUuid = isPremiumUuid(uuid) ? uuid : MHF_STEVE_UUID;
    const renderImg = await loadBodyRender(renderUuid);
    if (!fig.isConnected) return;
    if (renderImg) fig.appendChild(renderImg);
}

// Full-body render via skin APIs (crafty.gg 3D render, fallback mc-heads.net).
// mc-heads also bakes the layer-2 overlay.
const MHF_STEVE_UUID = 'c06f89064c8a49119c29ea1dbd1aab82';

const SKIN_RENDER_PROVIDERS = [
    (uuid) => `https://render.crafty.gg/3d/full/${uuid}?width=240&height=360`,
    (uuid) => `https://mc-heads.net/body/${uuid}`
];

function isPremiumUuid(uuid) {
    return /^[0-9a-f]{32}$/i.test(String(uuid || '').replace(/-/g, ''));
}

function loadBodyRender(uuid) {
    return new Promise((resolve) => {
        const tryProvider = (i) => {
            if (i >= SKIN_RENDER_PROVIDERS.length) { resolve(null); return; }
            const img = new Image();
            img.className = 'skin-img';
            img.style.cssText = 'width:100%;height:100%;object-fit:contain;';
            img.onload = () => resolve(img);
            img.onerror = () => tryProvider(i + 1);
            img.src = SKIN_RENDER_PROVIDERS[i](uuid);
        };
        tryProvider(0);
    });
}

// ===== Accounts =====
function getActive() {
    const accs = config.accounts || [];
    if (!accs.length) return null;
    if (config.lastUsername) {
        const found = accs.find(a => a.name === config.lastUsername);
        if (found) return found;
    }
    return accs[0];
}

function updateUI() {
    const acc = getActive();
    $('acc-name').textContent = acc ? acc.name : 'Non connesso';
    $('acc-type').textContent = acc ? (acc.type === 'microsoft' ? 'Premium' : 'Offline') : 'Account offline';
    refreshAvatar($('acc-avatar'), acc && acc.type === 'microsoft' ? acc.uuid : null, 26);
    $('welcome-name').textContent = acc ? acc.name : 'Player';
    renderDropdown();
    renderNav();
    renderStats();
    refreshSkinPanel();
}

function renderDropdown() {
    const accs = config.accounts || [];
    const active = getActive();
    let html = '';
    accs.forEach(a => {
        const isActive = active && a.name === active.name;
        html += `<div class="dd-item ${isActive ? 'active' : ''}" role="button" tabindex="0" data-name="${esc(a.name)}">
            <div class="dd-avatar">${esc(a.name.charAt(0).toUpperCase())}</div>
            <div class="dd-info"><div class="dd-name">${esc(a.name)}</div><div class="dd-type">${a.type === 'microsoft' ? 'Premium' : 'Offline'}</div></div>
            ${isActive ? '<svg class="dd-check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><path d="m9 11 3 3L22 4"/></svg>' : ''}
            <span class="dd-remove" role="button" tabindex="0" title="Rimuovi"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:14px;height:14px"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg></span>
        </div>`;
    });
    if (accs.length) html += '<div class="dd-divider"></div>';
    html += `<button class="dd-add" id="dd-add"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:16px;height:16px"><path d="M12 5v14"/><path d="M5 12h14"/></svg>Aggiungi account</button>`;
    $('acc-dropdown').innerHTML = html;

    $('acc-dropdown').querySelectorAll('.dd-item').forEach(el => {
        const a = accs.find(x => x.name === el.dataset.name);
        refreshAvatar(el.querySelector('.dd-avatar'), a && a.type === 'microsoft' ? a.uuid : null, 21);
        el.addEventListener('click', async (e) => {
            if (e.target.closest('.dd-remove')) {
                e.stopPropagation();
                if (confirm('Rimuovere questo account?')) {
                    config.accounts = config.accounts.filter(a => a.name !== el.dataset.name);
                    if (config.lastUsername === el.dataset.name) {
                        config.lastUsername = config.accounts[0]?.name || '';
                        config.lastAccountType = config.accounts[0]?.type || null;
                    }
                    await window.electronAPI.saveConfig(config);
                    updateUI();
                    toast('Account rimosso', 'success');
                }
                return;
            }
            config.lastUsername = el.dataset.name;
            config.lastAccountType = config.accounts.find(a => a.name === el.dataset.name)?.type;
            await window.electronAPI.saveConfig(config);
            updateUI();
            $('acc-dropdown').classList.remove('show');
            toast(`Account ${el.dataset.name} selezionato`, 'success');
        });
    });

    $('dd-add')?.addEventListener('click', () => {
        $('acc-dropdown').classList.remove('show');
        $('login-overlay').classList.remove('hidden');
    });
}

function renderNav() {
    $('nav').innerHTML = MENU.map(m => `<button class="menu-item ${currentPage === m.id ? 'active' : ''}" data-page="${m.id}">${m.icon}<span>${m.label}</span></button>`).join('');
    $('nav').querySelectorAll('.menu-item').forEach(b => b.addEventListener('click', () => navigate(b.dataset.page)));
}

async function renderStats() {
    const profile = (config.profiles || []).find(p => p.id === activeProfileId());
    const profileName = profile?.name || 'Default';
    const mcVer = profile?.mcVersion || config.settings?.mcVersionOverride || '1.21.8';
    const fabVer = profile?.fabricLoaderOverride || config.settings?.fabricLoaderOverride || 'latest';
    let modCount = '…';
    try {
        const mods = await window.electronAPI.listInstalledMods(activeProfileId());
        modCount = String(Array.isArray(mods) ? mods.length : 0);
    } catch (_) { modCount = '?'; }

    $('stats').innerHTML = `
        <div><div class="stat-label">Profilo</div><div class="stat-val"><span class="stat-dot" style="background:var(--accent)"></span>${esc(profileName)}<button type="button" class="stat-link" id="stat-change-profile">cambia</button></div></div>
        <div class="stat-sep"></div>
        <div><div class="stat-label">Stato</div><div class="stat-val"><span class="stat-dot" id="srv-dot" style="background:${gameRunning ? 'var(--ok)' : 'hsl(240 5% 65% / 0.4)'}"></span><span id="srv-status">${gameRunning ? 'In gioco' : 'Pronto'}</span></div></div>
        <div><div class="stat-label">Minecraft</div><div class="stat-val">${esc(mcVer)}</div></div>
        <div><div class="stat-label">Fabric</div><div class="stat-val">${esc(fabVer)}</div></div>
        <div><div class="stat-label">Mod</div><div class="stat-val">${esc(modCount)} installate</div></div>
    `;
    $('stat-change-profile')?.addEventListener('click', () => navigate('profiles'));
}

// ===== Navigation =====
function navigate(page) {
    currentPage = page;
    renderNav();
    document.querySelectorAll('[id^="page-"]').forEach(p => {
        const show = p.id === `page-${page}`;
        p.style.display = show ? '' : 'none';
        if (show) { p.classList.remove('page-enter'); void p.offsetWidth; p.classList.add('page-enter'); }
    });
    if (page === 'profiles') renderProfiles();
    if (page === 'mods') renderMods();
    if (page === 'versions') renderVersions();
    if (page === 'settings') renderSettings();
}

function activeProfileId() {
    const profiles = config.profiles || [];
    return config.lastProfileId || profiles[0]?.id || 'default';
}

async function ensureDefaultProfile() {
    if (!config.profiles) config.profiles = [];
    if (!config.profiles.length) {
        const profile = { id: 'default', name: 'Default', mcVersion: '1.21.8', ramMB: 4096, isFabric: true, iconColor: '#FCAD14' };
        config.profiles.push(profile);
        config.lastProfileId = 'default';
        await window.electronAPI.createInstance(profile);
        await window.electronAPI.saveConfig(config);
    }
}

async function syncActiveInstance(id) {
    try {
        const data = await window.electronAPI.getInstances();
        data.activeId = id;
        if (!data.instances.some(i => i.id === id)) {
            const p = (config.profiles || []).find(x => x.id === id);
            await window.electronAPI.createInstance(p || { id, name: id, mcVersion: '1.21.8', ramMB: 4096, isFabric: true });
            return;
        }
        await window.electronAPI.saveInstances(data);
    } catch (e) { console.error('syncActiveInstance', e); }
}

async function renderProfiles() {
    await ensureDefaultProfile();
    const profiles = config.profiles || [];
    const activeProfile = activeProfileId();
    const el = $('page-profiles');

    el.innerHTML = `
        <div class="page-head">
            <div><h1 class="page-title">Profili</h1><p class="page-subtitle">Ogni profilo ha la propria cartella mods/saves</p></div>
            <button class="btn btn-primary" id="new-profile-btn" style="height:36px;padding:0 16px;font-size:13px;">+ Nuovo profilo</button>
        </div>
        <div class="grid-2" id="profiles-grid"></div>
    `;

    const grid = $('profiles-grid');
    grid.innerHTML = profiles.map(p => `
        <div class="card ${p.id === activeProfile ? 'selected' : ''}" data-id="${esc(p.id)}" style="cursor:pointer;">
            <div class="card-head">
                <span class="card-title">${esc(p.name)}</span>
                ${p.id === activeProfile ? '<span class="badge badge-green">Attivo</span>' : ''}
            </div>
            <div class="card-desc">MC ${esc(p.mcVersion || '1.21.8')} · ${esc(p.ramMB || 4096)}MB RAM${p.isFabric !== false ? ' · Fabric' : ''}</div>
            <div class="card-foot">
                ${p.id === 'default' ? '' : `<button type="button" class="btn btn-ghost btn-sm profile-delete" data-id="${esc(p.id)}" style="color:var(--error);font-size:12px;">Elimina</button>`}
            </div>
        </div>
    `).join('');

    grid.querySelectorAll('.card').forEach(card => {
        card.addEventListener('click', async (e) => {
            const del = e.target.closest('.profile-delete');
            if (del) {
                e.stopPropagation();
                const id = del.dataset.id;
                if (id === 'default') return;
                if (confirm('Eliminare questo profilo e i suoi file locali?')) {
                    config.profiles = config.profiles.filter(p => p.id !== id);
                    if (config.lastProfileId === id) config.lastProfileId = config.profiles[0]?.id || 'default';
                    await window.electronAPI.deleteInstance(id);
                    await window.electronAPI.saveConfig(config);
                    await syncActiveInstance(config.lastProfileId);
                    renderProfiles();
                    toast('Profilo eliminato', 'success');
                }
                return;
            }
            config.lastProfileId = card.dataset.id;
            await window.electronAPI.saveConfig(config);
            await syncActiveInstance(config.lastProfileId);
            renderProfiles();
            toast('Profilo selezionato', 'success');
        });
    });

    $('new-profile-btn')?.addEventListener('click', async () => {
        const overlay = document.createElement('div');
        overlay.className = 'overlay';
        overlay.innerHTML = `
            <div class="dialog" style="max-width:320px;">
                <div class="dialog-head">
                    <h2 style="font-size:18px;font-weight:600;">Nuovo profilo</h2>
                    <p style="font-size:13px;color:var(--on-surface-variant);margin-top:4px;">Inserisci il nome del profilo</p>
                </div>
                <input type="text" class="input" id="profile-name-input" placeholder="Es. Modded 1.21" style="margin-bottom:16px;" autofocus>
                <div style="display:flex;gap:8px;justify-content:flex-end;">
                    <button type="button" class="btn btn-ghost btn-sm" id="profile-cancel" style="height:32px;padding:0 12px;">Annulla</button>
                    <button type="button" class="btn btn-primary btn-sm" id="profile-confirm" style="height:32px;padding:0 12px;">Crea</button>
                </div>
            </div>
        `;
        document.body.appendChild(overlay);
        const input = overlay.querySelector('#profile-name-input');
        input.focus();
        const doCreate = async () => {
            const name = input.value.trim();
            if (!name) return;
            const id = name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') + '-' + Date.now();
            const profile = {
                id, name,
                mcVersion: config.settings?.mcVersionOverride || '1.21.8',
                ramMB: config.settings?.ramMB || 4096,
                isFabric: true,
                fabricLoaderOverride: config.settings?.fabricLoaderOverride || '',
                iconColor: '#FCAD14'
            };
            if (!config.profiles) config.profiles = [];
            config.profiles.push(profile);
            config.lastProfileId = id;
            await window.electronAPI.createInstance(profile);
            await window.electronAPI.saveConfig(config);
            overlay.remove();
            renderProfiles();
            toast('Profilo creato!', 'success');
        };
        overlay.querySelector('#profile-confirm').addEventListener('click', doCreate);
        input.addEventListener('keydown', e => { if (e.key === 'Enter') doCreate(); if (e.key === 'Escape') overlay.remove(); });
        overlay.querySelector('#profile-cancel').addEventListener('click', () => overlay.remove());
        overlay.addEventListener('click', e => { if (e.target === overlay) overlay.remove(); });
    });
}

// ===== Mods Page =====
const KNOWN_MOD_NAMES = { 'fabric-api': 'Fabric API', 'roleplayclient': 'Roleplay Client' };

const MOD_COLORS = ['#FCAD14', '#247CE2', '#34D399', '#F472B6', '#60A5FA', '#FBBF24', '#A78BFA', '#FB7185', '#2DD4BF', '#FDBA74'];

// Pacchetti interni di Roleplay Client (stile moduli): mostrati come guida nella pagina Mod.
// { id, name, type, key, desc }
const LOCO_PACKAGES = [
    { id: 'fps', name: 'Contatore FPS', type: 'HUD', key: '', desc: 'Mostra gli FPS, posizionabile con l\'editor HUD.' },
    { id: 'cps', name: 'Contatore CPS', type: 'HUD', key: '', desc: 'Click al secondo, sinistro e destro.' },
    { id: 'coords', name: 'Coordinate', type: 'HUD', key: '', desc: 'Posizione X Y Z e dimensione corrente.' },
    { id: 'clock', name: 'Orologio', type: 'HUD', key: '', desc: 'Ora di sistema (orario reale).' },
    { id: 'ping', name: 'Ping', type: 'HUD', key: '', desc: 'Latenza verso il server in tempo reale.' },
    { id: 'armor', name: 'Armatura', type: 'HUD', key: '', desc: 'Armature equipaggiate con durabilità rimanente.' },
    { id: 'oramondo', name: 'Ora del mondo', type: 'HUD', key: '', desc: 'Ora in-game del server.' },
    { id: 'waypoint', name: 'Waypoint', type: 'HUD', key: '', desc: 'Posizioni salvate con distanza e direzione.' },
    { id: 'volti', name: 'Volti conosciuti', type: 'RP', key: '', desc: 'Badge "i" in chat e tab, nota al passaggio del mouse e avviso quando si collegano.' },
    { id: 'rpmessages', name: 'Messaggi rapidi', type: 'RP', key: '+', desc: 'Frasi e comandi pronti: "+" apre la lista, clic per inviare.' },
    { id: 'logscene', name: 'Diario scene', type: 'RP', key: '', desc: 'Registra le scene RP (frasi con *) nel file roleplay-client-scene-log.txt.' },
    { id: 'menzioni', name: 'Menzioni', type: 'RP', key: '', desc: 'Avviso quando il tuo nome viene menzionato in chat.' },
    { id: 'sveglie', name: 'Sveglie', type: 'Utility', key: '', desc: 'Sveglie a orario reale con avviso sonoro.' },
    { id: 'zoom', name: 'Zoom', type: 'Utility', key: 'Alt', desc: 'Tieni premuto Alt per ingrandire la visuale.' },
    { id: 'cinema', name: 'Cinema', type: 'Utility', key: 'ò', desc: 'Nasconde l\'HUD per una visuale cinematografica.' }
];

// Icone dei pacchetti (SVG Material, bianche su sfondo colorato).
const PACKAGE_ICONS = {
    fps: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M6 20v-8"/><path d="M12 20V6"/><path d="M18 20v-12"/></svg>',
    cps: '<svg viewBox="0 0 24 24" fill="currentColor"><circle cx="9" cy="12" r="3.5"/><circle cx="16.5" cy="12" r="3.5"/></svg>',
    coords: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="6"/><path d="M12 2v5M12 17v5M2 12h5M17 12h5"/></svg>',
    clock: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="8"/><path d="M12 7v5l3 2"/></svg>',
    ping: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12h4l3-7 4 14 3-7h6"/></svg>',
    armor: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round"><path d="M12 3l7 3v5c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V6z"/></svg>',
    oramondo: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c2.5 2.6 3.8 5.7 3.8 9S14.5 18.4 12 21c-2.5-2.6-3.8-5.7-3.8-9S9.5 5.6 12 3z"/></svg>',
    waypoint: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round"><path d="M12 3c-3.3 0-6 2.6-6 5.8 0 4 6 11 6 11s6-7 6-11C18 5.6 15.3 3 12 3z"/><circle cx="12" cy="9" r="2" fill="currentColor" stroke="none"/></svg>',
    volti: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="9" cy="8" r="3.5"/><path d="M3.5 20c.5-4 2.7-6 5.5-6s5 2 5.5 6"/><circle cx="16.5" cy="9" r="2.8"/><path d="M15 14.5c1.3-.5 2.5-.5 3.8 0"/></svg>',
    rpmessages: '<svg viewBox="0 0 24 24"><path d="M12 4c-4.4 0-8 3.1-8 7 0 2.1 1.1 4 2.9 5.3L6 20l3.9-2c.7.2 1.4.3 2.1.3 4.4 0 8-3.1 8-7s-3.6-7-8-7z" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/><circle cx="9" cy="11" r="1.1" fill="currentColor"/><circle cx="12.5" cy="11" r="1.1" fill="currentColor"/><circle cx="16" cy="11" r="1.1" fill="currentColor"/></svg>',
    logscene: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round"><path d="M12 5c-2-1.5-4.5-2-8-2v16c3.5 0 6 .5 8 2 2-1.5 4.5-2 8-2V3c-3.5 0-6 .5-8 2z"/><path d="M12 5v16"/></svg>',
    menzioni: '<svg viewBox="0 0 24 24"><path d="M12 4c-4.4 0-8 3.1-8 7 0 2.1 1.1 4 2.9 5.3L6 20l3.9-2c.7.2 1.4.3 2.1.3 4.4 0 8-3.1 8-7s-3.6-7-8-7z" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/><path d="M12 8v4" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><circle cx="12" cy="14.5" r="1" fill="currentColor"/></svg>',
    sveglie: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="13" r="7"/><path d="M12 10v3l2 2"/><path d="M8 3L5.5 6"/><path d="M16 3l2.5 3"/></svg>',
    zoom: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="6"/><path d="M15.5 15.5L20 20"/></svg>',
    cinema: '<svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5l11 7-11 7z"/></svg>'
};

function modColor(str) {
    let h = 0;
    for (let i = 0; i < str.length; i++) h = (h * 31 + str.charCodeAt(i)) >>> 0;
    return MOD_COLORS[h % MOD_COLORS.length];
}

async function renderMods() {
    const el = $('page-mods');
    el.innerHTML = `
        <div class="page-head">
            <div><h1 class="page-title">Mod</h1><p class="page-subtitle">Gestisci le mod dell'istanza</p></div>
            <div style="display:flex;gap:8px;">
                <button class="btn btn-ghost btn-sm" id="mod-browse">Sfoglia</button>
                <button class="btn btn-primary btn-sm" id="mod-modrinth">Scarica da Modrinth</button>
            </div>
        </div>
        <div id="mods-list"><div class="empty">Caricamento...</div></div>
        <div id="mods-guide" style="margin-top:24px;"></div>
    `;
    $('mod-browse').addEventListener('click', browseMods);
    $('mod-modrinth').addEventListener('click', openModrinth);
    renderPackagesGuide();
    await loadModsList();
}

// Guida dei pacchetti integrati nella pagina Mod
function renderPackagesGuide() {
    const el = $('mods-guide');
    const typeColor = { HUD: '#34D399', RP: '#FCAD14', Utility: '#247CE2' };
    const cards = LOCO_PACKAGES.map(p => `
        <div class="card" style="padding:12px;">
            <div class="card-head">
                <span style="display:inline-flex;align-items:center;gap:10px;">
                    <span class="mod-icon" style="background:${typeColor[p.type]}22;color:${typeColor[p.type]};">${PACKAGE_ICONS[p.id] || ''}</span>
                    <span class="card-title">${esc(p.name)}</span>
                </span>
                <span class="badge" style="background:${typeColor[p.type]}22;color:${typeColor[p.type]};">${p.type}</span>
            </div>
            <p class="card-desc">${esc(p.desc)}</p>
            ${p.key ? `<div class="badge badge-muted" style="margin-top:8px;">Tasto: ${esc(p.key)}</div>` : ''}
        </div>
    `).join('');
    el.innerHTML = `
        <details open>
            <summary style="cursor:pointer;font-size:16px;font-weight:700;margin-bottom:12px;user-select:none;">
                Pacchetti integrati del client
            </summary>
            <p class="card-desc" style="margin-bottom:12px;">
                I pacchetti si attivano dal menu in gioco: <b>Opzioni → Controlli → "Roleplay Client Mods" → Moduli</b>.
                Le posizioni dell'HUD si regolano con "Editor HUD". I tasti si cambiano da Opzioni → Controlli (categoria Roleplay Client).
                Le mod esterne sono sempre attive: si installano e si rimuovono solo dal launcher.
            </p>
            <div class="grid-3" style="grid-template-columns:repeat(auto-fill,minmax(260px,1fr));">${cards}</div>
        </details>
    `;
}

async function loadModsList() {
    const container = $('mods-list');
    let mods = [];
    try {
        mods = await window.electronAPI.listInstalledMods(activeProfileId());
    } catch (e) {
        container.innerHTML = `<div class="empty">Errore: ${esc(errMsg(e))}</div>`;
        return;
    }

    if (!mods.length) {
        container.innerHTML = `<div class="empty">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/></svg>
            <h3>Nessuna mod installata</h3>
            <p>Usa "Sfoglia" per aggiungere file .jar oppure scarica mod direttamente da Modrinth.</p>
        </div>`;
        return;
    }

    container.innerHTML = mods.map(m => {
        const isProtected = ['fabric-api', 'roleplayclient'].some(p => m.fileName.toLowerCase().includes(p));
        const color = modColor(m.id);
        const name = KNOWN_MOD_NAMES[m.fileName.replace(/\.jar$/i, '').toLowerCase()] || m.name;
        const icon = `<div class="mod-icon" style="background:${color}20;color:${color};">${esc(name.charAt(0))}${m.iconUrl ? `<img class="mod-icon-img" src="${esc(m.iconUrl)}" alt="" loading="lazy" onerror="this.remove()">` : ''}</div>`;
        return `
        <div class="mod-card" data-mod="${esc(m.fileName)}">
            ${icon}
            <div class="mod-info">
                <div class="mod-name">${esc(name)}</div>
                <div class="mod-ver">${esc(m.fileName)}</div>
                <div class="mod-desc">${isProtected ? 'Mod essenziale del client' : 'Caricata all\'avvio del gioco'}</div>
            </div>
            ${isProtected ? '' : `<button class="mod-del" title="Elimina mod" data-del="${esc(m.fileName)}"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/><path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg></button>`}
        </div>`;
    }).join('');

    container.querySelectorAll('.mod-del').forEach(btn => {
        btn.addEventListener('click', async () => {
            if (!confirm('Eliminare definitivamente questa mod?')) return;
            await window.electronAPI.deleteMod(btn.dataset.del, activeProfileId());
            toast('Mod eliminata', 'success');
            loadModsList();
        });
    });
}

async function browseMods() {
    try {
        const files = await window.electronAPI.selectModFile();
        if (!files || !files.length) return;
        const r = await window.electronAPI.installModFiles(files, activeProfileId());
        toast(r.installed.length ? `${r.installed.length} mod installate` : 'Nessun file .jar selezionato', r.installed.length ? 'success' : 'error');
        loadModsList();
    } catch (e) { toast(errMsg(e), 'error'); }
}

// ===== Modrinth =====
function isModInstalled(fileNames, slug) {
    const s = slug.toLowerCase();
    return fileNames.some(f => {
        const base = f.replace(/\.jar$/i, '').toLowerCase();
        return base === s || base.startsWith(s + '-') || base.startsWith(s + '_') || base.startsWith(s + '.');
    });
}

// Filtri categorie di default di Modrinth (lato sinistro del sito)
const MODRINTH_CATEGORIES = [
    { id: '', label: 'Tutte' },
    { id: 'optimization', label: 'Ottimizzazione' },
    { id: 'gameplay', label: 'Gameplay' },
    { id: 'worldgen', label: 'Mondo' },
    { id: 'utility', label: 'Utilità' },
    { id: 'technology', label: 'Tecnologia' },
    { id: 'library', label: 'Libreria' },
    { id: 'adventure', label: 'Avventura' },
    { id: 'decoration', label: 'Decorazione' },
    { id: 'food', label: 'Cibo' },
    { id: 'magic', label: 'Magia' },
    { id: 'social', label: 'Social' },
    { id: 'equipment', label: 'Equipaggiamento' },
    { id: 'storage', label: 'Storage' },
    { id: 'mobs', label: 'Mob' }
];

const MODRINTH_SORTS = [
    { id: 'downloads', label: 'Più scaricate' },
    { id: 'relevance', label: 'Rilevanza' },
    { id: 'follows', label: 'Più seguite' },
    { id: 'updated', label: 'Aggiornate di recente' },
    { id: 'newest', label: 'Nuove' }
];

function openModrinth() {
    const old = $('modrinth-overlay');
    if (old) old.remove();

    const mcVersion = config.settings?.mcVersionOverride || '1.21.8';
    const fabricVersion = config.settings?.fabricLoaderOverride || '0.19.3';
    const state = { categories: new Set(), index: 'downloads', lastQuery: '' };

    const overlay = document.createElement('div');
    overlay.className = 'overlay';
    overlay.id = 'modrinth-overlay';
    overlay.innerHTML = `
        <div class="dialog modrinth-dialog">
            <div class="dialog-head" style="text-align:left;">
                <div style="display:flex;align-items:flex-start;justify-content:space-between;gap:16px;">
                    <div>
                        <h2 class="dialog-title">Scarica da Modrinth</h2>
                        <p class="dialog-desc" id="modrinth-desc">Mod per Minecraft ${mcVersion} + Fabric ${fabricVersion}</p>
                    </div>
                    <button class="btn btn-ghost btn-sm" id="modrinth-close" style="flex-shrink:0;">✕</button>
                </div>
            </div>
            <div style="display:flex;gap:8px;margin-bottom:10px;">
                <input type="text" class="input" id="modrinth-q" placeholder="Cerca mod... (es. Sodium, Iris, Aether)" style="flex:1;" autocomplete="off">
                <button class="btn btn-primary" id="modrinth-go" style="height:40px;padding:0 24px;">Cerca</button>
            </div>
            <div style="display:flex;align-items:center;gap:8px;margin-bottom:10px;flex-wrap:wrap;">
                <label style="font-size:12px;color:var(--on-surface-variant);">Ordina:</label>
                <select class="input" id="modrinth-sort" style="width:auto;height:32px;padding:2px 8px;font-size:13px;">
                    ${MODRINTH_SORTS.map(s => `<option value="${s.id}" ${s.id === state.index ? 'selected' : ''}>${s.label}</option>`).join('')}
                </select>
            </div>
            <div style="display:flex;gap:6px;margin-bottom:12px;flex-wrap:wrap;" id="modrinth-cats">
                ${MODRINTH_CATEGORIES.map(c => `<button class="chip ${c.id === '' ? 'on' : ''}" data-cat="${c.id}">${c.label}</button>`).join('')}
            </div>
            <div id="modrinth-results" style="max-height:46vh;overflow-y:auto;display:flex;flex-direction:column;gap:8px;padding-right:4px;">
                <div class="empty" style="padding:32px;"><h3>Caricamento mod...</h3><p>Recupero le mod più scaricate compatibili con la tua versione.</p></div>
            </div>
        </div>
    `;
    document.body.appendChild(overlay);

    overlay.addEventListener('click', e => { if (e.target === overlay) overlay.remove(); });
    $('modrinth-close').addEventListener('click', () => overlay.remove());

    const render = () => modrinthLoad(mcVersion, state);

    // Toggle categorie (stile Modrinth: più categorie = AND)
    $('modrinth-cats').querySelectorAll('.chip').forEach(chip => {
        chip.addEventListener('click', () => {
            const cat = chip.dataset.cat;
            if (cat === '') {
                state.categories.clear();
                $('modrinth-cats').querySelectorAll('.chip').forEach(c => c.classList.toggle('on', c.dataset.cat === ''));
            } else {
                if (state.categories.has(cat)) {
                    state.categories.delete(cat);
                    chip.classList.remove('on');
                } else {
                    state.categories.add(cat);
                    chip.classList.add('on');
                }
                $('modrinth-cats').querySelectorAll('.chip').forEach(c => c.classList.toggle('on', c.dataset.cat === '' && state.categories.size === 0));
            }
            render();
        });
    });

    $('modrinth-sort').addEventListener('change', e => {
        state.index = e.target.value;
        render();
    });

    const doSearch = () => {
        state.lastQuery = $('modrinth-q').value.trim();
        render();
    };
    $('modrinth-go').addEventListener('click', doSearch);
    $('modrinth-q').addEventListener('keydown', e => { if (e.key === 'Enter') doSearch(); });

    // Al primo apertura mostra le mod consigliate per popolarità (senza ricerca)
    render();
    setTimeout(() => $('modrinth-q').focus(), 50);
}

async function modrinthLoad(mcVersion, state) {
    const results = $('modrinth-results');
    const isBrowse = !state.lastQuery && state.categories.size === 0 && state.index === 'downloads';
    $('modrinth-desc').textContent = isBrowse
        ? `Mod per Minecraft ${mcVersion} + Fabric — consigliate per popolarità`
        : `Mod per Minecraft ${mcVersion} + Fabric`;
    results.innerHTML = `<div class="empty"><h3>Caricamento mod...</h3><p>Connessione a Modrinth...</p></div>`;

    try {
        const hits = await window.electronAPI.modrinthSearch(state.lastQuery, {
            mcVersion, loader: 'fabric', index: state.index,
            categories: [...state.categories]
        });
        const installed = await window.electronAPI.listInstalledMods(activeProfileId());
        const installedFiles = installed.map(m => m.fileName);

        if (!hits.length) {
            results.innerHTML = `<div class="empty"><h3>Nessun risultato</h3><p>Nessuna mod trovata${state.lastQuery ? ` per "${esc(state.lastQuery)}"` : ''} compatibile con ${mcVersion} + Fabric.</p></div>`;
            return;
        }
        results.innerHTML = hits.map((h, i) => modrinthHitHtml(h, i, installedFiles)).join('');
        results.querySelectorAll('.modrinth-install').forEach(btn => {
            const hit = hits[Number(btn.dataset.idx)];
            btn.addEventListener('click', () => installFromModrinth(hit, btn, mcVersion));
        });
    } catch (e) {
        results.innerHTML = `<div class="empty" style="color:var(--error);"><h3>Errore</h3><p>${esc(errMsg(e))}</p></div>`;
    }
}

function modrinthHitHtml(h, idx, installedFiles) {
    const isInstalled = isModInstalled(installedFiles, h.slug);
    const desc = (h.description || '').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim();
    return `
    <div class="mod-card">
        ${h.icon_url
            ? `<div class="mod-icon" style="background:#FCAD1420;color:#FCAD14;">${esc((h.title || '?').charAt(0))}<img class="mod-icon-img" src="${esc(h.icon_url)}" alt="" loading="lazy" onerror="this.remove()"></div>`
            : `<div class="mod-icon" style="background:#FCAD1420;color:#FCAD14;">${esc((h.title || '?').charAt(0))}</div>`}
        <div class="mod-info">
            <div class="mod-name">${esc(h.title || h.slug)} ${isInstalled ? '<span class="badge badge-green" style="margin-left:6px;">Già installata</span>' : ''}</div>
            <div class="mod-desc">${esc(desc.slice(0, 140))}${desc.length > 140 ? '…' : ''}</div>
            <div class="mod-ver">⬇ ${(h.downloads || 0).toLocaleString('it-IT')} download · ${(h.follows || 0).toLocaleString('it-IT')} seguaci</div>
        </div>
        <button class="btn ${isInstalled ? 'btn-ghost' : 'btn-primary'} btn-sm modrinth-install" data-idx="${idx}" ${isInstalled ? 'disabled' : ''}>${isInstalled ? 'Installata' : 'Installa'}</button>
    </div>`;
}

async function installFromModrinth(hit, btn, mcVersion) {
    btn.disabled = true;
    btn.textContent = 'Scaricamento...';
    try {
        const r = await window.electronAPI.modrinthDownload(hit.project_id, {
            mcVersion, loader: 'fabric', slug: hit.slug, instanceId: activeProfileId()
        });
        if (!r.success) throw new Error(r.error);
        if (r.alreadyInstalled) {
            toast(`${hit.title} è già installata`, 'info');
            btn.textContent = 'Installata ✓';
        } else {
            const names = (r.installed || []).map(f => f.replace(/\.jar$/i, ''));
            toast(`Installate: ${names.join(', ')}`, 'success');
            if (r.dependencies && r.dependencies.length) {
                toast(`Dipendenze installate automaticamente: ${r.dependencies.join(', ')}`, 'success');
            }
            btn.textContent = 'Installata ✓';
        }
        loadModsList();
    } catch (e) {
        toast(errMsg(e), 'error');
        btn.disabled = false;
        btn.textContent = 'Installa';
    }
}

// ===== Versions Page =====
function renderVersions() {
    const el = $('page-versions');
    const currentMc = config.settings?.mcVersionOverride || '1.21.8';
    const currentFabric = config.settings?.fabricLoaderOverride || '0.19.3';
    
    const mcVersions = ['1.21.8', '1.21.7', '1.21.6', '1.21.5', '1.21.4', '1.21.3', '1.21.2', '1.21.1', '1.21', '1.20.6', '1.20.4', '1.20.2', '1.20.1', '1.20', '1.19.4', '1.19.3', '1.19.2'];
    const fabricVersions = ['0.19.3', '0.19.2', '0.19.1', '0.19.0', '0.18.10', '0.18.9', '0.18.8'];
    
    el.innerHTML = `
        <div class="page-head">
            <div><h1 class="page-title">Versione</h1><p class="page-subtitle">Seleziona la versione di Minecraft e Fabric</p></div>
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:24px;">
            <div>
                <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">Minecraft</h3>
                <div style="display:flex;flex-direction:column;gap:4px;max-height:400px;overflow-y:auto;">
                    ${mcVersions.map(v => `<button class="card" style="cursor:pointer;text-align:left;padding:12px;${v === currentMc ? 'border-color:var(--accent);box-shadow:0 0 0 1px var(--accent);' : ''}" data-mc="${v}"><span style="font-size:14px;font-weight:600;">${v}</span>${v === '1.21.8' ? ' <span class="badge badge-green" style="margin-left:8px;">Latest</span>' : ''}</button>`).join('')}
                </div>
            </div>
            <div>
                <h3 style="font-size:14px;font-weight:600;margin-bottom:12px;">Fabric Loader</h3>
                <div style="display:flex;flex-direction:column;gap:4px;max-height:400px;overflow-y:auto;">
                    ${fabricVersions.map(v => `<button class="card" style="cursor:pointer;text-align:left;padding:12px;${v === currentFabric ? 'border-color:var(--accent);box-shadow:0 0 0 1px var(--accent);' : ''}" data-fab="${v}"><span style="font-size:14px;font-weight:600;">${v}</span>${v === '0.19.3' ? ' <span class="badge badge-green" style="margin-left:8px;">Latest</span>' : ''}</button>`).join('')}
                </div>
            </div>
        </div>
    `;
    
    el.querySelectorAll('[data-mc]').forEach(btn => {
        btn.addEventListener('click', async () => {
            if (!config.settings) config.settings = {};
            config.settings.mcVersionOverride = btn.dataset.mc;
            await window.electronAPI.saveConfig(config);
            renderVersions();
            toast(`MC ${btn.dataset.mc} selezionato`, 'success');
        });
    });
    
    el.querySelectorAll('[data-fab]').forEach(btn => {
        btn.addEventListener('click', async () => {
            if (!config.settings) config.settings = {};
            config.settings.fabricLoaderOverride = btn.dataset.fab;
            await window.electronAPI.saveConfig(config);
            renderVersions();
            toast(`Fabric ${btn.dataset.fab} selezionato`, 'success');
        });
    });
}

// ===== Import da altri launcher =====
const IMPORT_CAT_LABELS = {
    options: 'Opzioni (tasti, audio, video…)',
    servers: 'Server multiplayer',
    mods: 'Mod Fabric',
    config: 'Config delle mod',
    resourcepacks: 'Resource pack',
    shaderpacks: 'Shader pack'
};

let importState = { sources: [], selected: null, categories: {}, dest: '__new__', busy: false };

function defaultImportCategories(src) {
    return {
        options: !!src.hasOptions,
        servers: !!src.hasServers,
        mods: (src.modCount || 0) > 0,
        config: !!src.hasConfig,
        resourcepacks: (src.resourcePackCount || 0) > 0,
        shaderpacks: (src.shaderPackCount || 0) > 0
    };
}

function sourceMetaLine(s) {
    const bits = [];
    if (s.mcVersion) bits.push('MC ' + s.mcVersion);
    if (s.fabricLoaderVersion) bits.push('Fabric ' + s.fabricLoaderVersion);
    bits.push(`${s.modCount || 0} mod`);
    if (s.hasConfig) bits.push('config');
    if (s.hasOptions) bits.push('opzioni');
    if (s.hasServers) bits.push('server');
    if (s.resourcePackCount) bits.push(`${s.resourcePackCount} RP`);
    if (s.shaderPackCount) bits.push(`${s.shaderPackCount} shader`);
    return bits.join(' · ');
}

async function markImportPromptDone() {
    if (!config.settings) config.settings = {};
    config.settings.importPromptDone = true;
    try { await window.electronAPI.saveConfig(config); } catch (e) { console.error(e); }
}

function closeImportOverlay() {
    $('import-overlay')?.classList.add('hidden');
}

async function openImportWizard(opts = {}) {
    const overlay = $('import-overlay');
    const body = $('import-body');
    const actions = $('import-actions');
    if (!overlay || !body || !actions) return;

    $('import-title').textContent = 'Importa da altro launcher';
    $('import-desc').textContent = 'Seleziona un profilo rilevato e cosa copiare.';
    body.innerHTML = '<p style="color:var(--on-variant);font-size:14px;">Cerco launcher installati…</p>';
    actions.innerHTML = `<button type="button" class="btn btn-ghost" id="import-cancel" style="height:40px;padding:0 16px;">Annulla</button>`;
    overlay.classList.remove('hidden');
    $('import-cancel')?.addEventListener('click', closeImportOverlay);

    let sources = [];
    try {
        const res = await window.electronAPI.importer.detect();
        if (!res?.success) throw new Error(res?.error || 'Detect fallito');
        sources = res.sources || [];
    } catch (e) {
        body.innerHTML = `<p style="color:var(--err);font-size:14px;">${esc(errMsg(e))}</p>`;
        return;
    }

    importState.sources = sources;
    importState.selected = sources[0] || null;
    importState.categories = importState.selected ? defaultImportCategories(importState.selected) : {};
    importState.dest = '__new__';
    renderImportWizardBody();
}

function renderImportWizardBody() {
    const body = $('import-body');
    const actions = $('import-actions');
    const sources = importState.sources;
    const sel = importState.selected;

    const profiles = config.profiles || [];
    const destOptions = [
        `<option value="__new__" ${importState.dest === '__new__' ? 'selected' : ''}>Nuovo profilo dedicato</option>`,
        ...profiles.map(p => `<option value="${esc(p.id)}" ${importState.dest === p.id ? 'selected' : ''}>${esc(p.name)}</option>`)
    ].join('');

    const listHtml = sources.length
        ? sources.map((s, i) => {
            const active = sel && sel.gameDir === s.gameDir;
            return `<button type="button" class="import-src ${active ? 'active' : ''}" data-idx="${i}" style="
                display:block;width:100%;text-align:left;padding:12px 14px;margin-bottom:8px;border-radius:16px;
                border:1px solid ${active ? 'var(--accent-line)' : 'var(--glass-line)'};
                background:${active ? 'var(--accent-soft)' : 'var(--glass-0)'};cursor:pointer;color:inherit;">
                <div style="font-weight:600;font-size:13px;">${esc(s.launcher)} · ${esc(s.profileName)}</div>
                <div style="font-size:12px;color:var(--on-variant);margin-top:4px;">${esc(sourceMetaLine(s))}</div>
            </button>`;
        }).join('')
        : `<p style="font-size:14px;color:var(--on-variant);margin-bottom:12px;">Nessun launcher trovato automaticamente. Puoi scegliere una cartella manualmente.</p>`;

    const catsHtml = sel
        ? Object.keys(IMPORT_CAT_LABELS).map(key => {
            const enabled = !!importState.categories[key];
            const available =
                (key === 'options' && sel.hasOptions) ||
                (key === 'servers' && sel.hasServers) ||
                (key === 'mods' && sel.modCount > 0) ||
                (key === 'config' && sel.hasConfig) ||
                (key === 'resourcepacks' && sel.resourcePackCount > 0) ||
                (key === 'shaderpacks' && sel.shaderPackCount > 0);
            if (!available) return '';
            return `<div style="display:flex;align-items:center;justify-content:space-between;padding:8px 0;border-bottom:1px solid var(--outline-soft);">
                <div style="font-size:13px;font-weight:500;">${esc(IMPORT_CAT_LABELS[key])}</div>
                <button type="button" class="toggle ${enabled ? 'on' : ''}" data-cat="${key}"></button>
            </div>`;
        }).join('')
        : '';

    body.innerHTML = `
        <div style="margin-bottom:14px;">
            <div style="font-size:12px;font-weight:600;color:var(--accent);letter-spacing:0.04em;text-transform:uppercase;margin-bottom:8px;">Sorgente</div>
            ${listHtml}
            <button type="button" class="btn btn-ghost btn-sm" id="import-browse" style="height:34px;padding:0 12px;margin-top:4px;">Scegli cartella…</button>
        </div>
        ${sel ? `
        <div style="margin-bottom:14px;">
            <div style="font-size:12px;font-weight:600;color:var(--accent);letter-spacing:0.04em;text-transform:uppercase;margin-bottom:8px;">Cosa importare</div>
            ${catsHtml || '<p style="font-size:13px;color:var(--on-variant);">Niente da importare in questa cartella.</p>'}
        </div>
        <div class="form-group" style="margin-bottom:0;">
            <label class="label">Destinazione</label>
            <select class="input" id="import-dest">${destOptions}</select>
            <p class="hint">Di default crea un nuovo profilo “Importato da …”.</p>
        </div>` : ''}
    `;

    actions.innerHTML = `
        <button type="button" class="btn btn-ghost" id="import-cancel" style="height:40px;padding:0 16px;">Annulla</button>
        <button type="button" class="btn btn-primary" id="import-run" style="height:40px;padding:0 18px;" ${sel ? '' : 'disabled'}>Importa</button>
    `;

    body.querySelectorAll('.import-src').forEach(btn => {
        btn.addEventListener('click', () => {
            const idx = parseInt(btn.dataset.idx, 10);
            importState.selected = sources[idx];
            importState.categories = defaultImportCategories(importState.selected);
            renderImportWizardBody();
        });
    });
    body.querySelectorAll('.toggle[data-cat]').forEach(btn => {
        btn.addEventListener('click', () => {
            const key = btn.dataset.cat;
            importState.categories[key] = !importState.categories[key];
            btn.classList.toggle('on');
        });
    });
    $('import-dest')?.addEventListener('change', (e) => { importState.dest = e.target.value; });
    $('import-cancel')?.addEventListener('click', closeImportOverlay);
    $('import-browse')?.addEventListener('click', async () => {
        try {
            const res = await window.electronAPI.importer.scanFolder();
            if (res?.cancelled) return;
            if (!res?.success || !res.source) {
                toast(res?.error || 'Cartella non valida', 'error');
                return;
            }
            importState.sources = [res.source, ...importState.sources.filter(s => s.gameDir !== res.source.gameDir)];
            importState.selected = res.source;
            importState.categories = defaultImportCategories(res.source);
            renderImportWizardBody();
        } catch (e) { toast(errMsg(e), 'error'); }
    });
    $('import-run')?.addEventListener('click', runImportSelected);
}

async function runImportSelected() {
    if (importState.busy || !importState.selected) return;
    const cats = Object.keys(importState.categories).filter(k => importState.categories[k]);
    if (!cats.length) {
        toast('Seleziona almeno una categoria', 'error');
        return;
    }
    importState.busy = true;
    const runBtn = $('import-run');
    if (runBtn) { runBtn.disabled = true; runBtn.textContent = 'Importo…'; }
    try {
        const payload = {
            gameDir: importState.selected.gameDir,
            launcher: importState.selected.launcher,
            profileName: importState.selected.profileName,
            categories: cats,
            newProfile: importState.dest === '__new__',
            newProfileName: importState.dest === '__new__'
                ? `Importato da ${importState.selected.profileName || importState.selected.launcher}`
                : undefined,
            instanceId: importState.dest === '__new__' ? undefined : importState.dest
        };
        const res = await window.electronAPI.importer.run(payload);
        if (!res?.success) throw new Error(res?.error || 'Import fallito');

        // refresh config/profiles from disk
        try { config = await window.electronAPI.getConfig() || config; } catch (_) {}
        await markImportPromptDone();
        closeImportOverlay();
        const st = res.stats || {};
        toast(`Importato in “${res.profileName || 'profilo'}”: ${st.modsCopied || 0} mod, ${st.optionsCopied || 0} opzioni, ${st.serversCopied || 0} server`, 'success');
        if (currentPage === 'profiles') renderProfiles();
        if (currentPage === 'settings') renderSettings();
        updateUI();
    } catch (e) {
        toast(errMsg(e), 'error');
    } finally {
        importState.busy = false;
        if (runBtn) { runBtn.disabled = false; runBtn.textContent = 'Importa'; }
    }
}

async function maybeShowImportPrompt() {
    if (!config.settings) config.settings = {};
    if (config.settings.importPromptDone) return;
    if (!window.electronAPI?.importer?.detect) return;

    let sources = [];
    try {
        const res = await window.electronAPI.importer.detect();
        sources = res?.sources || [];
    } catch (_) {
        return;
    }
    if (!sources.length) {
        await markImportPromptDone();
        return;
    }

    const prompt = $('import-prompt-overlay');
    if (!prompt) return;
    prompt.classList.remove('hidden');

    const later = () => {
        prompt.classList.add('hidden');
        markImportPromptDone();
    };
    const go = async () => {
        prompt.classList.add('hidden');
        await markImportPromptDone();
        openImportWizard();
    };
    $('import-prompt-later')?.addEventListener('click', later, { once: true });
    $('import-prompt-go')?.addEventListener('click', go, { once: true });
}

// ===== Settings Page =====
function renderSettings() {
    const el = $('page-settings');
    const s = config.settings || {};
    
    el.innerHTML = `
        <div class="page-head">
            <div><h1 class="page-title">Impostazioni</h1><p class="page-subtitle">Configura il launcher</p></div>
        </div>
        <div style="max-width:600px;display:flex;flex-direction:column;gap:24px;">
            <div class="card">
                <h3 style="font-size:14px;font-weight:600;margin-bottom:8px;">Importa da altro launcher</h3>
                <p style="font-size:13px;color:var(--on-variant);line-height:1.5;margin-bottom:14px;">
                    Porta opzioni, server, mod Fabric, config e resource pack da Minecraft, Modrinth, CurseForge, Prism e altri.
                </p>
                <button type="button" class="btn btn-primary" id="set-import" style="height:40px;padding:0 18px;">Apri importazione</button>
            </div>
            <div class="card">
                <h3 style="font-size:14px;font-weight:600;margin-bottom:16px;">Java</h3>
                <div class="form-group">
                    <label class="label">Percorso Java</label>
                    <div class="form-row">
                        <input type="text" class="input" id="set-java" value="${esc(s.javaPath || '')}" placeholder="Auto-detect (JDK 21)" style="flex:1;">
                        <button class="btn btn-ghost btn-sm" id="set-java-browse">Sfoglia</button>
                    </div>
                    <p class="hint">Lascia vuoto per auto-detect (preferenza Java 21)</p>
                </div>
                <div class="form-group">
                    <label class="label">RAM (MB)</label>
                    <input type="number" class="input" id="set-ram" value="${esc(s.ramMB || 4096)}" min="1024" max="32768" step="512">
                    <p class="hint">Consigliato: 4096 MB</p>
                </div>
                <div class="form-group">
                    <label class="label">Argomenti JVM</label>
                    <input type="text" class="input" id="set-jvm" value="${esc(s.jvmArgs || '')}" placeholder="-XX:+UnlockExperimentalVMOptions ...">
                </div>
            </div>
            
            <div class="card">
                <h3 style="font-size:14px;font-weight:600;margin-bottom:16px;">Generali</h3>
                <div style="display:flex;align-items:center;justify-content:space-between;padding:8px 0;border-bottom:1px solid var(--outline-soft);">
                    <div><div style="font-size:14px;font-weight:500;">Chiudi al lancio</div><div style="font-size:12px;color:var(--on-surface-variant);">Chiude il launcher quando Minecraft si avvia</div></div>
                    <button type="button" class="toggle ${s.closeOnLaunch ? 'on' : ''}" id="set-close" data-setting="closeOnLaunch"></button>
                </div>
                <div style="display:flex;align-items:center;justify-content:space-between;padding:8px 0;border-bottom:1px solid var(--outline-soft);">
                    <div><div style="font-size:14px;font-weight:500;">Ottimizza FPS</div><div style="font-size:12px;color:var(--on-surface-variant);">Aggiunge flag G1GC agli argomenti JVM al lancio</div></div>
                    <button type="button" class="toggle ${s.optimizeFps !== false ? 'on' : ''}" id="set-fps" data-setting="optimizeFps"></button>
                </div>
                <div style="display:flex;align-items:center;justify-content:space-between;padding:8px 0;">
                    <div><div style="font-size:14px;font-weight:500;">Apri cartella log all'avvio</div><div style="font-size:12px;color:var(--on-surface-variant);">Apre la cartella logs del profilo attivo</div></div>
                    <button type="button" class="toggle ${s.openLogsOnLaunch ? 'on' : ''}" id="set-logs" data-setting="openLogsOnLaunch"></button>
                </div>
            </div>
            
            <div style="display:flex;gap:12px;">
                <button class="btn btn-primary" id="set-save" style="height:40px;padding:0 24px;">Salva</button>
                <button class="btn btn-ghost" id="set-reset" style="height:40px;color:var(--error);">Ripristina default</button>
            </div>
        </div>
    `;
    
    // Toggle buttons
    el.querySelectorAll('.toggle[data-setting]').forEach(btn => {
        btn.addEventListener('click', () => btn.classList.toggle('on'));
    });
    
    // Save
    $('set-save')?.addEventListener('click', async () => {
        if (!config.settings) config.settings = {};
        config.settings.javaPath = $('set-java').value.trim();
        config.settings.ramMB = parseInt($('set-ram').value) || 4096;
        config.settings.jvmArgs = $('set-jvm').value.trim();
        config.settings.closeOnLaunch = $('set-close').classList.contains('on');
        config.settings.optimizeFps = $('set-fps').classList.contains('on');
        config.settings.openLogsOnLaunch = $('set-logs').classList.contains('on');
        await window.electronAPI.saveConfig(config);
        toast('Impostazioni salvate!', 'success');
    });
    
    // Reset
    $('set-reset')?.addEventListener('click', async () => {
        if (confirm('Ripristinare le impostazioni di default?')) {
            config.settings = {
                ramMB: 4096, jvmArgs: '', closeOnLaunch: false, mcVersionOverride: '',
                fabricLoaderOverride: '', javaPath: '', optimizeFps: true, openLogsOnLaunch: false,
                importPromptDone: config.settings?.importPromptDone || false
            };
            await window.electronAPI.saveConfig(config);
            renderSettings();
            toast('Impostazioni ripristinate', 'success');
        }
    });
    
    // Browse java
    $('set-java-browse')?.addEventListener('click', async () => {
        try {
            const path = await window.electronAPI.selectJavaPath();
            if (path) $('set-java').value = path;
        } catch (e) { toast(errMsg(e), 'error'); }
    });

    $('set-import')?.addEventListener('click', () => openImportWizard());
}

// ===== Tabs =====
function switchTab(tab) {
    document.querySelectorAll('.tab').forEach((t, i) => t.classList.toggle('active', (tab === 'ms' ? i === 0 : i === 1)));
    $('tab-ms').style.display = tab === 'ms' ? '' : 'none';
    $('tab-off').style.display = tab === 'off' ? '' : 'none';
}
window.switchTab = switchTab;

// ===== Login =====
async function msLogin() {
    const btn = $('ms-btn');
    btn.disabled = true; btn.textContent = 'Accesso...'; $('login-err').textContent = '';
    try {
        const r = await window.electronAPI.microsoftLogin();
        if (r.success && r.account) {
            if (!config.accounts) config.accounts = [];
            const idx = config.accounts.findIndex(a => a.name === r.account.name);
            if (idx >= 0) config.accounts[idx] = r.account; else config.accounts.push(r.account);
            config.lastUsername = r.account.name;
            config.lastAccountType = r.account.type;
            await window.electronAPI.saveConfig(config);
            $('login-overlay').classList.add('hidden');
            $('app').style.display = 'grid';
            updateUI();
            toast(`Bentornato, ${r.account.name}!`, 'success');
        } else {
            throw new Error(r.error || 'Login fallito');
        }
    } catch (e) { $('login-err').textContent = errMsg(e); }
    finally { btn.disabled = false; btn.textContent = 'Accedi con Microsoft'; }
}

async function offLogin() {
    const nick = $('nick-input').value.trim();
    const btn = $('off-btn');
    if (!/^[a-zA-Z0-9_]{3,16}$/.test(nick)) return;
    btn.disabled = true; btn.textContent = 'Accesso...'; $('login-err').textContent = '';
    try {
        const acc = { name: nick, uuid: 'offline-' + Date.now(), type: 'offline' };
        if (!config.accounts) config.accounts = [];
        config.accounts.push(acc);
        config.lastUsername = acc.name;
        config.lastAccountType = acc.type;
        await window.electronAPI.saveConfig(config);
        $('login-overlay').classList.add('hidden');
        $('app').style.display = 'grid';
        updateUI();
        toast(`Bentornato, ${acc.name}!`, 'success');
    } catch (e) { $('login-err').textContent = errMsg(e); }
    finally { btn.disabled = false; btn.textContent = 'Entra'; }
}

// ===== Play =====
window.electronAPI.onLaunchProgress((p) => {
    const fill = $('p-fill'), pct = $('p-pct'), msg = $('p-msg');
    if (!fill || !pct || !msg) return;
    const ratio = Math.max(0, Math.min(1, p.pct || 0));
    fill.style.transform = `translateX(${(ratio - 1) * 100}%)`;
    pct.textContent = `${Math.round(ratio * 100)}%`;
    if (p.msg) msg.textContent = p.msg;
});

async function play() {
    const acc = getActive();
    if (!acc) { toast('Nessun account selezionato', 'error'); $('login-overlay').classList.remove('hidden'); return; }
    if (gameRunning && !relaunching) { toast('Minecraft è già in esecuzione', 'error'); return; }
    const btn = $('play-btn');
    btn.disabled = true;
    $('progress').style.display = 'block';
    $('launch-err').style.display = 'none';
    try {
        $('p-fill').style.transform = 'translateX(-100%)';
        $('p-pct').textContent = '0%';
        $('p-msg').textContent = 'Avvio...';

        await ensureDefaultProfile();
        const profile = (config.profiles || []).find(p => p.id === activeProfileId()) || { id: 'default', mcVersion: '1.21.8', ramMB: 4096, isFabric: true };
        await syncActiveInstance(profile.id);

        // Refresh Microsoft token before launch if needed
        let account = acc;
        if (acc.type === 'microsoft') {
            try {
                const refreshed = await window.electronAPI.microsoftRefresh(acc);
                if (refreshed?.success && refreshed.account) {
                    account = refreshed.account;
                    const idx = config.accounts.findIndex(a => a.uuid === acc.uuid || a.name === acc.name);
                    if (idx >= 0) config.accounts[idx] = { ...config.accounts[idx], ...account };
                    await window.electronAPI.saveConfig(config);
                }
            } catch (e) { console.error('Token refresh fallito, provo con token corrente', e); }
        }

        let jvmArgs = config.settings?.jvmArgs || '';
        if (config.settings?.optimizeFps) {
            const fpsFlags = '-XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M';
            if (!jvmArgs.includes('UseG1GC')) jvmArgs = (jvmArgs + ' ' + fpsFlags).trim();
        }

        const instPath = await window.electronAPI.getInstancePath(profile.id);
        const r = await window.electronAPI.launchMinecraft({
            instancePath: instPath,
            mcVersion: profile.mcVersion || config.settings?.mcVersionOverride || '1.21.8',
            ramMB: profile.ramMB || config.settings?.ramMB || 4096,
            javaPath: config.settings?.javaPath || '',
            jvmArgs,
            account,
            isFabric: profile.isFabric !== false,
            fabricLoaderOverride: profile.fabricLoaderOverride || config.settings?.fabricLoaderOverride || ''
        });
        if (r.success) {
            gameRunning = true;
            toast('Minecraft avviato!', 'success');
            $('p-msg').textContent = 'Gioco in esecuzione…';
            if (config.settings?.closeOnLaunch) window.electronAPI.close();
            if (config.settings?.openLogsOnLaunch && window.electronAPI.openPath) {
                try { await window.electronAPI.openPath(instPath + '/logs'); } catch (_) {}
            }
        } else throw new Error(r.error || 'Errore sconosciuto');
    } catch (e) { $('launch-err').textContent = errMsg(e); $('launch-err').style.display = 'block'; toast(errMsg(e), 'error'); gameRunning = false; }
    finally {
        if (!gameRunning) { btn.disabled = false; $('progress').style.display = 'none'; }
        else { btn.disabled = true; }
        relaunching = false;
    }
}

// ===== Init =====
async function init() {
    try { config = await window.electronAPI.getConfig() || {}; } catch { config = {}; }
    if (!config.accounts) config.accounts = [];
    await ensureDefaultProfile();

    window.electronAPI.onMinecraftRestartRequested?.(async () => {
        toast('Riavvio del gioco in corso…', 'info');
        relaunching = true;
        gameRunning = false;
        await play();
    });
    window.electronAPI.onMinecraftClosed?.(({ code, error }) => {
        gameRunning = false;
        const btn = $('play-btn');
        if (btn) btn.disabled = false;
        $('progress').style.display = 'none';
        if (error) toast('Minecraft terminato: ' + error, 'error');
        else if (code && code !== 0) toast('Minecraft chiuso (codice ' + code + ')', 'error');
    });

    initTheme();
    const acc = getActive();
    $('loading').style.display = 'none';
    $('app').style.display = 'grid';

    if (!acc) {
        $('login-overlay').classList.remove('hidden');
    }
    updateUI();

    // Account dropdown toggle
    $('account-btn').addEventListener('click', e => { e.stopPropagation(); $('acc-dropdown').classList.toggle('show'); });
    document.addEventListener('click', e => { if (!e.target.closest('.account-sel')) $('acc-dropdown').classList.remove('show'); });

    // Window controls
    $('win-min').addEventListener('click', () => window.electronAPI.minimize());
    $('win-max').addEventListener('click', () => window.electronAPI.maximize());
    $('win-close').addEventListener('click', () => window.electronAPI.close());

    // Theme toggle
    $('theme-toggle').addEventListener('click', toggleTheme);

    // Login
    $('ms-btn').addEventListener('click', msLogin);
    $('off-btn').addEventListener('click', offLogin);
    $('nick-input').addEventListener('input', () => {
        const v = $('nick-input').value.trim();
        $('off-btn').disabled = !/^[a-zA-Z0-9_]{3,16}$/.test(v);
    });
    $('nick-input').addEventListener('keydown', e => { if (e.key === 'Enter' && !$('off-btn').disabled) offLogin(); });

    // Play
    $('play-btn').addEventListener('click', play);

    // Close login on backdrop
    $('login-overlay').addEventListener('click', e => { if (e.target === $('login-overlay') && getActive()) $('login-overlay').classList.add('hidden'); });

    // Primo avvio: proponi import se ci sono sorgenti (dopo login overlay se presente)
    if (acc) {
        setTimeout(() => maybeShowImportPrompt(), 400);
    } else {
        const watchLogin = new MutationObserver(() => {
            if ($('login-overlay')?.classList.contains('hidden') && getActive()) {
                watchLogin.disconnect();
                setTimeout(() => maybeShowImportPrompt(), 300);
            }
        });
        watchLogin.observe($('login-overlay'), { attributes: true, attributeFilter: ['class'] });
    }
}

init();
