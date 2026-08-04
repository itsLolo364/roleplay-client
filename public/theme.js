// Applica il tema salvato PRIMA del render per evitare flash.
// File esterno: con CSP script-src 'self' gli script inline sono bloccati.
(function () {
    try { document.documentElement.dataset.theme = localStorage.getItem('rc-theme') || 'dark'; } catch (e) {}
})();
