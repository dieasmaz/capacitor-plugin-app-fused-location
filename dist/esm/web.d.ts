import { WebPlugin } from '@capacitor/core';
import { CallbackID, CapacitorPluginAppFusedLocationPlugin, PermissionStatus, Position, PositionOptions, WatchPositionCallback } from './definitions';
export declare class CapacitorPluginAppFusedLocationWeb extends WebPlugin implements CapacitorPluginAppFusedLocationPlugin {
    constructor();
    getCurrentPosition(options?: PositionOptions): Promise<Position>;
    watchPosition(options: PositionOptions, callback: WatchPositionCallback): Promise<CallbackID>;
    clearWatch(options: {
        id: string;
    }): Promise<void>;
    checkPermissions(): Promise<PermissionStatus>;
    requestPermissions(): Promise<PermissionsRequestResult>;
}
declare const CapacitorPluginAppFusedLocation: CapacitorPluginAppFusedLocationWeb;
export { CapacitorPluginAppFusedLocation };
import { PermissionsRequestResult } from '@capacitor/core/dist/esm/definitions';
