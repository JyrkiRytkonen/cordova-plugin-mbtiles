var MBTilesPlugin;
MBTilesPlugin = (function () {
    function MBTilesPlugin(name, url, useSdCardRoot) {
        this.name = name;
        this.url =/* "cdvfile://localhost/persistent/" +*/ url;
        this.useSdCardRoot = useSdCardRoot;
    }

    MBTilesPlugin.prototype.open = function (onSuccess, onError) {
        return cordova.exec(onSuccess, onError, "MBTilesPlugin", "open", [
          {
              name: this.name,
              url: this.url
          }
        ]);
    };

    MBTilesPlugin.prototype.close = function (onSuccess, onError) {
        return cordova.exec(onSuccess, onError, "MBTilesPlugin", "close", [{ name: this.name }]);
    };

    MBTilesPlugin.prototype.getTile = function (params, onSuccess, onError) {
        return cordova.exec(onSuccess, onError, "MBTilesPlugin", "getTile", [this.name, params]);
    };

    MBTilesPlugin.prototype.getDatabaseSdCardPath = function (onSuccess, onError) {
        return cordova.exec(onSuccess, onError, "MBTilesPlugin", "getDatabaseSdCardPath", [{ useSdCardRoot: this.useSdCardRoot }]);
    };

    return MBTilesPlugin;

})();
module.exports = MBTilesPlugin;