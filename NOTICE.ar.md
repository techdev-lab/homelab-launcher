<div dir="rtl">

# إشعارات وشكر

[English](NOTICE.md) · العربية

## الأصل

هوم لاب لانشر (HomeLab Launcher) مُتفرّع (fork) من لانشر `tvhome` في مشروع [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv)، برخصة MIT، حقوق النشر (c) 2026 Gathin. تفرّع من الـcommit `e561b0551c57700397c6862e9513fc23ef601c02`. وبقي اسم حزمة أندرويد `com.gathin.tvhome` كما في الأصل عشان التثبيتات الموجودة تتحدّث مكانها.

## الإلهام في التصميم

خياري *شكل الرصيف* (الزجاجي العائم) و*شكل الساعة* (الزجاجية) في إعدادات المشغّل استُلهما من الوصف العلني لمشروع [tvOS Launcher](https://github.com/techgenius1/tvOS-Launcher) للمطوّر techgenius، برخصة Apache 2.0. قُرئ ملف README فقط. ما في هذا المشروع أي كود أو صورة أو خلفية منه: الرسم من عندنا ويستعمل حساب الألوان في `Glass`. ولو نُسخ كود منه يوماً، يحتفظ ذلك الملف بترويسة Apache 2.0 وتُضاف نسخة الرخصة ويذكر هذا القسم وش تغيّر.

وفكرة إجراء *التجاهل* لزر الريموت (`app_keys`) وخيار إخفاء التطبيقات من الشبكة استُلهمتا من ميزات في إصدارات CleanYourTV 3.x العلنية (شوف «الأصل»، رخصة MIT). الكود في الاثنتين مكتوب هنا وغير منسوخ.

والإصلاحات الخمسة في الدفعة F (`docs/BATCH_F_FIXES.ar.md`: مستمع التركيز المتسرّب، واستدعاءات `startActivity` المحمية في خدمتَي الأزرار، وتحديث الساعة قبل أندرويد 12، ومسار إعدادات الصورة في Xiaomi، وأيقونة البانر وخلفية منتقي الشاشة الرئيسية) كان سببها مشاكل اكتشفها مطوّر CleanYourTV وأصلحها في إصدارات لاحقة، له الشكر على اكتشافها. أُخذت الأفكار فقط: التعديلات اللاحقة ما فيها رخصة واضحة خاصة بها، وما في هذا المشروع أي كود منها، والكود مكتوب هنا.

وأفكار *الرصيف الجانبي* و*تعتيم الخلفية* و*الأرقام العربية للساعة* استُلهمت من الوصف العلني في README لمشروع [sukoon-tv](https://github.com/MultiX0/sukoon-tv) للمطوّر MultiX0. المشروع ما يعلن رخصة، فما أُخذ منه إلا الأفكار: قُرئ README فقط، وما في هذا المشروع أي كود أو صورة منه، والتنفيذ من عندنا.

## مظهر «مشاهد» (couch-tv)

مظهر الشاشة الرئيسية *مشاهد* (`docs/BATCH_S_SCENES.ar.md`) مستوحى من الشاشة الرئيسية في [couch-tv](https://github.com/Seagey/couch-tv) للمطوّر Seagey، رخصة MIT. المأخوذ: صور المشاهد الخمس (`scene-night` و`scene-fog` و`scene-golden` و`scene-dawn` و`scene-water`، لكل منها قصاصتها ونسختها المموّهة) من الإيداع `31b8f26f445aa023ff1df3cda660740a30a8a8a5`. يقول ملف `CREDITS.md` عندهم عنها: "Use them however you like". صُنعت لـ couch-tv من نص فقط بلا صورة مصدر، وحالة حقوقها خارج هذه العبارة غير مؤكدة عندنا. الأصول محفوظة كما هي في `scenes/master/` (بصمة SHA-256 لكل ملف في `scenes/manifest.json`)، والـ APK يحمل نسخاً WebP أعاد ترميزها `scenes/to-scenes.py`. غير المأخوذ: أي كود أو CSS أو نص أو خط. كود الرسم مكتوب هنا. لا نستعمل صور Classic عندهم (رخصة مشاركة بالمثل). الاسم *مشاهد* وصفي ولا يوحي أن صاحب المشروع يصونه أو يزكّيه.

نص رخصة couch-tv كما نُشر موجود في [النسخة الإنجليزية](NOTICE.md#scenes-look-couch-tv)، وحقوقه: `Copyright (c) 2026 Seagey`.

## الخطوط

الخطوط غير مشمولة برخصة MIT في [LICENSE](LICENSE). خط Outfit حقوقه لـThe Outfit Project Authors، وخط IBM Plex Sans Arabic حقوقه لشركة IBM Corp.، والاثنان برخصة SIL Open Font License 1.1.

| الخط | الملفات | الرخصة | المصدر |
|---|---|---|---|
| Outfit | `launcher/app/src/main/res/font/outfit*.ttf` | SIL OFL 1.1 (`launcher/FONT-LICENSE-OFL.txt`) | جاء مع الأصل |
| IBM Plex Sans Arabic | `launcher/app/src/main/res/font/plex_arabic.ttf` (عادي) و`plex_arabic_light.ttf` (خفيف) و`plex_arabic_medium.ttf` (متوسط) | SIL OFL 1.1 (`launcher/FONT-LICENSE-IBMPLEXSANSARABIC-OFL.txt`) | [google/fonts](https://github.com/google/fonts) عند الـcommit `9862abc066c9b83bf84c3e8ed1fc2d8d48f9bbf0`، الملفات `ofl/ibmplexsansarabic/IBMPlexSansArabic-{Regular,Light,Medium}.ttf`، بصمات SHA-256 مذكورة في النسخة الإنجليزية |
| Antonio (الأرقام والنقطتان فقط، وزن 300) | `launcher/app/src/main/res/font/antonio_digits.ttf` | SIL OFL 1.1 (`launcher/FONT-LICENSE-ANTONIO-OFL.txt`)، حقوقها The Antonio Project Authors | [google/fonts](https://github.com/google/fonts) عند الإيداع `95f4904fc8bcf26d3420fe315560c96417c6dec7`، الملف `ofl/antonio/Antonio[wght].ttf` (74,104 بايت)، SHA-256 `9E95A2258ECDF3E45C72C5BBEA1C4CD350E8F7BEBC87C9DBA53B29B1890B8903`؛ المجموعة الجزئية من `scenes/make-clock-font.py` (2,900 بايت) |

خط Readex Pro (كان للبطاقات لين 2.8) أُزيل في 2.9.

## مصادر البيانات (نسخة online)

- الكورة: واجهة ESPN العامة (`site.api.espn.com`)، بدون مفتاح، للوحات النتائج فقط. غير تابعين لـESPN.
- الدوريات العربية المحلية: واجهة [TheSportsDB](https://www.thesportsdb.com) المجانية v1 بمفتاحها العام الموثّق، المباريات الجاية فقط. غير تابعين لـTheSportsDB.
- طرق الصلاة: زوايا كل طريقة وفروق دقائقها تتبع [api.aladhan.com](https://aladhan.com/calculation-methods)؛ والأوقات تنحسب على التلفزيون. غير تابعين لـAladhan.
- الطقس: واجهة [Open-Meteo](https://open-meteo.com)، مجانية وبدون مفتاح.
- الأخبار (مطفية افتراضياً): أي RSS يضيفه المستخدم.

## العلامات التجارية

أسماء التطبيقات وشعاراتها في لقطات الشاشة (YouTube وNetflix وMBC Shahid وJellyfin وPrime Video وTOD وGoogle Play وغيرها) ملك أصحابها. تظهر لأن هذي التطبيقات كانت مثبّتة على التلفزيون التجريبي فقط. هذا المشروع غير تابع لجوجل ولا لأي منهم.

## الخلفيات

الصور في `wallpapers/homelab/` يرسمها `wallpapers/gen.html` (كود canvas في هذا المستودع، بدون صور من طرف ثالث) ومشمولة برخصة MIT أعلاه.

</div>
