declare namespace Jessibuca {

    /** 超时信息 */
    enum TIMEOUT {
        /** 当play()的时候，如果没有数据返回 */
        loadingTimeout = 'loadingTimeout',
        /** 当播放过程中，如果超过timeout之后没有数据渲染 */
        delayTimeout = 'delayTimeout',
    }

    /** 错误信息 */
    enum ERROR {
        /** 播放错误，url 为空的时候，调用 play 方法 */
        playError = 'playError',
        /** http 请求失败 */
        fetchError = 'fetchError',
        /** websocket 请求失败 */
        websocketError = 'websocketError',
        /** webcodecs 解码 h265 失败 */
        webcodecsH265NotSupport = 'webcodecsH265NotSupport',
        /** mediaSource 解码 h265 失败 */
        mediaSourceH265NotSupport = 'mediaSourceH265NotSupport',
        /** wasm 解码失败 */
        wasmDecodeError = 'wasmDecodeError',
    }

    interface Config {
        /**
         * 播放器容器
         * *  若为 string ，则底层调用的是 document.getElementById('id')
         * */
        container: HTMLElement | string;
        /**
         * 设置最大缓冲时长，单位秒，播放器会自动消除延迟
         */
        videoBuffer?: number;
        /**
         * worker地址
         * *  默认引用的是根目录下面的decoder.js文件 ，decoder.js 与 decoder.wasm文件必须是放在同一个目录下面。 */
        decoder?: string;
        /**
         * 是否不使用离屏模式（提升渲染能力）
         */
        forceNoOffscreen?: boolean;
        /**
         * 是否开启当页面的'visibilityState'变为'hidden'的时候，自动暂停播放。
         */
        hiddenAutoPause?: boolean;
        /**
         * 是否有音频，如果设置`false`，则不对音频数据解码，提升性能。
         */
        hasAudio?: boolean;
        /**
         * 设置旋转角度，只支持，0(默认)，180，270 三个值
         */
        rotate?: boolean;
        /**
         * 1. 当为`true`的时候：视频画面做等比缩放后,高或宽对齐canvas区域,画面不被拉伸,但有黑边。 等同于 `setScaleMode(1)`
         * 2. 当为`false`的时候：视频画面完全填充canvas区域,画面会被拉伸。等同于 `setScaleMode(0)`
         */
        isResize?: boolean;
        /**
         * 1. 当为`true`的时候：视频画面做等比缩放后,完全填充canvas区域,画面不被拉伸,没有黑边,但画面显示不全。等同于 `setScaleMode(2)`
         */
        isFullResize?: boolean;
        /**
         * 1. 当为`true`的时候：ws协议不检验是否以.flv为依据，进行协议解析。
         */
        isFlv?: boolean;
        /**
         * 是否开启控制台调试打
         */
        debug?: boolean;
        /**
         * 1. 设置超时时长, 单位秒
         * 2. 在连接成功之前(loading)和播放中途(heart),如果超过设定时长无数据返回,则回调timeout事件
         */
        timeout?: number;
        /**
         * 1. 设置超时时长, 单位秒
         * 2. 在连接成功之前,如果超过设定时长无数据返回,则回调timeout事件
         */
        heartTimeout?: number;
        /**
         * 1. 设置超时时长, 单位秒
         * 2. 在连接成功之前,如果超过设定时长无数据返回,则回调timeout事件
         */
        loadingTimeout?: number;
        /**
         * 是否支持屏幕的双击事件，触发全屏，取消全屏事件
         */
        supportDblclickFullscreen?: boolean;
        /**
         * 是否显示网
         */
        showBandwidth?: boolean;
        /**
         * 配置操作按钮
         */
        operateBtns?: {
            /** 是否显示全屏按钮 */
            fullscreen?: boolean;
            /** 是否显示截图按钮 */
            screenshot?: boolean;
            /** 是否显示播放暂停按钮 */
            play?: boolean;
            /** 是否显示声音按钮 */
            audio?: boolean;
            /** 是否显示录制按 */
            record?: boolean;
        };
        /**
         * 开启屏幕常亮，在手机浏览器上, canvas标签渲染视频并不会像video标签那样保持屏幕常亮
         */
        keepScreenOn?: boolean;
        /**
         * 是否开启声音，默认是关闭声音播放的
         */
        isNotMute?: boolean;
        /**
         * 加载过程中文案
         */
        loadingText?: string;
        /**
         * 背景图片
         */
        background?: string;
        /**
         * 是否开启MediaSource硬解码
         * * 视频编码只支持H.264视频（Safari on iOS不支持）
         * * 不支持 forceNoOffscreen 为 false (开启离屏渲染)
         */
        useMSE?: boolean;
        /**
         * 是否开启Webcodecs硬解码
         * *  视频编码只支持H.264视频 (需在chrome 94版本以上，需要https或者localhost环境)
         * *  支持 forceNoOffscreen 为 false （开启离屏渲染)
         * */
        useWCS?: boolean;
        /**
         * 是否开启键盘快捷键
         * 目前支持的键盘快捷键有：esc -> 退出全屏；arrowUp -> 声音增加；arrowDown -> 声音减少；
         */
        hotKey?: boolean;
        /**
         *  在使用MSE或者Webcodecs 播放H265的时候，是否自动降级到wasm模式。
         *  设置为false 则直接关闭播放，抛出Error 异常，设置为true 则会自动切换成wasm模式播放。
         */
        autoWasm?: boolean;
        /**
         * heartTimeout 心跳超时之后自动再播放,不再抛出异常，而直接重新播放视频地址。
         */
        heartTimeoutReplay?: boolean,
        /**
         * heartTimeoutReplay 从试次数，超过之后，不再自动播放
         */
        heartTimeoutReplayTimes?: number,
        /**
         * loadingTimeout loading之后自动再播放,不再抛出异常，而直接重新播放视频地址。
         */
        loadingTimeoutReplay?: boolean,
        /**
         * heartTimeoutReplay 从试次数，超过之后，不再自动播放
         */
        loadingTimeoutReplayTimes?: number
        /**
         * wasm解码报错之后，不再抛出异常，而是直接重新播放视频地址。
         */
        wasmDecodeErrorReplay?: boolean,
        /**
         * https://github.com/langhuihui/jessibuca/issues/152 解决方案
         * 例如：WebGL图像预处理默认每次取4字节的数据，但是540x960分辨率下的U、V分量宽度是540/2=270不能被4整除，导致绿屏。
         */
        openWebglAlignment?: boolean
    }
}


