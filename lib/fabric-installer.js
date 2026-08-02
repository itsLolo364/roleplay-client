const https = require('https');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { pipeline } = require('stream/promises');
const { execFileSync, spawn } = require('child_process');

const MAX_REDIRECTS = 5;
const REDIRECT_CODES = [301, 302, 303, 307, 308];

class FabricInstaller {
    constructor() {
        this.INSTALLER_URL = 'https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.0.1/fabric-installer-1.0.1.jar';
        this.METADATA_URL = 'https://meta.fabricmc.net/v2/versions/loader';
        this.MAVEN_REPOS = [
            'https://repo1.maven.org/maven2/',
            'https://maven.fabricmc.net/',
            'https://libraries.minecraft.net/'
        ];
    }

    fetchRaw(url, redirectCount = 0) {
        return new Promise((resolve, reject) => {
            if (!String(url).startsWith('https://')) {
                return reject(new Error(`URL non sicuro rifiutato: ${url}`));
            }
            https.get(url, { headers: { 'Accept': 'application/json' } }, (res) => {
                if (REDIRECT_CODES.includes(res.statusCode)) {
                    res.resume();
                    if (redirectCount >= MAX_REDIRECTS) return reject(new Error(`Too many redirects for ${url}`));
                    return this.fetchRaw(res.headers.location, redirectCount + 1).then(resolve).catch(reject);
                }
                if (res.statusCode !== 200) {
                    res.resume();
                    return reject(new Error(`HTTP ${res.statusCode} for ${url}`));
                }
                let data = '';
                res.on('data', chunk => data += chunk);
                res.on('error', reject);
                res.on('end', () => resolve(data));
            }).on('error', reject);
        });
    }

    async fetchJson(url) {
        const data = await this.fetchRaw(url);
        try { return JSON.parse(data); }
        catch (e) { throw new Error('JSON parse error'); }
    }

    // Legge lo .sha1 pubblicato accanto a un artifact Maven; null se non disponibile.
    async mavenSha1(url) {
        try {
            const text = await this.fetchRaw(`${url}.sha1`);
            const hash = String(text).trim().split(/\s+/)[0];
            return /^[a-fA-F0-9]{40}$/.test(hash) ? hash.toLowerCase() : null;
        } catch (e) {
            return null;
        }
    }

    sha1File(filePath) {
        return new Promise((resolve, reject) => {
            const hash = crypto.createHash('sha1');
            const stream = fs.createReadStream(filePath);
            stream.on('error', reject);
            stream.on('data', d => hash.update(d));
            stream.on('end', () => resolve(hash.digest('hex')));
        });
    }

    fetchToFile(url, dest, redirectCount = 0) {
        return new Promise((resolve, reject) => {
            if (!String(url).startsWith('https://')) {
                return reject(new Error(`URL non sicuro rifiutato: ${url}`));
            }
            https.get(url, (res) => {
                if (REDIRECT_CODES.includes(res.statusCode)) {
                    res.resume();
                    if (redirectCount >= MAX_REDIRECTS) return reject(new Error(`Too many redirects for ${url}`));
                    return this.fetchToFile(res.headers.location, dest, redirectCount + 1).then(resolve).catch(reject);
                }
                if (res.statusCode !== 200) {
                    res.resume();
                    return reject(new Error(`Download failed with status ${res.statusCode}`));
                }
                const file = fs.createWriteStream(dest);
                pipeline(res, file).then(resolve).catch(reject);
            }).on('error', reject);
        });
    }

    // Scarica su dest+'.part' e rinomina solo a download (e verifica) completati,
    // così un download interrotto non lascia mai un file troncato al posto finale.
    // opts.sha1 / opts.size: verifica integrità quando il manifest la fornisce.
    async downloadFile(url, dest, opts = {}) {
        const dir = path.dirname(dest);
        if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
        const tmp = `${dest}.part`;
        try {
            await this.fetchToFile(url, tmp);
            if (opts.size != null) {
                const actual = fs.statSync(tmp).size;
                if (actual !== opts.size) {
                    throw new Error(`Size mismatch for ${url}: expected ${opts.size}, got ${actual}`);
                }
            }
            if (opts.sha1) {
                const actual = await this.sha1File(tmp);
                if (actual.toLowerCase() !== String(opts.sha1).toLowerCase()) {
                    throw new Error(`SHA-1 mismatch for ${url}: expected ${opts.sha1}, got ${actual}`);
                }
            }
            fs.renameSync(tmp, dest);
            return dest;
        } catch (err) {
            try { fs.unlinkSync(tmp); } catch (e) {}
            throw err;
        }
    }

