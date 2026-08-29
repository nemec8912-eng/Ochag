package com.ochag.app.ui.localization

import java.util.Locale

private val en = mapOf(
    "Очаг" to "Hearth", "Чаты" to "Chats", "Контакты" to "Contacts",
    "Звонки" to "Calls", "Файлы" to "Files", "Профиль" to "Profile",
    "Поиск" to "Search", "Сбросить" to "Reset", "Непрочитанные" to "Unread",
    "Ничего не нашли" to "Nothing found", "Здесь будут ваши чаты" to "Your chats will appear here",
    "Попробуй изменить поиск" to "Try changing your search", "Начни разговор с близкими" to "Start a conversation with family",
    "Семейный чат" to "Family chat", "Личный разговор" to "Private conversation",
    "Групповой чат" to "Group chat", "Тематический чат" to "Topic chat",
    "Только для вас" to "Only for you", "В сети" to "Online", "Не в сети" to "Offline",
    "Гость" to "Guest", "Изменить тему" to "Change theme", "Режим приложения" to "App mode",
    "Светлая" to "Light", "Тёмная" to "Dark", "Стиль приложения и чатов" to "App and chat style",
    "Тёплая" to "Warm", "Современная" to "Modern", "Классическая" to "Classic",
    "Готово" to "Done", "Язык приложения" to "App language", "Сохранить" to "Save",
    "Отмена" to "Cancel", "Блокировка приложения" to "App lock", "Блокировка включена" to "App lock enabled",
    "Автоматическая блокировка" to "Automatic lock", "Сразу после сворачивания" to "Immediately after minimizing",
    "Через 1 минуту" to "After 1 minute", "Через 5 минут" to "After 5 minutes",
    "Через 30 минут" to "After 30 minutes", "Только после полного закрытия" to "Only after fully closing",
    "Отключить блокировку" to "Disable app lock", "Выберите способ защиты" to "Choose protection method",
    "Пароль" to "Password", "Графический ключ" to "Pattern", "Нарисуйте ключ" to "Draw pattern",
    "Повторите ключ" to "Repeat pattern", "Введите код или пароль" to "Enter code or password",
    "Повторите" to "Repeat", "Минимум 4 точки или символа" to "At least 4 dots or characters",
    "Включить" to "Enable", "Закрыть" to "Close", "Подключение…" to "Connecting…",
    "Личное пространство в «Очаге»" to "Your personal space in Hearth",
    "Владелец «Очага»" to "Hearth owner", "Внешний вид" to "Appearance",
    "Настрой «Очаг» под себя" to "Personalize Hearth", "Тёмная тема" to "Dark theme",
    "Светлая тема" to "Light theme", "Стиль шрифта" to "Font style",
    "Тёплый" to "Warm", "Современный" to "Modern", "Классика" to "Classic",
    "Размер текста всего приложения" to "Text size throughout the app",
    "Очень маленький" to "Very small", "Маленький" to "Small", "Средний" to "Medium",
    "Большой" to "Large", "Очень большой" to "Very large", "Семья" to "Family",
    "Безопасность" to "Security", "Телефон" to "Phone", "Сохранить номер" to "Save number",
    "Номер сохранён" to "Number saved", "Включена" to "Enabled", "Не включена" to "Disabled",
    "Выйти из «Очага»" to "Sign out of Hearth", "Новый чат" to "New chat",
    "Личный" to "Private", "Группа" to "Group", "Тема" to "Topic",
    "Название группы" to "Group name", "Название темы" to "Topic name",
    "Участники" to "Members", "Пригласить" to "Invite", "Создать группу" to "Create group",
    "Создать тему" to "Create topic", "Создать чат" to "Create chat",
    "Хранитель Очага" to "Keeper of the Hearth", "Выберите бесплатную нейросеть" to "Choose a free AI service",
    "В Очаге" to "Inside Hearth", "Открыть отдельно" to "Open separately", "Отдельно" to "External",
    "Введите PIN-код" to "Enter PIN", "Введите пароль" to "Enter password",
    "Введите графический ключ" to "Enter pattern", "Код доступа" to "Access code",
    "Неверный код или пароль" to "Incorrect code or password", "Открыть" to "Unlock",
    "Сбросить рисунок" to "Reset pattern"
)

private val common = mapOf(
    "de" to mapOf("Чаты" to "Chats", "Контакты" to "Kontakte", "Звонки" to "Anrufe", "Файлы" to "Dateien", "Профиль" to "Profil", "Сохранить" to "Speichern", "Отмена" to "Abbrechen", "Язык приложения" to "App-Sprache", "Хранитель Очага" to "Hüter des Herdes"),
    "fr" to mapOf("Чаты" to "Discussions", "Контакты" to "Contacts", "Звонки" to "Appels", "Файлы" to "Fichiers", "Профиль" to "Profil", "Сохранить" to "Enregistrer", "Отмена" to "Annuler", "Язык приложения" to "Langue de l’application", "Хранитель Очага" to "Gardien du foyer"),
    "es" to mapOf("Чаты" to "Chats", "Контакты" to "Contactos", "Звонки" to "Llamadas", "Файлы" to "Archivos", "Профиль" to "Perfil", "Сохранить" to "Guardar", "Отмена" to "Cancelar", "Язык приложения" to "Idioma de la aplicación", "Хранитель Очага" to "Guardián del hogar"),
    "zh" to mapOf("Чаты" to "聊天", "Контакты" to "联系人", "Звонки" to "通话", "Файлы" to "文件", "Профиль" to "个人资料", "Сохранить" to "保存", "Отмена" to "取消", "Язык приложения" to "应用语言", "Хранитель Очага" to "炉火守护者"),
    "ja" to mapOf("Чаты" to "チャット", "Контакты" to "連絡先", "Звонки" to "通話", "Файлы" to "ファイル", "Профиль" to "プロフィール", "Сохранить" to "保存", "Отмена" to "キャンセル", "Язык приложения" to "アプリの言語", "Хранитель Очага" to "炉の守護者")
)

fun ochagText(source: String): String {
    return when (Locale.getDefault().language) {
        "en" -> en[source] ?: source
        "ru" -> source
        else -> common[Locale.getDefault().language]?.get(source) ?: en[source] ?: source
    }
}