declare class Jessibuca {

    constructor(config?: Jessibuca.Config);

    /**
     * 是否开启控制台调试打印
     @example
     // 开启
     jessibuca.setDebug(true)
     // 关闭
     jessibuca.setDebug(false)
     */
    setDebug(flag: boolean): void;

    /**
     * 静音
     @example
     jessibuca.mute()
     */
    mute(): void;

    /**
     * 取消静音
     @example
     jessibuca.cancelMute()
     */
    cancelMute(): void;

    /**
     * 留给上层用户操作来触发音频恢复的方法。
     *
     * iPhone，chrome等要求自动播放时，音频必须静音，需要由一个真实的用户交互操作来恢复，不能使用代码。
     *
     * https://developers.google.com/web/updates/2017/09/autoplay-policy-changes
     */
    audioResume(): void;

    /**
     *
     * 设置超时时长, 单位秒
     * 在连接成功之前和播放中途,如果超过设定时长无数据返回,则回调timeout事件

     @example
     jessibuca.setTimeout(10)

     jessibuca.on('timeout',function(){
        //
    });
     */
    setTimeout(): void;

    /**
     * @param mode
     *      0 视频画面完全填充canvas区域,画面会被拉伸  等同于参数 `isResize` 为false
     *
     *      1 视频画面做等比缩放后,高或宽对齐canvas区域,画面不被拉伸,但有黑边 等同于参数 `isResize` 为true
     *
     *      2 视频画面做等比缩放后,完全填充canvas区域,画面不被拉伸,没有黑边,但画面显示不全 等同于参数 `isFullResize` 为true
     @example
     jessibuca.setScaleMode(0)

     jessibuca.setScaleMode(1)

     jessibuca.setScaleMode(2)
     */
    setScaleMode(mode: number): void;