    // Download da Maven con verifica dello .sha1 pubblicato accanto all'artifact.
    async downloadMavenFile(url, dest) {
        const sha1 = await this.mavenSha1(url);
        if (!sha1) console.warn(`[Download] Nessuno .sha1 disponibile per ${url}, procedo senza verifica`);
        return this.downloadFile(url, dest, sha1 ? { sha1 } : {});
    }

    async getLatestLoaderVersion() {
        const versions = await this.fetchJson(this.METADATA_URL);
        if (!versions || versions.length === 0) throw new Error('No Fabric loader versions found');
        return versions[0].version;
    }

    async resolveLoaderVersion(override) {
        const v = String(override || '').trim();
        if (!v || v === 'latest') return this.getLatestLoaderVersion();
        // Valida contro meta Fabric; se non trovato, usa latest
        try {
            const versions = await this.fetchJson(this.METADATA_URL);
            if (versions.some(x => x.version === v)) return v;
            console.warn(`[Fabric] Loader override ${v} non trovato, uso latest`);
        } catch (_) {}
        return this.getLatestLoaderVersion();
    }

    async installFabric(mcVersion, gameDir, javaPath, loaderOverride) {
        console.log(`[Fabric] Installing for Minecraft ${mcVersion}...`);
        
        const loaderVersion = await this.resolveLoaderVersion(loaderOverride);
        console.log(`[Fabric] Loader version: ${loaderVersion}`);
        
        const versionName = `fabric-loader-${loaderVersion}-${mcVersion}`;
        const versionsDir = path.join(gameDir, 'versions');
        const versionDir = path.join(versionsDir, versionName);
        const versionJsonPath = path.join(versionDir, `${versionName}.json`);
        
        if (fs.existsSync(versionJsonPath)) {
            console.log(`[Fabric] Already installed: ${versionName}`);
            // Still download MC libs if needed
            await this.downloadMinecraftLibraries(mcVersion, gameDir);
            return { versionName, installed: true, loaderVersion };
        }
        
        // Try official installer first
        const installerDir = path.join(gameDir, '.fabric-installer');
        const installerPath = path.join(installerDir, 'fabric-installer.jar');
        
        if (!fs.existsSync(installerPath)) {
            console.log('[Fabric] Downloading installer...');
            try {
                await this.downloadMavenFile(this.INSTALLER_URL, installerPath);
                console.log('[Fabric] Installer downloaded');
            } catch (e) {
                console.log('[Fabric] Failed to download installer, using manual method');
                return this.installManually(mcVersion, gameDir, loaderVersion);
            }
        }
        
        const javaExec = javaPath || this.findJava();
        if (!javaExec) {
            console.log('[Fabric] Java not found, using manual installation');
            return this.installManually(mcVersion, gameDir, loaderVersion);
        }
        
        return new Promise((resolve, reject) => {
            const args = [
                '-jar', installerPath,
                'client',
                '-dir', gameDir,
                '-mcversion', mcVersion,
                '-loader', loaderVersion
            ];
            
            console.log(`[Fabric] Running installer...`);
            
            // Niente shell: con shell:true gli argomenti non verrebbero quotati
            // (path con spazi rotti + metacaratteri interpretati dalla shell).
            const child = spawn(javaExec, args, {
                stdio: ['pipe', 'pipe', 'pipe']
            });
            
            let stdout = '';
            let stderr = '';
            
            child.stdout.on('data', (data) => { stdout += data.toString(); });
            child.stderr.on('data', (data) => { stderr += data.toString(); });
            
            child.on('close', async (code) => {
                console.log(`[Fabric] Installer exit code: ${code}`);
                if (code === 0 && fs.existsSync(versionJsonPath)) {
                    console.log(`[Fabric] Installation successful: ${versionName}`);
                    try { await this.downloadMinecraftLibraries(mcVersion, gameDir); } catch(e) { console.log('[MC Libs] Error:', e.message); }
                    resolve({ versionName, installed: true, loaderVersion });
                } else {
                    console.log('[Fabric] Official installer failed, trying manual...');
                    this.installManually(mcVersion, gameDir, loaderVersion)
                        .then(resolve)
                        .catch(reject);
                }
            });
            
            child.on('error', (err) => {
                console.log('[Fabric] Installer error, trying manual...');
                this.installManually(mcVersion, gameDir, loaderVersion)
                    .then(resolve)
                    .catch(reject);
            });
        });
    }

