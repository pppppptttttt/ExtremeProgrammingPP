"""
Два peer-а через build/install/.../ExtremeProgrammingPP (сначала: ./gradlew installDist).
Вывод в одну консоль с префиксами [Alice] / [Bob]; ввод сценарный.

Запуск (из корня репозитория):
  python3 scripts/demo_alice_bob.py
"""

from __future__ import annotations

import os
import socket
import subprocess
import sys
import threading
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BIN = ROOT / "build/install/ExtremeProgrammingPP/bin/ExtremeProgrammingPP"


def free_port() -> int:
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.bind(("", 0))
    port = s.getsockname()[1]
    s.close()
    return port


def pipe_output(prefix: str, stream) -> None:
    for line in iter(stream.readline, ""):
        if line == "":
            break
        sys.stdout.write(f"{prefix}{line}")
        sys.stdout.flush()
    stream.close()


def feed_alice(proc: subprocess.Popen) -> None:
    time.sleep(4)
    assert proc.stdin
    proc.stdin.write("hi from Alice\n")
    proc.stdin.flush()
    time.sleep(2)
    proc.stdin.write("/exit\n")
    proc.stdin.flush()
    proc.stdin.close()


def feed_bob(proc: subprocess.Popen) -> None:
    time.sleep(2)
    assert proc.stdin
    proc.stdin.write("hi from Bob\n")
    proc.stdin.flush()
    time.sleep(2)
    proc.stdin.write("/exit\n")
    proc.stdin.flush()
    proc.stdin.close()


def main() -> int:
    if not BIN.is_file():
        print("Нет дистрибутива. Выполни из корня: ./gradlew installDist", file=sys.stderr)
        return 1
    if os.name != "nt" and not os.access(BIN, os.X_OK):
        print("Бинарник не исполняемый:", BIN, file=sys.stderr)
        return 1

    port = free_port()
    print(f"Порт: {port} (Alice слушает, Bob подключается)")
    print("----------")

    alice = subprocess.Popen(
        [str(BIN), "--name", "Alice", "--listen-port", str(port)],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1,
    )
    threading.Thread(
        target=pipe_output, args=("[Alice] ", alice.stdout), daemon=True
    ).start()
    threading.Thread(target=feed_alice, args=(alice,), daemon=True).start()

    time.sleep(1)

    bob = subprocess.Popen(
        [
            str(BIN),
            "--name",
            "Bob",
            "--peer-host",
            "127.0.0.1",
            "--peer-port",
            str(port),
        ],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1,
    )
    threading.Thread(target=pipe_output, args=("[Bob] ", bob.stdout), daemon=True).start()
    threading.Thread(target=feed_bob, args=(bob,), daemon=True).start()

    try:
        alice.wait()
        bob.wait()
    finally:
        for p in (alice, bob):
            if p.poll() is None:
                p.terminate()
                try:
                    p.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    p.kill()

    print("----------")
    print("Готово.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
