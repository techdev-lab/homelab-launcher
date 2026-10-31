## What and why · وش التغيير وليش

<!-- One or two sentences. Link the issue if there is one. · جملة أو جملتين. حط رابط البلاغ لو في. -->

## Checklist · قائمة التحقق

- [ ] Code, comments, docs and commit messages are in English. Arabic appears only in `*.ar.md` docs, the store listing, these templates and localization data. · الكود والتعليقات والوثائق ورسائل الـcommit بالإنجليزي. والعربي فقط في وثائق `*.ar.md` وصفحة المتجر وهذي القوالب وبيانات الترجمة.
- [ ] A changed user-facing doc has its Arabic twin changed too (or the pull request says it will follow). · وثيقة موجّهة للمستخدم تغيّرت، ونسختها العربية تغيّرت معها (أو الطلب يقول إنها بتلحق).
- [ ] `cd launcher && ./gradlew testOfflineReleaseUnitTest testOnlineReleaseUnitTest` passes. · الاختبارات تنجح.
- [ ] New behavior has at least one test of a failure case. · السلوك الجديد له اختبار حالة فشل على الأقل.
- [ ] No new permission, or the reason is written below. The offline build still has no INTERNET permission. · بدون صلاحية جديدة، أو السبب مكتوب تحت. ونسخة offline لسه بدون INTERNET.
- [ ] No IP addresses, Wi-Fi names, pairing codes, ADB keys or `local.properties`. · بدون عناوين IP أو أسماء واي فاي أو رموز إقران أو مفاتيح ADB أو `local.properties`.
- [ ] `python3 scripts/ci/check_hygiene.py` passes. · فحص النظافة ينجح.
