var capacitorPlugin = (function (exports, core) {
    'use strict';

    var __awaiter = (undefined && undefined.__awaiter) || function (thisArg, _arguments, P, generator) {
        function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
        return new (P || (P = Promise))(function (resolve, reject) {
            function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
            function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
            function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
            step((generator = generator.apply(thisArg, _arguments || [])).next());
        });
    };
    class CapacitorPluginAppFusedLocationWeb extends core.WebPlugin {
        constructor() {
            super({
                name: 'CapacitorPluginAppFusedLocation',
                platforms: ['web'],
            });
        }
        getCurrentPosition(options) {
            return __awaiter(this, void 0, void 0, function* () {
                return new Promise((resolve, reject) => {
                    navigator.geolocation.getCurrentPosition(pos => {
                        resolve(pos);
                    }, err => {
                        reject(err);
                    }, Object.assign({ enableHighAccuracy: false, timeout: 10000, maximumAge: 0 }, options));
                });
            });
        }
        watchPosition(options, callback) {
            return __awaiter(this, void 0, void 0, function* () {
                const id = navigator.geolocation.watchPosition(pos => {
                    callback(pos);
                }, err => {
                    callback(null, err);
                }, Object.assign({ enableHighAccuracy: false, timeout: 10000, maximumAge: 0 }, options));
                return `${id}`;
            });
        }
        clearWatch(options) {
            return __awaiter(this, void 0, void 0, function* () {
                window.navigator.geolocation.clearWatch(parseInt(options.id, 10));
            });
        }
        checkPermissions() {
            return __awaiter(this, void 0, void 0, function* () {
                if (typeof navigator === 'undefined' || !navigator.permissions) {
                    throw new Error('Permissions API not available in this browser');
                }
                const permission = yield window.navigator.permissions.query({
                    name: 'geolocation',
                });
                return { location: permission.state };
            });
        }
        requestPermissions() {
            return __awaiter(this, void 0, void 0, function* () {
                throw new Error('Not implemented on web.');
            });
        }
    }
    const CapacitorPluginAppFusedLocation = new CapacitorPluginAppFusedLocationWeb();
    core.registerWebPlugin(CapacitorPluginAppFusedLocation);

    exports.CapacitorPluginAppFusedLocation = CapacitorPluginAppFusedLocation;
    exports.CapacitorPluginAppFusedLocationWeb = CapacitorPluginAppFusedLocationWeb;

    Object.defineProperty(exports, '__esModule', { value: true });

    return exports;

}({}, capacitorExports));
//# sourceMappingURL=plugin.js.map
