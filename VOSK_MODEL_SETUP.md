# اضافه کردن مدل صوتی Vosk (اجباری برای اجرا)

این پروژه از موتور Vosk برای wake word استفاده می‌کند (بدون نیاز به حساب/AccessKey).
بدون این مدل، اپ کامپایل و نصب می‌شود ولی گفتن "Jarvis" هیچ واکنشی نخواهد داشت
(Settings → Local model status: "Not installed" را نشان می‌دهد).

## مراحل

1. از https://alphacephei.com/vosk/models فایل `vosk-model-small-en-us-0.15` را دانلود کنید.
2. آن را از حالت zip خارج (extract) کنید.
3. پوشهٔ استخراج‌شده (`vosk-model-small-en-us-0.15`) را به اسم `model` تغییر نام دهید.
4. این پوشه را دقیقاً در مسیر زیر پروژه قرار دهید:
   ```
   app/src/main/assets/model/
   ```
   ساختار نهایی باید این‌گونه باشد:
   ```
   app/src/main/assets/model/am/
   app/src/main/assets/model/conf/
   app/src/main/assets/model/graph/
   app/src/main/assets/model/ivector/
   app/src/main/assets/model/README
   ```
5. commit و push کنید — چون این پوشه چند فایل باینری دارد، بهتر است این کار
   با `git add -A` از ترمینال (نه ویرایشگر وب) انجام شود.

## نکتهٔ مهم دربارهٔ مشکل قبلی "Local model status: not installed"

اگر این خطا را قبلاً دیده‌اید، علتش این بود که پوشهٔ `model` یا اصلاً به مسیر
`app/src/main/assets/model/` کپی نشده بود، یا موقع push به گیت‌هاب (به دلایل
جابه‌جایی/فشرده‌سازی پوشه‌های تودرتو در جلسات قبلی) از قلم افتاده بود.
**بعد از push، حتماً از طریق وب گیت‌هاب مسیر `app/src/main/assets/model/am/final.mdl`
را باز کنید و مطمئن شوید آن فایل واقعاً آنجا وجود دارد** — قبل از build کردن.
