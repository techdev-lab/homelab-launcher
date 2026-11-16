
---

<div dir="rtl">

### وش الملف اللي أنزّله؟

- **`HomeLab-Launcher-<version>.apk`**: التطبيق كامل (مواقيت الصلاة، والتاريخ الهجري، والطقس، والكورة، والأندية المفضلة، وثلاث مجموعات الخلفيات والألوان السادة). أغلب الناس يبونه هو.
- **`HomeLab-Launcher-<version>-offline.apk`**: نفسه بدون أي اتصال بالشبكة.
- **`SHA256SUMS.txt`**: بصمات الملفين.

### حجم الـAPK والذاكرة

يتعبّى مع كل إصدار بأرقام انقاست فعلاً؛ والصف اللي ما انقاس نكتب عليه إنه ما انقاس.

| البند | هذا الإصدار | الإصدار السابق |
|---|---|---|
| `HomeLab-Launcher-<version>.apk` (الكامل) | `<bytes>` بايت | `<bytes>` بايت |
| `HomeLab-Launcher-<version>-offline.apk` | `<bytes>` بايت | `<bytes>` بايت |
| الخلفيات المدمجة داخل الـAPK | `<bytes>` بايت | `<bytes>` بايت |
| الذاكرة في الشاشة الرئيسية (الجهاز، نسخة أندرويد، الشاشة الأمامية، مدة السكون) | `<MB PSS>` أو «ما انقاس» | `<MB PSS>` أو «ما انقاس» |
| العدّادات بعد 20 دخول وخروج من إعدادات المشغّل و5 دقايق سكون (Activities وAppContexts وViews) | `<قبل> / <بعد>` أو «ما انقاس» | |

سقف الـAPK الكامل 4.2 ميجا. صفوف الذاكرة للعلم؛ والمقياس الحاسم أن ترجع العدّادات إلى قيمها الأولى.

### التثبيت

`adb install HomeLab-Launcher-<version>.apk`، أو انقله للتلفزيون بـ *Send files to TV* أو *Downloader*. ينزل جنب الشاشة الأصلية. اقرأ [دليل التثبيت](https://github.com/techdev-lab/homelab-launcher/blob/main/docs/INSTALL.ar.md) قبل ما تخليه الشاشة الرئيسية، وتأكد من توقيع الملف بـ[هذي الخطوات](https://github.com/techdev-lab/homelab-launcher/blob/main/SECURITY.ar.md).

</div>