    async ensureMinecraftJar(mcVersion, gameDir) {
        const mcVersionDir = path.join(gameDir, 'versions', mcVersion);
        const mcJarPath = path.join(mcVersionDir, `${mcVersion}.jar`);
        
        // Read the version JSON to get download URL
        const versionJsonPath = path.join(mcVersionDir, `${mcVersion}.json`);
        let versionData;
        if (!fs.existsSync(versionJsonPath)) {
            // Download version manifest first
            const manifest = await this.fetchJson('https://launchermeta.mojang.com/mc/game/version_manifest_v2.json');
            const versionInfo = manifest.versions.find(v => v.id === mcVersion);
            if (!versionInfo) throw new Error(`Version ${mcVersion} not found`);

            versionData = await this.fetchJson(versionInfo.url);
            if (!fs.existsSync(mcVersionDir)) {
                fs.mkdirSync(mcVersionDir, { recursive: true });
            }
            fs.writeFileSync(versionJsonPath, JSON.stringify(versionData, null, 2));
        } else {
            versionData = JSON.parse(fs.readFileSync(versionJsonPath, 'utf8'));
        }

        const clientDl = versionData.downloads?.client;
        if (fs.existsSync(mcJarPath)) {
            // Un download interrotto in passato può aver lasciato un jar troncato: verifica la size.
            if (clientDl?.size == null || fs.statSync(mcJarPath).size === clientDl.size) {
                console.log(`[MC] JAR exists: ${mcJarPath}`);
                return mcJarPath;
            }
            console.log(`[MC] JAR esistente con size errata, lo riscarico...`);
        }

        const jarUrl = clientDl?.url;
        if (!jarUrl) throw new Error('MC JAR download URL not found');

        console.log(`[MC] Downloading from ${jarUrl}...`);
        await this.downloadFile(jarUrl, mcJarPath, { sha1: clientDl.sha1, size: clientDl.size });
        console.log(`[MC] Downloaded to ${mcJarPath}`);

        return mcJarPath;
    }

