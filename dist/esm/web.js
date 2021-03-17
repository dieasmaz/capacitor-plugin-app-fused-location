var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
import { WebPlugin } from '@capacitor/core';
export class CapacitorPluginAppFusedLocationWeb extends WebPlugin {
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
}
const CapacitorPluginAppFusedLocation = new CapacitorPluginAppFusedLocationWeb();
export { CapacitorPluginAppFusedLocation };
import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(CapacitorPluginAppFusedLocation);
//# sourceMappingURL=web.js.map