    /**
     * 暂停播放
     *
     * 可以在pause 之后，再调用 `play()`方法就继续播放之前的流。
     @example
     jessibuca.pause().then(()=>{
        console.log('pause success')

        jessibuca.play().then(()=>{

        }).catch((e)=>{

        })

    }).catch((e)=>{
        console.log('pause error',e);
    })
     */
    pause(): Promise<void>;

    /**
     * 关闭视频,不释放底层资源
     @example
     jessibuca.close();
     */
    close(): void;

    /**
     * 关闭视频，释放底层资源
     @example
     jessibuca.destroy()
     */
    destroy(): void;

    /**
     * 清理画布为黑色背景
     @example
     jessibuca.clearView()
     */
    clearView(): void;

    /**
     * 播放视频
     @example

     jessibuca.play('url').then(()=>{
        console.log('play success')
    }).catch((e)=>{
        console.log('play error',e)
    })
     //
     jessibuca.play()
     */
    play(url?: string): Promise<void>;

    /**
     * 重新调整视图大小
     */
    resize(): void;

    /**
     * 设置最大缓冲时长，单位秒，播放器会自动消除延迟。
     *
     * 等同于 `videoBuffer` 参数。
     *
     @example
     // 设置 200ms 缓冲
     jessibuca.setBufferTime(0.2)
     */
    setBufferTime(time: number): void;

    /**
     * 设置旋转角度，只支持，0(默认) ，180，270 三个值。
     *
     * > 可用于实现监控画面小窗和全屏效果，由于iOS没有全屏API，此方法可以模拟页面内全屏效果而且多端效果一致。   *
     @example
     jessibuca.setRotate(0)

     jessibuca.setRotate(90)

     jessibuca.setRotate(270)
     */
    setRotate(deg: number): void;

    /**
     *
     * 设置音量大小，取值0 — 1
     *
     * > 区别于 mute 和 cancelMute 方法，虽然设置setVolume(0) 也能达到 mute方法，但是mute 方法是不调用底层播放音频的，能提高性能。而setVolume(0)只是把声音设置为0 ，以达到效果。
     * @param volume 当为0时，完全无声;当为1时，最大音量，默认值
     @example
     jessibuca.setVolume(0.2)

     jessibuca.setVolume(0)

     jessibuca.setVolume(1)
     */
    setVolume(volume: number): void;

    /**
     * 返回是否加载完毕
     @example
     var result = jessibuca.hasLoaded()
     console.log(result) // true
     */
    hasLoaded(): boolean;

    /**
     * 开启屏幕常亮，在手机浏览器上, canvas标签渲染视频并不会像video标签那样保持屏幕常亮。
     * H5目前在chrome\edge 84, android chrome 84及以上有原生亮屏API, 需要是https页面
     * 其余平台为模拟实现，此时为兼容实现，并不保证所有浏览器都支持
     @example
     jessibuca.setKeepScreenOn()
     */
    setKeepScreenOn(): boolean;

    /**
     * 全屏(取消全屏)播放视频
     @example
     jessibuca.setFullscreen(true)
     //
     jessibuca.setFullscreen(false)
     */
    setFullscreen(flag: boolean): void;

