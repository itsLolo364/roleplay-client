'use strict';

const { detectImportSources, analyzeGameDir } = require('./detect');
const { runImport, OPTION_FILES } = require('./apply');

const ALL_CATEGORIES = ['options', 'servers', 'mods', 'config', 'resourcepacks', 'shaderpacks'];

function defaultCategoriesFor(source) {
    const cats = [];
    if (source.hasOptions) cats.push('options');
    if (source.hasServers) cats.push('servers');
    if (source.modCount > 0) cats.push('mods');
    if (source.hasConfig) cats.push('config');
    if (source.resourcePackCount > 0) cats.push('resourcepacks');
    if (source.shaderPackCount > 0) cats.push('shaderpacks');
    return cats;
}

module.exports = {
    detectImportSources,
    analyzeGameDir,
    runImport,
    ALL_CATEGORIES,
    defaultCategoriesFor,
    OPTION_FILES
};
