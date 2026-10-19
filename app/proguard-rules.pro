# Читабельні стек-трейси в Crashlytics (mapping-файл завантажується разом зі збіркою)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room, WorkManager, Glance, Firebase і Compose постачають власні consumer-правила.
# Окремі правила додавати лише якщо release-збірка покаже конкретну проблему.