    /**
     *
     * 截图，调用后弹出下载框保存截图
     * @param filename 可选参数, 保存的文件名, 默认 `时间戳`
     * @param format   可选参数, 截图的格式，可选png或jpeg或者webp ,默认 `png`
     * @param quality  可选参数, 当格式是jpeg或者webp时，压缩质量，取值0 ~ 1 ,默认 `0.92`
     * @param type 可选参数, 可选download或者base64或者blob，默认`download`

     @example

     jessibuca.screenshot("test","png",0.5)

     const base64 = jessibuca.screenshot("test","png",0.5,'base64')

     const fileBlob = jessibuca.screenshot("test",'blob')
     */
    screenshot(filename?: string, format?: string, quality?: number, type?: string): void;

    /**
     * 开始录制。
     * @param fileName 可选，默认时间戳
     * @param fileType 可选，默认webm，支持webm 和mp4 格式

     @example
     jessibuca.startRecord('xxx','webm')
     */
    startRecord(fileName: string, fileType: string): void;

    /**
     * 暂停录制并下载。
     @example
     jessibuca.stopRecordAndSave()
     */
    stopRecordAndSave(): void;

    /**
     * 返回是否正在播放中状态。
     @example
     var result = jessibuca.isPlaying()
     console.log(result) // true
     */
    isPlaying(): boolean;

    /**
     *   返回是否静音。
     @example
     var result = jessibuca.isMute()
     console.log(result) // true
     */
    isMute(): boolean;

    /**
     * 返回是否正在录制。
     @example
     var result = jessibuca.isRecording()
     console.log(result) // true
     */
    isRecording(): boolean;


    /**
     * 监听 jessibuca 初始化事件
     * @example
     * jessibuca.on("load",function(){console.log('load')})
     */
    on(event: 'load', callback: () => void): void;

    /**
     * 视频播放持续时间，单位ms
     * @example
     * jessibuca.on('timeUpdate',function (ts) {console.log('timeUpdate',ts);})
     */
    on(event: 'timeUpdate', callback: () => void): void;

    /**
     * 当解析出视频信息时回调，2个回调参数
     * @example
     * jessibuca.on("videoInfo",function(data){console.log('width:',data.width,'height:',data.width)})
     */
    on(event: 'videoInfo', callback: (data: {
        /** 视频宽 */
        width: number;
        /** 视频高 */
        height: number;
    }) => void): void;

    /**
     * 当解析出音频信息时回调，2个回调参数
     * @example
     * jessibuca.on("audioInfo",function(data){console.log('numOfChannels:',data.numOfChannels,'sampleRate',data.sampleRate)})
     */
    on(event: 'audioInfo', callback: (data: {
        /** 声频通道 */
        numOfChannels: number;
        /** 采样率 */
        sampleRate: number;
    }) => void): void;

    /**
     * 信息，包含错误信息
     * @example
     * jessibuca.on("log",function(data){console.log('data:',data)})
     */
    on(event: 'log', callback: () => void): void;

    /**
     * 错误信息
     * @example
     * jessibuca.on("error",function(error){
        if(error === Jessibuca.ERROR.fetchError){
            //
        }
        else if(error === Jessibuca.ERROR.webcodecsH265NotSupport){
            //
        }
        console.log('error:',error)
    })
     */
    on(event: 'error', callback: (err: Jessibuca.ERROR) => void): void;

    /**
     * 当前网速， 单位KB 每秒1次,
     * @example
     * jessibuca.on("kBps",function(data){console.log('kBps:',data)})
     */
    on(event: 'kBps', callback: (value: number) => void): void;

    /**
     * 渲染开始
     * @example
     * jessibuca.on("start",function(){console.log('start render')})
     */
    on(event: 'start', callback: () => void): void;

    /**
     * 当设定的超时时间内无数据返回,则回调
     * @example
     * jessibuca.on("timeout",function(error){console.log('timeout:',error)})
     */
    on(event: 'timeout', callback: (error: Jessibuca.TIMEOUT) => void): void;

