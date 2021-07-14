!function () {
  function e(e) {
    return e && e.__esModule ? e.default : e
  }

  function t(e, t, r) {
    Object.defineProperty(e, t, {get: r, enumerable: !0})
  }

  var r, n, o = !1;

  function a() {
    return o || (o = !0, n = e => {
      var t = ["attribute vec4 vertexPos;", "attribute vec4 texturePos;", "varying vec2 textureCoord;", "void main()", "{", "gl_Position = vertexPos;", "textureCoord = texturePos.xy;", "}"].join("\n"),
        r = ["precision highp float;", "varying highp vec2 textureCoord;", "uniform sampler2D ySampler;", "uniform sampler2D uSampler;", "uniform sampler2D vSampler;", "const mat4 YUV2RGB = mat4", "(", "1.1643828125, 0, 1.59602734375, -.87078515625,", "1.1643828125, -.39176171875, -.81296875, .52959375,", "1.1643828125, 2.017234375, 0, -1.081390625,", "0, 0, 0, 1", ");", "void main(void) {", "highp float y = texture2D(ySampler,  textureCoord).r;", "highp float u = texture2D(uSampler,  textureCoord).r;", "highp float v = texture2D(vSampler,  textureCoord).r;", "gl_FragColor = vec4(y, u, v, 1) * YUV2RGB;", "}"].join("\n"),
        n = e.createShader(e.VERTEX_SHADER);
      e.shaderSource(n, t), e.compileShader(n), e.getShaderParameter(n, e.COMPILE_STATUS) || console.log("Vertex shader failed to compile: " + e.getShaderInfoLog(n));
      var o = e.createShader(e.FRAGMENT_SHADER);
      e.shaderSource(o, r), e.compileShader(o), e.getShaderParameter(o, e.COMPILE_STATUS) || console.log("Fragment shader failed to compile: " + e.getShaderInfoLog(o));
      var a = e.createProgram();
      e.attachShader(a, n), e.attachShader(a, o), e.linkProgram(a), e.getProgramParameter(a, e.LINK_STATUS) || console.log("Program failed to compile: " + e.getProgramInfoLog(a)), e.useProgram(a);
      var i = e.createBuffer();
      e.bindBuffer(e.ARRAY_BUFFER, i), e.bufferData(e.ARRAY_BUFFER, new Float32Array([1, 1, -1, 1, 1, -1, -1, -1]), e.STATIC_DRAW);
      var s = e.getAttribLocation(a, "vertexPos");
      e.enableVertexAttribArray(s), e.vertexAttribPointer(s, 2, e.FLOAT, !1, 0, 0);
      var u = e.createBuffer();
      e.bindBuffer(e.ARRAY_BUFFER, u), e.bufferData(e.ARRAY_BUFFER, new Float32Array([1, 0, 0, 0, 1, 1, 0, 1]), e.STATIC_DRAW);
      var c = e.getAttribLocation(a, "texturePos");

      function l(t, r) {
        var n = e.createTexture();
        return e.bindTexture(e.TEXTURE_2D, n), e.texParameteri(e.TEXTURE_2D, e.TEXTURE_MAG_FILTER, e.LINEAR), e.texParameteri(e.TEXTURE_2D, e.TEXTURE_MIN_FILTER, e.LINEAR), e.texParameteri(e.TEXTURE_2D, e.TEXTURE_WRAP_S, e.CLAMP_TO_EDGE), e.texParameteri(e.TEXTURE_2D, e.TEXTURE_WRAP_T, e.CLAMP_TO_EDGE), e.bindTexture(e.TEXTURE_2D, null), e.uniform1i(e.getUniformLocation(a, t), r), n
      }

      e.enableVertexAttribArray(c), e.vertexAttribPointer(c, 2, e.FLOAT, !1, 0, 0);
      var d = l("ySampler", 0), f = l("uSampler", 1), p = l("vSampler", 2);
      return function (t, r, n, o, a) {
        e.viewport(0, 0, t, r), e.activeTexture(e.TEXTURE0), e.bindTexture(e.TEXTURE_2D, d), e.texImage2D(e.TEXTURE_2D, 0, e.LUMINANCE, t, r, 0, e.LUMINANCE, e.UNSIGNED_BYTE, n), e.activeTexture(e.TEXTURE1), e.bindTexture(e.TEXTURE_2D, f), e.texImage2D(e.TEXTURE_2D, 0, e.LUMINANCE, t / 2, r / 2, 0, e.LUMINANCE, e.UNSIGNED_BYTE, o), e.activeTexture(e.TEXTURE2), e.bindTexture(e.TEXTURE_2D, p), e.texImage2D(e.TEXTURE_2D, 0, e.LUMINANCE, t / 2, r / 2, 0, e.LUMINANCE, e.UNSIGNED_BYTE, a), e.drawArrays(e.TRIANGLE_STRIP, 0, 4)
      }
    }, t(r = {}, "default", (function () {
      return n
    }))), r
  }

  var i, s, u = !1;

  function c() {
    i = {}, a(), s = e => {
      const t = document.createElement("canvas");
      t.style.position = "absolute", t.style.top = 0, t.style.left = 0, e.$container.appendChild(t), e.$canvasElement = t, e.$container.style.overflow = "hidden", "absolute" !== e.$container.style.position && (e.$container.style.position = "relative");
      if (!e._supportOffscreen()) {
        const t = (() => {
          const t = e.$canvasElement;
          let r = null;
          const n = ["webgl", "experimental-webgl", "moz-webgl", "webkit-3d"];
          let o = 0;
          for (; !r && o < n.length;) {
            const a = n[o];
            try {
              let n = {preserveDrawingBuffer: !0};
              e._opt.contextOptions && (n = Object.assign(n, e._opt.contextOptions)), r = t.getContext(a, n)
            } catch (e) {
              r = null
            }
            r && "function" == typeof r.getParameter || (r = null), ++o
          }
          return r
        })();
        e._contextGLRender = a().default(t), e._contextGL = t
      }
      e._destroyContextGL = () => {
        e._contextGL && (e._contextGL = null), e._contextGLRender && (e._contextGLRender = null), e._bitmaprenderer && (e._bitmaprenderer = null)
      }
    }, t(i, "default", (function () {
      return s
    }))
  }

  function l() {
    return u || (u = !0, c()), i
  }

  var d, f, p, h, m, A, g = !1;

  function v() {
    return g || (g = !0, f = {
      videoBuffer: .5,
      vod: !1,
      isResize: !0,
      isFullSize: !1,
      debug: !1,
      timeout: 30,
      supportDblclickFullscreen: !1,
      showBandwidth: !1,
      keepScreenOn: !1,
      isNotMute: !1,
      hasAudio: !0,
      operateBtns: {fullscreen: !1, screenshot: !1, play: !1, audio: !1},
      loadingText: "",
      background: "",
      decoder: "index.js",
      rotate: 0,
      forceNoOffscreen: !1
    }, t(d = {}, "DEFAULT_OPTIONS", (function () {
      return f
    })), p = {
      init: "init",
      initSize: "initSize",
      render: "render",
      playAudio: "playAudio",
      print: "print",
      printErr: "printErr",
      initAudioPlanar: "initAudioPlanar",
      kBps: "kBps"
    }, t(d, "CMD_TYPE", (function () {
      return p
    })), h = {
      close: "close",
      play: "play",
      setVideoBuffer: "setVideoBuffer",
      init: "init"
    }, t(d, "POST_MESSAGE", (function () {
      return h
    })), m = {
      fullscreen: "fullscreen",
      play: "play",
      pause: "pause",
      mute: "mute",
      load: "load",
      videoInfo: "videoInfo",
      timeUpdate: "timeUpdate",
      audioInfo: "audioInfo",
      log: "log",
      error: "error",
      kBps: "kBps",
      timeout: "timeout",
      stats: "stats",
      performance: "performance",
      record: "record",
      buffer: "buffer",
      videoFrame: "videoFrame",
      start: "start",
      metadata: "metadata"
    }, t(d, "EVEMTS", (function () {
      return m
    })), A = {empty: "empty", buffering: "buffering", full: "full"}, t(d, "BUFFER_STATUS", (function () {
      return A
    }))), d
  }

  var E, y = !1;

  function w(e) {
    e.resume();
    const t = e.createBufferSource();
    t.buffer = e.createBuffer(1, 1, 22050), t.connect(e.destination), t.noteOn ? t.noteOn(0) : t.start(0)
  }

  function _(e, t) {
    e && (e.style.display = t ? "block" : "none")
  }

  function b(e = "") {
    const t = e.split(","), r = atob(t[1]), n = t[0].replace("data:", "").replace(";base64", "");
    let o = r.length, a = new Uint8Array(o);
    for (; o--;) a[o] = r.charCodeAt(o);
    return new File([a], "file", {type: n})
  }

  function k(e, t) {
    const r = document.createElement("a");
    r.download = t, r.href = URL.createObjectURL(e), r.click(), URL.revokeObjectURL(e)
  }

  function T(e) {
    if (null == e || "" === e) return "0 KB/S";
    let t = parseFloat(e);
    return t = t.toFixed(2), t + "KB/S"
  }

  function D(e) {
    let t = 0;
    return e >= 24 ? t = 2 : e >= 15 && (t = 1), t
  }

  function S(e, t) {
    Object.keys(t || {}).forEach((function (r) {
      e.style[r] = t[r]
    }))
  }

  function C() {
    let e = document.fullscreenElement || window.webkitFullscreenElement || document.msFullscreenElement;
    return void 0 === e && (e = !1), !!e
  }

  function O() {
  }

  function P() {
    return (new Date).getTime()
  }

  function x(e) {
    Object.keys(e || {}).forEach((t => {
      "bgDom" !== t && _(e[t], !1)
    }))
  }

  function B(e) {
    _(e.pauseDom, !0), _(e.screenshotsDom, !0), _(e.fullscreenDom, !0), _(e.quietAudioDom, !0), _(e.textDom, !0), _(e.speedDom, !0), _(e.recordDom, !0), _(e.loadingDom, !1), _(e.playDom, !1), _(e.playBigDom, !1), _(e.bgDom, !1)
  }

  function M(e, t) {
    let r = v().BUFFER_STATUS.buffering;
    return 0 === e ? r = v().BUFFER_STATUS.empty : e >= t && (r = v().BUFFER_STATUS.full), r
  }

  function R() {
    return y || (y = !0, E = {}, v(), t(E, "audioContextUnlock", (function () {
      return w
    })), t(E, "$domToggle", (function () {
      return _
    })), t(E, "dataURLToFile", (function () {
      return b
    })), t(E, "downloadImg", (function () {
      return k
    })), t(E, "bpsSize", (function () {
      return T
    })), t(E, "fpsStatus", (function () {
      return D
    })), t(E, "setStyle", (function () {
      return S
    })), t(E, "checkFull", (function () {
      return C
    })), t(E, "noop", (function () {
      return O
    })), t(E, "now", (function () {
      return P
    })), t(E, "$hideBtns", (function () {
      return x
    })), t(E, "$initBtns", (function () {
      return B
    })), t(E, "bufferStatus", (function () {
      return M
    }))), E
  }

  var L, I, F = !1;

  function U() {
    return F || (F = !0, L = {}, R(), I = e => {
      e._audioContext = new (window.AudioContext || window.webkitAudioContext), e._gainNode = e._audioContext.createGain(), e._audioEnabled = t => {
        t ? (R().audioContextUnlock(e._audioContext), e._audioEnabled = t => {
          t ? e._audioContext.resume() : e._audioContext.suspend()
        }, e._audioContext.resume()) : e._audioContext.suspend()
      }, e._audioEnabled(!0), e._mute = () => {
        e._audioEnabled(!1), e.quieting = !0
      }, e._cancelMute = () => {
        e._audioEnabled(!0), e.quieting = !1
      }, e._audioResume = () => {
        e._cancelMute()
      }, e._initAudioPlanar = t => {
        const r = e._audioContext;
        if (!r) return !1;
        let n = [];
        const o = r.createScriptProcessor(1024, 0, 2);
        o.onaudioprocess = function (e) {
          if (n.length) {
            const r = n.shift();
            for (let n = 0; n < t.channels; n++) {
              const t = r[n], o = e.outputBuffer.getChannelData(n);
              for (let e = 0; e < 1024; e++) o[e] = t[e]
            }
          }
        }, o.connect(e._gainNode), e._closeAudio = () => {
          o.disconnect(e._gainNode), e._gainNode.disconnect(r.destination), delete e._closeAudio, n = []
        }, e._gainNode.connect(r.destination), e._playAudio = e => n.push(e)
      }, e._destroyAudioContext = () => {
        e._audioContext.close(), e._audioContext = null, e._gainNode = null
      }
    }, t(L, "default", (function () {
      return I
    }))), L
  }

  var N, j, G = !1;

  function z() {
    return G || (G = !0, N = {}, R(), j = e => {
      e._resize$2 = () => e.resize(), e._handleVisibilityChange$2 = () => e._handleVisibilityChange(), e._onfullscreenchange$2 = () => e._onfullscreenchange(), e._handleWakeLock$2 = () => e._handleWakeLock(), window.addEventListener("resize", e._resize$2), window.addEventListener("fullscreenchange", e._onfullscreenchange$2), document.addEventListener("visibilitychange", e._handleVisibilityChange$2), document.addEventListener("visibilitychange", e._handleWakeLock$2), window.addEventListener("fullscreenchange", e._handleWakeLock$2), e._opt.supportDblclickFullscreen && e.$canvasElement.addEventListener("dblclick", (() => {
        e.fullscreen = !e.fullscreen
      }), !1), e._removeEventListener = () => {
        window.removeEventListener("resize", e._resize$2), window.removeEventListener("fullscreenchange", e._onfullscreenchange$2), document.removeEventListener("visibilitychange", e._handleWakeLock$2), document.removeEventListener("visibilitychange", e._handleVisibilityChange$2), window.removeEventListener("fullscreenchange", e._handleWakeLock$2)
      }, e.$doms.playDom && e.$doms.playDom.addEventListener("click", (t => {
        t.stopPropagation(), e._play()
      }), !1), e.$doms.playBigDom && e.$doms.playBigDom.addEventListener("click", (t => {
        t.stopPropagation(), e._play()
      }), !1), e.$doms.pauseDom && e.$doms.pauseDom.addEventListener("click", (t => {
        t.stopPropagation(), e._pause()
      }), !1), e.$doms.screenshotsDom && e.$doms.screenshotsDom.addEventListener("click", (t => {
        t.stopPropagation();
        const r = e._opt.text + "" + R().now();
        e._screenshot(r)
      }), !1), e.$doms.fullscreenDom && e.$doms.fullscreenDom.addEventListener("click", (t => {
        t.stopPropagation(), e.fullscreen = !0
      }), !1), e.$doms.minScreenDom && e.$doms.minScreenDom.addEventListener("click", (t => {
        t.stopPropagation(), e.fullscreen = !1
      }), !1), e.$doms.recordDom && e.$doms.recordDom.addEventListener("click", (t => {
        t.stopPropagation(), e.recording = !0
      }), !1), e.$doms.recordingDom && e.$doms.recordingDom.addEventListener("click", (t => {
        t.stopPropagation(), e.recording = !1
      }), !1), e.$doms.quietAudioDom && e.$doms.quietAudioDom.addEventListener("click", (t => {
        t.stopPropagation(), e._cancelMute()
      }), !1), e.$doms.playAudioDom && e.$doms.playAudioDom.addEventListener("click", (t => {
        t.stopPropagation(), e._mute()
      }), !1), e._enableWakeLock()
    }, t(N, "default", (function () {
      return j
    }))), N
  }

  var W, Y, H = !1;

  function $() {
    return H || (H = !0, W = {}, R(), z(), Y = e => {
      e._showControl = () => {
        let t = !1, r = !1;
        return Object.keys(e._opt.operateBtns).forEach((t => {
          e._opt.operateBtns[t] && (r = !0)
        })), (e._opt.showBandwidth || e._opt.text || r) && (t = !0), t
      };
      const t = {}, r = document.createDocumentFragment(), n = document.createElement("div"),
        o = document.createElement("div"), a = document.createElement("div"), i = document.createElement("div"),
        s = document.createElement("div"), u = document.createElement("div"), c = document.createElement("div"),
        l = document.createElement("div"), d = document.createElement("div"), f = document.createElement("div"),
        p = document.createElement("div"), h = document.createElement("div"), m = document.createElement("div"),
        A = document.createElement("div"), g = document.createElement("div"), v = document.createElement("div"),
        E = document.createElement("div"), y = document.createElement("div");
      m.innerText = e._opt.loadingText || "", i.innerText = e._opt.text || "", s.innerText = "", u.title = "播放", l.title = "暂停", d.title = "截屏", f.title = "全屏", p.title = "退出全屏", A.title = "静音", g.title = "取消静音", v.title = "录制", E.title = "取消录制";
      let w = {position: "absolute", width: "100%", height: "100%"};
      e._opt.background && (w = Object.assign({}, w, {
        backgroundRepeat: "no-repeat",
        backgroundPosition: "center",
        backgroundSize: "100%",
        backgroundImage: "url('" + e._opt.background + "')"
      }));
      const _ = {
        position: "absolute",
        width: "100%",
        height: "100%",
        textAlign: "center",
        color: "#fff",
        display: "none",
        backgroundImage: "url('data:image/gif;base64,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')",
        backgroundRepeat: "no-repeat",
        backgroundPosition: "center",
        backgroundSize: "40px 40px"
      }, b = {
        position: "absolute",
        width: "100%",
        height: "100%",
        display: "none",
        background: "rgba(0,0,0,0.4)",
        backgroundImage: "url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADAAAAAwEAYAAAAHkiXEAAAABGdBTUEAALGPC/xhBQAAAAFzUkdCAK7OHOkAAAAgY0hSTQAAeiYAAICEAAD6AAAAgOgAAHUwAADqYAAAOpgAABdwnLpRPAAAAAZiS0dEAAAAAAAA+UO7fwAAAAlwSFlzAAAASAAAAEgARslrPgAAByBJREFUeNrlXFlIVV0U3vsaaINmZoX0YAR6y8oGMkKLoMESSjBoUJEoIogoIggigoryIQoKGqi3Roh6TKGBIkNEe6hMgzTNKLPSUlMrNdvrf/juurlP5zpc7znb+r+X755pn7W+Pe+9zpVimIEUKVKJiUIKKWRqKs5OmwZOTBQkSFBUFK5HR+tPt7WBOzpwX3U1jquqwGVleK6iQkoppSQy7a8xEBERLVwIPnsWXF9PrqCxEXzxInjpUrDH47YO0h2hw8JwtG4deN8+8OzZA0vl7Vt/iZZCCtnUhPPt7fp9o0fjvpgYHHu9uD8+Hsdsh52hggTV1uLg2DHwpUvSIz3S093ttE4hB5qSxYuRAc+f910im5vBFy6As7LALORQ7RgzBullZIBPngQ3NPRt1+vXeH7NGtN69u8oERFFRIDPnQMrZe8YZ0huLhwMDzdjb1gYC4zj4uKAeaFIkbpxAwfWvse48FOngp89s7eeS1p2Nlg63vQF7Y8iRWrlSthZXR2wZhAR0dy55gwlIqI5c8AfPtgbeuUKHIqKMi3soP3z1UzwiRP2NbqtDbxsmXuGacK3tOgG/fwJ3rbNtIDO+J2ZiQzp6ND97uzE+RUrHDaAmxprif/+HQasXm1aKKcBPxcsADc1/VEjFClS8+eH7oXcuSpSpJ480V/Y0wPOyjItjNtgofWmiPHuHa7Hxg79RUT0e1Rjxb/X1ASnDw9vf/3S9bl1K/iEFSlSixbZdz7Xr5t2fLgBuuTn2xfUjRsHmVBYGNg6gWpo+FtHNU4DuowYAZ3Ky+11GzOm/4SIiGjDBvuczM52zAHua4iI6OpVcGEheO1a8PCdP/j9CNRyKFKk9u4doBDWCRXXBOcE0GekgVBUhPuSk00LPTAdCwp0+3n0GBER4AFenbQiJ8cdg7dvpwGB5xunT4PHjTMtuL0/qan29q9fH+AB62jnyxe31moGlwFWNDbCzq1bcez+snLffr14odtrMzrCBet6/Pnz7hoabAZY8fgxT5iGRwbs36/b19kJHjnS49+BEkIIMXmy/vjt26YdCA4pKdgHKC2Fo5cvh2xiFBTu3NGPw8Ox/5CW5tG3/hi8VffokRmDQwUeNOTlwc/KSmRIbq67djx9Cm5p+W2akEKmpfnaSt5zZdTXY8+0udmQcg5h0iQwD3MfPgRPn+7UG6GjUjiqrNSver0eVIWEBP85EiSIN7H/dSxZAuY1roMHHRt02OqamOhrgnoN46SQQn76ZFoad8Hj8kOH4D/PZJOSQvYKW11jYnxNkHWK3NFhWhKz8HrB9+7xaCU06fYKIiBBgiIjfRlgHTf/j+NlNMTFgceOHXJSJEgQ9wXCVyOk9AlvLfEDWDT6X+DAAXSiHz8OOSkppJCRkfrJ9vYR+NHaql8wNV42jVevUFJ37kQ8kHX8PlRMmOD/SYIEtbZ69IAkvsATs38dP36ADx8GJyc7IzyD+xbhqxE1Nb4a8PKlfiE+HsOxyEgYZI1A+9tRUADetQtNTF2dU29CJ84Twhkz9KtVVb4+oKxMvxAWxjM101KFBvX1qNmbNkHwNWucFl4HT/QmTvSfIkGCSks9HC2MsxxzyTekp5uWLjh0dYHz88FeL2ry5ctm7LHq2NMD7rXUg6rC0cKM9+/BfQS1hghDXg1VpEjdvasvLpqHf3VWs/P+/QA3Lltm75jz8T7BZQAvn9tscJgWXpEiNWuWvd2bNwcQwONbnq6p0R8oLnYnA7Zs6Vvw7m7Yd/z4gDe5DQH2Xrum29/SwoObfh7cts1egFWrnDU4Lg785g2Ytx4LC2H4zJmmhe3XD5+dsJsD1xhHjgwwgfBwPFBXpydQXe3uFqXzfU9o7ZUSXFRkX/IHMcENGKXgixY27fBwA8TZudO+5dixY4gJ37xpyQVfvEtmpmnHTQMFMiUFevBeL6OkZMg1GQlER4P5wwTGt29g65bmvw/4HShanD+5mjIlxC+cNw/cKxqYw7RDHZY9TOEXXpEiVVurC8+jtJUrnTNAkSK1fDle2NWlG9DeDs7IMC2UM35zU2Mt8Urhel6eywalp+vCMzhM++hRDlo1LeCg/dNGNdy5Wtt4LvEuCv+HodqHCu/e2Y8Cyss5aNW0sAPzh8fx1uEkgyMGHWxqgjM8NhYGWoNSraMnvm6+89aXDHjmap1AMUpKcD9/+D2MAYNzcsD9fRDNsZMcwsedfehiPJFeUhJ4925wWVnfdvFHiDt2gEM/MXT+rwp47UMKKeT27Ti7Zw+YA6UCgbdKKyr8cTVSSCEbG3Ge/5yDwWtD48fjfv6rAl7C6LUeb4uvX8FnzuD5U6ewjP35s9M6uQaUJP4Qgz8E4SbJ2sk5BV5jevAAvHmzqS9/hs0XJxBi1CgOWtVjVnlHKSEB16Oj/wgoE0L8LsFcM169AldV8Q4UjouKULKtNch9/AdsEf6XQYgIsAAAACV0RVh0ZGF0ZTpjcmVhdGUAMjAyMS0wMS0xMlQxMTo1NjowNSswODowMGcMj/QAAAAldEVYdGRhdGU6bW9kaWZ5ADIwMjEtMDEtMTJUMTE6NTY6MDUrMDg6MDAWUTdIAAAASXRFWHRzdmc6YmFzZS11cmkAZmlsZTovLy9ob21lL2FkbWluL2ljb24tZm9udC90bXAvaWNvbl9wZHMzeWYxNGczYi9ib2Zhbmcuc3Zn11us5wAAAABJRU5ErkJggg==')",
        backgroundRepeat: "no-repeat",
        backgroundPosition: "center",
        backgroundSize: "48px 48px",
        cursor: "pointer"
      }, k = {position: "absolute", top: 0, height: "100%", display: "flex", alignItems: "center"}, T = {
        display: "none",
        position: "relative",
        fontSize: "13px",
        color: "#fff",
        lineHeight: "20px",
        marginLeft: "5px",
        marginRight: "5px",
        userSelect: "none"
      }, D = {
        display: "none",
        position: "relative",
        width: "16px",
        height: "16px",
        marginLeft: "8px",
        marginRight: "8px",
        backgroundRepeat: "no-repeat",
        backgroundPosition: "center",
        backgroundSize: "100%",
        cursor: "pointer"
      };
      R().setStyle(y, w), R().setStyle(n, {
        height: "38px",
        zIndex: 11,
        position: "absolute",
        left: 0,
        bottom: 0,
        width: "100%",
        background: "rgba(0,0,0)"
      }), R().setStyle(h, _), R().setStyle(c, b), R().setStyle(m, {
        position: "absolute",
        width: "100%",
        top: "60%",
        textAlign: "center"
      }), R().setStyle(o, Object.assign({}, k, {left: 0})), R().setStyle(a, Object.assign({}, k, {right: 0})), R().setStyle(i, T), R().setStyle(s, T), R().setStyle(u, Object.assign({}, D, {backgroundImage: "url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQEAYAAABPYyMiAAAABGdBTUEAALGPC/xhBQAAAAFzUkdCAK7OHOkAAAAgY0hSTQAAeiYAAICEAAD6AAAAgOgAAHUwAADqYAAAOpgAABdwnLpRPAAAAAZiS0dEAAAAAAAA+UO7fwAAAAlwSFlzAAAASAAAAEgARslrPgAAARVJREFUSMe9laEOglAUhs+5k9lJFpsJ5QWMJoNGbEY0mEy+gr6GNo0a3SiQCegMRILzGdw4hl+Cd27KxPuXb2zA/91z2YXoGRERkX4fvN3A2QxUiv4dFM3n8jZRBLbbVfd+ubJuF4xjiCyXkksueb1uSKCIZYGLBTEx8ekEoV7PkICeVgs8HiGyXoO2bUigCDM4HoPnM7bI8wwJ6Gk0sEXbLSay30Oo2TQkoGcwgFCSQMhxDAvoETEscDiQkJC4LjMz8+XyZ4HrFYWjEQqHQ1asWGWZfmdFAsVINxuw00HhbvfpydpvxWkKTqdYaRCUfUPJCdzv4Gr1uqfli0tOIAzByUT/iCrL6+84y3Bw+D6ui5Ou+jwA8FnIO++FACgAAAAldEVYdGRhdGU6Y3JlYXRlADIwMjEtMDEtMDhUMTY6NDI6NTMrMDg6MDCKP7wnAAAAJXRFWHRkYXRlOm1vZGlmeQAyMDIxLTAxLTA4VDE2OjQyOjUzKzA4OjAw+2IEmwAAAEl0RVh0c3ZnOmJhc2UtdXJpAGZpbGU6Ly8vaG9tZS9hZG1pbi9pY29uLWZvbnQvdG1wL2ljb25fZ2Y3MDBzN2IzZncvYm9mYW5nLnN2Z8fICi0AAAAASUVORK5CYII=')"})), R().setStyle(l, Object.assign({}, D, {backgroundImage: "url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQEAYAAABPYyMiAAAABGdBTUEAALGPC/xhBQAAAAFzUkdCAK7OHOkAAAAgY0hSTQAAeiYAAICEAAD6AAAAgOgAAHUwAADqYAAAOpgAABdwnLpRPAAAAAZiS0dEAAAAAAAA+UO7fwAAAAlwSFlzAAAASAAAAEgARslrPgAAAHVJREFUSMftkCESwCAMBEOnCtdXVMKHeC7oInkEeQJXkRoEZWraipxZc8lsQqQZBACAlIS1oqGhhTCdu3oyxyyMcdRf79c5J7SWDBky+z4173rbJvR+VF/e/qwKqIAKqMBDgZyFzAQCoZTpxq7HLDyOrw/9b07l3z4dDnI2IAAAACV0RVh0ZGF0ZTpjcmVhdGUAMjAyMS0wMS0wOFQxNjo0Mjo1MyswODowMIo/vCcAAAAldEVYdGRhdGU6bW9kaWZ5ADIwMjEtMDEtMDhUMTY6NDI6NTMrMDg6MDD7YgSbAAAASnRFWHRzdmc6YmFzZS11cmkAZmlsZTovLy9ob21lL2FkbWluL2ljb24tZm9udC90bXAvaWNvbl9nZjcwMHM3YjNmdy96YW50aW5nLnN2ZxqNZJkAAAAASUVORK5CYII=')"})), R().setStyle(d, Object.assign({}, D, {backgroundImage: "url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQEAYAAABPYyMiAAAABGdBTUEAALGPC/xhBQAAAAFzUkdCAK7OHOkAAAAgY0hSTQAAeiYAAICEAAD6AAAAgOgAAHUwAADqYAAAOpgAABdwnLpRPAAAAAZiS0dEAAAAAAAA+UO7fwAAAAlwSFlzAAAASAAAAEgARslrPgAAAaxJREFUSMfNlLFOAkEQhmevAZMjR6OGRBJKsFBzdkYNpYSaWkopIOFRCBWh1ieA+ALGRgutjK0HzV2H5SX7W/zsmY3cnTEhcZovOzcz9+/s7Ir8d4OGht7fBwAgjvEri2OTl1ffSf0xAMBxRIkS1e3Se3+vcszEMe/6OqmT/aN2m1wsNu/o5YVsNHI7BgA4PCRfXzfXCwKy1RLbcXZG9nrkzc12jvT8nPU/PtatOThgAx8fuS4WyZ0de2e+T87n5OcnuVqRsxl5cpImQDnKUc7DA1fVqpimZCu+vCSjiNH9PlmpJNTQ0INBErfeafZRAakC6FWKfH9nwU7H/l6rGdqCOx3y7c3U+aOARsMMp+1vNskwTLjulB23XJL1epqA9OshIiKeJxAIoug7UyA4OuLi6Ynr52deu+NjOy4MSc9Ln8rMDpTLybBpaOjdXbJUIqdTm8a/t2fn/RSQewR24HicTLmGhnbdzcPquvYtGY3+PIR24UKBUXd35v6Sk4lN47+9NXm/FBAEedfGTjw9JYdDm76fm6+hoS8ujGAxT6L9Im7bTKeurvIEb92+AES1b6x283XSAAAAJXRFWHRkYXRlOmNyZWF0ZQAyMDIxLTAxLTA4VDE2OjQyOjUzKzA4OjAwij+8JwAAACV0RVh0ZGF0ZTptb2RpZnkAMjAyMS0wMS0wOFQxNjo0Mjo1MyswODowMPtiBJsAAABJdEVYdHN2ZzpiYXNlLXVyaQBmaWxlOi8vL2hvbWUvYWRtaW4vaWNvbi1mb250L3RtcC9pY29uX2dmNzAwczdiM2Z3L2NhbWVyYS5zdmeyubWEAAAAAElFTkSuQmCC')"})), R().setStyle(f, Object.assign({}, D, {backgroundImage: "url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQEAYAAABPYyMiAAAABGdBTUEAALGPC/xhBQAAAAFzUkdCAK7OHOkAAAAgY0hSTQAAeiYAAICEAAD6AAAAgOgAAHUwAADqYAAAOpgAABdwnLpRPAAAAAZiS0dEAAAAAAAA+UO7fwAAAAlwSFlzAAAASAAAAEgARslrPgAAALZJREFUSMftVbsORUAQVSj8DomChvh3lU5CoSVCQq2RObeYu8XG3deVoHCak81kds7Oaz3vxRcAAMwztOg6vX9d6/3XFQQC+b7iAoFhYE7Tvx9EIFAcy/ftO3MQGAQkCfM4MmeZWyajiLnvmYuCeduMAuSzvRBVYNluFHCssSgFp7Sq9ALKkjnPf9ubRtkDL27HNT3QtsY9cAjsNAVheHIKBOwD2wpxFHDbJpwmaHH2L1iWx+2BDy8RbXXtqbRBAAAAJXRFWHRkYXRlOmNyZWF0ZQAyMDIxLTAxLTA4VDE2OjQyOjUzKzA4OjAwij+8JwAAACV0RVh0ZGF0ZTptb2RpZnkAMjAyMS0wMS0wOFQxNjo0Mjo1MyswODowMPtiBJsAAABTdEVYdHN2ZzpiYXNlLXVyaQBmaWxlOi8vL2hvbWUvYWRtaW4vaWNvbi1mb250L3RtcC9pY29uX2dmNzAwczdiM2Z3L3F1YW5waW5nenVpZGFodWEuc3ZnTBoI7AAAAABJRU5ErkJggg==')"})), R().setStyle(p, Object.assign({}, D, {backgroundImage: "url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQEAYAAABPYyMiAAAABGdBTUEAALGPC/xhBQAAAAFzUkdCAK7OHOkAAAAgY0hSTQAAeiYAAICEAAD6AAAAgOgAAHUwAADqYAAAOpgAABdwnLpRPAAAAAZiS0dEAAAAAAAA+UO7fwAAAAlwSFlzAAAASAAAAEgARslrPgAAAYJJREFUSMfdVbGKwkAQnQn+geAfWBixUTsVgp3YGKxSWflVNmIjARULwc5KO40ipNHWRgs/wGLniucKa+Jd5ODuuGle5u3szGRmd5bor4iIiMhuB3Sc+HXXBdp2/Lpta7v4dccRJUrUdhtNQIkSVa3C8HwG1uumg34f2OnEB+h0tF1Sv5b+YIsttpZLEhKSdhvscPi8IXFF74GJiYnHY7Cex8zMvFgkbInjmJnv98kqoO30vmhLtaRMB60WtEbDNDudgMUiKiQSzfjOMzFxoQAyCPSfw7/nQZ/PUYnpNGV6OR6BmYzJbzYIoBQCzGaRBDQvJCTdLnTLolg5HN5t6f8V1h/oUT4PrVKJWBotmEzQw+vV3J9Ow851P2/BaoX9Yfh0BrJZYKlk8uUyHOpDeLuBHwzMBJtN2PV6IPUhXK9Nf5cLMAxfluanrmGkRBggtRo03wfq66P/6CsJAnOg+f6rgfZI4BGYiYlHIx048eR6krcnq34kkj1GuVz8+jceo9+SD5A8yGh8CTq7AAAAJXRFWHRkYXRlOmNyZWF0ZQAyMDIxLTAxLTA4VDE2OjQyOjUzKzA4OjAwij+8JwAAACV0RVh0ZGF0ZTptb2RpZnkAMjAyMS0wMS0wOFQxNjo0Mjo1MyswODowMPtiBJsAAABNdEVYdHN2ZzpiYXNlLXVyaQBmaWxlOi8vL2hvbWUvYWRtaW4vaWNvbi1mb250L3RtcC9pY29uX2dmNzAwczdiM2Z3L3p1aXhpYW9odWEuc3ZnoCFr0AAAAABJRU5ErkJggg==')"})), R().setStyle(A, Object.assign({}, D, {backgroundImage: "url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQEAYAAABPYyMiAAAABGdBTUEAALGPC/xhBQAAAAFzUkdCAK7OHOkAAAAgY0hSTQAAeiYAAICEAAD6AAAAgOgAAHUwAADqYAAAOpgAABdwnLpRPAAAAAZiS0dEAAAAAAAA+UO7fwAAAAlwSFlzAAAASAAAAEgARslrPgAAAR9JREFUSMfVlD0LglAYhe9VkwgNihpsjbYQf4JTS7+iuaGxpcGfJjS0NFRLk2NDi6MogafhJGRIX9yEzvJwrx/nvPd9VYh/F3LkyBuN2g3J1QoAgCQhPe/Hxq5Lo+0WlfJ9dYYAgGaTDAIyy/BUnwcwWJlhcLnZkN2ugIBAuy2kkEL2ep8F73S4kjfFcfn6cMj9KLodrWVBiXyf75tMyOOR+4MBOZ8XLXzorboA5UpnM/J0Ivd7+vX7xX2asqGpVKtFXi5sqWmypXefrfIWAACmU/JwKCoun8hu9zA0uk6u13wgirg+n7+bAcsibbt6SB3n9TQXPxwAwHJJpum7M6BcDDQa0SgMaw9QPkJNIxcLMo4ZcDz+eYDqQFLWbqxKV57EtW1WtMbmAAAAJXRFWHRkYXRlOmNyZWF0ZQAyMDIxLTAxLTA4VDE2OjQyOjUzKzA4OjAwij+8JwAAACV0RVh0ZGF0ZTptb2RpZnkAMjAyMS0wMS0wOFQxNjo0Mjo1MyswODowMPtiBJsAAABKdEVYdHN2ZzpiYXNlLXVyaQBmaWxlOi8vL2hvbWUvYWRtaW4vaWNvbi1mb250L3RtcC9pY29uX2dmNzAwczdiM2Z3L2ppbmd5aW4uc3ZnIlMYaQAAAABJRU5ErkJggg==')"})), R().setStyle(g, Object.assign({}, D, {backgroundImage: "url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQEAYAAABPYyMiAAAABGdBTUEAALGPC/xhBQAAAAFzUkdCAK7OHOkAAAAgY0hSTQAAeiYAAICEAAD6AAAAgOgAAHUwAADqYAAAOpgAABdwnLpRPAAAAAZiS0dEAAAAAAAA+UO7fwAAAAlwSFlzAAAASAAAAEgARslrPgAAAU5JREFUSMftkzGKwlAURf9PULBQwULSCKK1bZAgNuoaFFyAC3AdZg0uQCwshWzAShEEO7Gy0soUCu9Occ3An5nMGCfdzGsO7+Xy3/03iVL/lbAAACiVIBCI77O37Vi9QCDZbEqLm03ycEBUAoHk818v7nYpul5Jz4tf8HBKYa1mcjwmbzd8rG8NFIsU7ffk8UjmcjE3XK+RtB4G2PT75GbDeblMttumfjSKMRCGLxsQCKTReE9KIJDJxDw/SmKxiOZWWh+ntrSlre2WXRAorbTSrZapip7X66kbMKtQUFBQCENznsmQ93vqBhh5r8fO85jAcsnIrcce1yV3uxgD8zl5uZgU+dGBVlrp6GbTKRPwffaDAek45Gz2/M0AAJ0OeTol+w0rFYrOZ3K1MhNJEjEAwHF4cBA8Z8B1zcXV6msv+JMR2yaHQ1LrXx/8Z+sNRxsWcwZeb6UAAAAldEVYdGRhdGU6Y3JlYXRlADIwMjEtMDEtMDhUMTY6NDI6NTMrMDg6MDCKP7wnAAAAJXRFWHRkYXRlOm1vZGlmeQAyMDIxLTAxLTA4VDE2OjQyOjUzKzA4OjAw+2IEmwAAAEt0RVh0c3ZnOmJhc2UtdXJpAGZpbGU6Ly8vaG9tZS9hZG1pbi9pY29uLWZvbnQvdG1wL2ljb25fZ2Y3MDBzN2IzZncvc2hlbmd5aW4uc3ZnFog1MQAAAABJRU5ErkJggg==')"})), R().setStyle(v, Object.assign({}, D, {backgroundImage: "url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQEAYAAABPYyMiAAAABGdBTUEAALGPC/xhBQAAAAFzUkdCAK7OHOkAAAAgY0hSTQAAeiYAAICEAAD6AAAAgOgAAHUwAADqYAAAOpgAABdwnLpRPAAAAAZiS0dEAAAAAAAA+UO7fwAAAAlwSFlzAAAASAAAAEgARslrPgAAAPRJREFUSMflVDEOwjAQO0e8gr2sZYVunREbD6ISfAgmkBjpC/hBEQ+AtTWD6QAI0gBlqRfLp+TiXC5n1nXgMUCS5HBoNBqj6IOMMFwuEpsNAABl6d3HihWrOJaBsuRPkGW+c929HAxuYefb6L+R0ZgkMrJYiItCnCT1sl5Y1jwXj0bNniJNJWqujfX7LyrwJh8AYDxWgulU0dPp20IFlxoODm61kpE4VnS9/puBXyPYgH7LbKY3PhwUnUw+NdC4CdW9+71UgyZspwIBB9No3O0klktxUahyx+Pz+lYG0Xzu84lXRqTqwRQAGAzns8R223gUdxZXGcAK5Hp0ClIAAAAldEVYdGRhdGU6Y3JlYXRlADIwMjEtMDEtMDhUMTY6NDI6NTMrMDg6MDCKP7wnAAAAJXRFWHRkYXRlOm1vZGlmeQAyMDIxLTAxLTA4VDE2OjQyOjUzKzA4OjAw+2IEmwAAAE50RVh0c3ZnOmJhc2UtdXJpAGZpbGU6Ly8vaG9tZS9hZG1pbi9pY29uLWZvbnQvdG1wL2ljb25fZ2Y3MDBzN2IzZncvbHV6aGlzaGlwaW4uc3Zn5Zd7GQAAAABJRU5ErkJggg==')"})), R().setStyle(E, Object.assign({}, D, {backgroundImage: "url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQEAYAAABPYyMiAAAABGdBTUEAALGPC/xhBQAAAAFzUkdCAK7OHOkAAAAgY0hSTQAAeiYAAICEAAD6AAAAgOgAAHUwAADqYAAAOpgAABdwnLpRPAAAAAZiS0dEAAAAAAAA+UO7fwAAAAlwSFlzAAAASAAAAEgARslrPgAAAahJREFUSMdjYBjpgBFd4NZK+f+soQYG//T+yzFuUFUl2cApjEWM/758UZvysPDn3127GBkZGBgY/v4l6ICb9xTWsRbp6/9f9W8N44Jz5xgCGI4wfGFiIttrR/5n/3/U3KyR8rj8t0RdHS5lcAv+//yXzzhZTY1ii2FAmsGZocna+maD3GnWY62tNzbJBbDOffLkxie5eJYwa2uYMhaigzb2/zyGguPH/y9mTGKYYGlJUIMiYxDjHCen/4oMDAxznJzg4k8Z/jP+l5LCCAFCQP30Y5dfXVZWDI7/zzIs8PNjNGJ4/7/r+XNKA4rkoNZ4/lj0V9TmzUxJv0J+F+jrM3YyvPq/acsWujmA2oBkB9y4LifLxhoa+teAzYFtwtWr/8sZxBj9fHxo7oCbprJ72MqOHWNgZGBkYFy1isGGoZahTFSU0hAgOhcQnfph4P7/df9T9u1jPMn4nyHmxIn/bAzLGe7GxTHsZyj+f+zpUwYGBmmG6bQsiMr+L/v/rqlJY9Njm9889fW4lGEUxXCHwAomUgH3vxBG8c+f1WWf9P98sns3oaJ4FAAAbtWqHTT84QYAAAAldEVYdGRhdGU6Y3JlYXRlADIwMjEtMDEtMDhUMTY6MzU6MjMrMDg6MDBLHbvEAAAAJXRFWHRkYXRlOm1vZGlmeQAyMDIxLTAxLTA4VDE2OjM1OjIzKzA4OjAwOkADeAAAAE50RVh0c3ZnOmJhc2UtdXJpAGZpbGU6Ly8vaG9tZS9hZG1pbi9pY29uLWZvbnQvdG1wL2ljb25fcTM1YTFhNHBtY2MvbHV6aGlzaGlwaW4uc3Zn6xlv1QAAAABJRU5ErkJggg==')"})), h.appendChild(m), e._opt.text && (o.appendChild(i), t.textDom = i), e._opt.showBandwidth && (o.appendChild(s), t.speedDom = s), e._opt.operateBtns.record && (a.appendChild(E), a.appendChild(v), t.recordingDom = E, t.recordDom = v), e._opt.operateBtns.screenshot && (a.appendChild(d), t.screenshotsDom = d), e._opt.operateBtns.play && (a.appendChild(u), a.appendChild(l), t.playDom = u, t.pauseDom = l), e._opt.operateBtns.audio && (a.appendChild(g), a.appendChild(A), t.playAudioDom = g, t.quietAudioDom = A), e._opt.operateBtns.fullscreen && (a.appendChild(f), a.appendChild(p), t.fullscreenDom = f, t.minScreenDom = p), n.appendChild(o), n.appendChild(a), r.appendChild(y), t.bgDom = y, r.appendChild(h), t.loadingDom = h, e._showControl() && r.appendChild(n), e._opt.operateBtns.play && (r.appendChild(c), t.playBigDom = c), e.$container.appendChild(r), e.$doms = t, e._removeContainerChild = () => {
        for (; e.$container.firstChild;) e.$container.removeChild(e.$container.firstChild)
      }, z().default(e), R().$hideBtns(e.$doms), e._opt.isNotMute || e._mute()
    }, t(W, "default", (function () {
      return Y
    }))), W
  }

  var V, Q, X = !1;

  function q() {
    return X || (X = !0, V = {}, v(), R(), Q = e => {
      const t = new Worker(e._opt.decoder);
      t.onmessage = r => {
        const n = r.data;
        switch (n.cmd) {
          case v().CMD_TYPE.init:
            e.setBufferTime(e._opt.videoBuffer), t.postMessage({
              cmd: v().POST_MESSAGE.init,
              opt: JSON.stringify(e._opt),
              sampleRate: e._audioContext.sampleRate
            }), e._hasLoaded || (e._hasLoaded = !0, e.onLoad(), e._trigger(v().EVEMTS.load));
            break;
          case v().CMD_TYPE.initSize:
            e.$canvasElement.width = n.w, e.$canvasElement.height = n.h, e.onInitSize(), e._resize(), e._trigger(v().EVEMTS.videoInfo, {
              w: n.w,
              h: n.h
            }), e._trigger(v().EVEMTS.start), e._supportOffscreen() && (e._bitmaprenderer = e.$canvasElement.getContext("bitmaprenderer"));
            break;
          case v().CMD_TYPE.render:
            e.loading && (e.loading = !1, e.playing = !0, e._clearCheckLoading()), e.playing && (e._supportOffscreen() ? e._bitmaprenderer.transferFromImageBitmap(n.buffer) : e._contextGLRender(e.$canvasElement.width, e.$canvasElement.height, n.output[0], n.output[1], n.output[2])), e._trigger(v().EVEMTS.timeUpdate, n.ts), e.onTimeUpdate(n.ts), e._updateStats({
              buf: n.delay,
              ts: n.ts
            }), e._checkHeart();
            break;
          case v().CMD_TYPE.playAudio:
            e.playing && !e.quieting && e._playAudio(n.buffer);
            break;
          case v().CMD_TYPE.print:
            e.onLog(n.text), e._trigger(v().EVEMTS.log, n.text);
            break;
          case v().CMD_TYPE.printErr:
            e.onLog(n.text), e._trigger(v().EVEMTS.log, n.text), e.onError(n.text), e._trigger(v().EVEMTS.error, n.text);
            break;
          case v().CMD_TYPE.initAudioPlanar:
            e._initAudioPlanar(n), e._trigger(v().EVEMTS.audioInfo, {
              numOfChannels: n.channels,
              sampleRate: n.samplerate
            });
            break;
          case v().CMD_TYPE.kBps:
            e.playing && (e.$doms.speedDom && (e.$doms.speedDom.innerText = R().bpsSize(n.kBps)), e._trigger(v().EVEMTS.kBps, n.kBps));
          default:
            e[n.cmd] && e[n.cmd](n)
        }
      }, e._decoderWorker = t
    }, t(V, "default", (function () {
      return Q
    }))), V
  }

  var Z, K, J = !1;

  function ee() {
    return J || (J = !0, Z = {}, R(), v(), K = e => {
      e._loading = !0, e._recording = !1, e._playing = !1, e._audioPlaying = !1, e._quieting = !1, e._fullscreen = !1, e._stats = {
        buf: 0,
        fps: 0,
        abps: "",
        vbps: "",
        ts: ""
      }, e._hasLoaded = !1, e._playUrl = "", e._startBpsTime = "", e._bps = 0, e._checkHeartTimeout = null, e._wakeLock = null, e._contextGL = null, e._contextGLRender = null, e._checkLoadingTimeout = null, e._bitmaprenderer = null, e._isPlayingBeforePageHidden = !1, e._initCheckVariable = () => {
        e._startBpsTime = "", e._bps = 0, e._clearCheckHeartTimeout(), e._clearCheckLoading()
      }, e._clearCheckHeartTimeout = () => {
        e._checkHeartTimeout && (clearTimeout(e._checkHeartTimeout), e._checkHeartTimeout = null)
      }, e._startCheckHeartTimeout = () => {
        e._checkHeartTimeout = setTimeout((function () {
          e._trigger(v().EVEMTS.timeout), e.recording = !1, e.playing = !1, e._close()
        }), 1e3 * e._opt.timeout)
      }, e._clearCheckLoading = () => {
        e._checkLoadingTimeout && (clearTimeout(e._checkLoadingTimeout), e._checkLoadingTimeout = null)
      }, e._checkLoading = () => {
        e._clearCheckLoading(), e._checkLoadingTimeout = setTimeout((() => {
          e._trigger(v().EVEMTS.timeout), e.playing = !1, e._close(), R().$domToggle(e.$doms.loadingDom, !1)
        }), 1e3 * e._opt.timeout)
      }
    }, t(Z, "default", (function () {
      return K
    }))), Z
  }

  var te, re, ne = !1;

  function oe() {
    return ne || (ne = !0, te = {}, R(), re = e => {
      e.onPlay = R().noop, e.onPause = R().noop, e.onRecord = R().noop, e.onFullscreen = R().noop, e.onMute = R().noop, e.onLoad = R().noop, e.onLog = R().noop, e.onError = R().noop, e.onTimeUpdate = R().noop, e.onInitSize = R().noop
    }, t(te, "default", (function () {
      return re
    }))), te
  }

  var ae, ie, se = !1;

  function ue() {
    return se || (se = !0, ie = e => {
      e._on = (t, r) => {
        let n, o, a;
        if (!r) return e;
        for (n = e.__events || (e.__events = {}), t = t.split(/\s+/); o = t.shift();) a = n[o] || (n[o] = []), a.push(r);
        return e
      }, e._off = () => {
        let t;
        return (t = e.__events) ? (delete e.__events, e) : e
      }, e._trigger = (t, ...r) => {
        function n(e, t) {
          if (e) for (let r = 0, n = e.length; r < n; r += 1) e[r](...t)
        }

        let o, a, i;
        if (!(o = e.__events)) return e;
        for (t = t.split(/\s+/); a = t.shift();) (i = o[a]) && (i = i.slice()), n(i, r);
        return e
      }
    }, t(ae = {}, "default", (function () {
      return ie
    }))), ae
  }

  var ce, le, de = !1;

  function fe() {
    return de || (de = !0, ce = {}, R(), v(), le = e => {
      e._pause = () => {
        e._close(), e.loading && R().$domToggle(e.$doms.loadingDom, !1), e.recording = !1, e.playing = !1
      }, e._play = t => {
        if (!e._playUrl && !t) return;
        let r = !1;
        t ? (e._playUrl && (e._close(), r = !0, e.clearView()), e.loading = !0, R().$domToggle(e.$doms.bgDom, !1), e._checkLoading(), e._playUrl = t) : e._playUrl && (e.loading ? (R().$hideBtns(e.$doms), R().$domToggle(e.$doms.fullscreenDom, !0), R().$domToggle(e.$doms.pauseDom, !0), R().$domToggle(e.$doms.loadingDom, !0), e._checkLoading()) : e.playing = !0), e._initCheckVariable(), r ? setTimeout((() => {
          e._decoderWorker.postMessage({cmd: v().POST_MESSAGE.play, url: e._playUrl})
        }), 300) : e._decoderWorker.postMessage({cmd: v().POST_MESSAGE.play, url: e._playUrl})
      }, e._screenshot = (t, r, n) => {
        t = t || R().now();
        const o = {png: "image/png", jpeg: "image/jpeg", webp: "image/webp"};
        let a = .92;
        void 0 !== n && (a = Number(n));
        const i = e.$canvasElement.toDataURL(o[r] || o.png, a);
        R().downloadImg(R().dataURLToFile(i), t)
      }, e._close = () => {
        e._close$2(), e._clearView()
      }, e._close$2 = () => {
        e._opt.debug && console.log("_close$2-START"), e._closeAudio && e._closeAudio(), e._audioPlayBuffers = [], e._audioPlaying = !1, e._decoderWorker.postMessage({cmd: v().POST_MESSAGE.close}), delete e._playAudio, e._releaseWakeLock(), e._initCheckVariable(), e._opt.debug && console.log("_close$2-END")
      }, e._releaseWakeLock = () => {
        e._wakeLock && (e._wakeLock.release(), e._wakeLock = null)
      }, e._clearView = () => {
        e._contextGL && e._contextGL.clear(e._contextGL.COLOR_BUFFER_BIT)
      }, e._resize = () => {
        const t = e.$container.clientWidth;
        let r = e.$container.clientHeight;
        e._showControl() && (r -= 38);
        const n = e.$canvasElement.width, o = e.$canvasElement.height, a = e._opt.rotate, i = t / n, s = r / o;
        let u = i > s ? s : i;
        e._opt.isResize || i !== s && (u = i + "," + s), e._opt.isFullResize && (u = i > s ? i : s);
        let c = "scale(" + u + ")";
        a && (c += " rotate(" + a + "deg)"), e.$canvasElement.style.transform = c, e.$canvasElement.style.left = (t - n) / 2 + "px", e.$canvasElement.style.top = (r - o) / 2 + "px"
      }, e._enableWakeLock = () => {
        e._opt.keepScreenOn && "wakeLock" in navigator && navigator.wakeLock.request("screen").then((t => {
          e._wakeLock = t
        }))
      }, e._supportOffscreen = () => !e._opt.forceNoOffscreen && "function" == typeof e.$canvasElement.transferControlToOffscreen, e._checkHeart = () => {
        e._clearCheckHeartTimeout(), e._startCheckHeartTimeout()
      }, e._updateStats = t => {
        t = t || {}, e._startBpsTime || (e._startBpsTime = R().now());
        const r = R().now();
        r - e._startBpsTime < 1e3 ? e._stats.fps += 1 : (e._stats.ts = t.ts, e._stats.buf = t.buf, e._trigger(v().EVEMTS.stats, e._stats), e._trigger(v().EVEMTS.performance, R().fpsStatus(e._stats.fps)), e._trigger(v().EVEMTS.buffer, R().bufferStatus(e._stats.buf, 1e3 * e._opt.videoBuffer)), e._stats.fps = 0, e._startBpsTime = r)
      }, e._onfullscreenchange = () => {
        (void 0).fullscreen = R().checkFull()
      }, e._handleVisibilityChange = () => {
        e._opt.debug && console.log(document.visibilityState, e._isPlayingBeforePageHidden), "visible" === document.visibilityState ? e._isPlayingBeforePageHidden && e._play() : (e._isPlayingBeforePageHidden = e.playing, e.playing && e._pause())
      }, e._handleWakeLock = () => {
        null !== e._wakeLock && "visible" === document.visibilityState && e._enableWakeLock()
      }
    }, t(ce, "default", (function () {
      return le
    }))), ce
  }

  var pe, he, me = !1;

  function Ae() {
    return me || (me = !0, pe = {}, ee(), oe(), ue(), fe(), he = e => {
      ee().default(e), oe().default(e), ue().default(e), fe().default(e)
    }, t(pe, "default", (function () {
      return he
    }))), pe
  }

  var ge, ve = !1;
  var Ee, ye, we, _e, be, ke, Te, De, Se, Ce, Oe, Pe, xe, Be, Me, Re, Le, Ie, Fe, Ue, Ne, je, Ge, ze, We, Ye, He, $e,
    Ve, Qe, Xe, qe, Ze, Ke, Je, et, tt, rt, nt, ot, at, it, st, ut, ct, lt, dt, ft, pt, ht, mt, At, gt, vt, Et, yt, wt,
    _t, bt, kt, Tt, Dt, St, Ct, Ot, Pt, xt, Bt, Mt, Rt, Lt, It, Ft, Ut, Nt, jt, Gt, zt, Wt, Yt, Ht, $t, Vt, Qt, Xt, qt,
    Zt, Kt, Jt, er, tr, rr, nr, or, ar, ir, sr, ur, cr, lr, dr, fr, pr, hr, mr, Ar, gr, vr, Er, yr, wr, _r, br, kr, Tr,
    Dr = !1;

  function Sr(e) {
    return Ee.locateFile ? Ee.locateFile(e, Oe) : Oe + e
  }

  function Cr(e) {
    Cr.shown || (Cr.shown = {}), Cr.shown[e] || (Cr.shown[e] = 1, Me(e))
  }

  function Or(e, t) {
    e || Yr("Assertion failed: " + t)
  }

  function Pr(e, t, r) {
    for (var n = t + r, o = ""; !(t >= n);) {
      var a = e[t++];
      if (!a) return o;
      if (128 & a) {
        var i = 63 & e[t++];
        if (192 != (224 & a)) {
          var s = 63 & e[t++];
          if ((a = 224 == (240 & a) ? (15 & a) << 12 | i << 6 | s : (7 & a) << 18 | i << 12 | s << 6 | 63 & e[t++]) < 65536) o += String.fromCharCode(a); else {
            var u = a - 65536;
            o += String.fromCharCode(55296 | u >> 10, 56320 | 1023 & u)
          }
        } else o += String.fromCharCode((31 & a) << 6 | i)
      } else o += String.fromCharCode(a)
    }
    return o
  }

  function xr(e, t) {
    return e ? Pr(We, e, t) : ""
  }

  function Br(e, t, r, n) {
    if (!(n > 0)) return 0;
    for (var o = r, a = r + n - 1, i = 0; i < e.length; ++i) {
      var s = e.charCodeAt(i);
      if (s >= 55296 && s <= 57343) s = 65536 + ((1023 & s) << 10) | 1023 & e.charCodeAt(++i);
      if (s <= 127) {
        if (r >= a) break;
        t[r++] = s
      } else if (s <= 2047) {
        if (r + 1 >= a) break;
        t[r++] = 192 | s >> 6, t[r++] = 128 | 63 & s
      } else if (s <= 65535) {
        if (r + 2 >= a) break;
        t[r++] = 224 | s >> 12, t[r++] = 128 | s >> 6 & 63, t[r++] = 128 | 63 & s
      } else {
        if (r + 3 >= a) break;
        t[r++] = 240 | s >> 18, t[r++] = 128 | s >> 12 & 63, t[r++] = 128 | s >> 6 & 63, t[r++] = 128 | 63 & s
      }
    }
    return t[r] = 0, r - o
  }

  function Mr(e, t, r) {
    return Br(e, We, t, r)
  }

  function Rr(e) {
    for (var t = 0, r = 0; r < e.length; ++r) {
      var n = e.charCodeAt(r);
      n >= 55296 && n <= 57343 && (n = 65536 + ((1023 & n) << 10) | 1023 & e.charCodeAt(++r)), n <= 127 ? ++t : t += n <= 2047 ? 2 : n <= 65535 ? 3 : 4
    }
    return t
  }

  function Lr(e, t) {
    for (var r = "", n = 0; !(n >= t / 2); ++n) {
      var o = Ye[e + 2 * n >> 1];
      if (0 == o) break;
      r += String.fromCharCode(o)
    }
    return r
  }

  function Ir(e, t, r) {
    if (void 0 === r && (r = 2147483647), r < 2) return 0;
    for (var n = t, o = (r -= 2) < 2 * e.length ? r / 2 : e.length, a = 0; a < o; ++a) {
      var i = e.charCodeAt(a);
      Ye[t >> 1] = i, t += 2
    }
    return Ye[t >> 1] = 0, t - n
  }

  function Fr(e) {
    return 2 * e.length
  }

  function Ur(e, t) {
    for (var r = 0, n = ""; !(r >= t / 4);) {
      var o = $e[e + 4 * r >> 2];
      if (0 == o) break;
      if (++r, o >= 65536) {
        var a = o - 65536;
        n += String.fromCharCode(55296 | a >> 10, 56320 | 1023 & a)
      } else n += String.fromCharCode(o)
    }
    return n
  }

  function Nr(e, t, r) {
    if (void 0 === r && (r = 2147483647), r < 4) return 0;
    for (var n = t, o = n + r - 4, a = 0; a < e.length; ++a) {
      var i = e.charCodeAt(a);
      if (i >= 55296 && i <= 57343) i = 65536 + ((1023 & i) << 10) | 1023 & e.charCodeAt(++a);
      if ($e[t >> 2] = i, (t += 4) + 4 > o) break
    }
    return $e[t >> 2] = 0, t - n
  }

  function jr(e) {
    for (var t = 0, r = 0; r < e.length; ++r) {
      var n = e.charCodeAt(r);
      n >= 55296 && n <= 57343 && ++r, t += 4
    }
    return t
  }

  function Gr(e) {
    var t = Rr(e) + 1, r = Jt(t);
    return r && Br(e, ze, r, t), r
  }

  function zr(e) {
    Or(!Se, "addRunDependency cannot be used in a pthread worker"), nt++, Ee.monitorRunDependencies && Ee.monitorRunDependencies(nt)
  }

  function Wr(e) {
    if (nt--, Ee.monitorRunDependencies && Ee.monitorRunDependencies(nt), 0 == nt && (null !== ot && (clearInterval(ot), ot = null), at)) {
      var t = at;
      at = null, t()
    }
  }

  function Yr(e) {
    throw Ee.onAbort && Ee.onAbort(e), Se && console.error("Pthread aborting at " + (new Error).stack), Me(e += ""), je = !0, 1, e = "abort(" + e + "). Build with -s ASSERTIONS=1 for more info.", new WebAssembly.RuntimeError(e)
  }

  function Hr(e) {
    return t = e, r = it, String.prototype.startsWith ? t.startsWith(r) : 0 === t.indexOf(r);
    var t, r
  }

  function $r(e) {
    try {
      if (e == st && Ie) return new Uint8Array(Ie);
      if (xe) return xe(e);
      throw"both async and sync fetching of the wasm failed"
    } catch (e) {
      Yr(e)
    }
  }

  function Vr() {
    var e = {a: qt};

    function t(e, t) {
      var r = e.exports;
      Ee.asm = r, Ze = Ee.asm.da, Ne = t, Se || Wr()
    }

    function r(e) {
      t(e.instance, e.module)
    }

    function n(t) {
      return (Ie || !Te && !De || "function" != typeof fetch ? Promise.resolve().then((function () {
        return $r(st)
      })) : fetch(st, {credentials: "same-origin"}).then((function (e) {
        if (!e.ok) throw"failed to load wasm binary file at '" + st + "'";
        return e.arrayBuffer()
      })).catch((function () {
        return $r(st)
      }))).then((function (t) {
        return WebAssembly.instantiate(t, e)
      })).then(t, (function (e) {
        Me("failed to asynchronously prepare wasm: " + e), Yr(e)
      }))
    }

    if (Se || zr(), Ee.instantiateWasm) try {
      return Ee.instantiateWasm(e, t)
    } catch (e) {
      return Me("Module.instantiateWasm callback failed with error: " + e), !1
    }
    return Ie || "function" != typeof WebAssembly.instantiateStreaming || Hr(st) || "function" != typeof fetch ? n(r) : fetch(st, {credentials: "same-origin"}).then((function (t) {
      return WebAssembly.instantiateStreaming(t, e).then(r, (function (e) {
        return Me("wasm streaming compile failed: " + e), Me("falling back to ArrayBuffer instantiation"), n(r)
      }))
    })), {}
  }

  function Qr() {
    ft.initRuntime()
  }

  function Xr(e) {
    for (; e.length > 0;) {
      var t = e.shift();
      if ("function" != typeof t) {
        var r = t.func;
        "number" == typeof r ? void 0 === t.arg ? Ze.get(r)() : Ze.get(r)(t.arg) : r(void 0 === t.arg ? null : t.arg)
      } else t(Ee)
    }
  }

  function qr(e, t) {
    if (e <= 0 || e > ze.length || !0 & e || t < 0) return -28;
    if (0 == t) return 0;
    t >= 2147483647 && (t = 1 / 0);
    var r = Atomics.load($e, br >> 2), n = 0;
    if (r == e && (Atomics.compareExchange($e, br >> 2, r, 0) == r && (n = 1, --t <= 0))) return 1;
    var o = Atomics.notify($e, e >> 2, t);
    if (o >= 0) return o + n;
    throw"Atomics.notify returned an unexpected value " + o
  }

  function Zr(e) {
    if (Se) throw"Internal Error! cleanupThread() can only ever be called from main application thread!";
    if (!e) throw"Internal Error! Null pthread_ptr in cleanupThread!";
    $e[e + 12 >> 2] = 0;
    var t = ft.pthreads[e];
    if (t) {
      var r = t.worker;
      ft.returnWorkerToPool(r)
    }
  }

  function Kr(e, t) {
    yr(e, t), vr(e)
  }

  function Jr() {
    return Fe
  }

  function en(e, t) {
    return Ze.get(e)(t)
  }

  function tn() {
    var e = new Error;
    if (!e.stack) {
      try {
        throw new Error
      } catch (t) {
        e = t
      }
      if (!e.stack) return "(no stack trace available)"
    }
    return e.stack.toString()
  }

  function rn(e, t, r, n) {
    Yr("Assertion failed: " + xr(e) + ", at: " + [t ? xr(t) : "unknown filename", r, n ? xr(n) : "unknown function"])
  }

  function nn(e) {
    return $e[tr() >> 2] = e, e
  }

  function on(e) {
    for (var t = function (e, t) {
      return t || (t = Re), Math.ceil(e / t) * t
    }(e, 16384), r = Jt(t); e < t;) ze[r + e++] = 0;
    return r
  }

  function an(e, t, r) {
    if (Se) return zo(1, 1, e, t, r);
    Et.varargs = r;
    try {
      var n = Et.getStreamFromFD(e);
      switch (t) {
        case 0:
          return (o = Et.get()) < 0 ? -28 : vt.open(n.path, n.flags, 0, o).fd;
        case 1:
        case 2:
          return 0;
        case 3:
          return n.flags;
        case 4:
          var o = Et.get();
          return n.flags |= o, 0;
        case 12:
          o = Et.get();
          return Ye[o + 0 >> 1] = 2, 0;
        case 13:
        case 14:
          return 0;
        case 16:
        case 8:
          return -28;
        case 9:
          return nn(28), -1;
        default:
          return -28
      }
    } catch (e) {
      return void 0 !== vt && e instanceof vt.ErrnoError || Yr(e), -e.errno
    }
  }

  function sn(e, t, r) {
    if (Se) return zo(2, 1, e, t, r);
    Et.varargs = r;
    try {
      var n = Et.getStr(e), o = r ? Et.get() : 0;
      return vt.open(n, t, o).fd
    } catch (e) {
      return void 0 !== vt && e instanceof vt.ErrnoError || Yr(e), -e.errno
    }
  }

  function un(e) {
    switch (e) {
      case 1:
        return 0;
      case 2:
        return 1;
      case 4:
        return 2;
      case 8:
        return 3;
      default:
        throw new TypeError("Unknown type size: " + e)
    }
  }

  function cn(e) {
    for (var t = "", r = e; We[r];) t += yt[We[r++]];
    return t
  }

  function ln(e) {
    if (void 0 === e) return "_unknown";
    var t = (e = e.replace(/[^a-zA-Z0-9_]/g, "$")).charCodeAt(0);
    return t >= kt && t <= Tt ? "_" + e : e
  }

  function dn(e, t) {
    return e = ln(e), new Function("body", "return function " + e + '() {\n    "use strict";    return body.apply(this, arguments);\n};\n')(t)
  }

  function fn(e, t) {
    var r = dn(t, (function (e) {
      this.name = t, this.message = e;
      var r = new Error(e).stack;
      void 0 !== r && (this.stack = this.toString() + "\n" + r.replace(/^Error(:[^\n]*)?\n/, ""))
    }));
    return r.prototype = Object.create(e.prototype), r.prototype.constructor = r, r.prototype.toString = function () {
      return void 0 === this.message ? this.name : this.name + ": " + this.message
    }, r
  }

  function pn(e) {
    throw new Dt(e)
  }

  function hn(e) {
    throw new St(e)
  }

  function mn(e, t, r) {
    function n(t) {
      var n = r(t);
      n.length !== e.length && hn("Mismatched type converter count");
      for (var o = 0; o < e.length; ++o) An(e[o], n[o])
    }

    e.forEach((function (e) {
      bt[e] = t
    }));
    var o = new Array(t.length), a = [], i = 0;
    t.forEach((function (e, t) {
      _t.hasOwnProperty(e) ? o[t] = _t[e] : (a.push(e), wt.hasOwnProperty(e) || (wt[e] = []), wt[e].push((function () {
        o[t] = _t[e], ++i === a.length && n(o)
      })))
    })), 0 === a.length && n(o)
  }

  function An(e, t, r) {
    if (r = r || {}, !("argPackAdvance" in t)) throw new TypeError("registerType registeredInstance requires argPackAdvance");
    var n = t.name;
    if (e || pn('type "' + n + '" must have a positive integer typeid pointer'), _t.hasOwnProperty(e)) {
      if (r.ignoreDuplicateRegistrations) return;
      pn("Cannot register type '" + n + "' twice")
    }
    if (_t[e] = t, delete bt[e], wt.hasOwnProperty(e)) {
      var o = wt[e];
      delete wt[e], o.forEach((function (e) {
        e()
      }))
    }
  }

  function gn(e, t, r, n, o) {
    var a = un(r);
    An(e, {
      name: t = cn(t), fromWireType: function (e) {
        return !!e
      }, toWireType: function (e, t) {
        return t ? n : o
      }, argPackAdvance: 8, readValueFromPointer: function (e) {
        var n;
        if (1 === r) n = ze; else if (2 === r) n = Ye; else {
          if (4 !== r) throw new TypeError("Unknown boolean type size: " + t);
          n = $e
        }
        return this.fromWireType(n[e >> a])
      }, destructorFunction: null
    })
  }

  function vn(e) {
    if (!(this instanceof Cn)) return !1;
    if (!(e instanceof Cn)) return !1;
    for (var t = this.$$.ptrType.registeredClass, r = this.$$.ptr, n = e.$$.ptrType.registeredClass, o = e.$$.ptr; t.baseClass;) r = t.upcast(r), t = t.baseClass;
    for (; n.baseClass;) o = n.upcast(o), n = n.baseClass;
    return t === n && r === o
  }

  function En(e) {
    pn(e.$$.ptrType.registeredClass.name + " instance already deleted")
  }

  function yn(e) {
  }

  function wn(e) {
    e.count.value -= 1, 0 === e.count.value && function (e) {
      e.smartPtr ? e.smartPtrType.rawDestructor(e.smartPtr) : e.ptrType.registeredClass.rawDestructor(e.ptr)
    }(e)
  }

  function _n(e) {
    return "undefined" == typeof FinalizationGroup ? (_n = function (e) {
      return e
    }, e) : (Ct = new FinalizationGroup((function (e) {
      for (var t = e.next(); !t.done; t = e.next()) {
        var r = t.value;
        r.ptr ? wn(r) : console.warn("object already deleted: " + r.ptr)
      }
    })), yn = function (e) {
      Ct.unregister(e.$$)
    }, (_n = function (e) {
      return Ct.register(e, e.$$, e.$$), e
    })(e))
  }

  function bn() {
    if (this.$$.ptr || En(this), this.$$.preservePointerOnDelete) return this.$$.count.value += 1, this;
    var e, t = _n(Object.create(Object.getPrototypeOf(this), {
      $$: {
        value: (e = this.$$, {
          count: e.count,
          deleteScheduled: e.deleteScheduled,
          preservePointerOnDelete: e.preservePointerOnDelete,
          ptr: e.ptr,
          ptrType: e.ptrType,
          smartPtr: e.smartPtr,
          smartPtrType: e.smartPtrType
        })
      }
    }));
    return t.$$.count.value += 1, t.$$.deleteScheduled = !1, t
  }

  function kn() {
    this.$$.ptr || En(this), this.$$.deleteScheduled && !this.$$.preservePointerOnDelete && pn("Object already scheduled for deletion"), yn(this), wn(this.$$), this.$$.preservePointerOnDelete || (this.$$.smartPtr = void 0, this.$$.ptr = void 0)
  }

  function Tn() {
    return !this.$$.ptr
  }

  function Dn() {
    for (; Pt.length;) {
      var e = Pt.pop();
      e.$$.deleteScheduled = !1, e.delete()
    }
  }

  function Sn() {
    return this.$$.ptr || En(this), this.$$.deleteScheduled && !this.$$.preservePointerOnDelete && pn("Object already scheduled for deletion"), Pt.push(this), 1 === Pt.length && Ot && Ot(Dn), this.$$.deleteScheduled = !0, this
  }

  function Cn() {
  }

  function On(e, t, r) {
    if (void 0 === e[t].overloadTable) {
      var n = e[t];
      e[t] = function () {
        return e[t].overloadTable.hasOwnProperty(arguments.length) || pn("Function '" + r + "' called with an invalid number of arguments (" + arguments.length + ") - expects one of (" + e[t].overloadTable + ")!"), e[t].overloadTable[arguments.length].apply(this, arguments)
      }, e[t].overloadTable = [], e[t].overloadTable[n.argCount] = n
    }
  }

  function Pn(e, t, r, n, o, a, i, s) {
    this.name = e, this.constructor = t, this.instancePrototype = r, this.rawDestructor = n, this.baseClass = o, this.getActualType = a, this.upcast = i, this.downcast = s, this.pureVirtualFunctions = []
  }

  function xn(e, t, r) {
    for (; t !== r;) t.upcast || pn("Expected null or instance of " + r.name + ", got an instance of " + t.name), e = t.upcast(e), t = t.baseClass;
    return e
  }

  function Bn(e, t) {
    if (null === t) return this.isReference && pn("null is not a valid " + this.name), 0;
    t.$$ || pn('Cannot pass "' + lo(t) + '" as a ' + this.name), t.$$.ptr || pn("Cannot pass deleted object as a pointer of type " + this.name);
    var r = t.$$.ptrType.registeredClass;
    return xn(t.$$.ptr, r, this.registeredClass)
  }

  function Mn(e, t) {
    var r;
    if (null === t) return this.isReference && pn("null is not a valid " + this.name), this.isSmartPointer ? (r = this.rawConstructor(), null !== e && e.push(this.rawDestructor, r), r) : 0;
    t.$$ || pn('Cannot pass "' + lo(t) + '" as a ' + this.name), t.$$.ptr || pn("Cannot pass deleted object as a pointer of type " + this.name), !this.isConst && t.$$.ptrType.isConst && pn("Cannot convert argument of type " + (t.$$.smartPtrType ? t.$$.smartPtrType.name : t.$$.ptrType.name) + " to parameter type " + this.name);
    var n = t.$$.ptrType.registeredClass;
    if (r = xn(t.$$.ptr, n, this.registeredClass), this.isSmartPointer) switch (void 0 === t.$$.smartPtr && pn("Passing raw pointer to smart pointer is illegal"), this.sharingPolicy) {
      case 0:
        t.$$.smartPtrType === this ? r = t.$$.smartPtr : pn("Cannot convert argument of type " + (t.$$.smartPtrType ? t.$$.smartPtrType.name : t.$$.ptrType.name) + " to parameter type " + this.name);
        break;
      case 1:
        r = t.$$.smartPtr;
        break;
      case 2:
        if (t.$$.smartPtrType === this) r = t.$$.smartPtr; else {
          var o = t.clone();
          r = this.rawShare(r, uo((function () {
            o.delete()
          }))), null !== e && e.push(this.rawDestructor, r)
        }
        break;
      default:
        pn("Unsupporting sharing policy")
    }
    return r
  }

  function Rn(e, t) {
    if (null === t) return this.isReference && pn("null is not a valid " + this.name), 0;
    t.$$ || pn('Cannot pass "' + lo(t) + '" as a ' + this.name), t.$$.ptr || pn("Cannot pass deleted object as a pointer of type " + this.name), t.$$.ptrType.isConst && pn("Cannot convert argument of type " + t.$$.ptrType.name + " to parameter type " + this.name);
    var r = t.$$.ptrType.registeredClass;
    return xn(t.$$.ptr, r, this.registeredClass)
  }

  function Ln(e) {
    return this.fromWireType(Ve[e >> 2])
  }

  function In(e) {
    return this.rawGetPointee && (e = this.rawGetPointee(e)), e
  }

  function Fn(e) {
    this.rawDestructor && this.rawDestructor(e)
  }

  function Un(e) {
    null !== e && e.delete()
  }

  function Nn(e, t, r) {
    if (t === r) return e;
    if (void 0 === r.baseClass) return null;
    var n = Nn(e, t, r.baseClass);
    return null === n ? null : r.downcast(n)
  }

  function jn() {
    return Object.keys(Bt).length
  }

  function Gn() {
    var e = [];
    for (var t in Bt) Bt.hasOwnProperty(t) && e.push(Bt[t]);
    return e
  }

  function zn(e) {
    Ot = e, Pt.length && Ot && Ot(Dn)
  }

  function Wn(e, t) {
    return t = function (e, t) {
      for (void 0 === t && pn("ptr should not be undefined"); e.baseClass;) t = e.upcast(t), e = e.baseClass;
      return t
    }(e, t), Bt[t]
  }

  function Yn(e, t) {
    return t.ptrType && t.ptr || hn("makeClassHandle requires ptr and ptrType"), !!t.smartPtrType !== !!t.smartPtr && hn("Both smartPtrType and smartPtr must be specified"), t.count = {value: 1}, _n(Object.create(e, {$$: {value: t}}))
  }

  function Hn(e) {
    var t = this.getPointee(e);
    if (!t) return this.destructor(e), null;
    var r = Wn(this.registeredClass, t);
    if (void 0 !== r) {
      if (0 === r.$$.count.value) return r.$$.ptr = t, r.$$.smartPtr = e, r.clone();
      var n = r.clone();
      return this.destructor(e), n
    }

    function o() {
      return this.isSmartPointer ? Yn(this.registeredClass.instancePrototype, {
        ptrType: this.pointeeType,
        ptr: t,
        smartPtrType: this,
        smartPtr: e
      }) : Yn(this.registeredClass.instancePrototype, {ptrType: this, ptr: e})
    }

    var a, i = this.registeredClass.getActualType(t), s = xt[i];
    if (!s) return o.call(this);
    a = this.isConst ? s.constPointerType : s.pointerType;
    var u = Nn(t, this.registeredClass, a.registeredClass);
    return null === u ? o.call(this) : this.isSmartPointer ? Yn(a.registeredClass.instancePrototype, {
      ptrType: a,
      ptr: u,
      smartPtrType: this,
      smartPtr: e
    }) : Yn(a.registeredClass.instancePrototype, {ptrType: a, ptr: u})
  }

  function $n(e, t, r, n, o, a, i, s, u, c, l) {
    this.name = e, this.registeredClass = t, this.isReference = r, this.isConst = n, this.isSmartPointer = o, this.pointeeType = a, this.sharingPolicy = i, this.rawGetPointee = s, this.rawConstructor = u, this.rawShare = c, this.rawDestructor = l, o || void 0 !== t.baseClass ? this.toWireType = Mn : n ? (this.toWireType = Bn, this.destructorFunction = null) : (this.toWireType = Rn, this.destructorFunction = null)
  }

  function Vn(e, t, r) {
    return -1 != e.indexOf("j") ? function (e, t, r) {
      var n = Ee["dynCall_" + e];
      return r && r.length ? n.apply(null, [t].concat(r)) : n.call(null, t)
    }(e, t, r) : Ze.get(t).apply(null, r)
  }

  function Qn(e, t) {
    var r, n, o, a = -1 != (e = cn(e)).indexOf("j") ? (r = e, n = t, o = [], function () {
      o.length = arguments.length;
      for (var e = 0; e < arguments.length; e++) o[e] = arguments[e];
      return Vn(r, n, o)
    }) : Ze.get(t);
    return "function" != typeof a && pn("unknown function pointer with signature " + e + ": " + t), a
  }

  function Xn(e) {
    var t = rr(e), r = cn(t);
    return Kt(t), r
  }

  function qn(e, t) {
    var r = [], n = {};
    throw t.forEach((function e(t) {
      n[t] || _t[t] || (bt[t] ? bt[t].forEach(e) : (r.push(t), n[t] = !0))
    })), new Mt(e + ": " + r.map(Xn).join([", "]))
  }

  function Zn(e, t, r, n, o, a, i, s, u, c, l, d, f) {
    l = cn(l), a = Qn(o, a), s && (s = Qn(i, s)), c && (c = Qn(u, c)), f = Qn(d, f);
    var p = ln(l);
    !function (e, t, r) {
      Ee.hasOwnProperty(e) ? ((void 0 === r || void 0 !== Ee[e].overloadTable && void 0 !== Ee[e].overloadTable[r]) && pn("Cannot register public name '" + e + "' twice"), On(Ee, e, e), Ee.hasOwnProperty(r) && pn("Cannot register multiple overloads of a function with the same number of arguments (" + r + ")!"), Ee[e].overloadTable[r] = t) : (Ee[e] = t, void 0 !== r && (Ee[e].numArguments = r))
    }(p, (function () {
      qn("Cannot construct " + l + " due to unbound types", [n])
    })), mn([e, t, r], n ? [n] : [], (function (t) {
      var r, o;
      t = t[0], o = n ? (r = t.registeredClass).instancePrototype : Cn.prototype;
      var i = dn(p, (function () {
        if (Object.getPrototypeOf(this) !== u) throw new Dt("Use 'new' to construct " + l);
        if (void 0 === d.constructor_body) throw new Dt(l + " has no accessible constructor");
        var e = d.constructor_body[arguments.length];
        if (void 0 === e) throw new Dt("Tried to invoke ctor of " + l + " with invalid number of parameters (" + arguments.length + ") - expected (" + Object.keys(d.constructor_body).toString() + ") parameters instead!");
        return e.apply(this, arguments)
      })), u = Object.create(o, {constructor: {value: i}});
      i.prototype = u;
      var d = new Pn(l, i, u, f, r, a, s, c), h = new $n(l, d, !0, !1, !1), m = new $n(l + "*", d, !1, !1, !1),
        A = new $n(l + " const*", d, !1, !0, !1);
      return xt[e] = {pointerType: m, constPointerType: A}, function (e, t, r) {
        Ee.hasOwnProperty(e) || hn("Replacing nonexistant public symbol"), void 0 !== Ee[e].overloadTable && void 0 !== r ? Ee[e].overloadTable[r] = t : (Ee[e] = t, Ee[e].argCount = r)
      }(p, i), [h, m, A]
    }))
  }

  function Kn(e, t) {
    for (var r = [], n = 0; n < e; n++) r.push($e[(t >> 2) + n]);
    return r
  }

  function Jn(e) {
    for (; e.length;) {
      var t = e.pop();
      e.pop()(t)
    }
  }

  function eo(e, t, r, n, o, a) {
    Or(t > 0);
    var i = Kn(t, r);
    o = Qn(n, o);
    var s = [a], u = [];
    mn([], [e], (function (e) {
      var r = "constructor " + (e = e[0]).name;
      if (void 0 === e.registeredClass.constructor_body && (e.registeredClass.constructor_body = []), void 0 !== e.registeredClass.constructor_body[t - 1]) throw new Dt("Cannot register multiple constructors with identical number of parameters (" + (t - 1) + ") for class '" + e.name + "'! Overload resolution is currently only performed using the parameter count, not actual type info!");
      return e.registeredClass.constructor_body[t - 1] = function () {
        qn("Cannot construct " + e.name + " due to unbound types", i)
      }, mn([], i, (function (n) {
        return e.registeredClass.constructor_body[t - 1] = function () {
          arguments.length !== t - 1 && pn(r + " called with " + arguments.length + " arguments, expected " + (t - 1)), u.length = 0, s.length = t;
          for (var e = 1; e < t; ++e) s[e] = n[e].toWireType(u, arguments[e - 1]);
          var a = o.apply(null, s);
          return Jn(u), n[0].fromWireType(a)
        }, []
      })), []
    }))
  }

  function to(e, t) {
    if (!(e instanceof Function)) throw new TypeError("new_ called with constructor type " + typeof e + " which is not a function");
    var r = dn(e.name || "unknownFunctionName", (function () {
    }));
    r.prototype = e.prototype;
    var n = new r, o = e.apply(n, t);
    return o instanceof Object ? o : n
  }

  function ro(e, t, r, n, o, a, i, s) {
    var u = Kn(r, n);
    t = cn(t), a = Qn(o, a), mn([], [e], (function (e) {
      var n = (e = e[0]).name + "." + t;

      function o() {
        qn("Cannot call " + n + " due to unbound types", u)
      }

      s && e.registeredClass.pureVirtualFunctions.push(t);
      var c = e.registeredClass.instancePrototype, l = c[t];
      return void 0 === l || void 0 === l.overloadTable && l.className !== e.name && l.argCount === r - 2 ? (o.argCount = r - 2, o.className = e.name, c[t] = o) : (On(c, t, n), c[t].overloadTable[r - 2] = o), mn([], u, (function (o) {
        var s = function (e, t, r, n, o) {
          var a = t.length;
          a < 2 && pn("argTypes array size mismatch! Must at least get return value and 'this' types!");
          for (var i = null !== t[1] && null !== r, s = !1, u = 1; u < t.length; ++u) if (null !== t[u] && void 0 === t[u].destructorFunction) {
            s = !0;
            break
          }
          var c = "void" !== t[0].name, l = "", d = "";
          for (u = 0; u < a - 2; ++u) l += (0 !== u ? ", " : "") + "arg" + u, d += (0 !== u ? ", " : "") + "arg" + u + "Wired";
          var f = "return function " + ln(e) + "(" + l + ") {\nif (arguments.length !== " + (a - 2) + ") {\nthrowBindingError('function " + e + " called with ' + arguments.length + ' arguments, expected " + (a - 2) + " args!');\n}\n";
          s && (f += "var destructors = [];\n");
          var p = s ? "destructors" : "null",
            h = ["throwBindingError", "invoker", "fn", "runDestructors", "retType", "classParam"],
            m = [pn, n, o, Jn, t[0], t[1]];
          for (i && (f += "var thisWired = classParam.toWireType(" + p + ", this);\n"), u = 0; u < a - 2; ++u) f += "var arg" + u + "Wired = argType" + u + ".toWireType(" + p + ", arg" + u + "); // " + t[u + 2].name + "\n", h.push("argType" + u), m.push(t[u + 2]);
          if (i && (d = "thisWired" + (d.length > 0 ? ", " : "") + d), f += (c ? "var rv = " : "") + "invoker(fn" + (d.length > 0 ? ", " : "") + d + ");\n", s) f += "runDestructors(destructors);\n"; else for (u = i ? 1 : 2; u < t.length; ++u) {
            var A = 1 === u ? "thisWired" : "arg" + (u - 2) + "Wired";
            null !== t[u].destructorFunction && (f += A + "_dtor(" + A + "); // " + t[u].name + "\n", h.push(A + "_dtor"), m.push(t[u].destructorFunction))
          }
          return c && (f += "var ret = retType.fromWireType(rv);\nreturn ret;\n"), f += "}\n", h.push(f), to(Function, h).apply(null, m)
        }(n, o, e, a, i);
        return void 0 === c[t].overloadTable ? (s.argCount = r - 2, c[t] = s) : c[t].overloadTable[r - 2] = s, []
      })), []
    }))
  }

  function no(e, t, r) {
    return e instanceof Object || pn(r + ' with invalid "this": ' + e), e instanceof t.registeredClass.constructor || pn(r + ' incompatible with "this" of type ' + e.constructor.name), e.$$.ptr || pn("cannot call emscripten binding method " + r + " on deleted object"), xn(e.$$.ptr, e.$$.ptrType.registeredClass, t.registeredClass)
  }

  function oo(e, t, r, n, o, a, i, s, u, c) {
    t = cn(t), o = Qn(n, o), mn([], [e], (function (e) {
      var n = (e = e[0]).name + "." + t, l = {
        get: function () {
          qn("Cannot access " + n + " due to unbound types", [r, i])
        }, enumerable: !0, configurable: !0
      };
      return l.set = u ? function () {
        qn("Cannot access " + n + " due to unbound types", [r, i])
      } : function (e) {
        pn(n + " is a read-only property")
      }, Object.defineProperty(e.registeredClass.instancePrototype, t, l), mn([], u ? [r, i] : [r], (function (r) {
        var i = r[0], l = {
          get: function () {
            var t = no(this, e, n + " getter");
            return i.fromWireType(o(a, t))
          }, enumerable: !0
        };
        if (u) {
          u = Qn(s, u);
          var d = r[1];
          l.set = function (t) {
            var r = no(this, e, n + " setter"), o = [];
            u(c, r, d.toWireType(o, t)), Jn(o)
          }
        }
        return Object.defineProperty(e.registeredClass.instancePrototype, t, l), []
      })), []
    }))
  }

  function ao(e) {
    e > 4 && 0 == --Lt[e].refcount && (Lt[e] = void 0, Rt.push(e))
  }

  function io() {
    for (var e = 0, t = 5; t < Lt.length; ++t) void 0 !== Lt[t] && ++e;
    return e
  }

  function so() {
    for (var e = 5; e < Lt.length; ++e) if (void 0 !== Lt[e]) return Lt[e];
    return null
  }

  function uo(e) {
    switch (e) {
      case void 0:
        return 1;
      case null:
        return 2;
      case!0:
        return 3;
      case!1:
        return 4;
      default:
        var t = Rt.length ? Rt.pop() : Lt.length;
        return Lt[t] = {refcount: 1, value: e}, t
    }
  }

  function co(e, t) {
    An(e, {
      name: t = cn(t), fromWireType: function (e) {
        var t = Lt[e].value;
        return ao(e), t
      }, toWireType: function (e, t) {
        return uo(t)
      }, argPackAdvance: 8, readValueFromPointer: Ln, destructorFunction: null
    })
  }

  function lo(e) {
    if (null === e) return "null";
    var t = typeof e;
    return "object" === t || "array" === t || "function" === t ? e.toString() : "" + e
  }

  function fo(e, t) {
    switch (t) {
      case 2:
        return function (e) {
          return this.fromWireType(Qe[e >> 2])
        };
      case 3:
        return function (e) {
          return this.fromWireType(Xe[e >> 3])
        };
      default:
        throw new TypeError("Unknown float type: " + e)
    }
  }

  function po(e, t, r) {
    var n = un(r);
    An(e, {
      name: t = cn(t), fromWireType: function (e) {
        return e
      }, toWireType: function (e, t) {
        if ("number" != typeof t && "boolean" != typeof t) throw new TypeError('Cannot convert "' + lo(t) + '" to ' + this.name);
        return t
      }, argPackAdvance: 8, readValueFromPointer: fo(t, n), destructorFunction: null
    })
  }

  function ho(e, t, r) {
    switch (t) {
      case 0:
        return r ? function (e) {
          return ze[e]
        } : function (e) {
          return We[e]
        };
      case 1:
        return r ? function (e) {
          return Ye[e >> 1]
        } : function (e) {
          return He[e >> 1]
        };
      case 2:
        return r ? function (e) {
          return $e[e >> 2]
        } : function (e) {
          return Ve[e >> 2]
        };
      default:
        throw new TypeError("Unknown integer type: " + e)
    }
  }

  function mo(e, t, r, n, o) {
    t = cn(t), -1 === o && (o = 4294967295);
    var a = un(r), i = function (e) {
      return e
    };
    if (0 === n) {
      var s = 32 - 8 * r;
      i = function (e) {
        return e << s >>> s
      }
    }
    var u = -1 != t.indexOf("unsigned");
    An(e, {
      name: t, fromWireType: i, toWireType: function (e, r) {
        if ("number" != typeof r && "boolean" != typeof r) throw new TypeError('Cannot convert "' + lo(r) + '" to ' + this.name);
        if (r < n || r > o) throw new TypeError('Passing a number "' + lo(r) + '" from JS side to C/C++ side to an argument of type "' + t + '", which is outside the valid range [' + n + ", " + o + "]!");
        return u ? r >>> 0 : 0 | r
      }, argPackAdvance: 8, readValueFromPointer: ho(t, a, 0 !== n), destructorFunction: null
    })
  }

  function Ao(e, t, r) {
    var n = [Int8Array, Uint8Array, Int16Array, Uint16Array, Int32Array, Uint32Array, Float32Array, Float64Array][t];

    function o(e) {
      var t = Ve, r = t[e >>= 2], o = t[e + 1];
      return new n(Ge, o, r)
    }

    An(e, {
      name: r = cn(r),
      fromWireType: o,
      argPackAdvance: 8,
      readValueFromPointer: o
    }, {ignoreDuplicateRegistrations: !0})
  }

  function go(e, t) {
    var r = "std::string" === (t = cn(t));
    An(e, {
      name: t, fromWireType: function (e) {
        var t, n = Ve[e >> 2];
        if (r) for (var o = e + 4, a = 0; a <= n; ++a) {
          var i = e + 4 + a;
          if (a == n || 0 == We[i]) {
            var s = xr(o, i - o);
            void 0 === t ? t = s : (t += String.fromCharCode(0), t += s), o = i + 1
          }
        } else {
          var u = new Array(n);
          for (a = 0; a < n; ++a) u[a] = String.fromCharCode(We[e + 4 + a]);
          t = u.join("")
        }
        return Kt(e), t
      }, toWireType: function (e, t) {
        t instanceof ArrayBuffer && (t = new Uint8Array(t));
        var n = "string" == typeof t;
        n || t instanceof Uint8Array || t instanceof Uint8ClampedArray || t instanceof Int8Array || pn("Cannot pass non-string to std::string");
        var o = (r && n ? function () {
          return Rr(t)
        } : function () {
          return t.length
        })(), a = Jt(4 + o + 1);
        if (Ve[a >> 2] = o, r && n) Mr(t, a + 4, o + 1); else if (n) for (var i = 0; i < o; ++i) {
          var s = t.charCodeAt(i);
          s > 255 && (Kt(a), pn("String has UTF-16 code units that do not fit in 8 bits")), We[a + 4 + i] = s
        } else for (i = 0; i < o; ++i) We[a + 4 + i] = t[i];
        return null !== e && e.push(Kt, a), a
      }, argPackAdvance: 8, readValueFromPointer: Ln, destructorFunction: function (e) {
        Kt(e)
      }
    })
  }

  function vo(e, t, r) {
    var n, o, a, i, s;
    r = cn(r), 2 === t ? (n = Lr, o = Ir, i = Fr, a = function () {
      return He
    }, s = 1) : 4 === t && (n = Ur, o = Nr, i = jr, a = function () {
      return Ve
    }, s = 2), An(e, {
      name: r, fromWireType: function (e) {
        for (var r, o = Ve[e >> 2], i = a(), u = e + 4, c = 0; c <= o; ++c) {
          var l = e + 4 + c * t;
          if (c == o || 0 == i[l >> s]) {
            var d = n(u, l - u);
            void 0 === r ? r = d : (r += String.fromCharCode(0), r += d), u = l + t
          }
        }
        return Kt(e), r
      }, toWireType: function (e, n) {
        "string" != typeof n && pn("Cannot pass non-string to C++ string type " + r);
        var a = i(n), u = Jt(4 + a + t);
        return Ve[u >> 2] = a >> s, o(n, u + 4, a + t), null !== e && e.push(Kt, u), u
      }, argPackAdvance: 8, readValueFromPointer: Ln, destructorFunction: function (e) {
        Kt(e)
      }
    })
  }

  function Eo(e, t) {
    An(e, {
      isVoid: !0, name: t = cn(t), argPackAdvance: 0, fromWireType: function () {
      }, toWireType: function (e, t) {
      }
    })
  }

  function yo(e, t) {
    if (e == t) postMessage({cmd: "processQueuedMainThreadWork"}); else if (Se) postMessage({
      targetThread: e,
      cmd: "processThreadQueue"
    }); else {
      var r = ft.pthreads[e], n = r && r.worker;
      if (!n) return;
      n.postMessage({cmd: "processThreadQueue"})
    }
    return 1
  }

  function wo(e) {
    return e || pn("Cannot use deleted val. handle = " + e), Lt[e].value
  }

  function _o(e, t) {
    var r = _t[e];
    return void 0 === r && pn(t + " has unknown type " + Xn(e)), r
  }

  function bo(e, t, r) {
    e = wo(e), t = _o(t, "emval::as");
    var n = [], o = uo(n);
    return $e[r >> 2] = o, t.toWireType(n, e)
  }

  function ko(e, t, r, n) {
    var o, a;
    (e = Ft[e])(t = wo(t), r = void 0 === (a = It[o = r]) ? cn(o) : a, null, n)
  }

  function To(e, t) {
    for (var r = function (e, t) {
      for (var r = new Array(e), n = 0; n < e; ++n) r[n] = _o($e[(t >> 2) + n], "parameter " + n);
      return r
    }(e, t), n = r[0], o = n.name + "_$" + r.slice(1).map((function (e) {
      return e.name
    })).join("_") + "$", a = ["retType"], i = [n], s = "", u = 0; u < e - 1; ++u) s += (0 !== u ? ", " : "") + "arg" + u, a.push("argType" + u), i.push(r[1 + u]);
    var c = "return function " + ln("methodCaller_" + o) + "(handle, name, destructors, args) {\n", l = 0;
    for (u = 0; u < e - 1; ++u) c += "    var arg" + u + " = argType" + u + ".readValueFromPointer(args" + (l ? "+" + l : "") + ");\n", l += r[u + 1].argPackAdvance;
    c += "    var rv = handle[name](" + s + ");\n";
    for (u = 0; u < e - 1; ++u) r[u + 1].deleteObject && (c += "    argType" + u + ".deleteObject(arg" + u + ");\n");
    n.isVoid || (c += "    return retType.toWireType(destructors, rv);\n"), c += "};\n", a.push(c);
    var d, f, p = to(Function, a).apply(null, i);
    return d = p, f = Ft.length, Ft.push(d), f
  }

  function Do(e) {
    e > 4 && (Lt[e].refcount += 1)
  }

  function So(e) {
    Jn(Lt[e].value), ao(e)
  }

  function Co(e, t) {
    return uo((e = _o(e, "_emval_take_value")).readValueFromPointer(t))
  }

  function Oo() {
    Yr()
  }

  function Po() {
    return void 0 === Po.start && (Po.start = Date.now()), 1e3 * (Date.now() - Po.start) | 0
  }

  function xo(e, t, r) {
    var n = function (e, t) {
      var r;
      Nt.length = 0, t >>= 2;
      for (; r = We[e++];) {
        var n = r < 105;
        n && 1 & t && t++, Nt.push(n ? Xe[t++ >> 1] : $e[t]), ++t
      }
      return Nt
    }(t, r);
    return lt[e].apply(null, n)
  }

  function Bo() {
    De || Cr("Blocking on the main thread is very dangerous, see https://emscripten.org/docs/porting/pthreads.html#blocking-on-the-main-browser-thread")
  }

  function Mo(e, t) {
  }

  function Ro(e, t, r) {
    if (e <= 0 || e > ze.length || !0 & e) return -28;
    if (Te) {
      if (Atomics.load($e, e >> 2) != t) return -6;
      var n = performance.now(), o = n + r;
      for (Atomics.exchange($e, br >> 2, e); ;) {
        if ((n = performance.now()) > o) return Atomics.exchange($e, br >> 2, 0), -73;
        if (0 == Atomics.exchange($e, br >> 2, 0)) break;
        if (lr(), Atomics.load($e, e >> 2) != t) return -6;
        Atomics.exchange($e, br >> 2, e)
      }
      return 0
    }
    var a = Atomics.wait($e, e >> 2, t, r);
    if ("timed-out" === a) return -73;
    if ("not-equal" === a) return -6;
    if ("ok" === a) return 0;
    throw"Atomics.wait returned an unexpected value " + a
  }

  function Lo(e, t) {
    return (e >>> 0) + 4294967296 * t
  }

  function Io(e, t) {
    if (e <= 0) return e;
    var r = t <= 32 ? Math.abs(1 << t - 1) : Math.pow(2, t - 1);
    return e >= r && (t <= 32 || e > r) && (e = -2 * r + e), e
  }

  function Fo(e, t) {
    return e >= 0 ? e : t <= 32 ? 2 * Math.abs(1 << t - 1) + e : Math.pow(2, t) + e
  }

  function Uo(e) {
    if (!e || !e.callee || !e.callee.name) return [null, "", ""];
    e.callee.toString();
    var t = e.callee.name, r = "(", n = !0;
    for (var o in e) {
      var a = e[o];
      n || (r += ", "), n = !1, r += "number" == typeof a || "string" == typeof a ? a : "(" + typeof a + ")"
    }
    r += ")";
    var i = e.callee.caller;
    return n && (r = ""), [e = i ? i.arguments : [], t, r]
  }

  function No(e, t) {
    24 & e && (t = t.replace(/\s+$/, ""), t += (t.length > 0 ? "\n" : "") + function (e) {
      var t = tn(), r = t.lastIndexOf("_emscripten_log"), n = t.lastIndexOf("_emscripten_get_callstack"),
        o = t.indexOf("\n", Math.max(r, n)) + 1;
      t = t.slice(o), 32 & e && Cr("EM_LOG_DEMANGLE is deprecated; ignoring"), 8 & e && "undefined" == typeof emscripten_source_map && (Cr('Source map information is not available, emscripten_log with EM_LOG_C_STACK will be ignored. Build with "--pre-js $EMSCRIPTEN/src/emscripten-source-map.min.js" linker flag to add source map loading to code.'), e ^= 8, e |= 16);
      var a = null;
      if (128 & e) for (a = Uo(arguments); a[1].indexOf("_emscripten_") >= 0;) a = Uo(a[0]);
      var i = t.split("\n");
      t = "";
      var s = new RegExp("\\s*(.*?)@(.*?):([0-9]+):([0-9]+)"), u = new RegExp("\\s*(.*?)@(.*):(.*)(:(.*))?"),
        c = new RegExp("\\s*at (.*?) \\((.*):(.*):(.*)\\)");
      for (var l in i) {
        var d = i[l], f = "", p = "", h = 0, m = 0, A = c.exec(d);
        if (A && 5 == A.length) f = A[1], p = A[2], h = A[3], m = A[4]; else {
          if ((A = s.exec(d)) || (A = u.exec(d)), !(A && A.length >= 4)) {
            t += d + "\n";
            continue
          }
          f = A[1], p = A[2], h = A[3], m = 0 | A[4]
        }
        var g = !1;
        if (8 & e) {
          var v = emscripten_source_map.originalPositionFor({line: h, column: m});
          (g = v && v.source) && (64 & e && (v.source = v.source.substring(v.source.replace(/\\/g, "/").lastIndexOf("/") + 1)), t += "    at " + f + " (" + v.source + ":" + v.line + ":" + v.column + ")\n")
        }
        (16 & e || !g) && (64 & e && (p = p.substring(p.replace(/\\/g, "/").lastIndexOf("/") + 1)), t += (g ? "     = " + f : "    at " + f) + " (" + p + ":" + h + ":" + m + ")\n"), 128 & e && a[0] && (a[1] == f && a[2].length > 0 && (t = t.replace(/\s+$/, ""), t += " with values: " + a[1] + a[2] + "\n"), a = Uo(a[0]))
      }
      return t.replace(/\s+$/, "")
    }(e)), 1 & e ? 4 & e ? console.error(t) : 2 & e ? console.warn(t) : 512 & e ? console.info(t) : 256 & e ? console.debug(t) : console.log(t) : 6 & e ? Me(t) : Be(t)
  }

  function jo(e, t, r) {
    No(e, Pr(function (e, t) {
      var r = e, n = t;

      function o(e) {
        var t;
        return n = function (e, t) {
          return "double" !== t && "i64" !== t || 7 & e && (e += 4), e
        }(n, e), "double" === e ? (t = Xe[n >> 3], n += 8) : "i64" == e ? (t = [$e[n >> 2], $e[n + 4 >> 2]], n += 8) : (e = "i32", t = $e[n >> 2], n += 4), t
      }

      for (var a, i, s, u, c = []; ;) {
        var l = r;
        if (0 === (a = ze[r >> 0])) break;
        if (i = ze[r + 1 >> 0], 37 == a) {
          var d = !1, f = !1, p = !1, h = !1, m = !1;
          e:for (; ;) {
            switch (i) {
              case 43:
                d = !0;
                break;
              case 45:
                f = !0;
                break;
              case 35:
                p = !0;
                break;
              case 48:
                if (h) break e;
                h = !0;
                break;
              case 32:
                m = !0;
                break;
              default:
                break e
            }
            r++, i = ze[r + 1 >> 0]
          }
          var A = 0;
          if (42 == i) A = o("i32"), r++, i = ze[r + 1 >> 0]; else for (; i >= 48 && i <= 57;) A = 10 * A + (i - 48), r++, i = ze[r + 1 >> 0];
          var g, v = !1, E = -1;
          if (46 == i) {
            if (E = 0, v = !0, r++, 42 == (i = ze[r + 1 >> 0])) E = o("i32"), r++; else for (; ;) {
              var y = ze[r + 1 >> 0];
              if (y < 48 || y > 57) break;
              E = 10 * E + (y - 48), r++
            }
            i = ze[r + 1 >> 0]
          }
          switch (E < 0 && (E = 6, v = !1), String.fromCharCode(i)) {
            case"h":
              104 == ze[r + 2 >> 0] ? (r++, g = 1) : g = 2;
              break;
            case"l":
              108 == ze[r + 2 >> 0] ? (r++, g = 8) : g = 4;
              break;
            case"L":
            case"q":
            case"j":
              g = 8;
              break;
            case"z":
            case"t":
            case"I":
              g = 4;
              break;
            default:
              g = null
          }
          switch (g && r++, i = ze[r + 1 >> 0], String.fromCharCode(i)) {
            case"d":
            case"i":
            case"u":
            case"o":
            case"x":
            case"X":
            case"p":
              var w = 100 == i || 105 == i;
              s = o("i" + 8 * (g = g || 4)), 8 == g && (s = 117 == i ? (s[0] >>> 0) + 4294967296 * (s[1] >>> 0) : Lo(s[0], s[1])), g <= 4 && (s = (w ? Io : Fo)(s & Math.pow(256, g) - 1, 8 * g));
              var _ = Math.abs(s), b = "";
              if (100 == i || 105 == i) D = Io(s, 8 * g).toString(10); else if (117 == i) D = Fo(s, 8 * g).toString(10), s = Math.abs(s); else if (111 == i) D = (p ? "0" : "") + _.toString(8); else if (120 == i || 88 == i) {
                if (b = p && 0 != s ? "0x" : "", s < 0) {
                  s = -s, D = (_ - 1).toString(16);
                  for (var k = [], T = 0; T < D.length; T++) k.push((15 - parseInt(D[T], 16)).toString(16));
                  for (D = k.join(""); D.length < 2 * g;) D = "f" + D
                } else D = _.toString(16);
                88 == i && (b = b.toUpperCase(), D = D.toUpperCase())
              } else 112 == i && (0 === _ ? D = "(nil)" : (b = "0x", D = _.toString(16)));
              if (v) for (; D.length < E;) D = "0" + D;
              for (s >= 0 && (d ? b = "+" + b : m && (b = " " + b)), "-" == D.charAt(0) && (b = "-" + b, D = D.substr(1)); b.length + D.length < A;) f ? D += " " : h ? D = "0" + D : b = " " + b;
              (D = b + D).split("").forEach((function (e) {
                c.push(e.charCodeAt(0))
              }));
              break;
            case"f":
            case"F":
            case"e":
            case"E":
            case"g":
            case"G":
              var D;
              if (s = o("double"), isNaN(s)) D = "nan", h = !1; else if (isFinite(s)) {
                var S = !1, C = Math.min(E, 20);
                if (103 == i || 71 == i) {
                  S = !0, E = E || 1;
                  var O = parseInt(s.toExponential(C).split("e")[1], 10);
                  E > O && O >= -4 ? (i = (103 == i ? "f" : "F").charCodeAt(0), E -= O + 1) : (i = (103 == i ? "e" : "E").charCodeAt(0), E--), C = Math.min(E, 20)
                }
                101 == i || 69 == i ? (D = s.toExponential(C), /[eE][-+]\d$/.test(D) && (D = D.slice(0, -1) + "0" + D.slice(-1))) : 102 != i && 70 != i || (D = s.toFixed(C), 0 === s && ((u = s) < 0 || 0 === u && 1 / u == -1 / 0) && (D = "-" + D));
                var P = D.split("e");
                if (S && !p) for (; P[0].length > 1 && -1 != P[0].indexOf(".") && ("0" == P[0].slice(-1) || "." == P[0].slice(-1));) P[0] = P[0].slice(0, -1); else for (p && -1 == D.indexOf(".") && (P[0] += "."); E > C++;) P[0] += "0";
                D = P[0] + (P.length > 1 ? "e" + P[1] : ""), 69 == i && (D = D.toUpperCase()), s >= 0 && (d ? D = "+" + D : m && (D = " " + D))
              } else D = (s < 0 ? "-" : "") + "inf", h = !1;
              for (; D.length < A;) f ? D += " " : D = !h || "-" != D[0] && "+" != D[0] ? (h ? "0" : " ") + D : D[0] + "0" + D.slice(1);
              i < 97 && (D = D.toUpperCase()), D.split("").forEach((function (e) {
                c.push(e.charCodeAt(0))
              }));
              break;
            case"s":
              var x = o("i8*"), B = x ? er(x) : "(null)".length;
              if (v && (B = Math.min(B, E)), !f) for (; B < A--;) c.push(32);
              if (x) for (T = 0; T < B; T++) c.push(We[x++ >> 0]); else c = c.concat(ga("(null)".substr(0, B), !0));
              if (f) for (; B < A--;) c.push(32);
              break;
            case"c":
              for (f && c.push(o("i8")); --A > 0;) c.push(32);
              f || c.push(o("i8"));
              break;
            case"n":
              var M = o("i32*");
              $e[M >> 2] = c.length;
              break;
            case"%":
              c.push(a);
              break;
            default:
              for (T = l; T < r + 2; T++) c.push(ze[T >> 0])
          }
          r += 2
        } else c.push(a), r += 1
      }
      return c
    }(t, r), 0))
  }

  function Go(e, t, r) {
    We.copyWithin(e, t, t + r)
  }

  function zo(e, t) {
    for (var r = arguments.length - 2, n = gr(), o = r, a = Er(8 * o), i = a >> 3, s = 0; s < r; s++) {
      var u = arguments[2 + s];
      Xe[i + s] = u
    }
    var c = hr(e, o, a, t);
    return vr(n), c
  }

  function Wo(e, t, r) {
    Ut.length = t;
    for (var n = r >> 3, o = 0; o < t; o++) Ut[o] = Xe[n + o];
    return (e < 0 ? lt[-e - 1] : Xt[e]).apply(null, Ut)
  }

  function Yo(e) {
    Yr("OOM")
  }

  function Ho(e, t, r, n) {
    var o, a, i, s = gr(), u = Er(12), c = 0;
    t && (a = Rr(o = t) + 1, i = Jt(a), Mr(o, i, a), c = i), $e[u >> 2] = c, $e[u + 4 >> 2] = r, $e[u + 8 >> 2] = n, mr(0, e, 657457152, 0, c, u), vr(s)
  }

  function $o(e) {
    var t;
    return e = (t = e) > 2 ? xr(t) : t, Gt[e] || ("undefined" != typeof document ? document.querySelector(e) : void 0)
  }

  function Vo(e) {
    return $o(e)
  }

  function Qo(e, t, r) {
    var n = Vo(e);
    if (!n) return -4;
    if (n.canvasSharedPtr && ($e[n.canvasSharedPtr >> 2] = t, $e[n.canvasSharedPtr + 4 >> 2] = r), !n.offscreenCanvas && n.controlTransferredOffscreen) return n.canvasSharedPtr ? (function (e, t, r, n) {
      Ho(e, t = t ? xr(t) : "", r, n)
    }($e[n.canvasSharedPtr + 8 >> 2], e, t, r), 1) : -4;
    n.offscreenCanvas && (n = n.offscreenCanvas);
    var o = !1;
    if (n.GLctxObject && n.GLctxObject.GLctx) {
      var a = n.GLctxObject.GLctx.getParameter(2978);
      o = 0 === a[0] && 0 === a[1] && a[2] === n.width && a[3] === n.height
    }
    return n.width = t, n.height = r, o && n.GLctxObject.GLctx.viewport(0, 0, t, r), 0
  }

  function Xo(e, t, r) {
    return Se ? zo(3, 1, e, t, r) : Qo(e, t, r)
  }

  function qo(e, t, r) {
    return Vo(e) ? Qo(e, t, r) : Xo(e, t, r)
  }

  function Zo(e) {
  }

  function Ko(e, t) {
    return r = e, o = $e[(n = t >> 2) + 6], a = {
      alpha: !!$e[n + 0],
      depth: !!$e[n + 1],
      stencil: !!$e[n + 2],
      antialias: !!$e[n + 3],
      premultipliedAlpha: !!$e[n + 4],
      preserveDrawingBuffer: !!$e[n + 5],
      powerPreference: Wt[o],
      failIfMajorPerformanceCaveat: !!$e[n + 7],
      majorVersion: $e[n + 8],
      minorVersion: $e[n + 9],
      enableExtensionsByDefault: $e[n + 10],
      explicitSwapControl: $e[n + 11],
      proxyContextToMainThread: $e[n + 12],
      renderViaOffscreenBackBuffer: $e[n + 13]
    }, (i = Vo(r)) ? a.explicitSwapControl ? 0 : zt.createContext(i, a) : 0;
    var r, n, o, a, i
  }

  function Jo() {
    if (!Jo.strings) {
      var e = {
        USER: "web_user",
        LOGNAME: "web_user",
        PATH: "/",
        PWD: "/",
        HOME: "/home/web_user",
        LANG: ("object" == typeof navigator && navigator.languages && navigator.languages[0] || "C").replace("-", "_") + ".UTF-8",
        _: be || "./this.program"
      };
      for (var t in Yt) e[t] = Yt[t];
      var r = [];
      for (var t in e) r.push(t + "=" + e[t]);
      Jo.strings = r
    }
    return Jo.strings
  }

  function ea(e, t) {
    if (Se) return zo(4, 1, e, t);
    try {
      var r = 0;
      return Jo().forEach((function (n, o) {
        var a = t + r;
        $e[e + 4 * o >> 2] = a, function (e, t, r) {
          for (var n = 0; n < e.length; ++n) ze[t++ >> 0] = e.charCodeAt(n);
          r || (ze[t >> 0] = 0)
        }(n, a), r += n.length + 1
      })), 0
    } catch (e) {
      return void 0 !== vt && e instanceof vt.ErrnoError || Yr(e), e.errno
    }
  }

  function ta(e, t) {
    if (Se) return zo(5, 1, e, t);
    try {
      var r = Jo();
      $e[e >> 2] = r.length;
      var n = 0;
      return r.forEach((function (e) {
        n += e.length + 1
      })), $e[t >> 2] = n, 0
    } catch (e) {
      return void 0 !== vt && e instanceof vt.ErrnoError || Yr(e), e.errno
    }
  }

  function ra(e) {
    if (Se) return zo(6, 1, e);
    try {
      var t = Et.getStreamFromFD(e);
      return vt.close(t), 0
    } catch (e) {
      return void 0 !== vt && e instanceof vt.ErrnoError || Yr(e), e.errno
    }
  }

  function na(e, t) {
    if (Se) return zo(7, 1, e, t);
    try {
      var r = Et.getStreamFromFD(e), n = r.tty ? 2 : vt.isDir(r.mode) ? 3 : vt.isLink(r.mode) ? 7 : 4;
      return ze[t >> 0] = n, 0
    } catch (e) {
      return void 0 !== vt && e instanceof vt.ErrnoError || Yr(e), e.errno
    }
  }

  function oa(e, t, r, n) {
    if (Se) return zo(8, 1, e, t, r, n);
    try {
      var o = Et.getStreamFromFD(e), a = Et.doReadv(o, t, r);
      return $e[n >> 2] = a, 0
    } catch (e) {
      return void 0 !== vt && e instanceof vt.ErrnoError || Yr(e), e.errno
    }
  }

  function aa(e, t, r, n, o) {
    if (Se) return zo(9, 1, e, t, r, n, o);
    try {
      var a = Et.getStreamFromFD(e), i = 4294967296 * r + (t >>> 0), s = 9007199254740992;
      return i <= -s || i >= s ? -61 : (vt.llseek(a, i, n), ct = [a.position >>> 0, (ut = a.position, +Math.abs(ut) >= 1 ? ut > 0 ? (0 | Math.min(+Math.floor(ut / 4294967296), 4294967295)) >>> 0 : ~~+Math.ceil((ut - +(~~ut >>> 0)) / 4294967296) >>> 0 : 0)], $e[o >> 2] = ct[0], $e[o + 4 >> 2] = ct[1], a.getdents && 0 === i && 0 === n && (a.getdents = null), 0)
    } catch (e) {
      return void 0 !== vt && e instanceof vt.ErrnoError || Yr(e), e.errno
    }
  }

  function ia(e, t, r, n) {
    if (Se) return zo(10, 1, e, t, r, n);
    try {
      var o = Et.getStreamFromFD(e), a = Et.doWritev(o, t, r);
      return $e[n >> 2] = a, 0
    } catch (e) {
      return void 0 !== vt && e instanceof vt.ErrnoError || Yr(e), e.errno
    }
  }

  function sa(e) {
    var t = Date.now();
    return $e[e >> 2] = t / 1e3 | 0, $e[e + 4 >> 2] = t % 1e3 * 1e3 | 0, 0
  }

  function ua() {
    if (Se) return zo(11, 1);
    if (!ua.called) {
      ua.called = !0;
      var e = (new Date).getFullYear(), t = new Date(e, 0, 1), r = new Date(e, 6, 1), n = t.getTimezoneOffset(),
        o = r.getTimezoneOffset(), a = Math.max(n, o);
      $e[ir() >> 2] = 60 * a, $e[ar() >> 2] = Number(n != o);
      var i = l(t), s = l(r), u = Gr(i), c = Gr(s);
      o < n ? ($e[or() >> 2] = u, $e[or() + 4 >> 2] = c) : ($e[or() >> 2] = c, $e[or() + 4 >> 2] = u)
    }

    function l(e) {
      var t = e.toTimeString().match(/\(([A-Za-z ]+)\)$/);
      return t ? t[1] : "GMT"
    }
  }

  function ca(e) {
    var t = ft.threadExitHandlers.pop();
    e && t()
  }

  function la(e, t) {
    ft.threadExitHandlers.push((function () {
      Ze.get(e)(t)
    }))
  }

  function da(e) {
    if (Se) throw"Internal Error! spawnThread() can only ever be called from main application thread!";
    var t = ft.getNewWorker();
    if (void 0 !== t.pthread) throw"Internal error!";
    if (!e.pthread_ptr) throw"Internal error, no pthread ptr!";
    ft.runningWorkers.push(t);
    for (var r = Jt(512), n = 0; n < 128; ++n) $e[r + 4 * n >> 2] = 0;
    var o = e.stackBase + e.stackSize, a = ft.pthreads[e.pthread_ptr] = {
      worker: t,
      stackBase: e.stackBase,
      stackSize: e.stackSize,
      allocatedOwnStack: e.allocatedOwnStack,
      threadInfoStruct: e.pthread_ptr
    }, i = a.threadInfoStruct >> 2;
    Atomics.store(Ve, i + 16, e.detached), Atomics.store(Ve, i + 25, r), Atomics.store(Ve, i + 10, a.threadInfoStruct), Atomics.store(Ve, i + 20, e.stackSize), Atomics.store(Ve, i + 19, o), Atomics.store(Ve, i + 26, e.stackSize), Atomics.store(Ve, i + 28, o), Atomics.store(Ve, i + 29, e.detached);
    var s = nr() + 40;
    Atomics.store(Ve, i + 43, s), t.pthread = a;
    var u = {
      cmd: "run",
      start_routine: e.startRoutine,
      arg: e.arg,
      threadInfoStruct: e.pthread_ptr,
      stackBase: e.stackBase,
      stackSize: e.stackSize
    };
    t.runPthread = function () {
      u.time = performance.now(), t.postMessage(u, e.transferList)
    }, t.loaded && (t.runPthread(), delete t.runPthread)
  }

  function fa(e, t, r, n) {
    if ("undefined" == typeof SharedArrayBuffer) return Me("Current environment does not support SharedArrayBuffer, pthreads are not available!"), 6;
    if (!e) return Me("pthread_create called with a null thread pointer!"), 28;
    var o = [];
    if (Se && 0 === o.length) return pr(687865856, e, t, r, n);
    var a = 0, i = 0, s = 0;
    t && -1 != t ? (a = $e[t >> 2], a += 81920, i = $e[t + 8 >> 2], s = 0 !== $e[t + 12 >> 2]) : a = 2097152;
    var u = 0 == i;
    u ? i = wr(16, a) : Or((i -= a) > 0);
    for (var c = Jt(228), l = 0; l < 57; ++l) Ve[(c >> 2) + l] = 0;
    $e[e >> 2] = c, $e[c + 12 >> 2] = c;
    var d = c + 152;
    $e[d >> 2] = d;
    var f = {
      stackBase: i,
      stackSize: a,
      allocatedOwnStack: u,
      detached: s,
      startRoutine: r,
      pthread_ptr: c,
      arg: n,
      transferList: o
    };
    return Se ? (f.cmd = "spawnThread", postMessage(f, o)) : da(f), 0
  }

  function pa() {
    if (Se) {
      var e = sr();
      if (e) if (!Atomics.load(Ve, e + 56 >> 2)) if (2 == Atomics.load(Ve, e + 0 >> 2)) throw"Canceled!"
    }
  }

  function ha(e, t) {
    return function (e, t, r) {
      if (!e) return Me("pthread_join attempted on a null thread pointer!"), dt.ESRCH;
      if (Se && sr() == e) return Me("PThread " + e + " is attempting to join to itself!"), dt.EDEADLK;
      if (!Se && ur() == e) return Me("Main thread " + e + " is attempting to join to itself!"), dt.EDEADLK;
      if ($e[e + 12 >> 2] !== e) return Me("pthread_join attempted on thread " + e + ", which does not point to a valid thread, or does not exist anymore!"), dt.ESRCH;
      if (Atomics.load(Ve, e + 64 >> 2)) return Me("Attempted to join thread " + e + ", which was already detached!"), dt.EINVAL;
      for (r && Bo(); ;) {
        var n = Atomics.load(Ve, e + 0 >> 2);
        if (1 == n) {
          var o = Atomics.load(Ve, e + 4 >> 2);
          return t && ($e[t >> 2] = o), Atomics.store(Ve, e + 64 >> 2, 1), Se ? postMessage({
            cmd: "cleanupThread",
            thread: e
          }) : Zr(e), 0
        }
        if (!r) return dt.EBUSY;
        pa(), Se || lr(), Ro(e + 0, n, Se ? 100 : 1)
      }
    }(e, t, !0)
  }

  function ma(e) {
    Le(0 | e)
  }

  function Aa(e) {
    if (Se) return zo(12, 1, e);
    switch (e) {
      case 30:
        return 16384;
      case 85:
        return We.length / 16384;
      case 132:
      case 133:
      case 12:
      case 137:
      case 138:
      case 15:
      case 235:
      case 16:
      case 17:
      case 18:
      case 19:
      case 20:
      case 149:
      case 13:
      case 10:
      case 236:
      case 153:
      case 9:
      case 21:
      case 22:
      case 159:
      case 154:
      case 14:
      case 77:
      case 78:
      case 139:
      case 82:
      case 68:
      case 67:
      case 164:
      case 11:
      case 29:
      case 47:
      case 48:
      case 95:
      case 52:
      case 51:
      case 46:
        return 200809;
      case 27:
      case 246:
      case 127:
      case 128:
      case 23:
      case 24:
      case 160:
      case 161:
      case 181:
      case 182:
      case 242:
      case 183:
      case 184:
      case 243:
      case 244:
      case 245:
      case 165:
      case 178:
      case 179:
      case 49:
      case 50:
      case 168:
      case 169:
      case 175:
      case 170:
      case 171:
      case 172:
      case 97:
      case 76:
      case 32:
      case 173:
      case 35:
      case 80:
      case 81:
      case 79:
        return -1;
      case 176:
      case 177:
      case 7:
      case 155:
      case 8:
      case 157:
      case 125:
      case 126:
      case 92:
      case 93:
      case 129:
      case 130:
      case 131:
      case 94:
      case 91:
        return 1;
      case 74:
      case 60:
      case 69:
      case 70:
      case 4:
        return 1024;
      case 31:
      case 42:
      case 72:
        return 32;
      case 87:
      case 26:
      case 33:
        return 2147483647;
      case 34:
      case 1:
        return 47839;
      case 38:
      case 36:
        return 99;
      case 43:
      case 37:
        return 2048;
      case 0:
        return 2097152;
      case 3:
        return 65536;
      case 28:
        return 32768;
      case 44:
        return 32767;
      case 75:
        return 16384;
      case 39:
        return 1e3;
      case 89:
        return 700;
      case 71:
        return 256;
      case 40:
        return 255;
      case 2:
        return 100;
      case 180:
        return 64;
      case 25:
        return 20;
      case 5:
        return 16;
      case 6:
        return 6;
      case 73:
        return 4;
      case 84:
        return "object" == typeof navigator && navigator.hardwareConcurrency || 1
    }
    return nn(28), -1
  }

  function ga(e, t, r) {
    var n = r > 0 ? r : Rr(e) + 1, o = new Array(n), a = Br(e, o, 0, o.length);
    return t && (o.length = a), o
  }

  function va(e) {
    this.name = "ExitStatus", this.message = "Program terminated with exit(" + e + ")", this.status = e
  }

  function Ea(e) {
    function t() {
      kr || (kr = !0, Ee.calledRun = !0, je || (!0, Ee.noFSInit || vt.init.initialized || vt.init(), At.init(), Xr(Je), Se || (vt.ignorePermissions = !1, Xr(et)), Ee.onRuntimeInitialized && Ee.onRuntimeInitialized(), function () {
        if (!Se) {
          if (Ee.postRun) for ("function" == typeof Ee.postRun && (Ee.postRun = [Ee.postRun]); Ee.postRun.length;) e = Ee.postRun.shift(), rt.unshift(e);
          var e;
          Xr(rt)
        }
      }()))
    }

    e = e || _e, nt > 0 || (Se ? postMessage({cmd: "loaded"}) : (!function () {
      if (!Se) {
        if (Ee.preRun) for ("function" == typeof Ee.preRun && (Ee.preRun = [Ee.preRun]); Ee.preRun.length;) e = Ee.preRun.shift(), Ke.unshift(e);
        var e;
        Xr(Ke)
      }
    }(), nt > 0 || (Ee.setStatus ? (Ee.setStatus("Running..."), setTimeout((function () {
      setTimeout((function () {
        Ee.setStatus("")
      }), 1), t()
    }), 1)) : t())))
  }

  function ya(e, t) {
    if (!t || !Fe || 0 !== e) {
      if (!t && Se) throw postMessage({cmd: "exitProcess", returnCode: e}), new va(e);
      Fe || (ft.terminateAllThreads(), e, Se || !0, Ee.onExit && Ee.onExit(e), je = !0), ke(e, new va(e))
    }
  }

  function wa() {
    for (we in {}, ye = {}, Ee = void 0 !== Ee ? Ee : {}) Ee.hasOwnProperty(we) && (ye[we] = Ee[we]);
    for (we in _e = [], be = "./this.program", ke = function (e, t) {
      throw t
    }, Te = !1, De = !0, !1, (Se = Ee.ENVIRONMENT_IS_PTHREAD || !1) && (Ge = Ee.buffer), Ce = "undefined" != typeof document && document.currentScript ? document.currentScript.src : void 0, De && (Ce = self.location.href), Oe = "", (Te || De) && (De ? Oe = self.location.href : "undefined" != typeof document && document.currentScript && (Oe = document.currentScript.src), Oe = 0 !== Oe.indexOf("blob:") ? Oe.substr(0, Oe.lastIndexOf("/") + 1) : "", Pe = function (e) {
      var t = new XMLHttpRequest;
      return t.open("GET", e, !1), t.send(null), t.responseText
    }, De && (xe = function (e) {
      var t = new XMLHttpRequest;
      return t.open("GET", e, !1), t.responseType = "arraybuffer", t.send(null), new Uint8Array(t.response)
    }), function (e, t, r) {
      var n = new XMLHttpRequest;
      n.open("GET", e, !0), n.responseType = "arraybuffer", n.onload = function () {
        200 == n.status || 0 == n.status && n.response ? t(n.response) : r()
      }, n.onerror = r, n.send(null)
    }, function (e) {
      document.title = e
    }), Be = Ee.print || console.log.bind(console), Me = Ee.printErr || console.warn.bind(console), ye) ye.hasOwnProperty(we) && (Ee[we] = ye[we]);
    if (ye = null, Ee.arguments && (_e = Ee.arguments), Ee.thisProgram && (be = Ee.thisProgram), Ee.quit && (ke = Ee.quit), Re = 16, 0, Le = function (e) {
      e
    }, Atomics.load, Atomics.store, Atomics.compareExchange, Ee.wasmBinary && (Ie = Ee.wasmBinary), Fe = Ee.noExitRuntime || !0, "object" != typeof WebAssembly && Yr("no native wasm support detected"), je = !1, qe = Ee.INITIAL_MEMORY || 67108864, Se) Ue = Ee.wasmMemory, Ge = Ee.buffer; else if (Ee.wasmMemory) Ue = Ee.wasmMemory; else if (!((Ue = new WebAssembly.Memory({
      initial: qe / 65536,
      maximum: qe / 65536,
      shared: !0
    })).buffer instanceof SharedArrayBuffer)) throw Me("requested a shared WebAssembly.Memory but the returned buffer is not a SharedArrayBuffer, indicating that while the browser has SharedArrayBuffer it does not have WebAssembly threads support - you may need to set a flag"), Error("bad memory");
    var t;
    if (Ue && (Ge = Ue.buffer), qe = Ge.byteLength, Ge = t = Ge, Ee.HEAP8 = ze = new Int8Array(t), Ee.HEAP16 = Ye = new Int16Array(t), Ee.HEAP32 = $e = new Int32Array(t), Ee.HEAPU8 = We = new Uint8Array(t), Ee.HEAPU16 = He = new Uint16Array(t), Ee.HEAPU32 = Ve = new Uint32Array(t), Ee.HEAPF32 = Qe = new Float32Array(t), Ee.HEAPF64 = Xe = new Float64Array(t), Ke = [], Je = [], et = [], tt = [], rt = [], !1, !1, Se || Je.push({
      func: function () {
        Zt()
      }
    }), Se && !0, nt = 0, ot = null, at = null, Ee.preloadedImages = {}, Ee.preloadedAudios = {}, it = "data:application/octet-stream;base64,", Hr(st = "ff.wasm") || (st = Sr(st)), lt = {
      155132: function () {
        throw"Canceled!"
      }, 155395: function (e, t) {
        setTimeout((function () {
          fr(e, t)
        }), 0)
      }
    }, dt = {
      EPERM: 63,
      ENOENT: 44,
      ESRCH: 71,
      EINTR: 27,
      EIO: 29,
      ENXIO: 60,
      E2BIG: 1,
      ENOEXEC: 45,
      EBADF: 8,
      ECHILD: 12,
      EAGAIN: 6,
      EWOULDBLOCK: 6,
      ENOMEM: 48,
      EACCES: 2,
      EFAULT: 21,
      ENOTBLK: 105,
      EBUSY: 10,
      EEXIST: 20,
      EXDEV: 75,
      ENODEV: 43,
      ENOTDIR: 54,
      EISDIR: 31,
      EINVAL: 28,
      ENFILE: 41,
      EMFILE: 33,
      ENOTTY: 59,
      ETXTBSY: 74,
      EFBIG: 22,
      ENOSPC: 51,
      ESPIPE: 70,
      EROFS: 69,
      EMLINK: 34,
      EPIPE: 64,
      EDOM: 18,
      ERANGE: 68,
      ENOMSG: 49,
      EIDRM: 24,
      ECHRNG: 106,
      EL2NSYNC: 156,
      EL3HLT: 107,
      EL3RST: 108,
      ELNRNG: 109,
      EUNATCH: 110,
      ENOCSI: 111,
      EL2HLT: 112,
      EDEADLK: 16,
      ENOLCK: 46,
      EBADE: 113,
      EBADR: 114,
      EXFULL: 115,
      ENOANO: 104,
      EBADRQC: 103,
      EBADSLT: 102,
      EDEADLOCK: 16,
      EBFONT: 101,
      ENOSTR: 100,
      ENODATA: 116,
      ETIME: 117,
      ENOSR: 118,
      ENONET: 119,
      ENOPKG: 120,
      EREMOTE: 121,
      ENOLINK: 47,
      EADV: 122,
      ESRMNT: 123,
      ECOMM: 124,
      EPROTO: 65,
      EMULTIHOP: 36,
      EDOTDOT: 125,
      EBADMSG: 9,
      ENOTUNIQ: 126,
      EBADFD: 127,
      EREMCHG: 128,
      ELIBACC: 129,
      ELIBBAD: 130,
      ELIBSCN: 131,
      ELIBMAX: 132,
      ELIBEXEC: 133,
      ENOSYS: 52,
      ENOTEMPTY: 55,
      ENAMETOOLONG: 37,
      ELOOP: 32,
      EOPNOTSUPP: 138,
      EPFNOSUPPORT: 139,
      ECONNRESET: 15,
      ENOBUFS: 42,
      EAFNOSUPPORT: 5,
      EPROTOTYPE: 67,
      ENOTSOCK: 57,
      ENOPROTOOPT: 50,
      ESHUTDOWN: 140,
      ECONNREFUSED: 14,
      EADDRINUSE: 3,
      ECONNABORTED: 13,
      ENETUNREACH: 40,
      ENETDOWN: 38,
      ETIMEDOUT: 73,
      EHOSTDOWN: 142,
      EHOSTUNREACH: 23,
      EINPROGRESS: 26,
      EALREADY: 7,
      EDESTADDRREQ: 17,
      EMSGSIZE: 35,
      EPROTONOSUPPORT: 66,
      ESOCKTNOSUPPORT: 137,
      EADDRNOTAVAIL: 4,
      ENETRESET: 39,
      EISCONN: 30,
      ENOTCONN: 53,
      ETOOMANYREFS: 141,
      EUSERS: 136,
      EDQUOT: 19,
      ESTALE: 72,
      ENOTSUP: 138,
      ENOMEDIUM: 148,
      EILSEQ: 25,
      EOVERFLOW: 61,
      ECANCELED: 11,
      ENOTRECOVERABLE: 56,
      EOWNERDEAD: 62,
      ESTRPIPE: 135
    }, Ee._emscripten_futex_wake = qr, ft = {
      unusedWorkers: [], runningWorkers: [], initMainThreadBlock: function () {
      }, initRuntime: function () {
        for (var e = Jt(228), t = 0; t < 57; ++t) Ve[e / 4 + t] = 0;
        $e[e + 12 >> 2] = e;
        var r = e + 152;
        $e[r >> 2] = r;
        var n = Jt(512);
        for (t = 0; t < 128; ++t) Ve[n / 4 + t] = 0;
        Atomics.store(Ve, e + 100 >> 2, n), Atomics.store(Ve, e + 40 >> 2, e), Ar(e, !De, 1), dr(e)
      }, initWorker: function () {
      }, pthreads: {}, threadExitHandlers: [], setThreadStatus: function () {
      }, runExitHandlers: function () {
        for (; ft.threadExitHandlers.length > 0;) ft.threadExitHandlers.pop()();
        Se && sr() && cr()
      }, threadExit: function (e) {
        var t = sr();
        t && (Atomics.store(Ve, t + 4 >> 2, e), Atomics.store(Ve, t + 0 >> 2, 1), Atomics.store(Ve, t + 56 >> 2, 1), Atomics.store(Ve, t + 60 >> 2, 0), ft.runExitHandlers(), qr(t + 0, 2147483647), Ar(0, 0, 0), Se && postMessage({cmd: "exit"}))
      }, threadCancel: function () {
        ft.runExitHandlers();
        var e = sr();
        Atomics.store(Ve, e + 4 >> 2, -1), Atomics.store(Ve, e + 0 >> 2, 1), qr(e + 0, 2147483647), Ar(0, 0, 0), postMessage({cmd: "cancelDone"})
      }, terminateAllThreads: function () {
        for (var e in ft.pthreads) {
          (n = ft.pthreads[e]) && n.worker && ft.returnWorkerToPool(n.worker)
        }
        ft.pthreads = {};
        for (var t = 0; t < ft.unusedWorkers.length; ++t) {
          (r = ft.unusedWorkers[t]).terminate()
        }
        ft.unusedWorkers = [];
        for (t = 0; t < ft.runningWorkers.length; ++t) {
          var r, n = (r = ft.runningWorkers[t]).pthread;
          ft.freeThreadData(n), r.terminate()
        }
        ft.runningWorkers = []
      }, freeThreadData: function (e) {
        if (e) {
          if (e.threadInfoStruct) {
            var t = $e[e.threadInfoStruct + 100 >> 2];
            $e[e.threadInfoStruct + 100 >> 2] = 0, Kt(t), Kt(e.threadInfoStruct)
          }
          e.threadInfoStruct = 0, e.allocatedOwnStack && e.stackBase && Kt(e.stackBase), e.stackBase = 0, e.worker && (e.worker.pthread = null)
        }
      }, returnWorkerToPool: function (e) {
        ft.runWithoutMainThreadQueuedCalls((function () {
          delete ft.pthreads[e.pthread.threadInfoStruct], ft.unusedWorkers.push(e), ft.runningWorkers.splice(ft.runningWorkers.indexOf(e), 1), ft.freeThreadData(e.pthread), e.pthread = void 0
        }))
      }, runWithoutMainThreadQueuedCalls: function (e) {
        $e[_r >> 2] = 0;
        try {
          e()
        } finally {
          $e[_r >> 2] = 1
        }
      }, receiveObjectTransfer: function (e) {
      }, loadWasmModuleToWorker: function (e, t) {
        e.onmessage = function (r) {
          var n = r.data, o = n.cmd;
          if (e.pthread && (ft.currentProxiedOperationCallerThread = e.pthread.threadInfoStruct), n.targetThread && n.targetThread != sr()) {
            var a = ft.pthreads[n.targetThread];
            return a ? a.worker.postMessage(r.data, n.transferList) : console.error('Internal error! Worker sent a message "' + o + '" to target pthread ' + n.targetThread + ", but that thread no longer exists!"), void (ft.currentProxiedOperationCallerThread = void 0)
          }
          if ("processQueuedMainThreadWork" === o) lr(); else if ("spawnThread" === o) da(r.data); else if ("cleanupThread" === o) Zr(n.thread); else if ("killThread" === o) !function (e) {
            if (Se) throw"Internal Error! killThread() can only ever be called from main application thread!";
            if (!e) throw"Internal Error! Null pthread_ptr in killThread!";
            $e[e + 12 >> 2] = 0;
            var t = ft.pthreads[e];
            t.worker.terminate(), ft.freeThreadData(t), ft.runningWorkers.splice(ft.runningWorkers.indexOf(t.worker), 1), t.worker.pthread = void 0
          }(n.thread); else if ("cancelThread" === o) !function (e) {
            if (Se) throw"Internal Error! cancelThread() can only ever be called from main application thread!";
            if (!e) throw"Internal Error! Null pthread_ptr in cancelThread!";
            ft.pthreads[e].worker.postMessage({cmd: "cancel"})
          }(n.thread); else if ("loaded" === o) e.loaded = !0, t && t(e), e.runPthread && (e.runPthread(), delete e.runPthread); else if ("print" === o) Be("Thread " + n.threadId + ": " + n.text); else if ("printErr" === o) Me("Thread " + n.threadId + ": " + n.text); else if ("alert" === o) alert("Thread " + n.threadId + ": " + n.text); else if ("exit" === o) {
            e.pthread && Atomics.load(Ve, e.pthread.threadInfoStruct + 64 >> 2) && ft.returnWorkerToPool(e)
          } else if ("exitProcess" === o) try {
            ya(n.returnCode)
          } catch (r) {
            if (r instanceof va) return;
            throw r
          } else "cancelDone" === o ? ft.returnWorkerToPool(e) : "objectTransfer" === o ? ft.receiveObjectTransfer(r.data) : "setimmediate" === r.data.target ? e.postMessage(r.data) : Me("worker sent an unknown command " + o);
          ft.currentProxiedOperationCallerThread = void 0
        }, e.onerror = function (e) {
          Me("pthread sent an error! " + e.filename + ":" + e.lineno + ": " + e.message)
        }, e.postMessage({cmd: "load", urlOrBlob: Ee.mainScriptUrlOrBlob || Ce, wasmMemory: Ue, wasmModule: Ne})
      }, allocateUnusedWorker: function () {
        var e = Sr("ff.worker.js");
        ft.unusedWorkers.push(new Worker(e))
      }, getNewWorker: function () {
        return 0 == ft.unusedWorkers.length && (ft.allocateUnusedWorker(), ft.loadWasmModuleToWorker(ft.unusedWorkers[0])), ft.unusedWorkers.length > 0 ? ft.unusedWorkers.pop() : null
      }, busySpinWait: function (e) {
        for (var t = performance.now() + e; performance.now() < t;) ;
      }
    }, Ee.establishStackSpace = Kr, Ee.getNoExitRuntime = Jr, Ee.invokeEntryPoint = en, pt = Se ? function () {
      return performance.now() - Ee.__performance_now_clock_drift
    } : function () {
      return performance.now()
    }, ht = {
      splitPath: function (e) {
        return /^(\/?|)([\s\S]*?)((?:\.{1,2}|[^\/]+?|)(\.[^.\/]*|))(?:[\/]*)$/.exec(e).slice(1)
      }, normalizeArray: function (e, t) {
        for (var r = 0, n = e.length - 1; n >= 0; n--) {
          var o = e[n];
          "." === o ? e.splice(n, 1) : ".." === o ? (e.splice(n, 1), r++) : r && (e.splice(n, 1), r--)
        }
        if (t) for (; r; r--) e.unshift("..");
        return e
      }, normalize: function (e) {
        var t = "/" === e.charAt(0), r = "/" === e.substr(-1);
        return (e = ht.normalizeArray(e.split("/").filter((function (e) {
          return !!e
        })), !t).join("/")) || t || (e = "."), e && r && (e += "/"), (t ? "/" : "") + e
      }, dirname: function (e) {
        var t = ht.splitPath(e), r = t[0], n = t[1];
        return r || n ? (n && (n = n.substr(0, n.length - 1)), r + n) : "."
      }, basename: function (e) {
        if ("/" === e) return "/";
        var t = (e = (e = ht.normalize(e)).replace(/\/$/, "")).lastIndexOf("/");
        return -1 === t ? e : e.substr(t + 1)
      }, extname: function (e) {
        return ht.splitPath(e)[3]
      }, join: function () {
        var e = Array.prototype.slice.call(arguments, 0);
        return ht.normalize(e.join("/"))
      }, join2: function (e, t) {
        return ht.normalize(e + "/" + t)
      }
    }, mt = {
      resolve: function () {
        for (var e = "", t = !1, r = arguments.length - 1; r >= -1 && !t; r--) {
          var n = r >= 0 ? arguments[r] : vt.cwd();
          if ("string" != typeof n) throw new TypeError("Arguments to path.resolve must be strings");
          if (!n) return "";
          e = n + "/" + e, t = "/" === n.charAt(0)
        }
        return (t ? "/" : "") + (e = ht.normalizeArray(e.split("/").filter((function (e) {
          return !!e
        })), !t).join("/")) || "."
      }, relative: function (e, t) {
        function r(e) {
          for (var t = 0; t < e.length && "" === e[t]; t++) ;
          for (var r = e.length - 1; r >= 0 && "" === e[r]; r--) ;
          return t > r ? [] : e.slice(t, r - t + 1)
        }

        e = mt.resolve(e).substr(1), t = mt.resolve(t).substr(1);
        for (var n = r(e.split("/")), o = r(t.split("/")), a = Math.min(n.length, o.length), i = a, s = 0; s < a; s++) if (n[s] !== o[s]) {
          i = s;
          break
        }
        var u = [];
        for (s = i; s < n.length; s++) u.push("..");
        return (u = u.concat(o.slice(i))).join("/")
      }
    }, At = {
      ttys: [], init: function () {
      }, shutdown: function () {
      }, register: function (e, t) {
        At.ttys[e] = {input: [], output: [], ops: t}, vt.registerDevice(e, At.stream_ops)
      }, stream_ops: {
        open: function (e) {
          var t = At.ttys[e.node.rdev];
          if (!t) throw new vt.ErrnoError(43);
          e.tty = t, e.seekable = !1
        }, close: function (e) {
          e.tty.ops.flush(e.tty)
        }, flush: function (e) {
          e.tty.ops.flush(e.tty)
        }, read: function (e, t, r, n, o) {
          if (!e.tty || !e.tty.ops.get_char) throw new vt.ErrnoError(60);
          for (var a = 0, i = 0; i < n; i++) {
            var s;
            try {
              s = e.tty.ops.get_char(e.tty)
            } catch (e) {
              throw new vt.ErrnoError(29)
            }
            if (void 0 === s && 0 === a) throw new vt.ErrnoError(6);
            if (null == s) break;
            a++, t[r + i] = s
          }
          return a && (e.node.timestamp = Date.now()), a
        }, write: function (e, t, r, n, o) {
          if (!e.tty || !e.tty.ops.put_char) throw new vt.ErrnoError(60);
          try {
            for (var a = 0; a < n; a++) e.tty.ops.put_char(e.tty, t[r + a])
          } catch (e) {
            throw new vt.ErrnoError(29)
          }
          return n && (e.node.timestamp = Date.now()), a
        }
      }, default_tty_ops: {
        get_char: function (e) {
          if (!e.input.length) {
            var t = null;
            if ("undefined" != typeof window && "function" == typeof window.prompt ? null !== (t = window.prompt("Input: ")) && (t += "\n") : "function" == typeof readline && null !== (t = readline()) && (t += "\n"), !t) return null;
            e.input = ga(t, !0)
          }
          return e.input.shift()
        }, put_char: function (e, t) {
          null === t || 10 === t ? (Be(Pr(e.output, 0)), e.output = []) : 0 != t && e.output.push(t)
        }, flush: function (e) {
          e.output && e.output.length > 0 && (Be(Pr(e.output, 0)), e.output = [])
        }
      }, default_tty1_ops: {
        put_char: function (e, t) {
          null === t || 10 === t ? (Me(Pr(e.output, 0)), e.output = []) : 0 != t && e.output.push(t)
        }, flush: function (e) {
          e.output && e.output.length > 0 && (Me(Pr(e.output, 0)), e.output = [])
        }
      }
    }, gt = {
      ops_table: null, mount: function (e) {
        return gt.createNode(null, "/", 16895, 0)
      }, createNode: function (e, t, r, n) {
        if (vt.isBlkdev(r) || vt.isFIFO(r)) throw new vt.ErrnoError(63);
        gt.ops_table || (gt.ops_table = {
          dir: {
            node: {
              getattr: gt.node_ops.getattr,
              setattr: gt.node_ops.setattr,
              lookup: gt.node_ops.lookup,
              mknod: gt.node_ops.mknod,
              rename: gt.node_ops.rename,
              unlink: gt.node_ops.unlink,
              rmdir: gt.node_ops.rmdir,
              readdir: gt.node_ops.readdir,
              symlink: gt.node_ops.symlink
            }, stream: {llseek: gt.stream_ops.llseek}
          },
          file: {
            node: {getattr: gt.node_ops.getattr, setattr: gt.node_ops.setattr},
            stream: {
              llseek: gt.stream_ops.llseek,
              read: gt.stream_ops.read,
              write: gt.stream_ops.write,
              allocate: gt.stream_ops.allocate,
              mmap: gt.stream_ops.mmap,
              msync: gt.stream_ops.msync
            }
          },
          link: {
            node: {getattr: gt.node_ops.getattr, setattr: gt.node_ops.setattr, readlink: gt.node_ops.readlink},
            stream: {}
          },
          chrdev: {node: {getattr: gt.node_ops.getattr, setattr: gt.node_ops.setattr}, stream: vt.chrdev_stream_ops}
        });
        var o = vt.createNode(e, t, r, n);
        return vt.isDir(o.mode) ? (o.node_ops = gt.ops_table.dir.node, o.stream_ops = gt.ops_table.dir.stream, o.contents = {}) : vt.isFile(o.mode) ? (o.node_ops = gt.ops_table.file.node, o.stream_ops = gt.ops_table.file.stream, o.usedBytes = 0, o.contents = null) : vt.isLink(o.mode) ? (o.node_ops = gt.ops_table.link.node, o.stream_ops = gt.ops_table.link.stream) : vt.isChrdev(o.mode) && (o.node_ops = gt.ops_table.chrdev.node, o.stream_ops = gt.ops_table.chrdev.stream), o.timestamp = Date.now(), e && (e.contents[t] = o, e.timestamp = o.timestamp), o
      }, getFileDataAsTypedArray: function (e) {
        return e.contents ? e.contents.subarray ? e.contents.subarray(0, e.usedBytes) : new Uint8Array(e.contents) : new Uint8Array(0)
      }, expandFileStorage: function (e, t) {
        var r = e.contents ? e.contents.length : 0;
        if (!(r >= t)) {
          t = Math.max(t, r * (r < 1048576 ? 2 : 1.125) >>> 0), 0 != r && (t = Math.max(t, 256));
          var n = e.contents;
          e.contents = new Uint8Array(t), e.usedBytes > 0 && e.contents.set(n.subarray(0, e.usedBytes), 0)
        }
      }, resizeFileStorage: function (e, t) {
        if (e.usedBytes != t) if (0 == t) e.contents = null, e.usedBytes = 0; else {
          var r = e.contents;
          e.contents = new Uint8Array(t), r && e.contents.set(r.subarray(0, Math.min(t, e.usedBytes))), e.usedBytes = t
        }
      }, node_ops: {
        getattr: function (e) {
          var t = {};
          return t.dev = vt.isChrdev(e.mode) ? e.id : 1, t.ino = e.id, t.mode = e.mode, t.nlink = 1, t.uid = 0, t.gid = 0, t.rdev = e.rdev, vt.isDir(e.mode) ? t.size = 4096 : vt.isFile(e.mode) ? t.size = e.usedBytes : vt.isLink(e.mode) ? t.size = e.link.length : t.size = 0, t.atime = new Date(e.timestamp), t.mtime = new Date(e.timestamp), t.ctime = new Date(e.timestamp), t.blksize = 4096, t.blocks = Math.ceil(t.size / t.blksize), t
        }, setattr: function (e, t) {
          void 0 !== t.mode && (e.mode = t.mode), void 0 !== t.timestamp && (e.timestamp = t.timestamp), void 0 !== t.size && gt.resizeFileStorage(e, t.size)
        }, lookup: function (e, t) {
          throw vt.genericErrors[44]
        }, mknod: function (e, t, r, n) {
          return gt.createNode(e, t, r, n)
        }, rename: function (e, t, r) {
          if (vt.isDir(e.mode)) {
            var n;
            try {
              n = vt.lookupNode(t, r)
            } catch (e) {
            }
            if (n) for (var o in n.contents) throw new vt.ErrnoError(55)
          }
          delete e.parent.contents[e.name], e.parent.timestamp = Date.now(), e.name = r, t.contents[r] = e, t.timestamp = e.parent.timestamp, e.parent = t
        }, unlink: function (e, t) {
          delete e.contents[t], e.timestamp = Date.now()
        }, rmdir: function (e, t) {
          var r = vt.lookupNode(e, t);
          for (var n in r.contents) throw new vt.ErrnoError(55);
          delete e.contents[t], e.timestamp = Date.now()
        }, readdir: function (e) {
          var t = [".", ".."];
          for (var r in e.contents) e.contents.hasOwnProperty(r) && t.push(r);
          return t
        }, symlink: function (e, t, r) {
          var n = gt.createNode(e, t, 41471, 0);
          return n.link = r, n
        }, readlink: function (e) {
          if (!vt.isLink(e.mode)) throw new vt.ErrnoError(28);
          return e.link
        }
      }, stream_ops: {
        read: function (e, t, r, n, o) {
          var a = e.node.contents;
          if (o >= e.node.usedBytes) return 0;
          var i = Math.min(e.node.usedBytes - o, n);
          if (i > 8 && a.subarray) t.set(a.subarray(o, o + i), r); else for (var s = 0; s < i; s++) t[r + s] = a[o + s];
          return i
        }, write: function (e, t, r, n, o, a) {
          if (!n) return 0;
          var i = e.node;
          if (i.timestamp = Date.now(), t.subarray && (!i.contents || i.contents.subarray)) {
            if (a) return i.contents = t.subarray(r, r + n), i.usedBytes = n, n;
            if (0 === i.usedBytes && 0 === o) return i.contents = t.slice(r, r + n), i.usedBytes = n, n;
            if (o + n <= i.usedBytes) return i.contents.set(t.subarray(r, r + n), o), n
          }
          if (gt.expandFileStorage(i, o + n), i.contents.subarray && t.subarray) i.contents.set(t.subarray(r, r + n), o); else for (var s = 0; s < n; s++) i.contents[o + s] = t[r + s];
          return i.usedBytes = Math.max(i.usedBytes, o + n), n
        }, llseek: function (e, t, r) {
          var n = t;
          if (1 === r ? n += e.position : 2 === r && vt.isFile(e.node.mode) && (n += e.node.usedBytes), n < 0) throw new vt.ErrnoError(28);
          return n
        }, allocate: function (e, t, r) {
          gt.expandFileStorage(e.node, t + r), e.node.usedBytes = Math.max(e.node.usedBytes, t + r)
        }, mmap: function (e, t, r, n, o, a) {
          if (0 !== t) throw new vt.ErrnoError(28);
          if (!vt.isFile(e.node.mode)) throw new vt.ErrnoError(43);
          var i, s, u = e.node.contents;
          if (2 & a || u.buffer !== Ge) {
            if ((n > 0 || n + r < u.length) && (u = u.subarray ? u.subarray(n, n + r) : Array.prototype.slice.call(u, n, n + r)), s = !0, !(i = on(r))) throw new vt.ErrnoError(48);
            ze.set(u, i)
          } else s = !1, i = u.byteOffset;
          return {ptr: i, allocated: s}
        }, msync: function (e, t, r, n, o) {
          if (!vt.isFile(e.node.mode)) throw new vt.ErrnoError(43);
          if (2 & o) return 0;
          gt.stream_ops.write(e, t, 0, n, r, !1);
          return 0
        }
      }
    }, vt = {
      root: null,
      mounts: [],
      devices: {},
      streams: [],
      nextInode: 1,
      nameTable: null,
      currentPath: "/",
      initialized: !1,
      ignorePermissions: !0,
      trackingDelegate: {},
      tracking: {openFlags: {READ: 1, WRITE: 2}},
      ErrnoError: null,
      genericErrors: {},
      filesystems: null,
      syncFSRequests: 0,
      lookupPath: function (e, t) {
        if (t = t || {}, !(e = mt.resolve(vt.cwd(), e))) return {path: "", node: null};
        var r = {follow_mount: !0, recurse_count: 0};
        for (var n in r) void 0 === t[n] && (t[n] = r[n]);
        if (t.recurse_count > 8) throw new vt.ErrnoError(32);
        for (var o = ht.normalizeArray(e.split("/").filter((function (e) {
          return !!e
        })), !1), a = vt.root, i = "/", s = 0; s < o.length; s++) {
          var u = s === o.length - 1;
          if (u && t.parent) break;
          if (a = vt.lookupNode(a, o[s]), i = ht.join2(i, o[s]), vt.isMountpoint(a) && (!u || u && t.follow_mount) && (a = a.mounted.root), !u || t.follow) for (var c = 0; vt.isLink(a.mode);) {
            var l = vt.readlink(i);
            if (i = mt.resolve(ht.dirname(i), l), a = vt.lookupPath(i, {recurse_count: t.recurse_count}).node, c++ > 40) throw new vt.ErrnoError(32)
          }
        }
        return {path: i, node: a}
      },
      getPath: function (e) {
        for (var t; ;) {
          if (vt.isRoot(e)) {
            var r = e.mount.mountpoint;
            return t ? "/" !== r[r.length - 1] ? r + "/" + t : r + t : r
          }
          t = t ? e.name + "/" + t : e.name, e = e.parent
        }
      },
      hashName: function (e, t) {
        for (var r = 0, n = 0; n < t.length; n++) r = (r << 5) - r + t.charCodeAt(n) | 0;
        return (e + r >>> 0) % vt.nameTable.length
      },
      hashAddNode: function (e) {
        var t = vt.hashName(e.parent.id, e.name);
        e.name_next = vt.nameTable[t], vt.nameTable[t] = e
      },
      hashRemoveNode: function (e) {
        var t = vt.hashName(e.parent.id, e.name);
        if (vt.nameTable[t] === e) vt.nameTable[t] = e.name_next; else for (var r = vt.nameTable[t]; r;) {
          if (r.name_next === e) {
            r.name_next = e.name_next;
            break
          }
          r = r.name_next
        }
      },
      lookupNode: function (e, t) {
        var r = vt.mayLookup(e);
        if (r) throw new vt.ErrnoError(r, e);
        for (var n = vt.hashName(e.id, t), o = vt.nameTable[n]; o; o = o.name_next) {
          var a = o.name;
          if (o.parent.id === e.id && a === t) return o
        }
        return vt.lookup(e, t)
      },
      createNode: function (e, t, r, n) {
        var o = new vt.FSNode(e, t, r, n);
        return vt.hashAddNode(o), o
      },
      destroyNode: function (e) {
        vt.hashRemoveNode(e)
      },
      isRoot: function (e) {
        return e === e.parent
      },
      isMountpoint: function (e) {
        return !!e.mounted
      },
      isFile: function (e) {
        return 32768 == (61440 & e)
      },
      isDir: function (e) {
        return 16384 == (61440 & e)
      },
      isLink: function (e) {
        return 40960 == (61440 & e)
      },
      isChrdev: function (e) {
        return 8192 == (61440 & e)
      },
      isBlkdev: function (e) {
        return 24576 == (61440 & e)
      },
      isFIFO: function (e) {
        return 4096 == (61440 & e)
      },
      isSocket: function (e) {
        return 49152 == (49152 & e)
      },
      flagModes: {r: 0, "r+": 2, w: 577, "w+": 578, a: 1089, "a+": 1090},
      modeStringToFlags: function (e) {
        var t = vt.flagModes[e];
        if (void 0 === t) throw new Error("Unknown file open mode: " + e);
        return t
      },
      flagsToPermissionString: function (e) {
        var t = ["r", "w", "rw"][3 & e];
        return 512 & e && (t += "w"), t
      },
      nodePermissions: function (e, t) {
        return vt.ignorePermissions || (-1 === t.indexOf("r") || 292 & e.mode) && (-1 === t.indexOf("w") || 146 & e.mode) && (-1 === t.indexOf("x") || 73 & e.mode) ? 0 : 2
      },
      mayLookup: function (e) {
        var t = vt.nodePermissions(e, "x");
        return t || (e.node_ops.lookup ? 0 : 2)
      },
      mayCreate: function (e, t) {
        try {
          vt.lookupNode(e, t);
          return 20
        } catch (e) {
        }
        return vt.nodePermissions(e, "wx")
      },
      mayDelete: function (e, t, r) {
        var n;
        try {
          n = vt.lookupNode(e, t)
        } catch (e) {
          return e.errno
        }
        var o = vt.nodePermissions(e, "wx");
        if (o) return o;
        if (r) {
          if (!vt.isDir(n.mode)) return 54;
          if (vt.isRoot(n) || vt.getPath(n) === vt.cwd()) return 10
        } else if (vt.isDir(n.mode)) return 31;
        return 0
      },
      mayOpen: function (e, t) {
        return e ? vt.isLink(e.mode) ? 32 : vt.isDir(e.mode) && ("r" !== vt.flagsToPermissionString(t) || 512 & t) ? 31 : vt.nodePermissions(e, vt.flagsToPermissionString(t)) : 44
      },
      MAX_OPEN_FDS: 4096,
      nextfd: function (e, t) {
        e = e || 0, t = t || vt.MAX_OPEN_FDS;
        for (var r = e; r <= t; r++) if (!vt.streams[r]) return r;
        throw new vt.ErrnoError(33)
      },
      getStream: function (e) {
        return vt.streams[e]
      },
      createStream: function (e, t, r) {
        vt.FSStream || (vt.FSStream = function () {
        }, vt.FSStream.prototype = {
          object: {
            get: function () {
              return this.node
            }, set: function (e) {
              this.node = e
            }
          }, isRead: {
            get: function () {
              return 1 != (2097155 & this.flags)
            }
          }, isWrite: {
            get: function () {
              return 0 != (2097155 & this.flags)
            }
          }, isAppend: {
            get: function () {
              return 1024 & this.flags
            }
          }
        });
        var n = new vt.FSStream;
        for (var o in e) n[o] = e[o];
        e = n;
        var a = vt.nextfd(t, r);
        return e.fd = a, vt.streams[a] = e, e
      },
      closeStream: function (e) {
        vt.streams[e] = null
      },
      chrdev_stream_ops: {
        open: function (e) {
          var t = vt.getDevice(e.node.rdev);
          e.stream_ops = t.stream_ops, e.stream_ops.open && e.stream_ops.open(e)
        }, llseek: function () {
          throw new vt.ErrnoError(70)
        }
      },
      major: function (e) {
        return e >> 8
      },
      minor: function (e) {
        return 255 & e
      },
      makedev: function (e, t) {
        return e << 8 | t
      },
      registerDevice: function (e, t) {
        vt.devices[e] = {stream_ops: t}
      },
      getDevice: function (e) {
        return vt.devices[e]
      },
      getMounts: function (e) {
        for (var t = [], r = [e]; r.length;) {
          var n = r.pop();
          t.push(n), r.push.apply(r, n.mounts)
        }
        return t
      },
      syncfs: function (e, t) {
        "function" == typeof e && (t = e, e = !1), vt.syncFSRequests++, vt.syncFSRequests > 1 && Me("warning: " + vt.syncFSRequests + " FS.syncfs operations in flight at once, probably just doing extra work");
        var r = vt.getMounts(vt.root.mount), n = 0;

        function o(e) {
          return vt.syncFSRequests--, t(e)
        }

        function a(e) {
          if (e) return a.errored ? void 0 : (a.errored = !0, o(e));
          ++n >= r.length && o(null)
        }

        r.forEach((function (t) {
          if (!t.type.syncfs) return a(null);
          t.type.syncfs(t, e, a)
        }))
      },
      mount: function (e, t, r) {
        var n, o = "/" === r, a = !r;
        if (o && vt.root) throw new vt.ErrnoError(10);
        if (!o && !a) {
          var i = vt.lookupPath(r, {follow_mount: !1});
          if (r = i.path, n = i.node, vt.isMountpoint(n)) throw new vt.ErrnoError(10);
          if (!vt.isDir(n.mode)) throw new vt.ErrnoError(54)
        }
        var s = {type: e, opts: t, mountpoint: r, mounts: []}, u = e.mount(s);
        return u.mount = s, s.root = u, o ? vt.root = u : n && (n.mounted = s, n.mount && n.mount.mounts.push(s)), u
      },
      unmount: function (e) {
        var t = vt.lookupPath(e, {follow_mount: !1});
        if (!vt.isMountpoint(t.node)) throw new vt.ErrnoError(28);
        var r = t.node, n = r.mounted, o = vt.getMounts(n);
        Object.keys(vt.nameTable).forEach((function (e) {
          for (var t = vt.nameTable[e]; t;) {
            var r = t.name_next;
            -1 !== o.indexOf(t.mount) && vt.destroyNode(t), t = r
          }
        })), r.mounted = null;
        var a = r.mount.mounts.indexOf(n);
        r.mount.mounts.splice(a, 1)
      },
      lookup: function (e, t) {
        return e.node_ops.lookup(e, t)
      },
      mknod: function (e, t, r) {
        var n = vt.lookupPath(e, {parent: !0}).node, o = ht.basename(e);
        if (!o || "." === o || ".." === o) throw new vt.ErrnoError(28);
        var a = vt.mayCreate(n, o);
        if (a) throw new vt.ErrnoError(a);
        if (!n.node_ops.mknod) throw new vt.ErrnoError(63);
        return n.node_ops.mknod(n, o, t, r)
      },
      create: function (e, t) {
        return t = void 0 !== t ? t : 438, t &= 4095, t |= 32768, vt.mknod(e, t, 0)
      },
      mkdir: function (e, t) {
        return t = void 0 !== t ? t : 511, t &= 1023, t |= 16384, vt.mknod(e, t, 0)
      },
      mkdirTree: function (e, t) {
        for (var r = e.split("/"), n = "", o = 0; o < r.length; ++o) if (r[o]) {
          n += "/" + r[o];
          try {
            vt.mkdir(n, t)
          } catch (e) {
            if (20 != e.errno) throw e
          }
        }
      },
      mkdev: function (e, t, r) {
        return void 0 === r && (r = t, t = 438), t |= 8192, vt.mknod(e, t, r)
      },
      symlink: function (e, t) {
        if (!mt.resolve(e)) throw new vt.ErrnoError(44);
        var r = vt.lookupPath(t, {parent: !0}).node;
        if (!r) throw new vt.ErrnoError(44);
        var n = ht.basename(t), o = vt.mayCreate(r, n);
        if (o) throw new vt.ErrnoError(o);
        if (!r.node_ops.symlink) throw new vt.ErrnoError(63);
        return r.node_ops.symlink(r, n, e)
      },
      rename: function (e, t) {
        var r, n, o = ht.dirname(e), a = ht.dirname(t), i = ht.basename(e), s = ht.basename(t);
        if (r = vt.lookupPath(e, {parent: !0}).node, n = vt.lookupPath(t, {parent: !0}).node, !r || !n) throw new vt.ErrnoError(44);
        if (r.mount !== n.mount) throw new vt.ErrnoError(75);
        var u, c = vt.lookupNode(r, i), l = mt.relative(e, a);
        if ("." !== l.charAt(0)) throw new vt.ErrnoError(28);
        if ("." !== (l = mt.relative(t, o)).charAt(0)) throw new vt.ErrnoError(55);
        try {
          u = vt.lookupNode(n, s)
        } catch (e) {
        }
        if (c !== u) {
          var d = vt.isDir(c.mode), f = vt.mayDelete(r, i, d);
          if (f) throw new vt.ErrnoError(f);
          if (f = u ? vt.mayDelete(n, s, d) : vt.mayCreate(n, s)) throw new vt.ErrnoError(f);
          if (!r.node_ops.rename) throw new vt.ErrnoError(63);
          if (vt.isMountpoint(c) || u && vt.isMountpoint(u)) throw new vt.ErrnoError(10);
          if (n !== r && (f = vt.nodePermissions(r, "w"))) throw new vt.ErrnoError(f);
          try {
            vt.trackingDelegate.willMovePath && vt.trackingDelegate.willMovePath(e, t)
          } catch (r) {
            Me("FS.trackingDelegate['willMovePath']('" + e + "', '" + t + "') threw an exception: " + r.message)
          }
          vt.hashRemoveNode(c);
          try {
            r.node_ops.rename(c, n, s)
          } catch (e) {
            throw e
          } finally {
            vt.hashAddNode(c)
          }
          try {
            vt.trackingDelegate.onMovePath && vt.trackingDelegate.onMovePath(e, t)
          } catch (r) {
            Me("FS.trackingDelegate['onMovePath']('" + e + "', '" + t + "') threw an exception: " + r.message)
          }
        }
      },
      rmdir: function (e) {
        var t = vt.lookupPath(e, {parent: !0}).node, r = ht.basename(e), n = vt.lookupNode(t, r),
          o = vt.mayDelete(t, r, !0);
        if (o) throw new vt.ErrnoError(o);
        if (!t.node_ops.rmdir) throw new vt.ErrnoError(63);
        if (vt.isMountpoint(n)) throw new vt.ErrnoError(10);
        try {
          vt.trackingDelegate.willDeletePath && vt.trackingDelegate.willDeletePath(e)
        } catch (t) {
          Me("FS.trackingDelegate['willDeletePath']('" + e + "') threw an exception: " + t.message)
        }
        t.node_ops.rmdir(t, r), vt.destroyNode(n);
        try {
          vt.trackingDelegate.onDeletePath && vt.trackingDelegate.onDeletePath(e)
        } catch (t) {
          Me("FS.trackingDelegate['onDeletePath']('" + e + "') threw an exception: " + t.message)
        }
      },
      readdir: function (e) {
        var t = vt.lookupPath(e, {follow: !0}).node;
        if (!t.node_ops.readdir) throw new vt.ErrnoError(54);
        return t.node_ops.readdir(t)
      },
      unlink: function (e) {
        var t = vt.lookupPath(e, {parent: !0}).node, r = ht.basename(e), n = vt.lookupNode(t, r),
          o = vt.mayDelete(t, r, !1);
        if (o) throw new vt.ErrnoError(o);
        if (!t.node_ops.unlink) throw new vt.ErrnoError(63);
        if (vt.isMountpoint(n)) throw new vt.ErrnoError(10);
        try {
          vt.trackingDelegate.willDeletePath && vt.trackingDelegate.willDeletePath(e)
        } catch (t) {
          Me("FS.trackingDelegate['willDeletePath']('" + e + "') threw an exception: " + t.message)
        }
        t.node_ops.unlink(t, r), vt.destroyNode(n);
        try {
          vt.trackingDelegate.onDeletePath && vt.trackingDelegate.onDeletePath(e)
        } catch (t) {
          Me("FS.trackingDelegate['onDeletePath']('" + e + "') threw an exception: " + t.message)
        }
      },
      readlink: function (e) {
        var t = vt.lookupPath(e).node;
        if (!t) throw new vt.ErrnoError(44);
        if (!t.node_ops.readlink) throw new vt.ErrnoError(28);
        return mt.resolve(vt.getPath(t.parent), t.node_ops.readlink(t))
      },
      stat: function (e, t) {
        var r = vt.lookupPath(e, {follow: !t}).node;
        if (!r) throw new vt.ErrnoError(44);
        if (!r.node_ops.getattr) throw new vt.ErrnoError(63);
        return r.node_ops.getattr(r)
      },
      lstat: function (e) {
        return vt.stat(e, !0)
      },
      chmod: function (e, t, r) {
        var n;
        "string" == typeof e ? n = vt.lookupPath(e, {follow: !r}).node : n = e;
        if (!n.node_ops.setattr) throw new vt.ErrnoError(63);
        n.node_ops.setattr(n, {mode: 4095 & t | -4096 & n.mode, timestamp: Date.now()})
      },
      lchmod: function (e, t) {
        vt.chmod(e, t, !0)
      },
      fchmod: function (e, t) {
        var r = vt.getStream(e);
        if (!r) throw new vt.ErrnoError(8);
        vt.chmod(r.node, t)
      },
      chown: function (e, t, r, n) {
        var o;
        "string" == typeof e ? o = vt.lookupPath(e, {follow: !n}).node : o = e;
        if (!o.node_ops.setattr) throw new vt.ErrnoError(63);
        o.node_ops.setattr(o, {timestamp: Date.now()})
      },
      lchown: function (e, t, r) {
        vt.chown(e, t, r, !0)
      },
      fchown: function (e, t, r) {
        var n = vt.getStream(e);
        if (!n) throw new vt.ErrnoError(8);
        vt.chown(n.node, t, r)
      },
      truncate: function (e, t) {
        if (t < 0) throw new vt.ErrnoError(28);
        var r;
        "string" == typeof e ? r = vt.lookupPath(e, {follow: !0}).node : r = e;
        if (!r.node_ops.setattr) throw new vt.ErrnoError(63);
        if (vt.isDir(r.mode)) throw new vt.ErrnoError(31);
        if (!vt.isFile(r.mode)) throw new vt.ErrnoError(28);
        var n = vt.nodePermissions(r, "w");
        if (n) throw new vt.ErrnoError(n);
        r.node_ops.setattr(r, {size: t, timestamp: Date.now()})
      },
      ftruncate: function (e, t) {
        var r = vt.getStream(e);
        if (!r) throw new vt.ErrnoError(8);
        if (0 == (2097155 & r.flags)) throw new vt.ErrnoError(28);
        vt.truncate(r.node, t)
      },
      utime: function (e, t, r) {
        var n = vt.lookupPath(e, {follow: !0}).node;
        n.node_ops.setattr(n, {timestamp: Math.max(t, r)})
      },
      open: function (e, t, r, n, o) {
        if ("" === e) throw new vt.ErrnoError(44);
        var a;
        if (r = void 0 === r ? 438 : r, r = 64 & (t = "string" == typeof t ? vt.modeStringToFlags(t) : t) ? 4095 & r | 32768 : 0, "object" == typeof e) a = e; else {
          e = ht.normalize(e);
          try {
            a = vt.lookupPath(e, {follow: !(131072 & t)}).node
          } catch (e) {
          }
        }
        var i = !1;
        if (64 & t) if (a) {
          if (128 & t) throw new vt.ErrnoError(20)
        } else a = vt.mknod(e, r, 0), i = !0;
        if (!a) throw new vt.ErrnoError(44);
        if (vt.isChrdev(a.mode) && (t &= -513), 65536 & t && !vt.isDir(a.mode)) throw new vt.ErrnoError(54);
        if (!i) {
          var s = vt.mayOpen(a, t);
          if (s) throw new vt.ErrnoError(s)
        }
        512 & t && vt.truncate(a, 0), t &= -131713;
        var u = vt.createStream({
          node: a,
          path: vt.getPath(a),
          flags: t,
          seekable: !0,
          position: 0,
          stream_ops: a.stream_ops,
          ungotten: [],
          error: !1
        }, n, o);
        u.stream_ops.open && u.stream_ops.open(u), !Ee.logReadFiles || 1 & t || (vt.readFiles || (vt.readFiles = {}), e in vt.readFiles || (vt.readFiles[e] = 1, Me("FS.trackingDelegate error on read file: " + e)));
        try {
          if (vt.trackingDelegate.onOpenFile) {
            var c = 0;
            1 != (2097155 & t) && (c |= vt.tracking.openFlags.READ), 0 != (2097155 & t) && (c |= vt.tracking.openFlags.WRITE), vt.trackingDelegate.onOpenFile(e, c)
          }
        } catch (t) {
          Me("FS.trackingDelegate['onOpenFile']('" + e + "', flags) threw an exception: " + t.message)
        }
        return u
      },
      close: function (e) {
        if (vt.isClosed(e)) throw new vt.ErrnoError(8);
        e.getdents && (e.getdents = null);
        try {
          e.stream_ops.close && e.stream_ops.close(e)
        } catch (e) {
          throw e
        } finally {
          vt.closeStream(e.fd)
        }
        e.fd = null
      },
      isClosed: function (e) {
        return null === e.fd
      },
      llseek: function (e, t, r) {
        if (vt.isClosed(e)) throw new vt.ErrnoError(8);
        if (!e.seekable || !e.stream_ops.llseek) throw new vt.ErrnoError(70);
        if (0 != r && 1 != r && 2 != r) throw new vt.ErrnoError(28);
        return e.position = e.stream_ops.llseek(e, t, r), e.ungotten = [], e.position
      },
      read: function (e, t, r, n, o) {
        if (n < 0 || o < 0) throw new vt.ErrnoError(28);
        if (vt.isClosed(e)) throw new vt.ErrnoError(8);
        if (1 == (2097155 & e.flags)) throw new vt.ErrnoError(8);
        if (vt.isDir(e.node.mode)) throw new vt.ErrnoError(31);
        if (!e.stream_ops.read) throw new vt.ErrnoError(28);
        var a = void 0 !== o;
        if (a) {
          if (!e.seekable) throw new vt.ErrnoError(70)
        } else o = e.position;
        var i = e.stream_ops.read(e, t, r, n, o);
        return a || (e.position += i), i
      },
      write: function (e, t, r, n, o, a) {
        if (n < 0 || o < 0) throw new vt.ErrnoError(28);
        if (vt.isClosed(e)) throw new vt.ErrnoError(8);
        if (0 == (2097155 & e.flags)) throw new vt.ErrnoError(8);
        if (vt.isDir(e.node.mode)) throw new vt.ErrnoError(31);
        if (!e.stream_ops.write) throw new vt.ErrnoError(28);
        e.seekable && 1024 & e.flags && vt.llseek(e, 0, 2);
        var i = void 0 !== o;
        if (i) {
          if (!e.seekable) throw new vt.ErrnoError(70)
        } else o = e.position;
        var s = e.stream_ops.write(e, t, r, n, o, a);
        i || (e.position += s);
        try {
          e.path && vt.trackingDelegate.onWriteToFile && vt.trackingDelegate.onWriteToFile(e.path)
        } catch (t) {
          Me("FS.trackingDelegate['onWriteToFile']('" + e.path + "') threw an exception: " + t.message)
        }
        return s
      },
      allocate: function (e, t, r) {
        if (vt.isClosed(e)) throw new vt.ErrnoError(8);
        if (t < 0 || r <= 0) throw new vt.ErrnoError(28);
        if (0 == (2097155 & e.flags)) throw new vt.ErrnoError(8);
        if (!vt.isFile(e.node.mode) && !vt.isDir(e.node.mode)) throw new vt.ErrnoError(43);
        if (!e.stream_ops.allocate) throw new vt.ErrnoError(138);
        e.stream_ops.allocate(e, t, r)
      },
      mmap: function (e, t, r, n, o, a) {
        if (0 != (2 & o) && 0 == (2 & a) && 2 != (2097155 & e.flags)) throw new vt.ErrnoError(2);
        if (1 == (2097155 & e.flags)) throw new vt.ErrnoError(2);
        if (!e.stream_ops.mmap) throw new vt.ErrnoError(43);
        return e.stream_ops.mmap(e, t, r, n, o, a)
      },
      msync: function (e, t, r, n, o) {
        return e && e.stream_ops.msync ? e.stream_ops.msync(e, t, r, n, o) : 0
      },
      munmap: function (e) {
        return 0
      },
      ioctl: function (e, t, r) {
        if (!e.stream_ops.ioctl) throw new vt.ErrnoError(59);
        return e.stream_ops.ioctl(e, t, r)
      },
      readFile: function (e, t) {
        if ((t = t || {}).flags = t.flags || 0, t.encoding = t.encoding || "binary", "utf8" !== t.encoding && "binary" !== t.encoding) throw new Error('Invalid encoding type "' + t.encoding + '"');
        var r, n = vt.open(e, t.flags), o = vt.stat(e).size, a = new Uint8Array(o);
        return vt.read(n, a, 0, o, 0), "utf8" === t.encoding ? r = Pr(a, 0) : "binary" === t.encoding && (r = a), vt.close(n), r
      },
      writeFile: function (e, t, r) {
        (r = r || {}).flags = r.flags || 577;
        var n = vt.open(e, r.flags, r.mode);
        if ("string" == typeof t) {
          var o = new Uint8Array(Rr(t) + 1), a = Br(t, o, 0, o.length);
          vt.write(n, o, 0, a, void 0, r.canOwn)
        } else {
          if (!ArrayBuffer.isView(t)) throw new Error("Unsupported data type");
          vt.write(n, t, 0, t.byteLength, void 0, r.canOwn)
        }
        vt.close(n)
      },
      cwd: function () {
        return vt.currentPath
      },
      chdir: function (e) {
        var t = vt.lookupPath(e, {follow: !0});
        if (null === t.node) throw new vt.ErrnoError(44);
        if (!vt.isDir(t.node.mode)) throw new vt.ErrnoError(54);
        var r = vt.nodePermissions(t.node, "x");
        if (r) throw new vt.ErrnoError(r);
        vt.currentPath = t.path
      },
      createDefaultDirectories: function () {
        vt.mkdir("/tmp"), vt.mkdir("/home"), vt.mkdir("/home/web_user")
      },
      createDefaultDevices: function () {
        vt.mkdir("/dev"), vt.registerDevice(vt.makedev(1, 3), {
          read: function () {
            return 0
          }, write: function (e, t, r, n, o) {
            return n
          }
        }), vt.mkdev("/dev/null", vt.makedev(1, 3)), At.register(vt.makedev(5, 0), At.default_tty_ops), At.register(vt.makedev(6, 0), At.default_tty1_ops), vt.mkdev("/dev/tty", vt.makedev(5, 0)), vt.mkdev("/dev/tty1", vt.makedev(6, 0));
        var e = function () {
          if ("object" == typeof crypto && "function" == typeof crypto.getRandomValues) {
            var e = new Uint8Array(1);
            return function () {
              return crypto.getRandomValues(e), e[0]
            }
          }
          return function () {
            Yr("randomDevice")
          }
        }();
        vt.createDevice("/dev", "random", e), vt.createDevice("/dev", "urandom", e), vt.mkdir("/dev/shm"), vt.mkdir("/dev/shm/tmp")
      },
      createSpecialDirectories: function () {
        vt.mkdir("/proc");
        var e = vt.mkdir("/proc/self");
        vt.mkdir("/proc/self/fd"), vt.mount({
          mount: function () {
            var t = vt.createNode(e, "fd", 16895, 73);
            return t.node_ops = {
              lookup: function (e, t) {
                var r = +t, n = vt.getStream(r);
                if (!n) throw new vt.ErrnoError(8);
                var o = {
                  parent: null, mount: {mountpoint: "fake"}, node_ops: {
                    readlink: function () {
                      return n.path
                    }
                  }
                };
                return o.parent = o, o
              }
            }, t
          }
        }, {}, "/proc/self/fd")
      },
      createStandardStreams: function () {
        Ee.stdin ? vt.createDevice("/dev", "stdin", Ee.stdin) : vt.symlink("/dev/tty", "/dev/stdin"), Ee.stdout ? vt.createDevice("/dev", "stdout", null, Ee.stdout) : vt.symlink("/dev/tty", "/dev/stdout"), Ee.stderr ? vt.createDevice("/dev", "stderr", null, Ee.stderr) : vt.symlink("/dev/tty1", "/dev/stderr");
        vt.open("/dev/stdin", 0), vt.open("/dev/stdout", 1), vt.open("/dev/stderr", 1)
      },
      ensureErrnoError: function () {
        vt.ErrnoError || (vt.ErrnoError = function (e, t) {
          this.node = t, this.setErrno = function (e) {
            this.errno = e
          }, this.setErrno(e), this.message = "FS error"
        }, vt.ErrnoError.prototype = new Error, vt.ErrnoError.prototype.constructor = vt.ErrnoError, [44].forEach((function (e) {
          vt.genericErrors[e] = new vt.ErrnoError(e), vt.genericErrors[e].stack = "<generic error, no stack>"
        })))
      },
      staticInit: function () {
        vt.ensureErrnoError(), vt.nameTable = new Array(4096), vt.mount(gt, {}, "/"), vt.createDefaultDirectories(), vt.createDefaultDevices(), vt.createSpecialDirectories(), vt.filesystems = {MEMFS: gt}
      },
      init: function (e, t, r) {
        vt.init.initialized = !0, vt.ensureErrnoError(), Ee.stdin = e || Ee.stdin, Ee.stdout = t || Ee.stdout, Ee.stderr = r || Ee.stderr, vt.createStandardStreams()
      },
      quit: function () {
        vt.init.initialized = !1;
        var e = Ee._fflush;
        e && e(0);
        for (var t = 0; t < vt.streams.length; t++) {
          var r = vt.streams[t];
          r && vt.close(r)
        }
      },
      getMode: function (e, t) {
        var r = 0;
        return e && (r |= 365), t && (r |= 146), r
      },
      findObject: function (e, t) {
        var r = vt.analyzePath(e, t);
        return r.exists ? r.object : null
      },
      analyzePath: function (e, t) {
        try {
          e = (n = vt.lookupPath(e, {follow: !t})).path
        } catch (e) {
        }
        var r = {
          isRoot: !1,
          exists: !1,
          error: 0,
          name: null,
          path: null,
          object: null,
          parentExists: !1,
          parentPath: null,
          parentObject: null
        };
        try {
          var n = vt.lookupPath(e, {parent: !0});
          r.parentExists = !0, r.parentPath = n.path, r.parentObject = n.node, r.name = ht.basename(e), n = vt.lookupPath(e, {follow: !t}), r.exists = !0, r.path = n.path, r.object = n.node, r.name = n.node.name, r.isRoot = "/" === n.path
        } catch (e) {
          r.error = e.errno
        }
        return r
      },
      createPath: function (e, t, r, n) {
        e = "string" == typeof e ? e : vt.getPath(e);
        for (var o = t.split("/").reverse(); o.length;) {
          var a = o.pop();
          if (a) {
            var i = ht.join2(e, a);
            try {
              vt.mkdir(i)
            } catch (e) {
            }
            e = i
          }
        }
        return i
      },
      createFile: function (e, t, r, n, o) {
        var a = ht.join2("string" == typeof e ? e : vt.getPath(e), t), i = vt.getMode(n, o);
        return vt.create(a, i)
      },
      createDataFile: function (e, t, r, n, o, a) {
        var i = t ? ht.join2("string" == typeof e ? e : vt.getPath(e), t) : e, s = vt.getMode(n, o),
          u = vt.create(i, s);
        if (r) {
          if ("string" == typeof r) {
            for (var c = new Array(r.length), l = 0, d = r.length; l < d; ++l) c[l] = r.charCodeAt(l);
            r = c
          }
          vt.chmod(u, 146 | s);
          var f = vt.open(u, 577);
          vt.write(f, r, 0, r.length, 0, a), vt.close(f), vt.chmod(u, s)
        }
        return u
      },
      createDevice: function (e, t, r, n) {
        var o = ht.join2("string" == typeof e ? e : vt.getPath(e), t), a = vt.getMode(!!r, !!n);
        vt.createDevice.major || (vt.createDevice.major = 64);
        var i = vt.makedev(vt.createDevice.major++, 0);
        return vt.registerDevice(i, {
          open: function (e) {
            e.seekable = !1
          }, close: function (e) {
            n && n.buffer && n.buffer.length && n(10)
          }, read: function (e, t, n, o, a) {
            for (var i = 0, s = 0; s < o; s++) {
              var u;
              try {
                u = r()
              } catch (e) {
                throw new vt.ErrnoError(29)
              }
              if (void 0 === u && 0 === i) throw new vt.ErrnoError(6);
              if (null == u) break;
              i++, t[n + s] = u
            }
            return i && (e.node.timestamp = Date.now()), i
          }, write: function (e, t, r, o, a) {
            for (var i = 0; i < o; i++) try {
              n(t[r + i])
            } catch (e) {
              throw new vt.ErrnoError(29)
            }
            return o && (e.node.timestamp = Date.now()), i
          }
        }), vt.mkdev(o, a, i)
      },
      forceLoadFile: function (e) {
        if (e.isDevice || e.isFolder || e.link || e.contents) return !0;
        if ("undefined" != typeof XMLHttpRequest) throw new Error("Lazy loading should have been performed (contents set) in createLazyFile, but it was not. Lazy loading only works in web workers. Use --embed-file or --preload-file in emcc on the main thread.");
        if (!Pe) throw new Error("Cannot load without read() or XMLHttpRequest.");
        try {
          e.contents = ga(Pe(e.url), !0), e.usedBytes = e.contents.length
        } catch (e) {
          throw new vt.ErrnoError(29)
        }
      },
      createLazyFile: function (e, t, r, n, o) {
        function a() {
          this.lengthKnown = !1, this.chunks = []
        }

        if (a.prototype.get = function (e) {
          if (!(e > this.length - 1 || e < 0)) {
            var t = e % this.chunkSize, r = e / this.chunkSize | 0;
            return this.getter(r)[t]
          }
        }, a.prototype.setDataGetter = function (e) {
          this.getter = e
        }, a.prototype.cacheLength = function () {
          var e = new XMLHttpRequest;
          if (e.open("HEAD", r, !1), e.send(null), !(e.status >= 200 && e.status < 300 || 304 === e.status)) throw new Error("Couldn't load " + r + ". Status: " + e.status);
          var t, n = Number(e.getResponseHeader("Content-length")),
            o = (t = e.getResponseHeader("Accept-Ranges")) && "bytes" === t,
            a = (t = e.getResponseHeader("Content-Encoding")) && "gzip" === t, i = 1048576;
          o || (i = n);
          var s = this;
          s.setDataGetter((function (e) {
            var t = e * i, o = (e + 1) * i - 1;
            if (o = Math.min(o, n - 1), void 0 === s.chunks[e] && (s.chunks[e] = function (e, t) {
              if (e > t) throw new Error("invalid range (" + e + ", " + t + ") or no bytes requested!");
              if (t > n - 1) throw new Error("only " + n + " bytes available! programmer error!");
              var o = new XMLHttpRequest;
              if (o.open("GET", r, !1), n !== i && o.setRequestHeader("Range", "bytes=" + e + "-" + t), "undefined" != typeof Uint8Array && (o.responseType = "arraybuffer"), o.overrideMimeType && o.overrideMimeType("text/plain; charset=x-user-defined"), o.send(null), !(o.status >= 200 && o.status < 300 || 304 === o.status)) throw new Error("Couldn't load " + r + ". Status: " + o.status);
              return void 0 !== o.response ? new Uint8Array(o.response || []) : ga(o.responseText || "", !0)
            }(t, o)), void 0 === s.chunks[e]) throw new Error("doXHR failed!");
            return s.chunks[e]
          })), !a && n || (i = n = 1, n = this.getter(0).length, i = n, Be("LazyFiles on gzip forces download of the whole file when length is accessed")), this._length = n, this._chunkSize = i, this.lengthKnown = !0
        }, "undefined" != typeof XMLHttpRequest) {
          if (!De) throw"Cannot do synchronous binary XHRs outside webworkers in modern browsers. Use --embed-file or --preload-file in emcc";
          var i = new a;
          Object.defineProperties(i, {
            length: {
              get: function () {
                return this.lengthKnown || this.cacheLength(), this._length
              }
            }, chunkSize: {
              get: function () {
                return this.lengthKnown || this.cacheLength(), this._chunkSize
              }
            }
          });
          var s = {isDevice: !1, contents: i}
        } else s = {isDevice: !1, url: r};
        var u = vt.createFile(e, t, s, n, o);
        s.contents ? u.contents = s.contents : s.url && (u.contents = null, u.url = s.url), Object.defineProperties(u, {
          usedBytes: {
            get: function () {
              return this.contents.length
            }
          }
        });
        var c = {};
        return Object.keys(u.stream_ops).forEach((function (e) {
          var t = u.stream_ops[e];
          c[e] = function () {
            return vt.forceLoadFile(u), t.apply(null, arguments)
          }
        })), c.read = function (e, t, r, n, o) {
          vt.forceLoadFile(u);
          var a = e.node.contents;
          if (o >= a.length) return 0;
          var i = Math.min(a.length - o, n);
          if (a.slice) for (var s = 0; s < i; s++) t[r + s] = a[o + s]; else for (s = 0; s < i; s++) t[r + s] = a.get(o + s);
          return i
        }, u.stream_ops = c, u
      },
      createPreloadedFile: function (e, t, r, n, o, a, i, s, u, c) {
        Browser.init();
        var l = t ? mt.resolve(ht.join2(e, t)) : e;

        function d(r) {
          function d(r) {
            c && c(), s || vt.createDataFile(e, t, r, n, o, u), a && a(), Wr()
          }

          var f = !1;
          Ee.preloadPlugins.forEach((function (e) {
            f || e.canHandle(l) && (e.handle(r, l, d, (function () {
              i && i(), Wr()
            })), f = !0)
          })), f || d(r)
        }

        zr(), "string" == typeof r ? Browser.asyncLoad(r, (function (e) {
          d(e)
        }), i) : d(r)
      },
      indexedDB: function () {
        return window.indexedDB || window.mozIndexedDB || window.webkitIndexedDB || window.msIndexedDB
      },
      DB_NAME: function () {
        return "EM_FS_" + window.location.pathname
      },
      DB_VERSION: 20,
      DB_STORE_NAME: "FILE_DATA",
      saveFilesToDB: function (e, t, r) {
        t = t || function () {
        }, r = r || function () {
        };
        var n = vt.indexedDB();
        try {
          var o = n.open(vt.DB_NAME(), vt.DB_VERSION)
        } catch (e) {
          return r(e)
        }
        o.onupgradeneeded = function () {
          Be("creating db"), o.result.createObjectStore(vt.DB_STORE_NAME)
        }, o.onsuccess = function () {
          var n = o.result.transaction([vt.DB_STORE_NAME], "readwrite"), a = n.objectStore(vt.DB_STORE_NAME), i = 0,
            s = 0, u = e.length;

          function c() {
            0 == s ? t() : r()
          }

          e.forEach((function (e) {
            var t = a.put(vt.analyzePath(e).object.contents, e);
            t.onsuccess = function () {
              ++i + s == u && c()
            }, t.onerror = function () {
              s++, i + s == u && c()
            }
          })), n.onerror = r
        }, o.onerror = r
      },
      loadFilesFromDB: function (e, t, r) {
        t = t || function () {
        }, r = r || function () {
        };
        var n = vt.indexedDB();
        try {
          var o = n.open(vt.DB_NAME(), vt.DB_VERSION)
        } catch (e) {
          return r(e)
        }
        o.onupgradeneeded = r, o.onsuccess = function () {
          var n = o.result;
          try {
            var a = n.transaction([vt.DB_STORE_NAME], "readonly")
          } catch (e) {
            return void r(e)
          }
          var i = a.objectStore(vt.DB_STORE_NAME), s = 0, u = 0, c = e.length;

          function l() {
            0 == u ? t() : r()
          }

          e.forEach((function (e) {
            var t = i.get(e);
            t.onsuccess = function () {
              vt.analyzePath(e).exists && vt.unlink(e), vt.createDataFile(ht.dirname(e), ht.basename(e), t.result, !0, !0, !0), ++s + u == c && l()
            }, t.onerror = function () {
              u++, s + u == c && l()
            }
          })), a.onerror = r
        }, o.onerror = r
      }
    }, Et = {
      mappings: {}, DEFAULT_POLLMASK: 5, umask: 511, calculateAt: function (e, t, r) {
        if ("/" === t[0]) return t;
        var n;
        if (-100 === e) n = vt.cwd(); else {
          var o = vt.getStream(e);
          if (!o) throw new vt.ErrnoError(8);
          n = o.path
        }
        if (0 == t.length) {
          if (!r) throw new vt.ErrnoError(44);
          return n
        }
        return ht.join2(n, t)
      }, doStat: function (e, t, r) {
        try {
          var n = e(t)
        } catch (e) {
          if (e && e.node && ht.normalize(t) !== ht.normalize(vt.getPath(e.node))) return -54;
          throw e
        }
        return $e[r >> 2] = n.dev, $e[r + 4 >> 2] = 0, $e[r + 8 >> 2] = n.ino, $e[r + 12 >> 2] = n.mode, $e[r + 16 >> 2] = n.nlink, $e[r + 20 >> 2] = n.uid, $e[r + 24 >> 2] = n.gid, $e[r + 28 >> 2] = n.rdev, $e[r + 32 >> 2] = 0, ct = [n.size >>> 0, (ut = n.size, +Math.abs(ut) >= 1 ? ut > 0 ? (0 | Math.min(+Math.floor(ut / 4294967296), 4294967295)) >>> 0 : ~~+Math.ceil((ut - +(~~ut >>> 0)) / 4294967296) >>> 0 : 0)], $e[r + 40 >> 2] = ct[0], $e[r + 44 >> 2] = ct[1], $e[r + 48 >> 2] = 4096, $e[r + 52 >> 2] = n.blocks, $e[r + 56 >> 2] = n.atime.getTime() / 1e3 | 0, $e[r + 60 >> 2] = 0, $e[r + 64 >> 2] = n.mtime.getTime() / 1e3 | 0, $e[r + 68 >> 2] = 0, $e[r + 72 >> 2] = n.ctime.getTime() / 1e3 | 0, $e[r + 76 >> 2] = 0, ct = [n.ino >>> 0, (ut = n.ino, +Math.abs(ut) >= 1 ? ut > 0 ? (0 | Math.min(+Math.floor(ut / 4294967296), 4294967295)) >>> 0 : ~~+Math.ceil((ut - +(~~ut >>> 0)) / 4294967296) >>> 0 : 0)], $e[r + 80 >> 2] = ct[0], $e[r + 84 >> 2] = ct[1], 0
      }, doMsync: function (e, t, r, n, o) {
        var a = We.slice(e, e + r);
        vt.msync(t, a, o, r, n)
      }, doMkdir: function (e, t) {
        return "/" === (e = ht.normalize(e))[e.length - 1] && (e = e.substr(0, e.length - 1)), vt.mkdir(e, t, 0), 0
      }, doMknod: function (e, t, r) {
        switch (61440 & t) {
          case 32768:
          case 8192:
          case 24576:
          case 4096:
          case 49152:
            break;
          default:
            return -28
        }
        return vt.mknod(e, t, r), 0
      }, doReadlink: function (e, t, r) {
        if (r <= 0) return -28;
        var n = vt.readlink(e), o = Math.min(r, Rr(n)), a = ze[t + o];
        return Mr(n, t, r + 1), ze[t + o] = a, o
      }, doAccess: function (e, t) {
        if (-8 & t) return -28;
        var r;
        if (!(r = vt.lookupPath(e, {follow: !0}).node)) return -44;
        var n = "";
        return 4 & t && (n += "r"), 2 & t && (n += "w"), 1 & t && (n += "x"), n && vt.nodePermissions(r, n) ? -2 : 0
      }, doDup: function (e, t, r) {
        var n = vt.getStream(r);
        return n && vt.close(n), vt.open(e, t, 0, r, r).fd
      }, doReadv: function (e, t, r, n) {
        for (var o = 0, a = 0; a < r; a++) {
          var i = $e[t + 8 * a >> 2], s = $e[t + (8 * a + 4) >> 2], u = vt.read(e, ze, i, s, n);
          if (u < 0) return -1;
          if (o += u, u < s) break
        }
        return o
      }, doWritev: function (e, t, r, n) {
        for (var o = 0, a = 0; a < r; a++) {
          var i = $e[t + 8 * a >> 2], s = $e[t + (8 * a + 4) >> 2], u = vt.write(e, ze, i, s, n);
          if (u < 0) return -1;
          o += u
        }
        return o
      }, varargs: void 0, get: function () {
        return Et.varargs += 4, $e[Et.varargs - 4 >> 2]
      }, getStr: function (e) {
        return xr(e)
      }, getStreamFromFD: function (e) {
        var t = vt.getStream(e);
        if (!t) throw new vt.ErrnoError(8);
        return t
      }, get64: function (e, t) {
        return e
      }
    }, yt = void 0, wt = {}, _t = {}, bt = {}, kt = 48, Tt = 57, Dt = void 0, St = void 0, Ct = !1, Ot = void 0, Pt = [], xt = {}, Bt = {}, Mt = void 0, Rt = [], Lt = [{}, {value: void 0}, {value: null}, {value: !0}, {value: !1}], It = {}, Ft = [], Ut = [], Nt = [], jt = {
      inEventHandler: 0, removeAllEventListeners: function () {
        for (var e = jt.eventHandlers.length - 1; e >= 0; --e) jt._removeHandler(e);
        jt.eventHandlers = [], jt.deferredCalls = []
      }, registerRemoveEventListeners: function () {
        jt.removeEventListenersRegistered || (tt.push(jt.removeAllEventListeners), jt.removeEventListenersRegistered = !0)
      }, deferredCalls: [], deferCall: function (e, t, r) {
        function n(e, t) {
          if (e.length != t.length) return !1;
          for (var r in e) if (e[r] != t[r]) return !1;
          return !0
        }

        for (var o in jt.deferredCalls) {
          var a = jt.deferredCalls[o];
          if (a.targetFunction == e && n(a.argsList, r)) return
        }
        jt.deferredCalls.push({targetFunction: e, precedence: t, argsList: r}), jt.deferredCalls.sort((function (e, t) {
          return e.precedence < t.precedence
        }))
      }, removeDeferredCalls: function (e) {
        for (var t = 0; t < jt.deferredCalls.length; ++t) jt.deferredCalls[t].targetFunction == e && (jt.deferredCalls.splice(t, 1), --t)
      }, canPerformEventHandlerRequests: function () {
        return jt.inEventHandler && jt.currentEventHandler.allowsDeferredCalls
      }, runDeferredCalls: function () {
        if (jt.canPerformEventHandlerRequests()) for (var e = 0; e < jt.deferredCalls.length; ++e) {
          var t = jt.deferredCalls[e];
          jt.deferredCalls.splice(e, 1), --e, t.targetFunction.apply(null, t.argsList)
        }
      }, eventHandlers: [], removeAllHandlersOnTarget: function (e, t) {
        for (var r = 0; r < jt.eventHandlers.length; ++r) jt.eventHandlers[r].target != e || t && t != jt.eventHandlers[r].eventTypeString || jt._removeHandler(r--)
      }, _removeHandler: function (e) {
        var t = jt.eventHandlers[e];
        t.target.removeEventListener(t.eventTypeString, t.eventListenerFunc, t.useCapture), jt.eventHandlers.splice(e, 1)
      }, registerOrRemoveHandler: function (e) {
        var t = function (t) {
          ++jt.inEventHandler, jt.currentEventHandler = e, jt.runDeferredCalls(), e.handlerFunc(t), jt.runDeferredCalls(), --jt.inEventHandler
        };
        if (e.callbackfunc) e.eventListenerFunc = t, e.target.addEventListener(e.eventTypeString, t, e.useCapture), jt.eventHandlers.push(e), jt.registerRemoveEventListeners(); else for (var r = 0; r < jt.eventHandlers.length; ++r) jt.eventHandlers[r].target == e.target && jt.eventHandlers[r].eventTypeString == e.eventTypeString && jt._removeHandler(r--)
      }, queueEventHandlerOnThread_iiii: function (e, t, r, n, o) {
        var a = gr(), i = Er(12);
        $e[i >> 2] = r, $e[i + 4 >> 2] = n, $e[i + 8 >> 2] = o, mr(0, e, 637534208, t, n, i), vr(a)
      }, getTargetThreadForEventCallback: function (e) {
        switch (e) {
          case 1:
            return 0;
          case 2:
            return ft.currentProxiedOperationCallerThread;
          default:
            return e
        }
      }, getNodeNameForTarget: function (e) {
        return e ? e == window ? "#window" : e == screen ? "#screen" : e && e.nodeName ? e.nodeName : "" : ""
      }, fullscreenEnabled: function () {
        return document.fullscreenEnabled || document.webkitFullscreenEnabled
      }
    }, Gt = [0, "undefined" != typeof document ? document : 0, "undefined" != typeof window ? window : 0], zt = {
      counter: 1,
      buffers: [],
      programs: [],
      framebuffers: [],
      renderbuffers: [],
      textures: [],
      uniforms: [],
      shaders: [],
      vaos: [],
      contexts: {},
      offscreenCanvases: {},
      timerQueriesEXT: [],
      programInfos: {},
      stringCache: {},
      unpackAlignment: 4,
      recordError: function (e) {
        zt.lastError || (zt.lastError = e)
      },
      getNewId: function (e) {
        for (var t = zt.counter++, r = e.length; r < t; r++) e[r] = null;
        return t
      },
      getSource: function (e, t, r, n) {
        for (var o = "", a = 0; a < t; ++a) {
          var i = n ? $e[n + 4 * a >> 2] : -1;
          o += xr($e[r + 4 * a >> 2], i < 0 ? void 0 : i)
        }
        return o
      },
      createContext: function (e, t) {
        var r = e.getContext("webgl", t);
        return r ? zt.registerContext(r, t) : 0
      },
      registerContext: function (e, t) {
        var r = Jt(8);
        $e[r + 4 >> 2] = sr();
        var n = {handle: r, attributes: t, version: t.majorVersion, GLctx: e};
        return e.canvas && (e.canvas.GLctxObject = n), zt.contexts[r] = n, (void 0 === t.enableExtensionsByDefault || t.enableExtensionsByDefault) && zt.initExtensions(n), r
      },
      makeContextCurrent: function (e) {
        return zt.currentContext = zt.contexts[e], Ee.ctx = Qt = zt.currentContext && zt.currentContext.GLctx, !(e && !Qt)
      },
      getContext: function (e) {
        return zt.contexts[e]
      },
      deleteContext: function (e) {
        zt.currentContext === zt.contexts[e] && (zt.currentContext = null), "object" == typeof jt && jt.removeAllHandlersOnTarget(zt.contexts[e].GLctx.canvas), zt.contexts[e] && zt.contexts[e].GLctx.canvas && (zt.contexts[e].GLctx.canvas.GLctxObject = void 0), Kt(zt.contexts[e].handle), zt.contexts[e] = null
      },
      initExtensions: function (e) {
        if (e || (e = zt.currentContext), !e.initExtensionsDone) {
          e.initExtensionsDone = !0;
          var t, r = e.GLctx;
          !function (e) {
            var t = e.getExtension("ANGLE_instanced_arrays");
            if (t) e.vertexAttribDivisor = function (e, r) {
              t.vertexAttribDivisorANGLE(e, r)
            }, e.drawArraysInstanced = function (e, r, n, o) {
              t.drawArraysInstancedANGLE(e, r, n, o)
            }, e.drawElementsInstanced = function (e, r, n, o, a) {
              t.drawElementsInstancedANGLE(e, r, n, o, a)
            }
          }(r), function (e) {
            var t = e.getExtension("OES_vertex_array_object");
            if (t) e.createVertexArray = function () {
              return t.createVertexArrayOES()
            }, e.deleteVertexArray = function (e) {
              t.deleteVertexArrayOES(e)
            }, e.bindVertexArray = function (e) {
              t.bindVertexArrayOES(e)
            }, e.isVertexArray = function (e) {
              return t.isVertexArrayOES(e)
            }
          }(r), function (e) {
            var t = e.getExtension("WEBGL_draw_buffers");
            if (t) e.drawBuffers = function (e, r) {
              t.drawBuffersWEBGL(e, r)
            }
          }(r), r.disjointTimerQueryExt = r.getExtension("EXT_disjoint_timer_query"), (t = r).multiDrawWebgl = t.getExtension("WEBGL_multi_draw"), (r.getSupportedExtensions() || []).forEach((function (e) {
            e.indexOf("lose_context") < 0 && e.indexOf("debug") < 0 && r.getExtension(e)
          }))
        }
      },
      populateUniformTable: function (e) {
        for (var t = zt.programs[e], r = zt.programInfos[e] = {
          uniforms: {},
          maxUniformLength: 0,
          maxAttributeLength: -1,
          maxUniformBlockNameLength: -1
        }, n = r.uniforms, o = Qt.getProgramParameter(t, 35718), a = 0; a < o; ++a) {
          var i = Qt.getActiveUniform(t, a), s = i.name;
          r.maxUniformLength = Math.max(r.maxUniformLength, s.length + 1), "]" == s.slice(-1) && (s = s.slice(0, s.lastIndexOf("[")));
          var u = Qt.getUniformLocation(t, s);
          if (u) {
            var c = zt.getNewId(zt.uniforms);
            n[s] = [i.size, c], zt.uniforms[c] = u;
            for (var l = 1; l < i.size; ++l) {
              var d = s + "[" + l + "]";
              u = Qt.getUniformLocation(t, d), c = zt.getNewId(zt.uniforms), zt.uniforms[c] = u
            }
          }
        }
      }
    }, Wt = ["default", "low-power", "high-performance"], Yt = {}, Se || ft.initMainThreadBlock(), Ht = function (e, t, r, n) {
      e || (e = this), this.parent = e, this.mount = e.mount, this.mounted = null, this.id = vt.nextInode++, this.name = t, this.mode = r, this.node_ops = {}, this.stream_ops = {}, this.rdev = n
    }, $t = 365, Vt = 146, Object.defineProperties(Ht.prototype, {
      read: {
        get: function () {
          return (this.mode & $t) === $t
        }, set: function (e) {
          e ? this.mode |= $t : this.mode &= -366
        }
      }, write: {
        get: function () {
          return (this.mode & Vt) === Vt
        }, set: function (e) {
          e ? this.mode |= Vt : this.mode &= -147
        }
      }, isFolder: {
        get: function () {
          return vt.isDir(this.mode)
        }
      }, isDevice: {
        get: function () {
          return vt.isChrdev(this.mode)
        }
      }
    }), vt.FSNode = Ht, vt.staticInit(), function () {
      for (var e = new Array(256), t = 0; t < 256; ++t) e[t] = String.fromCharCode(t);
      yt = e
    }(), Dt = Ee.BindingError = fn(Error, "BindingError"), St = Ee.InternalError = fn(Error, "InternalError"), Cn.prototype.isAliasOf = vn, Cn.prototype.clone = bn, Cn.prototype.delete = kn, Cn.prototype.isDeleted = Tn, Cn.prototype.deleteLater = Sn, $n.prototype.getPointee = In, $n.prototype.destructor = Fn, $n.prototype.argPackAdvance = 8, $n.prototype.readValueFromPointer = Ln, $n.prototype.deleteObject = Un, $n.prototype.fromWireType = Hn, Ee.getInheritedInstanceCount = jn, Ee.getLiveInheritedInstances = Gn, Ee.flushPendingDeletes = Dn, Ee.setDelayFunction = zn, Mt = Ee.UnboundTypeError = fn(Error, "UnboundTypeError"), Ee.count_emval_handles = io, Ee.get_first_emval = so, Xt = [null, an, sn, Xo, ea, ta, ra, na, oa, aa, ia, ua, Aa], qt = {
      e: rn,
      Q: an,
      P: sn,
      V: gn,
      y: Zn,
      q: eo,
      m: ro,
      $: oo,
      U: co,
      w: po,
      d: mo,
      c: Ao,
      x: go,
      o: vo,
      W: Eo,
      I: yo,
      J: bo,
      i: ko,
      z: ao,
      h: To,
      _: Do,
      C: So,
      R: Co,
      b: Oo,
      Y: Po,
      s: xo,
      M: Bo,
      t: Mo,
      g: Ro,
      f: qr,
      k: pt,
      j: jo,
      D: Go,
      F: Wo,
      E: Yo,
      G: qo,
      r: Zo,
      H: Ko,
      N: ea,
      O: ta,
      v: ra,
      S: na,
      T: oa,
      A: aa,
      u: ia,
      X: sa,
      B: Qr,
      a: Ue,
      K: ca,
      L: la,
      l: fa,
      p: ha,
      n: ma,
      Z: Aa
    }, Vr(), Zt = Ee.___wasm_call_ctors = function () {
      return (Zt = Ee.___wasm_call_ctors = Ee.asm.aa).apply(null, arguments)
    }, Kt = Ee._free = function () {
      return (Kt = Ee._free = Ee.asm.ba).apply(null, arguments)
    }, Jt = Ee._malloc = function () {
      return (Jt = Ee._malloc = Ee.asm.ca).apply(null, arguments)
    }, er = Ee._strlen = function () {
      return (er = Ee._strlen = Ee.asm.ea).apply(null, arguments)
    },tr = Ee.___errno_location = function () {
      return (tr = Ee.___errno_location = Ee.asm.fa).apply(null, arguments)
    },rr = Ee.___getTypeName = function () {
      return (rr = Ee.___getTypeName = Ee.asm.ga).apply(null, arguments)
    },Ee.___embind_register_native_and_builtin_types = function () {
      return (Ee.___embind_register_native_and_builtin_types = Ee.asm.ha).apply(null, arguments)
    },nr = Ee._emscripten_get_global_libc = function () {
      return (nr = Ee._emscripten_get_global_libc = Ee.asm.ia).apply(null, arguments)
    },or = Ee.__get_tzname = function () {
      return (or = Ee.__get_tzname = Ee.asm.ja).apply(null, arguments)
    },ar = Ee.__get_daylight = function () {
      return (ar = Ee.__get_daylight = Ee.asm.ka).apply(null, arguments)
    },ir = Ee.__get_timezone = function () {
      return (ir = Ee.__get_timezone = Ee.asm.la).apply(null, arguments)
    },sr = Ee._pthread_self = function () {
      return (sr = Ee._pthread_self = Ee.asm.ma).apply(null, arguments)
    },ur = Ee._emscripten_main_browser_thread_id = function () {
      return (ur = Ee._emscripten_main_browser_thread_id = Ee.asm.na).apply(null, arguments)
    },cr = Ee.___pthread_tsd_run_dtors = function () {
      return (cr = Ee.___pthread_tsd_run_dtors = Ee.asm.oa).apply(null, arguments)
    },lr = Ee._emscripten_main_thread_process_queued_calls = function () {
      return (lr = Ee._emscripten_main_thread_process_queued_calls = Ee.asm.pa).apply(null, arguments)
    },Ee._emscripten_current_thread_process_queued_calls = function () {
      return (Ee._emscripten_current_thread_process_queued_calls = Ee.asm.qa).apply(null, arguments)
    },dr = Ee._emscripten_register_main_browser_thread_id = function () {
      return (dr = Ee._emscripten_register_main_browser_thread_id = Ee.asm.ra).apply(null, arguments)
    },fr = Ee.__emscripten_do_dispatch_to_thread = function () {
      return (fr = Ee.__emscripten_do_dispatch_to_thread = Ee.asm.sa).apply(null, arguments)
    },pr = Ee._emscripten_sync_run_in_main_thread_4 = function () {
      return (pr = Ee._emscripten_sync_run_in_main_thread_4 = Ee.asm.ta).apply(null, arguments)
    },hr = Ee._emscripten_run_in_main_runtime_thread_js = function () {
      return (hr = Ee._emscripten_run_in_main_runtime_thread_js = Ee.asm.ua).apply(null, arguments)
    },mr = Ee.__emscripten_call_on_thread = function () {
      return (mr = Ee.__emscripten_call_on_thread = Ee.asm.va).apply(null, arguments)
    },Ee._emscripten_tls_init = function () {
      return (Ee._emscripten_tls_init = Ee.asm.wa).apply(null, arguments)
    },Ar = Ee.__emscripten_thread_init = function () {
      return (Ar = Ee.__emscripten_thread_init = Ee.asm.xa).apply(null, arguments)
    },gr = Ee.stackSave = function () {
      return (gr = Ee.stackSave = Ee.asm.ya).apply(null, arguments)
    },vr = Ee.stackRestore = function () {
      return (vr = Ee.stackRestore = Ee.asm.za).apply(null, arguments)
    },Er = Ee.stackAlloc = function () {
      return (Er = Ee.stackAlloc = Ee.asm.Aa).apply(null, arguments)
    },yr = Ee._emscripten_stack_set_limits = function () {
      return (yr = Ee._emscripten_stack_set_limits = Ee.asm.Ba).apply(null, arguments)
    },wr = Ee._memalign = function () {
      return (wr = Ee._memalign = Ee.asm.Ca).apply(null, arguments)
    },Ee.dynCall_ijiii = function () {
      return (Ee.dynCall_ijiii = Ee.asm.Da).apply(null, arguments)
    },Ee.dynCall_viiijj = function () {
      return (Ee.dynCall_viiijj = Ee.asm.Ea).apply(null, arguments)
    },Ee.dynCall_jij = function () {
      return (Ee.dynCall_jij = Ee.asm.Fa).apply(null, arguments)
    },Ee.dynCall_jii = function () {
      return (Ee.dynCall_jii = Ee.asm.Ga).apply(null, arguments)
    },Ee.dynCall_jiji = function () {
      return (Ee.dynCall_jiji = Ee.asm.Ha).apply(null, arguments)
    },Ee._ff_h264_cabac_tables = 77706,_r = Ee.__emscripten_allow_main_runtime_queued_calls = 241152,br = Ee.__emscripten_main_thread_futex = 1178e3,Ee.PThread = ft,Ee.PThread = ft,Ee.wasmMemory = Ue,Ee.ExitStatus = va,at = function e() {
      kr || Ea(), kr || (at = e)
    },Ee.run = Ea,Ee.preInit) for ("function" == typeof Ee.preInit && (Ee.preInit = [Ee.preInit]); Ee.preInit.length > 0;) Ee.preInit.pop()();
    Se && (Fe = !1, ft.initWorker()), Ea(), Tr = e(Ee)
  }

  var _a = !1;

  function ba(e) {
    let t = e.next(), r = null;
    return n => {
      var o = new Uint8Array(n);
      if (r) {
        var a = new Uint8Array(r.length + o.length);
        a.set(r), a.set(o, r.length), o = a, r = null
      }
      for (; o.length >= t.value;) {
        var i = o.slice(t.value);
        t = e.next(o.slice(0, t.value)), o = i
      }
      o.length > 0 && (r = o)
    }
  }

  function ka() {
    ({}), Dr || (Dr = !0, wa()), a(), (() => {
      try {
        if ("object" == typeof WebAssembly && "function" == typeof WebAssembly.instantiate) {
          const e = new WebAssembly.Module(Uint8Array.of(0, 97, 115, 109, 1, 0, 0, 0));
          if (e instanceof WebAssembly.Module) return new WebAssembly.Instance(e) instanceof WebAssembly.Instance
        }
      } catch (e) {
      }
      return !1
    })(), Date.now || (Date.now = function () {
      return (new Date).getTime()
    }), Tr.print = function (e) {
      postMessage({cmd: "print", text: e})
    }, Tr.printErr = function (e) {
      postMessage({cmd: "printErr", text: e})
    }, Tr.postRun = function () {
      var e = [], t = {
        _firstCheckpoint: 0, _lastCheckpoint: 0, _intervalBytes: 0, _lastSecondBytes: 0, addBytes: function (e) {
          0 === t._firstCheckpoint ? (t._firstCheckpoint = Date.now(), t._lastCheckpoint = t._firstCheckpoint, t._intervalBytes += e) : Date.now() - t._lastCheckpoint < 1e3 ? t._intervalBytes += e : (t._lastSecondBytes = t._intervalBytes, t._intervalBytes = e, t._lastCheckpoint = Date.now())
        }, reset: function () {
          t._firstCheckpoint = t._lastCheckpoint = 0, t._intervalBytes = 0, t._lastSecondBytes = 0
        }, getCurrentKBps: function () {
          t.addBytes(0);
          var e = (Date.now() - t._lastCheckpoint) / 1e3;
          return 0 == e && (e = 1), t._intervalBytes / e / 1024
        }, getLastSecondKBps: function () {
          return t.addBytes(0), 0 !== t._lastSecondBytes ? t._lastSecondBytes / 1024 : Date.now() - t._lastCheckpoint >= 500 ? t.getCurrentKBps() : 0
        }
      }, r = {
        opt: {}, initAudioPlanar: function (e, t) {
          postMessage({cmd: "initAudioPlanar", samplerate: t, channels: e});
          var r = [], n = [], o = 0;
          this.playAudioPlanar = function (t, a) {
            for (var i = a, s = [], u = 0, c = 0; c < 2; c++) {
              var l = Tr.HEAPU32[(t >> 2) + c] >> 2;
              s[c] = Tr.HEAPF32.subarray(l, l + i)
            }
            if (o) {
              if (!(i >= (a = 1024 - o))) return o += i, r[0] = Float32Array.of(...r[0], ...s[0]), void (2 == e && (r[1] = Float32Array.of(...r[1], ...s[1])));
              n[0] = Float32Array.of(...r[0], ...s[0].subarray(0, a)), 2 == e && (n[1] = Float32Array.of(...r[1], ...s[1].subarray(0, a))), postMessage({
                cmd: "playAudio",
                buffer: n
              }, n.map((e => e.buffer))), u = a, i -= a
            }
            for (o = i; o >= 1024; o -= 1024) n[0] = s[0].slice(u, u += 1024), 2 == e && (n[1] = s[1].slice(u - 1024, u)), postMessage({
              cmd: "playAudio",
              buffer: n
            }, n.map((e => e.buffer)));
            o && (r[0] = s[0].slice(u), 2 == e && (r[1] = s[1].slice(u)))
          }
        }, inputFlv: function* () {
          yield 9;
          for (var t = new ArrayBuffer(4), r = new Uint8Array(t), a = new Uint32Array(t); ;) {
            r[3] = 0;
            var i = yield 15, s = i[4];
            r[0] = i[7], r[1] = i[6], r[2] = i[5];
            var u = a[0];
            r[0] = i[10], r[1] = i[9], r[2] = i[8];
            var c = a[0];
            16777215 === c && (r[3] = i[11], c = a[0]);
            var l = yield u;
            switch (s) {
              case 8:
                this.opt.hasAudio && e.push({ts: c, payload: l, decoder: n, type: 0});
                break;
              case 9:
                e.push({ts: c, payload: l, decoder: o, type: l[0] >> 4})
            }
          }
        }, play: function (r) {
          this.opt.debug && console.log("Jessibuca play", r), this.getDelay = function (e) {
            return e ? (this.firstTimestamp = e, this.startTimestamp = Date.now(), this.getDelay = function (e) {
              return this.delay = Date.now() - this.startTimestamp - (e - this.firstTimestamp), this.delay
            }, -1) : -1
          };
          var i = this.opt.vod ? () => {
            if (e.length) {
              var t = e[0];
              if (-1 === this.getDelay(t.ts)) e.shift(), this.ts = t.ts, t.decoder.decode(t.payload); else for (; e.length && (t = e[0], this.getDelay(t.ts) > this.videoBuffer);) e.shift(), this.ts = t.ts, t.decoder.decode(t.payload)
            }
          } : () => {
            if (e.length) if (this.dropping) 1 == (t = e.shift()).type ? (this.dropping = !1, this.ts = t.ts, t.decoder.decode(t.payload)) : 0 == t.type && (this.ts = t.ts, t.decoder.decode(t.payload)); else {
              var t = e[0];
              if (-1 === this.getDelay(t.ts)) e.shift(), this.ts = t.ts, t.decoder.decode(t.payload); else if (this.delay > this.videoBuffer + 1e3) this.dropping = !0; else for (; e.length && (t = e[0], this.getDelay(t.ts) > this.videoBuffer);) e.shift(), this.ts = t.ts, t.decoder.decode(t.payload)
            }
          };
          if (this.stopId = setInterval(i, 10), this.speedSamplerId = setInterval((() => {
            postMessage({cmd: "kBps", kBps: t.getLastSecondKBps()})
          }), 1e3), 0 == r.indexOf("http")) {
            this.flvMode = !0;
            var s = this, u = new AbortController;
            fetch(r, {signal: u.signal}).then((function (e) {
              var r = e.body.getReader(), n = s.inputFlv(), o = ba(n), a = function () {
                r.read().then((({done: e, value: r}) => {
                  e ? n.return(null) : (t.addBytes(r.byteLength), o(r), a())
                })).catch((function (e) {
                  n.return(null), s.opt.debug && console.error(e), -1 === e.toString().indexOf("The user aborted a request") && postMessage({
                    cmd: "printErr",
                    text: e.toString()
                  })
                }))
              };
              a()
            })).catch((e => {
              postMessage({cmd: "printErr", text: e.message})
            })), this._close = function () {
              u.abort()
            }
          } else {
            if (this.flvMode = -1 != r.indexOf(".flv"), this.ws = new WebSocket(r), this.ws.binaryType = "arraybuffer", this.flvMode) {
              let e = this.inputFlv();
              var c = ba(e);
              this.ws.onmessage = e => {
                t.addBytes(e.data.byteLength), c(e.data)
              }, this.ws.onerror = t => {
                e.return(null), postMessage({cmd: "printErr", text: t.toString()})
              }
            } else this.ws.onmessage = r => {
              t.addBytes(r.data.byteLength);
              var a = new DataView(r.data);
              switch (a.getUint8(0)) {
                case 1:
                  this.opt.hasAudio && e.push({
                    ts: a.getUint32(1, !1),
                    payload: new Uint8Array(r.data, 5),
                    decoder: n,
                    type: 0
                  });
                  break;
                case 2:
                  e.push({
                    ts: a.getUint32(1, !1),
                    payload: new Uint8Array(r.data, 5),
                    decoder: o,
                    type: a.getUint8(5) >> 4
                  })
              }
            }, this.ws.onerror = e => {
              postMessage({cmd: "printErr", text: e.toString()})
            };
            this._close = function () {
              this.ws.close(), this.ws = null
            }
          }
          this.setVideoSize = function (e, t) {
            postMessage({cmd: "initSize", w: e, h: t});
            var r = e * t, n = r >> 2;
            if (this.opt.forceNoOffscreen || "undefined" == typeof OffscreenCanvas) this.draw = function (e, t, o, a) {
              var i = [Tr.HEAPU8.subarray(t, t + r), Tr.HEAPU8.subarray(o, o + n), Tr.HEAPU8.subarray(a, a + n)].map((e => Uint8Array.from(e)));
              postMessage({
                cmd: "render",
                compositionTime: e,
                delay: this.delay,
                ts: this.ts,
                output: i
              }, i.map((e => e.buffer)))
            }; else {
              var o = new OffscreenCanvas(e, t), i = o.getContext("webgl"), s = a().default(i);
              this.draw = function (a, i, u, c) {
                s(e, t, Tr.HEAPU8.subarray(i, i + r), Tr.HEAPU8.subarray(u, u + n), Tr.HEAPU8.subarray(c, c + n));
                let l = o.transferToImageBitmap();
                postMessage({cmd: "render", compositionTime: a, delay: this.delay, ts: this.ts, buffer: l}, [l])
              }
            }
          }
        }, close: function () {
          this._close && (this.opt.debug && console.log("worker close"), this._close(), clearInterval(this.stopId), this.stopId = null, clearInterval(this.speedSamplerId), this.speedSamplerId = null, t.reset(), this.ws = null, n.clear(), o.clear(), this.firstTimestamp = 0, this.startTimestamp = 0, this.delay = 0, this.ts = 0, this.flvMode = !1, e = [], delete this.playAudioPlanar, delete this.draw, delete this.getDelay)
        }
      }, n = new Tr.AudioDecoder(r), o = new Tr.VideoDecoder(r);
      postMessage({cmd: "init"}), self.onmessage = function (e) {
        var t = e.data;
        switch (t.cmd) {
          case"init":
            r.opt = JSON.parse(t.opt), n.sample_rate = t.sampleRate;
            break;
          case"getProp":
            postMessage({cmd: "getProp", value: r[t.prop]});
            break;
          case"setProp":
            r[t.prop] = t.value;
            break;
          case"play":
            r.play(t.url);
            break;
          case"setVideoBuffer":
            r.videoBuffer = 1e3 * t.time | 0;
            break;
          case"close":
            r.close()
        }
      }
    }
  }

  "undefined" == typeof importScripts ? ve || (ve = !0, {}, l(), U(), $(), q(), Ae(), v(), R(), ge = class {
    constructor(e) {
      if (this._opt = Object.assign(v().DEFAULT_OPTIONS, e), this.$container = e.container, "string" == typeof e.container && (this.$container = document.querySelector(e.container)), !this.$container) throw new Error("Jessibuca need container option");
      delete this._opt.container, this._opt.debug && console.log("options", this._opt), Ae().default(this), l().default(this), U().default(this), q().default(this), $().default(this)
    }

    set fullscreen(e) {
      e ? (R().checkFull() || this.$container.requestFullscreen(), R().$domToggle(this.$doms.minScreenDom, !0), R().$domToggle(this.$doms.fullscreenDom, !1)) : (R().checkFull() && document.exitFullscreen(), R().$domToggle(this.$doms.minScreenDom, !1), R().$domToggle(this.$doms.fullscreenDom, !0)), this._fullscreen !== e && (this.onFullscreen(e), this._trigger(v().EVEMTS.fullscreen, e)), this._fullscreen = e
    }

    get fullscreen() {
      return this._fullscreen
    }

    set playing(e) {
      e ? (R().$domToggle(this.$doms.playBigDom, !1), R().$domToggle(this.$doms.playDom, !1), R().$domToggle(this.$doms.pauseDom, !0), R().$domToggle(this.$doms.screenshotsDom, !0), R().$domToggle(this.$doms.recordDom, !0), this._quieting ? (R().$domToggle(this.$doms.quietAudioDom, !0), R().$domToggle(this.$doms.playAudioDom, !1)) : (R().$domToggle(this.$doms.quietAudioDom, !1), R().$domToggle(this.$doms.playAudioDom, !0))) : (this.$doms.speedDom && (this.$doms.speedDom.innerText = ""), this._playUrl && (R().$domToggle(this.$doms.playDom, !0), R().$domToggle(this.$doms.playBigDom, !0), R().$domToggle(this.$doms.pauseDom, !1)), R().$domToggle(this.$doms.recordDom, !1), R().$domToggle(this.$doms.recordingDom, !1), R().$domToggle(this.$doms.screenshotsDom, !1), R().$domToggle(this.$doms.quietAudioDom, !1), R().$domToggle(this.$doms.playAudioDom, !1)), this._playing !== e && (e ? (this.onPlay(), this._trigger(v().EVEMTS.play)) : (this.onPause(), this._trigger(v().EVEMTS.pause))), this._playing = e
    }

    get playing() {
      return this._playing
    }

    set quieting(e) {
      e ? (R().$domToggle(this.$doms.quietAudioDom, !0), R().$domToggle(this.$doms.playAudioDom, !1)) : (R().$domToggle(this.$doms.quietAudioDom, !1), R().$domToggle(this.$doms.playAudioDom, !0)), this._quieting !== e && (this.onMute(e), this._trigger(v().EVEMTS.mute, e)), this._quieting = e
    }

    get quieting() {
      return this._quieting
    }

    set loading(e) {
      e ? (R().$hideBtns(this.$doms), R().$domToggle(this.$doms.fullscreenDom, !0), R().$domToggle(this.$doms.pauseDom, !0), R().$domToggle(this.$doms.loadingDom, !0)) : R().$initBtns(this.$doms), this._loading = e
    }

    get loading() {
      return this._loading
    }

    set recording(e) {
      e ? (R().$domToggle(this.$doms.recordDom, !1), R().$domToggle(this.$doms.recordingDom, !0)) : (R().$domToggle(this.$doms.recordDom, !0), R().$domToggle(this.$doms.recordingDom, !1)), this._recording !== e && (this.onRecord(e), this._trigger(v().EVEMTS.record, e), this._recording = e)
    }

    get recording() {
      return this._recording
    }

    setDebug(e) {
      this._opt.isDebug = !!e
    }

    setTimeout(e) {
      this._opt.timeout = Number(e)
    }

    setVod(e) {
      this._opt.vod = !!e
    }

    setNoOffscreen(e) {
      this._opt.forceNoOffscreen = !!e
    }

    setScaleMode(e) {
      0 === (e = Number(e)) ? (this._opt.isFullResize = !1, this._opt.isResize = !1) : 1 === e ? (this._opt.isFullResize = !1, this._opt.isResize = !0) : 2 === e && (this._opt.isFullResize = !0), this._resize()
    }

    mute() {
      this._mute()
    }

    cancelMute() {
      this._cancelMute()
    }

    audioResume() {
      this._cancelMute()
    }

    pause() {
      this._pause()
    }

    play(e) {
      this._play(e)
    }

    close() {
      this._close()
    }

    destroy() {
      this._close(), this._destroyAudioContext(), this._destroyContextGL(), this._decoderWorker.terminate(), this._removeEventListener(), this._initCheckVariable(), this._off(), this._removeContainerChild()
    }

    clearView() {
      this._clearView()
    }

    resize() {
      this._resize()
    }

    setBufferTime(e) {
      e = Number(e), this._decoderWorker.postMessage({cmd: v().POST_MESSAGE.setVideoBuffer, time: e})
    }

    setRotate(e) {
      e = parseInt(e, 10), this._opt.rotate !== e && -1 !== [0, 90, 270].indexOf(e) && (this._opt.rotate = e, this.resize())
    }

    setVolume(e) {
      if (this._gainNode) {
        if (e = parseFloat(e), isNaN(e)) return;
        this._gainNode.gain.setValueAtTime(e, this._audioContext.currentTime)
      }
    }

    setKeepScreenOn() {
      this._opt.keepScreenOn = !0
    }

    setFullscreen(e) {
      const t = !!e;
      this.fullscreen !== t && (this.fullscreen = t)
    }

    hasLoaded() {
      return this._hasLoaded
    }

    screenshot(e, t, r) {
      this._screenshot(e, t, r)
    }

    on(e, t) {
      this._on(e, t)
    }
  }, window.Jessibuca = ge) : _a || (_a = !0, ka())
}();
