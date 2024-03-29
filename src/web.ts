import { WebPlugin } from '@capacitor/core';
import {
  CallbackID,
  CapacitorPluginAppFusedLocationPlugin,
  Position,
  PositionOptions,
  WatchPositionCallback,
} from './definitions';

export class CapacitorPluginAppFusedLocationWeb
  extends WebPlugin
  implements CapacitorPluginAppFusedLocationPlugin {
  constructor() {
    super({
      name: 'CapacitorPluginAppFusedLocation',
      platforms: ['web'],
    });
  }

  async getCurrentPosition(options?: PositionOptions): Promise<Position> {
    return new Promise((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(
        pos => {
          resolve(pos);
        },
        err => {
          reject(err);
        },
        {
          enableHighAccuracy: false,
          timeout: 10000,
          maximumAge: 0,
          ...options,
        },
      );
    });
  }

  async watchPosition(
    options: PositionOptions,
    callback: WatchPositionCallback,
  ): Promise<CallbackID> {
    const id = navigator.geolocation.watchPosition(
      pos => {
        callback(pos);
      },
      err => {
        callback(null, err);
      },
      {
        enableHighAccuracy: false,
        timeout: 10000,
        maximumAge: 0,
        ...options,
      },
    );

    return `${id}`;
  }

  async clearWatch(options: { id: string }): Promise<void> {
    window.navigator.geolocation.clearWatch(parseInt(options.id, 10));
  }
}

const CapacitorPluginAppFusedLocation = new CapacitorPluginAppFusedLocationWeb();

export { CapacitorPluginAppFusedLocation };

import { registerWebPlugin } from '@capacitor/core';

registerWebPlugin(CapacitorPluginAppFusedLocation);
