# auto install dependency
import subprocess, sys

for module, package in {'Crypto': 'pyCryptodome'}.items():
    try:
        __import__(module)
    except ModuleNotFoundError:
        if subprocess.call([sys.executable, '-m', 'pip', 'install', package]): raise

import json
import base64
import re
import hashlib
import binascii
from urllib.request import urlopen
from urllib.error import HTTPError
from datetime import datetime
from Crypto.Cipher import AES


def main():
    with open("cfg.json", 'r') as f:
        params = json.load(f)
        f.close()

    try:
        raw = urlopen(params["url"]).read().decode("utf-8")
        data = json.loads(raw)
    except HTTPError:
        print("Failed to read " + params["url"])
        return
    except json.decoder.JSONDecodeError:
        print("Failed to parse json " + raw)
        return

    if data.get("ok"):
        for row in data.get("messages", []):
            try:
                encoded = base64.urlsafe_b64decode(row["msg"])
            except binascii.Error as e:
                print(e, row["msg"])
                continue

            msg = decode(encoded, params["password"])
            if msg is None:
                print("Invalid password?")
                msg = encoded
            try:
                msg = str(msg, "utf-8")
            except UnicodeDecodeError:
                print("skip message " + row["msg"] + "\n")
                continue

            # 1ая строка - timestamp
            lines = re.split("\r\n", msg)
            if len(lines) > 0 and lines[0].isdigit():
                lines[0] = str(datetime.fromtimestamp(int(lines[0]) // 1000))
            print(*lines, "", sep="\n")


def decode(encoded, password):
    if password == "":
        return encoded
    try:
        # AES-128-CBC
        hashkey = hashlib.sha1(bytes(password, "utf-8")).digest()[0:16]
        iv = hashlib.sha1(hashkey).digest()[0:16]
        c = AES.new(hashkey, AES.MODE_CBC, iv)
        plaintext = c.decrypt(encoded)
        # remove padding
        return plaintext[:-plaintext[-1]]
    except Exception as e:
        return None


main()