    async installManually(mcVersion, gameDir, loaderVersion) {
        console.log('[Fabric] Manual installation...');
        
        const versionName = `fabric-loader-${loaderVersion}-${mcVersion}`;
        const versionsDir = path.join(gameDir, 'versions');
        const versionDir = path.join(versionsDir, versionName);
        
        if (!fs.existsSync(versionDir)) {
            fs.mkdirSync(versionDir, { recursive: true });
        }
        
        const loaderJarUrl = `https://maven.fabricmc.net/net/fabricmc/fabric-loader/${loaderVersion}/fabric-loader-${loaderVersion}.jar`;
        const loaderJarPath = path.join(versionDir, `fabric-loader-${loaderVersion}.jar`);
        
        if (!fs.existsSync(loaderJarPath)) {
            console.log('[Fabric] Downloading loader JAR...');
            await this.downloadMavenFile(loaderJarUrl, loaderJarPath);
        }
        
        // Also copy to libraries folder so MCLC can find it
        const libsLoaderDir = path.join(gameDir, 'libraries', 'net', 'fabricmc', 'fabric-loader', loaderVersion);
        if (!fs.existsSync(libsLoaderDir)) {
            fs.mkdirSync(libsLoaderDir, { recursive: true });
        }
        const libsLoaderPath = path.join(libsLoaderDir, `fabric-loader-${loaderVersion}.jar`);
        if (!fs.existsSync(libsLoaderPath)) {
            fs.copyFileSync(loaderJarPath, libsLoaderPath);
            console.log(`[Fabric] Copied loader to libraries`);
        }
        
        // Read the original Minecraft version JSON to get all libraries
        const mcVersionDir = path.join(gameDir, 'versions', mcVersion);
        const mcVersionJsonPath = path.join(mcVersionDir, `${mcVersion}.json`);
        
        let baseJson = null;
        if (fs.existsSync(mcVersionJsonPath)) {
            console.log(`[Fabric] Reading base MC version from ${mcVersionJsonPath}`);
            baseJson = JSON.parse(fs.readFileSync(mcVersionJsonPath, 'utf8'));
        } else {
            console.log(`[Fabric] MC version JSON not found at ${mcVersionJsonPath}, will download...`);
            // Download the MC version JSON
            const mcMetaUrl = `https://launchermeta.mojang.com/mc/game/version_manifest_v2.json`;
            const manifest = await this.fetchJson(mcMetaUrl);
            const versionInfo = manifest.versions.find(v => v.id === mcVersion);
            if (versionInfo) {
                const versionData = await this.fetchJson(versionInfo.url);
                baseJson = versionData;
                // Save it for future use
                if (!fs.existsSync(mcVersionDir)) {
                    fs.mkdirSync(mcVersionDir, { recursive: true });
                }
                fs.writeFileSync(mcVersionJsonPath, JSON.stringify(versionData, null, 2));
            }
        }
        
        // Create the complete version JSON
        let versionJson;
        if (baseJson) {
            // Create a COMPLETE version JSON - no inheritsFrom!
            // Copy the jar field to point to the actual MC jar
            versionJson = {
                id: versionName,
                type: "release",
                mainClass: "net.fabricmc.loader.launch.knot.KnotClient",
                // Keep the original arguments structure
                arguments: {
                    jvm: [
                        ...(baseJson.arguments?.jvm || []),
                        `-Dfabric.classPath=${path.join(gameDir, 'libraries')}`,
                        "-Dfabric.useLegacyProtocol=false"
                    ],
                    game: baseJson.arguments?.game || []
                },
                // Include ALL libraries - Fabric first, then MC
                libraries: [
                    // Fabric libraries (NO ASM - MC already has it)
                    { name: `net.fabricmc:fabric-loader:${loaderVersion}`, url: "https://maven.fabricmc.net/" },
                    { name: `net.fabricmc:intermediary:${mcVersion}`, url: "https://maven.fabricmc.net/" },
                    // MC libraries
                    ...(baseJson.libraries || [])
                ],
                // Keep the MC assets and other settings
                assetIndex: baseJson.assetIndex,
                assets: baseJson.assets,
                downloads: baseJson.downloads,
                logging: baseJson.logging,
                minArguments: baseJson.minArguments,
                releaseTime: baseJson.releaseTime,
                time: baseJson.time
            };
        } else {
            // Minimal JSON without base MC
            versionJson = {
                id: versionName,
                type: "release",
                mainClass: "net.fabricmc.loader.launch.knot.KnotClient",
                arguments: {
                    jvm: [
                        `-Dfabric.classPath=${path.join(gameDir, 'libraries')}`,
                        "-Dfabric.useLegacyProtocol=false"
                    ],
                    game: []
                },
                libraries: [
                    { name: `net.fabricmc:fabric-loader:${loaderVersion}`, url: "https://maven.fabricmc.net/" },
                    { name: `net.fabricmc:intermediary:${mcVersion}`, url: "https://maven.fabricmc.net/" },
                    { name: "org.ow2.asm:asm:9.7.1", url: "https://repo1.maven.org/maven2/" },
                    { name: "org.ow2.asm:asm-commons:9.7.1", url: "https://repo1.maven.org/maven2/" },
                    { name: "org.ow2.asm:asm-tree:9.7.1", url: "https://repo1.maven.org/maven2/" }
                ]
            };
        }
        
        const versionJsonPath = path.join(versionDir, `${versionName}.json`);
        fs.writeFileSync(versionJsonPath, JSON.stringify(versionJson, null, 2));
        console.log(`[Fabric] Version JSON saved with ${versionJson.libraries.length} libraries`);

        // Scarica intermediary + dipendenze Fabric dichiarate
        await this.downloadFabricLibraries(versionJson.libraries, gameDir);
        
        console.log(`[Fabric] Manual installation complete: ${versionName}`);
        
        // Download all MC Java libraries
        await this.downloadMinecraftLibraries(mcVersion, gameDir);
        
        return { versionName, installed: true, loaderVersion, manual: true };
    }

