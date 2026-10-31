<div dir="rtl">

# الإصدارات

[English](RELEASING.md) · العربية

كل إصدار في [صفحة الإصدارات](https://github.com/techdev-lab/homelab-launcher/releases) يبنيه سير عمل `Release APK` (`.github/workflows/release.yml`) من وسم (tag)، وما يُرفع بيد. يشغّل اختبارات الوحدة، ويبني النكهتين، ويوقّعهما بمفتاح المشروع، ويكتب `SHA256SUMS.txt`، وينشر الإصدار بملاحظات تلك النسخة من `CHANGELOG.md`.

## ملفات الإصدار

| الملف | النكهة |
|---|---|
| `HomeLab-Launcher-<version>.apk` | `online`: كل شي، مع الطقس والكورة والأندية المفضلة |
| `HomeLab-Launcher-<version>-offline.apk` | `offline`: بدون صلاحية INTERNET أبداً |
| `SHA256SUMS.txt` | بصمة SHA-256 للملفين |

الاثنين يبقيان `versionCode 4` (شوف `CHANGELOG.md`)، فأي إصدار يتثبّت فوق ثاني بـ `adb install -r`. والمتجر يحتاج `versionCode` يرتفع مع كل إصدار: شوف «قرار مفتوح للإصدار» في `docs/DISTRIBUTION.ar.md`.

صفحة الإصدار تحمل الملاحظات بالإنجليزي ثم بالعربي: سير العمل ياخذ قسم النسخة من `CHANGELOG.md` ومن `CHANGELOG.ar.md`، فالملفين يحتاجون عنوان `## <version> (...)` قبل أي وسم.

## مفتاح التوقيع

كل إصدار يُوقَّع بمفتاح واحد طويل الأمد للمشروع. بصمة شهادته SHA-256 هي
`14849bdb3ed13523f75eede24e6b8cc36270d3a6f8f4bcb86f454311c616affe` (معلومة علنية). `release.yml` و`scripts/Test-Release.ps1` يثبّتانها الاثنين، وسير العمل يتوقف قبل النشر لو أي APK يحمل شهادة ثانية.

المفتاح أنشأته أدوات أندرويد على جهاز المشرف (`debug.keystore`)، وأول التلفزيونات تثبّتت به. أندرويد يرفض أي تحديث موقّع بمفتاح ثاني إلا بعد حذف التطبيق، وهذا يمسح `config.json` من التلفزيون، فالمشروع يبقي هذا المفتاح. مفتاح debug فريد لكل جهاز أنشأه، لكن سهل يضيع وله كلمة سر افتراضية معروفة، فعامله كمفتاح إصدار:

- احفظ المفتاح في ملف مفتاح خاص بالمشروع بكلمة سر قوية خاصة، خارج مجلد أدوات أندرويد. الأمر `keytool -importkeystore` ينسخ المفتاح وشهادته كما هي، فالبصمة أعلاه تبقى نفسها.
- احتفظ بنسختين مشفّرتين في مكانين مختلفين. وافحص أي نسخة بـ `keytool -list -v`: لازم تطابق SHA-256.
- لو ضاع المفتاح، ما في تلفزيون مثبّت يتحدّث إلا بحذف التطبيق. ولو تسرّب، أي واحد يقدر يوقّع تحديثات تقبلها التلفزيونات.

### إعداد مرة وحدة: أسرار المستودع

سير العمل يحتاج المفتاح كأسرار في المستودع. بدونها المهمة تتوقف عمداً: ملف موقّع بمفتاح مؤقت ما يقدر الإصدار اللي بعده يحدّثه أبداً.

1. على جهاز المشرف، حوّل ملف المفتاح لسطر واحد base64 (PowerShell):
   ```powershell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("<path to the key file>")) | Set-Content -NoNewline keystore.b64
   ```
2. في GitHub: *Settings > Secrets and variables > Actions > New repository secret*. الاسم `TVHOME_KEYSTORE_B64`، والقيمة: محتوى `keystore.b64`. احذف `keystore.b64` بعدها.
3. أضف كذلك `TVHOME_KEYSTORE_PASSWORD` و`TVHOME_KEY_ALIAS` و`TVHOME_KEY_PASSWORD`. القيم الافتراضية (`android` و`androiddebugkey`) تنطبق فقط لو ما تحددت، وهذا حال مفتاح debug الأصلي.

استعمال نفس المفتاح في بناءات المشرف المحلية معناه إن تلفزيون يشغّل بناءً محلياً يقدر ياخذ APK إصدار بـ `adb install -r`، والعكس.

## عمل إصدار

1. ارفع `versionName` في `launcher/app/build.gradle` وأضف قسم `## <version> (<date>)` في `CHANGELOG.md` وفي `CHANGELOG.ar.md`.
2. ادمج ذلك في `main` عبر طلب دمج (الفحوص الثلاثة لازم تنجح).
3. اوسم الـcommit المدموج وادفع الوسم:
   ```bash
   git tag v<version> <commit>
   git push origin v<version>
   ```
4. تابع *Actions > Release APK*. لما يصير أخضر الإصدار صار علنياً.

لإعادة التشغيل لوسم موجود: *Actions > Release APK > Run workflow*، واكتب الوسم.

## التحقق من ملف منزَّل

```bash
sha256sum -c SHA256SUMS.txt
apksigner verify --print-certs HomeLab-Launcher-<version>.apk
```

وعلى ويندوز تقدر تشغّل الفحصين مع بعض (يحتاج Android build-tools وJava):

```powershell
powershell -ExecutionPolicy Bypass -File scripts/Test-Release.ps1 -Apk .\HomeLab-Launcher-<version>.apk
```

يطبع `RESULT: OK` فقط لو الملف يطابق `SHA256SUMS.txt` وموقّع بشهادة المشروع.

سجل سير العمل يطبع بصمة SHA-256 للشهادة في كل إصدار، فتقدر تقارنها بالبصمة اللي على ملفك.

</div>
