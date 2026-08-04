const { contextBridge, ipcRenderer } = require('electron');

const api = {
    // Window controls
    minimize: () => ipcRenderer.invoke('window-minimize'),
    maximize: () => ipcRenderer.invoke('window-maximize'),
    close: () => ipcRenderer.invoke('window-close'),

    // Config & instances
    getConfig: () => ipcRenderer.invoke('get-config'),
    saveConfig: (config) => ipcRenderer.invoke('save-config', config),
    getInstances: () => ipcRenderer.invoke('get-instances'),
    saveInstances: (instances) => ipcRenderer.invoke('save-instances', instances),
    createInstance: (data) => ipcRenderer.invoke('create-instance', data),
    deleteInstance: (id) => ipcRenderer.invoke('delete-instance', id),

    // Paths
    getAppDataPath: () => ipcRenderer.invoke('get-app-data-path'),
    getLauncherModsPath: () => ipcRenderer.invoke('get-launcher-mods-path'),
    getInstancePath: (id) => ipcRenderer.invoke('get-instance-path', id),

    // Java
    selectJavaPath: () => ipcRenderer.invoke('select-java-path'),

    // Auth
    microsoftLogin: () => ipcRenderer.invoke('microsoft-login'),
    microsoftRefresh: (data) => ipcRenderer.invoke('microsoft-refresh', data),

    // Game
    launchMinecraft: (opts) => ipcRenderer.invoke('launch-minecraft', opts),
    cancelDownload: () => ipcRenderer.invoke('cancel-download'),
    onLaunchProgress: (callback) => {
        const handler = (event, p) => callback(p);
        ipcRenderer.on('launch-progress', handler);
        return () => ipcRenderer.removeListener('launch-progress', handler);
    },
    onMinecraftRestartRequested: (callback) => {
        const handler = (event, data) => callback(data);
        ipcRenderer.on('minecraft-restart-requested', handler);
        return () => ipcRenderer.removeListener('minecraft-restart-requested', handler);
    },
    onMinecraftClosed: (callback) => {
        const handler = (event, data) => callback(data);
        ipcRenderer.on('minecraft-closed', handler);
        return () => ipcRenderer.removeListener('minecraft-closed', handler);
    },

    // Skins
    getSkin: (opts) => ipcRenderer.invoke('get-skin', opts),

    // Mods
    listInstalledMods: (instanceId) => ipcRenderer.invoke('list-installed-mods', instanceId),
    deleteMod: (fileName, instanceId) => ipcRenderer.invoke('delete-mod', fileName, instanceId),
    selectModFile: () => ipcRenderer.invoke('select-mod-file'),
    installModFiles: (files, instanceId) => ipcRenderer.invoke('install-mod-files', files, instanceId),
    modrinthSearch: (query, opts) => ipcRenderer.invoke('modrinth-search', query, opts),
    modrinthDownload: (projectId, opts) => ipcRenderer.invoke('modrinth-download', projectId, opts),
    selectMinecraftDir: () => ipcRenderer.invoke('select-minecraft-dir'),

    // External
    openExternal: (url) => ipcRenderer.invoke('open-external', url),
    openPath: (targetPath) => ipcRenderer.invoke('open-path', targetPath),

    // Import da altri launcher
    importer: {
        detect: () => ipcRenderer.invoke('import:detect'),
        scanFolder: () => ipcRenderer.invoke('import:scan-folder'),
        run: (payload) => ipcRenderer.invoke('import:run', payload)
    }
};

contextBridge.exposeInMainWorld('electronAPI', api);
