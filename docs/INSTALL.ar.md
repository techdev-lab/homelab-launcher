<div dir="rtl">

# دليل التثبيت

[English](INSTALL.md) · العربية

هذا هو الترتيب الذي استُخدم بالضبط على Chromecast with Google TV 4K (أندرويد 14)، ومن الصفر على Xiaomi Mi TV (أندرويد 11): شوف [الأجهزة المجرّبة](TESTED_DEVICES.ar.md). اقرأه مرة كاملة قبل ما تبدأ.
كل خطوة تغيّر شيئاً في التلفزيون لها رجوع، و[قسم الإنقاذ](#الإنقاذ) في آخره يشتغل حتى لو انعكست الأمور كلها.

**الوقت:** حوالي نص ساعة. **تحتاج:** كمبيوتر على نفس الواي فاي حق التلفزيون، وريموت التلفزيون، وكلمة سر الواي فاي مكتوبة على ورقة.

---

## 0. قبل أي شي

1. **اكتب كلمة سر الواي فاي.** إحدى الخطوات تحت ممكن تخلي التلفزيون ينسى شبكة محفوظة.
2. **حدّث التلفزيون:** Settings ← System ← About ← System update. ثبّت كل اللي يعرضه أول.
3. **خيارات المطوّر (Developer options):** Settings ← System ← About ← اضغط **Android TV OS build** سبع مرات.
4. **Wireless debugging:** Settings ← System ← Developer options ← **Wireless debugging** ← On.
5. على الكمبيوتر نزّل الأداة الرسمية [Android platform-tools](https://developer.android.com/tools/releases/platform-tools) وتأكد:
   ```bash
   adb version
   ```

## 1. الإقران والاتصال (مرة وحدة)

على التلفزيون: Wireless debugging ← **Pair device with pairing code**. خلّ النافذة مفتوحة وشغّل:

```bash
adb pair <TV-IP>:<PAIR-PORT> <CODE>
adb connect <TV-IP>:<PORT>
adb devices
```

`<PAIR-PORT>` هو المنفذ اللي داخل نافذة الإقران، و`<PORT>` هو اللي في الشاشة الرئيسية لـ Wireless debugging. هم مختلفين. الرمز يشتغل مرة وحدة؛ لو فشل الإقران افتح نافذة إقران جديدة واستعمل الرمز الجديد.

المنفذ يتغيّر بعد كل إعادة تشغيل. الأمر `adb mdns services` يعرض المنفذ الحالي (السطر اللي فيه `_adb-tls-connect`). الإقران يبقى محفوظ، فبعدين تحتاج `adb connect` بس.

## 2. شوف قبل ما تلمس (قراءة فقط)

```powershell
$env:AL_TV = "<TV-IP>:<PORT>"
.\scripts\Get-DeviceInventory.ps1
```

يطبع الموديل، ونسخة أندرويد، والشاشة الرئيسية الحالية، وتطبيقاتك المثبّتة (استفد منها في `dock_apps`). ما يغيّر أي شي.

## 3. ثبّت اللانشر كتطبيق عادي

```bash
adb install tvhome-online.apk
adb shell am start -n com.gathin.tvhome/.MainActivity
```

الشاشة الأصلية لسه هي الرئيسية في هذي المرحلة. جرّب الريموت على tvhome: الأسهم، وفتح تطبيق، وزر الرجوع. كمّل بس إذا كل شي اشتغل.

## 4. إعداداتك

انسخ `config/config.example.json` (أو واحد من `config/examples/`)، وحط إحداثيات مدينتك والدوريات وتطبيقات الشريط ([دليل الإعدادات](CONFIG.ar.md))، ثم:

```bash
adb push config.json /sdcard/Android/data/com.gathin.tvhome/files/config.json
adb shell am force-stop com.gathin.tvhome
```

اضغط HOME، وافتح tvhome مرة ثانية تشوف النتيجة.

**الخلفيات ما تحتاج دفع.** الأمثلة تستعمل `"wallpaper_mode": "serene"`، وهي وحدة من ثلاث مجموعات داخل الـAPK (`homelab` و`serene` و`kids`). غيّر القيمة، أو اختر مجموعة أو لوناً سادة من التلفزيون (ضغطة مطوّلة على زر الإعدادات ← إعدادات المشغّل ← الخلفيات). ولو تبي صورك أنت، حط `"wallpaper_mode": "rotate"` وادفعها:

```bash
adb shell mkdir -p /sdcard/Android/data/com.gathin.tvhome/files/wallpapers
adb push my-pictures/. /sdcard/Android/data/com.gathin.tvhome/files/wallpapers/
```

اللانشر وشاشة التوقف يتبعون لغة التلفزيون. لو تبيهم بالعربي على تلفزيون لغته الإنجليزية، من غير ما تغيّر لغة التلفزيون لكل التطبيقات، حط `"language": "ar"` في `config.json` (يشتغل على كل نسخ أندرويد)، أو اعطِ اللانشر لغة خاصة فيه (أندرويد 13 وما فوق؛ والأمر `adb shell cmd locale help` يشرح كيف تمسحها):

```bash
adb shell cmd locale set-app-locales com.gathin.tvhome --locales ar
```

اختياري، عشان يطلع اسم شبكة الواي فاي في كبسولة الواي فاي: أندرويد 10 وما فوق ما يعطي اسم الشبكة إلا لتطبيق عنده صلاحية الموقع، وما في هذا الدليل أي شي يمنحها لك. بدونها الكبسولة تشتغل عادي وبس ما يظهر الاسم. التطبيق ما يطلب موقعك أبداً؛ الصلاحية معلنة للاسم فقط (شوف [الأمان](../SECURITY.ar.md)).

```bash
adb shell pm grant com.gathin.tvhome android.permission.ACCESS_FINE_LOCATION
```

## 5. خلّه الشاشة الرئيسية

جوجل تي في عنده حزمتين تملكان الشاشة الرئيسية: اللانشر الأصلي `launcherx` ومعالج الإعداد `setupwraith`. **الاثنين** لازم يتعطّلون، و**`setupwraith` أولاً**. لو عطّلت `launcherx` بس، معالج الإعداد يصير هو الشاشة الرئيسية والتلفزيون ممكن يدخل في إعادة تشغيل متكررة.

تعطيل `setupwraith` ممكن كذلك يخلي التلفزيون ينسى شبكة واي فاي حفظها معالج الإعداد. السكربت يفحص الشبكة اللي أنت عليها ويوقف لو ما عرف مين يملكها.

```powershell
$env:AL_TV = "<TV-IP>:<PORT>"
.\scripts\Set-TvhomeAsHome.ps1 -Alias livingroom                    # تجربة بدون تغيير: فحوصات قراءة فقط والخطة
.\scripts\Set-TvhomeAsHome.ps1 -Alias livingroom -Execute -ConfirmText "CONFIRM HOME livingroom" -EnableAppKeys
```

يكتب ملف رجوع أول شي، ثم يعطّل `setupwraith` بعدها `launcherx`، ويجعل tvhome هو الرئيسية، ويتحقق من الهوم والواي فاي وADB. لو فشل أي فحص يرجّع كل شي فوراً.

لو انقطع التلفزيون عن الواي فاي في هذي الخطوة، أعد كتابة كلمة سر الواي فاي بالريموت، وبعدين أعد اتصال adb.

الرجوع في أي وقت:
```powershell
.\scripts\Set-TvhomeAsHome.ps1 -Alias livingroom -Rollback -ConfirmText "CONFIRM ROLLBACK livingroom"
```

## 6. زر يوتيوب وشاشة التوقف

زر يوتيوب في الريموت مفتاح عام ما كان يسمعه إلا اللانشر الأصلي. tvhome يسمعه عن طريق خدمة وصول صغيرة تفلتر المفاتيح فقط (`capabilities=8`، ما تقدر تقرأ الشاشة).

```bash
adb shell am force-stop com.gathin.tvhome
adb shell settings put secure enabled_accessibility_services com.gathin.tvhome/.HomeKeyService
adb shell settings put secure accessibility_enabled 1
adb shell settings put secure screensaver_components com.gathin.tvhome/.ClockDream
```

**تنبيه: الأزرار المضغوطة.** تصفية الأزرار في أندرويد قد توقف تكرار الزر المضغوط على بعض التلفزيونات. على Xiaomi Mi TV (MOOR2، أندرويد 11) كان الضغط المطوّل على رفع الصوت يغيّر الصوت خطوة واحدة لكل ضغطة والخدمة تفلتر، ويتكرر عادي بدونها. لو شفت هذا، أوقف التصفية وخلّ الخدمة: **إعدادات المشغّل ← الحركة ← أزرار الريموت ← إيقاف**، أو `"key_filter": "off"` في `config.json`. تبقى الخدمة مفعّلة لكنها ما تفلتر شي، فكل زر يوصل التلفزيون بلا تدخّل، ويتوقف زر يوتيوب وأي زر عيّنته في `app_keys` أو الإعدادات وزر Home اللي يرسل `KEYCODE_ALL_APPS` (وجرّب زر Home بالريموت). والرجوع إلى *تشغيل* يرجعها فوراً. التفاصيل: [KEY_FILTER.ar.md](KEY_FILTER.ar.md). وإطفاء الخدمة من إعدادات أندرويد له نفس الأثر على الصوت ونفس الثمن، لكن تسويه بالأمرين اللي تحت:

```bash
adb shell settings delete secure enabled_accessibility_services
adb shell settings put secure accessibility_enabled 0
```

و`-EnableAppKeys` في الخطوة 5 اختياري: اتركه لو ريموتك ما فيه زر من هذا النوع، وشغّل الخدمة لاحقاً بأمري `settings put` اللي في هذي الخطوة (اللي يذكر `HomeKeyService` واللي يضبط `accessibility_enabled` على 1).

الترتيب مهم: فعّل خدمة الوصول **بعد** أي `force-stop` (لأن الإيقاف القسري بعدها يمسحها)، وكذلك بعد كل إعادة تثبيت لـ tvhome. جرّب زر يوتيوب بالريموت الحقيقي؛ المفاتيح اللي تنرسل بـ `adb shell input` ما تمر على الخدمة.

شاشة التوقف تستعمل نفس لغة الشاشة الرئيسية (شوف الخطوة 4). تقدر تقرأ قيمة شاشة التوقف القديمة أول بالأمر `adb shell settings get secure screensaver_components` لو تبي ترجع لها.

## 7. الختام

1. أعد تشغيل التلفزيون (`adb reboot`) وتأكد إن tvhome يرجع هو الرئيسية والواي فاي يتصل.
2. **أطفئ Wireless debugging** في التلفزيون. يبقى شغّال بعد إعادة التشغيل لين تطفيه أنت.

---

## التحديث بعدين (دفعة وحدة)

للتلفزيون اللي يشغّل tvhome كشاشة رئيسية أصلاً، السكربت `scripts/Apply-TvhomeKit.ps1` يثبّت APK جديد، ويدفع الإعدادات والخلفيات، ويعيد تفعيل خدمة يوتيوب وشاشة التوقف، وياخذ نسخة احتياطية من كل شي أول. حط الملفات جنبه كذا:

```
kit/
  Apply-TvhomeKit.ps1
  apk/tvhome-online.apk
  apk/SHA256SUMS.txt          "<sha256>  tvhome-online.apk"
  config/livingroom.config.json
  wallpapers/*.jpg
```

شغّله من مجلد الـkit، مع عنوان التلفزيون (منفذ الاتصال من Wireless debugging):

```powershell
$env:AL_TV = "<TV-IP>:<PORT>"
.\Apply-TvhomeKit.ps1 -Alias livingroom                                          # تجربة بدون تغيير
.\Apply-TvhomeKit.ps1 -Alias livingroom -Execute -ConfirmText "CONFIRM KIT livingroom"
.\Apply-TvhomeKit.ps1 -Alias livingroom -Rollback -ConfirmText "CONFIRM ROLLBACK livingroom"
```

النسخة الاحتياطية تروح إلى `%USERPROFILE%\.homelab-launcher\state\`؛ والرجوع يعيد الـAPK والإعدادات وشاشة التوقف وإعداد خدمة الوصول السابقة. السكربت كذلك يجعل شاشة التوقف هي ساعة tvhome، فاكتب قيمتك القديمة أول لو تبي ترجع لها.

يرفض يشتغل إلا لو `launcherx` و`setupwraith` معطّلين الاثنين للمستخدم 0، لأن إعادة تثبيت تطبيق الشاشة الرئيسية الوحيد وقت يقدر معالج الإعداد يتولاها هي بالضبط الطريقة اللي تبدأ فيها حلقة إعادة التشغيل.

## اختياري: تلفزيون أهدأ

هذي الحزم الـ12 عُطّلت في الـChromecast التجريبي بدون أي أثر على الريموت أو الصوت أو الكاست أو لوحة المفاتيح أو شاشة التوقف. الأمر `disable-user` له رجوع بـ `adb shell pm enable <package>`. سوّها على دفعتين وجرّب الريموت والصوت والكاست بينهم.

```
com.google.android.feedback                    com.android.tv.feedbackconsent
com.google.android.tv.remotecontrol.logging    com.google.android.apps.tv.netoscope
com.google.android.federatedcompute            com.google.android.ondevicepersonalization.services
com.google.android.adservices.api              com.google.android.ext.adservices.api
com.google.android.play.games                  com.google.android.videos
com.android.printspooler                       com.google.android.backdrop
```

```bash
adb shell pm disable-user --user 0 <package>
```

**لا تعطّل أبداً:** خدمات Google Play (`com.google.android.gms` و`com.google.android.gsf`)، ومتجر Play (`com.android.vending`)، والإعدادات (`com.android.tv.settings`)، وخدمة الريموت (`com.google.android.tv.remote.service`)، ولوحة المفاتيح (`com.google.android.inputmethod.latin`)، والكاست (`com.google.android.apps.mediashell`)، ومضيف شاشة التوقف `com.google.android.apps.tv.dreamx` وهو شاشة التوقف عندك، ولا محدّث برنامج الريموت (`com.google.android.tv.dfuservice`). ولا تسوّي `pm uninstall` لأي حزمة نظام.

---

## الإنقاذ

**لو التلفزيون يدخل في إعادة تشغيل متكررة** (شعار جوجل، ألوان Google TV، شاشة سودا، ويعيد):

1. خلّه موصول بالتلفزيون والكهرباء.
2. اضغط مطوّلاً الزر اللي ورا الـChromecast. الضوء يومض **أصفر**.
3. كمّل الضغط لين يصير **أبيض ثابت**، ثم اترك. الجهاز يرجع لإعدادات المصنع.
4. أعد إعداده، ثم **System update**.

إعادة ضبط المصنع تمسح بيانات المستخدم فقط: كل اللي يغيّره هذا المشروع موجود فيها، والنظام نفسه ما ينلمس، فالتلفزيون يرجع على نفس نسخة أندرويد اللي كانت عليه. (المصدر: دعم جوجل، «How to factory reset your Chromecast».)

وش اللي **ما يشتغل**: adb عبر كيبل USB (تصحيح USB مطفي افتراضياً وما احتجناه هنا أبداً)، و`cmd dreams` (يحتاج روت)، وفتح الـbootloader (يمسح التلفزيون على كل حال وما له داعي). جوجل ما تنشر صور مصنع قابلة للتنزيل لهذا الجهاز.

**لو شي شكله غلط بس التلفزيون لسه يشتغل:** شغّل أمر الرجوع في السكربت اللي استعملته، أو يدوياً:

```bash
adb shell pm enable com.google.android.tungsten.setupwraith
adb shell pm enable com.google.android.apps.tv.launcherx
adb shell cmd package set-home-activity --user 0 com.google.android.apps.tv.launcherx/.home.HomeActivity
```

## الرجوع إلى 2.9.0 أو أقدم

الإصدار 3.0.0 رقمه `versionCode` 5، وكل الإصدارات الأقدم رقمها 4. أندرويد يثبّت 3.0.0 فوقها، لكنه يرفض تثبيت بناء أقدم فوق 3.0.0 لأن ملفات APK مو قابلة للتصحيح. وللرجوع: انسخ `config.json` من التلفزيون أول (`adb pull /sdcard/Android/data/com.gathin.tvhome/files/config.json`)، ثم `adb uninstall com.gathin.tvhome`، وثبّت APK الأقدم، وأرجع `config.json`. الحذف يمسح اختيارات التطبيق اللي سويتها على التلفزيون (اللغة والخلفية والأعمدة).

</div>
