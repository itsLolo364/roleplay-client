const https = require('https');
const http = require('http');
const fs = require('fs');
const path = require('path');
const { execSync, spawn } = require('child_process');

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

    fetchJson(url) {
        return new Promise((resolve, reject) => {
            const client = url.startsWith('https') ? https : http;
            client.get(url, { headers: { 'Accept': 'application/json' } }, (res) => {
                if (res.statusCode === 301 || res.statusCode === 302) {
                    return this.fetchJson(res.headers.location).then(resolve).catch(reject);
                }
                let data = '';
                res.on('data', chunk => data += chunk);
                res.on('end', () => {
                    try { resolve(JSON.parse(data)); } 
                    catch (e) { reject(new Error('JSON parse error')); }
                });
            }).on('error', reject);
        });
    }

    downloadFile(url, dest) {
        return new Promise((resolve, reject) => {
            const dir = path.dirname(dest);
            if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
            
            const protocol = url.startsWith('https') ? https : http;
            const file = fs.createWriteStream(dest);
            
            protocol.get(url, (res) => {
                if (res.statusCode === 301 || res.statusCode === 302) {
                    file.close();
                    try { fs.unlinkSync(dest); } catch(e) {}
                    return this.downloadFile(res.headers.location, dest).then(resolve).catch(reject);
                }
                if (res.statusCode !== 200) {
                    file.close();
                    try { fs.unlinkSync(dest); } catch(e) {}
                    return reject(new Error(`Download failed with status ${res.statusCode}`));
                }
                res.pipe(file);
                file.on('finish', () => { file.close(); resolve(dest); });
            }).on('error', (err) => {
                try { fs.unlinkSync(dest); } catch(e) {}
                reject(err);
            });
        });
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
                await this.downloadFile(this.INSTALLER_URL, installerPath);
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
            
            const child = spawn(javaExec, args, { 
                stdio: ['pipe', 'pipe', 'pipe'],
                shell: process.platform === 'win32'
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
        
        if (fs.existsSync(mcJarPath)) {
            console.log(`[MC] JAR exists: ${mcJarPath}`);
            return mcJarPath;
        }
        
        console.log(`[MC] JAR not found, downloading...`);
        
        // Read the version JSON to get download URL
        const versionJsonPath = path.join(mcVersionDir, `${mcVersion}.json`);
        if (!fs.existsSync(versionJsonPath)) {
            // Download version manifest first
            const manifest = await this.fetchJson('https://launchermeta.mojang.com/mc/game/version_manifest_v2.json');
            const versionInfo = manifest.versions.find(v => v.id === mcVersion);
            if (!versionInfo) throw new Error(`Version ${mcVersion} not found`);
            
            const versionData = await this.fetchJson(versionInfo.url);
            if (!fs.existsSync(mcVersionDir)) {
                fs.mkdirSync(mcVersionDir, { recursive: true });
            }
            fs.writeFileSync(versionJsonPath, JSON.stringify(versionData, null, 2));
            
            // Download the JAR
            const jarUrl = versionData.downloads?.client?.url;
            if (!jarUrl) throw new Error('MC JAR download URL not found');
            
            console.log(`[MC] Downloading from ${jarUrl}...`);
            await this.downloadFile(jarUrl, mcJarPath);
            console.log(`[MC] Downloaded to ${mcJarPath}`);
        } else {
            const versionData = JSON.parse(fs.readFileSync(versionJsonPath, 'utf8'));
            const jarUrl = versionData.downloads?.client?.url;
            if (!jarUrl) throw new Error('MC JAR download URL not found');
            
            console.log(`[MC] Downloading from ${jarUrl}...`);
            await this.downloadFile(jarUrl, mcJarPath);
            console.log(`[MC] Downloaded to ${mcJarPath}`);
        }
        
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
            await this.downloadFile(loaderJarUrl, loaderJarPath);
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
                await this.downloadFile(url, dest);
            } catch (e) {
                console.error(`[Fabric] Lib download failed ${lib.name}:`, e.message);
            }
        }
    }

    findJava() {
        const possiblePaths = [
            'java',
            'javaw',
            process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin', 'java.exe') : null,
            'C:/Program Files/Eclipse Adoptium/jdk-21*/bin/java.exe',
            'C:/Program Files/Java/jdk-21*/bin/java.exe',
            'C:/Program Files/Microsoft/jdk-21*/bin/java.exe',
            'C:/Program Files/Eclipse Adoptium/jdk*/bin/java.exe',
            'C:/Program Files/Java/jdk*/bin/java.exe',
            'C:/Program Files/Microsoft/jdk*/bin/java.exe'
        ].filter(Boolean);
        
        for (const p of possiblePaths) {
            if (p.includes('*')) continue;
            try {
                execSync(`"${p}" -version 2>&1`, { encoding: 'utf8', timeout: 5000 });
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
            
            if (fs.existsSync(destPath)) {
                skipped++;
                continue;
            }
            
            if (!fs.existsSync(destPath)) {
                // Determine download URL
                let downloadUrl = null;
                // Check downloads.artifact.url first (standard MC format)
                if (lib.downloads?.artifact?.url) {
                    downloadUrl = lib.downloads.artifact.url;
                } else if (lib.url) {
                    downloadUrl = lib.url + relativePath;
                } else {
                    // Try common Maven repos
                    for (const repo of this.MAVEN_REPOS) {
                        downloadUrl = repo + relativePath;
                        break;
                    }
                }

                // Handle natives
                if (lib.natives && lib.natives[process.platform === 'win32' ? 'windows' : process.platform]) {
                    const nativeClassifier = lib.natives[process.platform === 'win32' ? 'windows' : process.platform];
                    const nativeFileName = `${artifact}-${version}-${nativeClassifier}.jar`;
                    const nativeRelativePath = `${groupPath}/${artifact}/${version}/${nativeFileName}`;
                    const nativeDestPath = path.join(libsDir, nativeRelativePath);
                    
                    if (!fs.existsSync(nativeDestPath)) {
                        let nativeUrl = null;
                        if (lib.downloads?.classifiers?.[nativeClassifier]?.url) {
                            nativeUrl = lib.downloads.classifiers[nativeClassifier].url;
                        } else if (lib.url) {
                            nativeUrl = lib.url + nativeRelativePath;
                        }
                        if (nativeUrl) {
                            try {
                                const dir = path.dirname(nativeDestPath);
                                if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
                                await this.downloadFile(nativeUrl, nativeDestPath);
                                console.log(`[MC Libs] Downloaded native: ${nativeFileName}`);
                            } catch(e) {
                                console.log(`[MC Libs] Failed to download native: ${nativeFileName}: ${e.message}`);
                            }
                        }
                    }
                }

                if (!downloadUrl) {
                    console.log(`[MC Libs] No URL for: ${lib.name}`);
                    failed++;
                } else {
                    try {
                        const dir = path.dirname(destPath);
                        if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
                        await this.downloadFile(downloadUrl, destPath);
                        downloaded++;
                    } catch (e) {
                        console.log(`[MC Libs] Failed: ${lib.name}: ${e.message}`);
                        failed++;
                    }
                }
            }
        }
        
        console.log(`[MC Libs] Done: ${downloaded} downloaded, ${skipped} skipped, ${failed} failed`);
        return { downloaded, skipped, failed, total: libraries.length };
    }
}

module.exports = FabricInstaller;
