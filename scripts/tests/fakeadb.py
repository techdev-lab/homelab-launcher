"""A fake adb that imitates one Google TV, for testing Set-TvhomeAsHome.ps1 without a device.
State lives in the JSON file named by FAKE_STATE; every call is appended to the log named by FAKE_LOG.
FAKE_MODE: normal | sethome_ignored (set-home-activity has no effect) | setup_owns_wifi | owner_unknown
(the network in use has no cname line, as seen on a real Chromecast) | bind_fails | builtin_walls (no pushed
wallpapers folder, and saved networks listed as "* ID: n SSID:" as on some Android 14 builds) | android11
(`dumpsys activity activities` has no topResumedActivity line, only mResumedActivity, as on the Mi TV MOOR2 with Android 11)
"""
import json, os, re, sys

state_path = os.environ["FAKE_STATE"]
log_path = os.environ["FAKE_LOG"]
mode = os.environ.get("FAKE_MODE", "normal")
PINNED = "3E5C3ADC93CF4B870931F3692231A095EBDB537837BBD5CF5547C4BAC2DED595"
LX = "com.google.android.apps.tv.launcherx"
SW = "com.google.android.tungsten.setupwraith"
TV = "com.gathin.tvhome"

if os.path.exists(state_path):
    st = json.load(open(state_path))
else:
    st = {"enabled": [LX, SW], "home": LX, "apk": "EB7A2A80" + "0" * 56, "a11y": "null", "a11y_on": "0",
          "files": True, "deadman": False, "cancel": False}

args = sys.argv[1:]
with open(log_path, "a", encoding="utf-8") as fh:
    fh.write(" ".join(args) + "\n")


def out(s=""):
    print(s)


def save():
    json.dump(st, open(state_path, "w"))


if "-s" in args:
    i = args.index("-s")
    args = args[:i] + args[i + 2:]
cmd = args[0] if args else ""
if cmd == "connect":
    out("already connected"); sys.exit(0)
if cmd == "get-state":
    out("device"); sys.exit(0)
if cmd == "push":
    out("1 file pushed"); sys.exit(0)
if cmd == "install":
    if "-r" in args:
        st["apk"] = PINNED
        st["a11y_on"] = "0"
        save()
    out("Success"); sys.exit(0)
if cmd != "shell":
    sys.exit(0)

c = " ".join(args[1:])
if c.startswith("pm list packages -e"):
    for p in (LX, SW):
        if p in c and p in st["enabled"]:
            out("package:" + p)
elif c.startswith("pm list packages"):
    out("package:" + TV); out("package:" + LX)
elif c.startswith("ls /sdcard/Android/data/com.gathin.tvhome/files"):
    walls = c.split()[1].endswith("/wallpapers")
    present = st["files"] and not (walls and mode == "builtin_walls")
    out(c.split()[1] if present else "ls: No such file or directory")
elif c.startswith("dumpsys wifi |") and "Supplicant" in c:
    out("mWifiInfo SSID: HomeNet, Supplicant state: COMPLETED, RSSI: -47")
elif c.startswith("dumpsys wifi"):
    owner_cur = SW if mode == "setup_owns_wifi" else "android.uid.system:1000"
    out('mWifiInfo SSID: HomeNet, BSSID: x, MAC: y, Supplicant state: COMPLETED')
    star = "* " if mode == "builtin_walls" else ""
    out(star + 'ID: 0 SSID: "OldNet" PROVIDER-NAME: null BSSID: null')
    out(' cuid=10059 cname=' + SW + ' luid=10059')
    out(star + 'ID: 2 SSID: "HomeNet" PROVIDER-NAME: null BSSID: null')
    if mode != "owner_unknown":
        out(' cuid=1000 cname=' + owner_cur + ' luid=1000')
    out('mPerId={0=ID: 0 SSID: "OldNet" cname=' + SW)
elif "sha256sum" in c:
    out(st["apk"].lower() + "  /data/app/x/base.apk")
elif c.startswith("settings get secure enabled_accessibility_services"):
    out(st["a11y"])
elif c.startswith("settings get secure accessibility_enabled"):
    out(st["a11y_on"])
elif c.startswith("settings put secure enabled_accessibility_services"):
    st["a11y"] = c.split()[-1]; save()
elif c.startswith("settings put secure accessibility_enabled"):
    st["a11y_on"] = c.split()[-1]; save()
elif c.startswith("settings delete secure enabled_accessibility_services"):
    st["a11y"] = "null"; save()
elif c.startswith("dumpsys accessibility"):
    if mode != "bind_fails" and st["a11y"] != "null" and st["a11y_on"] == "1":
        out("Bound services:{Service[label=Home button, capabilities=8]}")
elif c.startswith("cmd package resolve-activity"):
    out(st["home"] + "/.MainActivity")
elif c.startswith("dumpsys activity activities"):
    rec = "ActivityRecord{1 u0 " + st["home"] + "/.MainActivity t1}"
    if mode == "android11":
        lines = ["  mResumedActivity: " + rec, "  ResumedActivity: " + rec, "  mFocusedApp=" + rec]
    else:
        lines = ["  topResumedActivity=" + rec, "  ResumedActivity: " + rec]
    g = re.search(r"grep -m1(?: -E)? '?([^'|]+(?:\|[^'|]+)*)'?\s*$", c)
    if g:
        lines = [x for x in lines if re.search(g.group(1), x)][:1]
    for x in lines:
        out(x)
elif c.startswith("pm disable-user"):
    p = c.split()[-1]
    if p in st["enabled"]:
        st["enabled"].remove(p)
    save()
elif c.startswith("pm enable"):
    p = c.split()[-1]
    if p not in st["enabled"]:
        st["enabled"].append(p)
    save()
elif c.startswith("cmd package set-home-activity"):
    comp = c.split()[-1]
    if mode != "sethome_ignored":
        st["home"] = comp.split("/")[0]
    else:
        st["home"] = LX if LX in st["enabled"] else "android"
    save()
elif c.startswith("nohup"):
    st["deadman"] = True; save()
elif c.startswith("touch /data/local/tmp/tvhome-deadman.cancel"):
    st["cancel"] = True; save()
elif c.startswith("rm -f /data/local/tmp/tvhome-deadman.cancel"):
    st["cancel"] = False; save()
# input keyevent, rm, etc.: no output
sys.exit(0)
