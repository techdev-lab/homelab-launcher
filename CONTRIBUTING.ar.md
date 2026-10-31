<div dir="rtl">

# المساهمة

[English](CONTRIBUTING.md) · العربية

شكراً لمساعدتك. أنفع المساهمات حالياً:

1. **أسماء عربية لفرق أكثر.** أضف إدخالات إلى `launcher/app/src/main/assets/names_ar.json` تحت `"teams"`، بمعرّف الفريق في ESPN من `https://site.api.espn.com/apis/site/v2/sports/soccer/<league>/teams`، والدوري تحت `"leagues"`. لا تغيّر إدخالاً موجوداً بدون سبب مكتوب في الطلب.
2. **مصدر كورة مجاني للدوريات العربية الناقصة** (الكويت والأردن وتونس والجزائر)، أو للنتائج المباشرة في مصر والإمارات وقطر والمغرب (واجهة TheSportsDB المجانية تعطي موعد المباراة الجاية فقط). لازم يكون HTTPS، بدون مفتاح أو مجاني، ومسموح استعماله لهذا الغرض.
3. **طريقة صلاة ناقصة** (شوف `Prayer.METHODS`). أضفها مع صف في `PrayerMethodsTest` من مصدر مستقل.
4. **تجربة على أجهزة ثانية** (Chromecast HD وGoogle TV Streamer وصناديق Android TV الأخرى). بلّغ وش اشتغل ووش ما اشتغل.

## القواعد

- الكود والتعليقات والوثائق ورسائل الـcommit بالإنجليزي. نصوص الواجهة للمستخدم تروح في موارد التطبيق (`values-ar/` و`values/`)، والبيانات العربية في `names_ar.json`. وكل وثيقة موجّهة للمستخدم لها نسخة عربية بجنبها باسم `*.ar.md` (مثل `docs/INSTALL.ar.md`)؛ لو غيّرت وثيقة غيّر نسختها العربية معها، أو اذكر في الطلب إنك ما قدرت وبنكمّلها.
- شغّل الاختبارات قبل أي طلب دمج:
  ```bash
  cd launcher && ./gradlew testOfflineReleaseUnitTest testOnlineReleaseUnitTest
  ```
  وعلى ويندوز، اختبارات التلفزيون الوهمي للسكربتات:
  ```powershell
  powershell -ExecutionPolicy Bypass -File scripts/tests/run-tests.ps1
  ```
- شغّل فحوصات المستودع (العربي فقط في مكانه، وبدون مفاتيح أو `local.properties` متتبَّعة)؛ والـCI يشغّل نفسها:
  ```bash
  python3 scripts/ci/check_hygiene.py
  python3 -m unittest discover -s scripts/ci
  ```
- كل ميزة جديدة تجي مع اختبار واحد على الأقل لحالة فشل.
- ما في صلاحيات جديدة بدون سبب مكتوب في الطلب. ونسخة offline لازم تبقى بدون INTERNET.
- لا ترفع أبداً عناوين IP أو أسماء واي فاي أو رموز إقران أو مفاتيح ADB أو `local.properties`.

</div>
