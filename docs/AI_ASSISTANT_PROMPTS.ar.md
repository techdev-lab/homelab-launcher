<div dir="rtl">

# أوامر جاهزة لمساعد الذكاء الاصطناعي

تقدر تسوي التثبيت كامل مع مساعد ذكاء اصطناعي يقدر يشغّل أوامر على جهازك (مثل OpenCode أو Codex أو Claude Code)، أو مع مساعد محادثة (مثل ChatGPT) وأنت تلصق له النتائج بنفسك. انسخ الأمر، عبّي الأجزاء اللي بين `< >`، والصقه.

قواعد الأمان موجودة داخل كل أمر عن قصد: هي اللي حمت جهاز التجربة من المشاكل. الأوامر بالإنجليزي لأن المساعدات تفهمها أدق، وتقدر تطلب منه يرد عليك بالعربي.

---

## 1. ثبّت اللانشر على الكروم كاست

```text
You are helping me install HomeLab Launcher (an Android TV home screen, package com.gathin.tvhome)
on my Chromecast with Google TV. The project is in this folder; read README.md and docs/INSTALL.md first
and follow docs/INSTALL.md in order. Answer me in Arabic; keep commands in English.

My setup:
- Computer OS: <Windows / macOS / Linux>
- TV IP and Wireless debugging port: <shown on the TV>
- City for prayer times: <city name> (look up its latitude/longitude)
- Football: <e.g. Saudi Pro League + the big European leagues>
- Dock apps I want on top: <e.g. YouTube, Netflix, Shahid, Jellyfin>

Rules you must follow:
1. Explain each command before running it, run one step at a time, and show me the real output.
2. Read-only checks first. Before any command that changes the TV, stop and wait for me to type "yes".
3. Never run "pm uninstall" on a system package. Use "pm disable-user --user 0" only.
4. Never disable com.google.android.apps.tv.launcherx while com.google.android.tungsten.setupwraith is enabled.
   Use scripts/Set-TvhomeAsHome.ps1 for the Home switch (dry run first). On macOS/Linux, do the same steps by
   hand in the same order: setupwraith first, then launcherx, then set-home-activity.
5. Before the Home switch, remind me to write down my Wi-Fi password.
6. Never reinstall tvhome while it is the Home app unless launcherx and setupwraith are both disabled.
7. Set the accessibility service only after any "am force-stop com.gathin.tvhome".
8. At the end: reboot test, then remind me to turn Wireless debugging off.
9. If the TV stops responding to adb, do not guess: tell me the factory reset steps from docs/INSTALL.md#rescue.
```

## 2. اكتب لي ملف الإعدادات

```text
Write a config.json for HomeLab Launcher using config/config.example.json as the template and docs/CONFIG.md
as the reference. My city is <city>, my language is <Arabic/English/system>, my leagues are <list>, and my
dock apps are <list>. Use only league codes listed as working in docs/CONFIG.md; if my league is not
available, say so and suggest the closest available one. Keep news off. Validate that the JSON parses.
Answer me in Arabic.
```

## 3. أضف أسماء عربية لدوري جديد

```text
In HomeLab Launcher, Arabic team names live in launcher/app/src/main/assets/names_ar.json under "teams",
keyed by ESPN team id. Fetch https://site.api.espn.com/apis/site/v2/sports/soccer/<league-code>/teams,
list every team id with its English name, and propose the common Arabic name for each. Show me the table
first; after I approve it, add the entries (do not change existing ones), keep the JSON valid, and add the
league's Arabic name under "leagues".
```

## 4. صار شي غلط

```text
My Chromecast with Google TV has a problem after installing HomeLab Launcher: <describe what you see>.
Read docs/INSTALL.md (Rescue section) and docs/JOURNEY.md. First tell me what you think happened and how
sure you are. Suggest only read-only checks until I agree. If the TV is in a boot loop and adb cannot
connect, give me the factory reset steps (button on the back, yellow then white). Answer me in Arabic.
```

</div>
