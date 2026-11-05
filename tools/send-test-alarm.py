#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
向 WVP 的 Redis 发布报警测试数据（无第三方依赖，只用 socket）。

默认发到本机 C 的 Redis：
    host=127.0.0.1  port=6379  db=1  password=空
    channel=alarm_receive

示例：
    # 发一条默认报警
    python3 send-test-alarm.py

    # 指定描述（描述里的雪花 ID 会作为 traceId）
    python3 send-test-alarm.py --desc "2001#,LCPC,100038,1234567890123456789"

    # 连发 5 条，每 2 秒一条
    python3 send-test-alarm.py --count 5 --interval 2

    # 每条同时发 2001# 和 2003#
    python3 send-test-alarm.py --pair

    # 发到远端 A（Redis DB6）
    python3 send-test-alarm.py --host 192.168.1.66 --db 6 \
        --password "$REDIS_PASSWORD"

    # 只看要发送的内容，不真正发布
    python3 send-test-alarm.py --count 2 --dry-run
"""

import argparse
import json
import random
import socket
import sys
import time


# ---------------------------------------------------------------------------
# 极简 RESP 客户端
# ---------------------------------------------------------------------------

def _encode(*parts):
    out = b"*%d\r\n" % len(parts)
    for part in parts:
        if isinstance(part, str):
            part = part.encode("utf-8")
        out += b"$%d\r\n%s\r\n" % (len(part), part)
    return out


def read_reply(sock):
    """按字节读取一个完整的 RESP 回复。"""
    def read_line():
        data = b""
        while not data.endswith(b"\r\n"):
            ch = sock.recv(1)
            if not ch:
                raise ConnectionError("Redis 连接已关闭")
            data += ch
        return data[:-2]

    line = read_line()
    if not line:
        raise RuntimeError("Redis 返回空回复")
    prefix = line[:1]
    body = line[1:]

    if prefix == b"+":
        return body.decode("utf-8", errors="replace")
    if prefix == b"-":
        raise RuntimeError(body.decode("utf-8", errors="replace"))
    if prefix == b":":
        return int(body)
    if prefix == b"$":
        length = int(body)
        if length == -1:
            return None
        data = b""
        while len(data) < length + 2:
            chunk = sock.recv(length + 2 - len(data))
            if not chunk:
                raise ConnectionError("Redis 连接已关闭")
            data += chunk
        return data[:-2].decode("utf-8", errors="replace")
    if prefix == b"*":
        count = int(body)
        if count == -1:
            return None
        return [read_reply(sock) for _ in range(count)]
    raise RuntimeError("未知的 RESP 回复类型: %r" % prefix)


class RedisClient(object):
    def __init__(self, host, port, password, db, timeout=5):
        self.sock = socket.create_connection((host, port), timeout=timeout)
        self.sock.settimeout(timeout)
        if password:
            self.command("AUTH", password)
        self.command("SELECT", str(db))

    def command(self, *parts):
        self.sock.sendall(_encode(*parts))
        return read_reply(self.sock)

    def publish(self, channel, message):
        return self.command("PUBLISH", channel, message)

    def close(self):
        try:
            self.sock.close()
        except Exception:
            pass


# ---------------------------------------------------------------------------
# 测试数据
# ---------------------------------------------------------------------------

def snowflake_id():
    """生成 19 位数字 ID，方便页面按 traceId 搜索。"""
    return str(int(time.time() * 1000)) + "".join(
        random.choice("0123456789") for _ in range(6)
    )


def build_messages(args, trace_id):
    """返回 [(alarmSn, description), ...]。"""
    if args.desc:
        return [(args.alarm_sn, args.desc)]

    if args.pair:
        return [
            (7, "2001#,LCPC,100038,%s" % trace_id),
            (7, "2003#,100038,%s" % trace_id),
        ]

    return [(args.alarm_sn, args.prefix + trace_id)]


def build_payload(args, alarm_sn, description):
    payload = {
        "gbId": args.gb_id,
        "alarmSn": alarm_sn,
        "alarmDescription": description,
    }
    if args.alarm_type is not None:
        payload["alarmType"] = args.alarm_type
    return json.dumps(payload, ensure_ascii=False, separators=(",", ":"))


def main():
    parser = argparse.ArgumentParser(
        description="向 WVP Redis 发布报警测试数据",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    parser.add_argument("--host", default="127.0.0.1", help="Redis IP，默认 127.0.0.1")
    parser.add_argument("--port", type=int, default=6379, help="Redis 端口，默认 6379")
    parser.add_argument("--password", default="", help="Redis 密码，默认空")
    parser.add_argument("--db", type=int, default=1, help="Redis DB，默认 1")
    parser.add_argument("--channel", default="alarm_receive",
                        help="发布频道，默认 alarm_receive")

    parser.add_argument("--desc", default=None,
                        help="完整报警描述；不填则自动生成 2001#,LCPC,100038,<id>")
    parser.add_argument("--id", default=None,
                        help="指定雪花 ID；不填则自动生成 19 位数字")
    parser.add_argument("--prefix", default="2001#,LCPC,100038,",
                        help="自动生成描述时使用的前缀")
    parser.add_argument("--alarm-sn", type=int, default=7,
                        help="alarmSn，默认 7（Other）")
    parser.add_argument("--alarm-type", type=int, default=None,
                        help="可选 alarmType")
    parser.add_argument("--gb-id", default="",
                        help="可选 gbId，默认空")

    parser.add_argument("--count", type=int, default=1,
                        help="发送多少组，默认 1")
    parser.add_argument("--interval", type=float, default=1.0,
                        help="每组之间的间隔秒数，默认 1")
    parser.add_argument("--pair", action="store_true",
                        help="每组发送 2001# 和 2003# 两条")
    parser.add_argument("--dry-run", action="store_true",
                        help="只打印，不连接 Redis")

    args = parser.parse_args()

    trace_ids = []
    client = None
    if not args.dry_run:
        try:
            client = RedisClient(args.host, args.port, args.password, args.db)
        except Exception as exc:
            print("连接 Redis 失败: %s" % exc, file=sys.stderr)
            return 1

    sent = 0
    try:
        for index in range(args.count):
            trace_id = args.id if args.id else snowflake_id()
            if args.count > 1 and args.id:
                trace_id = "%s%d" % (args.id, index + 1)
            trace_ids.append(trace_id)

            messages = build_messages(args, trace_id)
            for alarm_sn, description in messages:
                payload = build_payload(args, alarm_sn, description)
                print("[%d/%d] PUBLISH %s" % (sent + 1, args.count * len(messages), args.channel))
                print("        %s" % payload)
                if args.dry_run:
                    print("        -> dry-run")
                else:
                    try:
                        receivers = client.publish(args.channel, payload)
                        print("        -> %s 个订阅者" % receivers)
                    except Exception as exc:
                        print("        -> 发布失败: %s" % exc, file=sys.stderr)
                        return 1
                sent += 1

            if index < args.count - 1 and args.interval > 0:
                time.sleep(args.interval)
    finally:
        if client is not None:
            client.close()

    print("")
    print("完成：发送 %d 条" % sent)
    if trace_ids:
        print("traceId：%s" % ", ".join(trace_ids))
        print("页面搜索：%s" % trace_ids[0])
    print("提示：页面需要同时连接本地 WVP 和对应远端 WVP 的 WS，才能看到完整链路。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