    /**
     * 当play()的时候，如果没有数据返回，则回调
     * @example
     * jessibuca.on("loadingTimeout",function(){console.log('timeout')})
     */
    on(event: 'loadingTimeout', callback: () => void): void;

    /**
     * 当播放过程中，如果超过timeout之后没有数据渲染，则抛出异常。
     * @example
     * jessibuca.on("delayTimeout",function(){console.log('timeout')})
     */
    on(event: 'delayTimeout', callback: () => void): void;

    /**
     * 当前是否全屏
     * @example
     * jessibuca.on("fullscreen",function(flag){console.log('is fullscreen',flag)})
     */
    on(event: 'fullscreen', callback: () => void): void;

    /**
     * 触发播放事件
     * @example
     * jessibuca.on("play",function(flag){console.log('play')})
     */
    on(event: 'play', callback: () => void): void;

    /**
     * 触发暂停事件
     * @example
     * jessibuca.on("pause",function(flag){console.log('pause')})
     */
    on(event: 'pause', callback: () => void): void;

    /**
     * 触发声音事件，返回boolean值
     * @example
     * jessibuca.on("mute",function(flag){console.log('is mute',flag)})
     */
    on(event: 'mute', callback: () => void): void;

    /**
     * 流状态统计，流开始播放后回调，每秒1次。
     * @example
     * jessibuca.on("stats",function(s){console.log("stats is",s)})
     */
    on(event: 'stats', callback: (stats: {
        /** 当前缓冲区时长，单位毫秒 */
        buf: number;
        /** 当前视频帧率 */
        fps: number;
        /** 当前音频码率，单位byte */
        abps: number;
        /** 当前视频码率，单位byte */
        vbps: number;
        /** 当前视频帧pts，单位毫秒 */
        ts: number;
    }) => void): void;

    /**
     * 渲染性能统计，流开始播放后回调，每秒1次。
     * @param performance 0: 表示卡顿,1: 表示流畅,2: 表示非常流程
     * @example
     * jessibuca.on("performance",function(performance){console.log("performance is",performance)})
     */
    on(event: 'performance', callback: (performance: 0 | 1 | 2) => void): void;

    /**
     * 录制开始的事件

     * @example
     * jessibuca.on("recordStart",function(){console.log("record start")})
     */
    on(event: 'recordStart', callback: () => void): void;

    /**
     * 录制结束的事件

     * @example
     * jessibuca.on("recordEnd",function(){console.log("record end")})
     */
    on(event: 'recordEnd', callback: () => void): void;

    /**
     * 录制的时候，返回的录制时长，1s一次

     * @example
     * jessibuca.on("recordingTimestamp",function(timestamp){console.log("recordingTimestamp is",timestamp)})
     */
    on(event: 'recordingTimestamp', callback: (timestamp: number) => void): void;

    /**
     * 监听调用play方法 经过 初始化-> 网络请求-> 解封装 -> 解码 -> 渲染 一系列过程的时间消耗
     * @param event
     * @param callback
     */
    on(event: 'playToRenderTimes', callback: (times: {
        playInitStart: number, // 1 初始化
        playStart: number, // 2 初始化
        streamStart: number, // 3 网络请求
        streamResponse: number, // 4 网络请求
        demuxStart: number, // 5 解封装
        decodeStart: number, // 6 解码
        videoStart: number, // 7 渲染
        playTimestamp: number,// playStart- playInitStart
        streamTimestamp: number,// streamStart - playStart
        streamResponseTimestamp: number,// streamResponse - streamStart
        demuxTimestamp: number, // demuxStart - streamResponse
        decodeTimestamp: number, // decodeStart - demuxStart
        videoTimestamp: number,// videoStart - decodeStart
        allTimestamp: number // videoStart - playInitStart
    }) => void): void

    /**
     * 监听方法
     *
     @example

     jessibuca.on("load",function(){console.log('load')})
     */
    on(event: string, callback: Function): void;

}

export default Jessibuca;
