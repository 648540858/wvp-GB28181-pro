export interface Web265JsExtraConfig {
  moovStartFlag?: boolean
  rawFps?: number
  autoCrop?: boolean
  core?: 0 | 1
  coreProbePart?: number
  ignoreAudio?: 0 | 1
  probeSize?: number
}

export interface Web265JsConfig {
  /**
   *The type of the file to be played, do not fill in the automatic identification
   */
  type?: 'mp4' | 'hls' | 'ts' | 'raw265' | 'flv'
  /**
   * playback window dom id value
   */
  player: string
  /**
   * the video window width size
   */
  width: number
  /**
   * the video window height size
   */
  height: number
  /**
   * player token value
   */
  token: string
  extInfo?: Web265JsExtraConfig
}

export interface Web265JsMediaInfo {
  audioNone: boolean
  durationMs: number
  fps: number
  sampleRate: number
  size: {
    height: number
    width: number
  }
  videoCodec: 0 | 1
  isHEVC: boolean
  videoType: Web265JsConfig['type']
}

interface New265WebJs {
  onSeekFinish(): void
  onRender(
    width: number,
    height: number,
    imageBufferY: typeof Uint8Array,
    imageBufferB: typeof Uint8Array,
    imageBufferR: typeof Uint8Array
  ): void
  onLoadFinish(): void
  onPlayTime(videoPTS: number): void
  onPlayFinish(): void
  onCacheProcess(cPts: number): void
  onReadyShowDone(): void
  onLoadCache(): void
  onLoadCacheFinshed(): void
  onOpenFullScreen(): void
  onCloseFullScreen(): void
  do(): void
  pause(): void
  isPlaying(): boolean
  setRenderScreen(state: boolean): void
  seek(pts: number): void
  setVoice(volume: number): void
  mediaInfo(): Web265JsMediaInfo
  fullScreen(): void
  closeFullScreen(): void
  playNextFrame(): void
  snapshot(): void
  release(): void
  setPlaybackRate(rate: number): void
  getPlaybackRate(): number
}

declare type new265webJsFn = (
  url: string,
  config: Web265JsConfig
) => New265WebJs

declare global {
  interface Window {
    new265webjs: new265webJsFn
  }
}

export default class H265webjsModule {
  static createPlayer: (url: string, config: Web265JsConfig) => New265WebJs
  static clear(): void
}