    async downloadFabricLibraries(libraries, gameDir) {
        const libs = (libraries || []).filter(l => l && l.name && (l.url || '').includes('fabricmc'));
        for (const lib of libs) {
            try {
                const parts = lib.name.split(':');
                if (parts.length < 3) continue;
                const [group, artifact, ver] = parts;
                const groupPath = group.replace(/\./g, '/');
                const fileName = `${artifact}-${ver}.jar`;
                const dest = path.join(gameDir, 'libraries', groupPath, artifact, ver, fileName);
                if (fs.existsSync(dest)) continue;
                const base = lib.url || 'https://maven.fabricmc.net/';
                const url = `${base}${groupPath}/${artifact}/${ver}/${fileName}`;
                console.log(`[Fabric] Downloading ${lib.name}`);
                await this.downloadMavenFile(url, dest);
            } catch (e) {
                console.error(`[Fabric] Lib download failed ${lib.name}:`, e.message);
            }
        }
    }

    findJava() {
        const isWin = process.platform === 'win32';
        const javaBin = isWin ? 'java.exe' : 'java';
        const candidates = [
            process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin', javaBin) : null,
            'java',
            isWin ? 'javaw' : null
        ].filter(Boolean);

        // Scansiona le directory di installazione standard su Windows (più recenti prima).
        if (isWin) {
            const roots = [
                'C:/Program Files/Eclipse Adoptium',
                'C:/Program Files/Java',
                'C:/Program Files/Microsoft'
            ];
            for (const root of roots) {
                try {
                    const entries = fs.readdirSync(root)
                        .filter(d => d.toLowerCase().startsWith('jdk') || d.toLowerCase().startsWith('jre'))
                        .sort()
                        .reverse();
                    for (const entry of entries) {
                        candidates.push(path.join(root, entry, 'bin', 'java.exe'));
                    }
                } catch (e) {}
            }
        }

        for (const p of candidates) {
            try {
                // execFile: niente shell, niente problemi di quoting con spazi/metacaratteri nel path.
                execFileSync(p, ['-version'], { encoding: 'utf8', timeout: 5000, stdio: ['ignore', 'pipe', 'pipe'] });
                return p;
            } catch (e) {}
        }
        return null;
    }

