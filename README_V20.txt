ВОДОГРАЙ V20 — ХМАРНА ЗБІРКА APK

Це Android-проєкт для збірки APK через GitHub Actions без Android Studio.

Що зроблено:
- Android Gradle Plugin 8.7.3
- Gradle 8.9 у GitHub Actions
- Java 17 у GitHub Actions
- автоматична збірка debug APK
- APK зберігається як GitHub Actions artifact
- Supabase URL та Publishable key вже знаходяться в app/src/main/assets/www/config.js

Після завантаження файлів у GitHub:
1. Відкрити вкладку Actions.
2. Обрати Build VodoHrai APK.
3. Натиснути Run workflow.
4. Дочекатися завершення.
5. Відкрити готовий запуск і завантажити artifact VodoHrai-APK.
6. Розпакувати artifact і встановити APK на телефон.

Примітка: debug APK призначений для тестування. Для публічного релізу потрібна окрема release-збірка з власним ключем підпису.