    async downloadMinecraftLibraries(mcVersion, gameDir) {
        console.log('[MC Libs] Downloading Minecraft libraries...');
        
        const mcVersionDir = path.join(gameDir, 'versions', mcVersion);
        const versionJsonPath = path.join(mcVersionDir, `${mcVersion}.json`);
        
        if (!fs.existsSync(versionJsonPath)) {
            console.log('[MC Libs] Version JSON not found, downloading...');
            const manifest = await this.fetchJson('https://launchermeta.mojang.com/mc/game/version_manifest_v2.json');
            const versionInfo = manifest.versions.find(v => v.id === mcVersion);
            if (!versionInfo) throw new Error(`Version ${mcVersion} not found`);
            const versionData = await this.fetchJson(versionInfo.url);
            if (!fs.existsSync(mcVersionDir)) fs.mkdirSync(mcVersionDir, { recursive: true });
            fs.writeFileSync(versionJsonPath, JSON.stringify(versionData, null, 2));
        }
        
        const versionData = JSON.parse(fs.readFileSync(versionJsonPath, 'utf8'));
        const libraries = versionData.libraries || [];
        const libsDir = path.join(gameDir, 'libraries');
        
        let downloaded = 0;
        let skipped = 0;
        let failed = 0;
        
        for (const lib of libraries) {
            if (!lib.name) continue;
            
            // Parse Maven coordinate: group:artifact:version[:classifier]
            const parts = lib.name.split(':');
            if (parts.length < 3) continue;
            
            const [group, artifact, version, classifier] = parts;
            const groupPath = group.replace(/\./g, '/');
            const fileName = classifier ? `${artifact}-${version}-${classifier}.jar` : `${artifact}-${version}.jar`;
            const relativePath = `${groupPath}/${artifact}/${version}/${fileName}`;
            const destPath = path.join(libsDir, relativePath);

            const artifactDl = lib.downloads?.artifact;
            if (fs.existsSync(destPath)) {
                // Self-heal: un jar troncato da un download interrotto ha una size diversa dal manifest.
                if (artifactDl?.size == null || fs.statSync(destPath).size === artifactDl.size) {
                    skipped++;
                } else {
                    console.log(`[MC Libs] ${fileName} ha size errata, lo riscarico...`);
                    try {
                        await this.downloadFile(artifactDl.url, destPath, { sha1: artifactDl.sha1, size: artifactDl.size });
                        downloaded++;
                    } catch (e) {
                        console.log(`[MC Libs] Failed: ${lib.name}: ${e.message}`);
                        failed++;
                    }
                }
            } else {
                // Determine download URL (il manifest Mojang fornisce anche sha1 + size)
                let downloadUrl = null;
                let integrity = {};
                if (artifactDl?.url) {
                    downloadUrl = artifactDl.url;
                    integrity = { sha1: artifactDl.sha1, size: artifactDl.size };
                } else if (lib.url) {
                    downloadUrl = lib.url + relativePath;
                } else {
                    // Try common Maven repos
                    for (const repo of this.MAVEN_REPOS) {
                        downloadUrl = repo + relativePath;
                        break;
                    }
                }

                if (!downloadUrl) {
                    console.log(`[MC Libs] No URL for: ${lib.name}`);
                    failed++;
                } else {
                    try {
                        if (integrity.sha1 || integrity.size != null) {
                            await this.downloadFile(downloadUrl, destPath, integrity);
                        } else {
                            await this.downloadMavenFile(downloadUrl, destPath);
                        }
                        downloaded++;
                    } catch (e) {
                        console.log(`[MC Libs] Failed: ${lib.name}: ${e.message}`);
                        failed++;
                    }
                }
            }

            // Handle natives
            if (lib.natives && lib.natives[process.platform === 'win32' ? 'windows' : process.platform]) {
                const nativeClassifier = lib.natives[process.platform === 'win32' ? 'windows' : process.platform];
                const nativeFileName = `${artifact}-${version}-${nativeClassifier}.jar`;
                const nativeRelativePath = `${groupPath}/${artifact}/${version}/${nativeFileName}`;
                const nativeDestPath = path.join(libsDir, nativeRelativePath);

                if (!fs.existsSync(nativeDestPath)) {
                    const nativeDl = lib.downloads?.classifiers?.[nativeClassifier];
                    let nativeUrl = null;
                    let nativeIntegrity = {};
                    if (nativeDl?.url) {
                        nativeUrl = nativeDl.url;
                        nativeIntegrity = { sha1: nativeDl.sha1, size: nativeDl.size };
                    } else if (lib.url) {
                        nativeUrl = lib.url + nativeRelativePath;
                    }
                    if (nativeUrl) {
                        try {
                            await this.downloadFile(nativeUrl, nativeDestPath, nativeIntegrity);
                            console.log(`[MC Libs] Downloaded native: ${nativeFileName}`);
                        } catch(e) {
                            console.log(`[MC Libs] Failed to download native: ${nativeFileName}: ${e.message}`);
                        }
                    }
                }
            }
        }
        
        console.log(`[MC Libs] Done: ${downloaded} downloaded, ${skipped} skipped, ${failed} failed`);
        return { downloaded, skipped, failed, total: libraries.length };
    }
}

module.exports = FabricInstaller;
