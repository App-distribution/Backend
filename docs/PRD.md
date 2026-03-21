# AppDistribution — Product Requirements Document

**Version:** 1.0
**Date:** 2026-03-21
**Status:** Approved for MVP

---

## 1. Цели продукта

### Проблема
Команды разработки и QA тратят значительное время на распространение тестовых APK:
- APK отправляются в мессенджеры, теряются в истории чатов
- Нет структуры и истории сборок
- Тестировщики не знают, какая версия актуальна
- Нет возможности отследить, кто что установил
- Установка из неизвестных источников вызывает затруднения у нетехнических пользователей

### Решение
Централизованная платформа для дистрибуции тестовых сборок Android-приложений с мобильным клиентом, которая делает процесс тестирования быстрым, прозрачным и управляемым.

### Бизнес-цели
1. Сократить время от загрузки сборки до установки у тестировщика с часов до минут
2. Обеспечить полную прозрачность: кто какую версию установил
3. Предоставить структурированный каталог сборок с историей
4. Интегрировать в CI/CD pipeline через API

---

## 2. Роли и персонажи

| Роль | Описание | Ключевые задачи |
|------|----------|-----------------|
| **Admin** | Системный администратор workspace | Управление командой, проектами, доступами, просмотр аудита |
| **Release Manager / Uploader** | Разработчик, отвечающий за релизы | Загрузка APK, управление каналами, создание invite-ссылок |
| **Tester** | QA-инженер или тестировщик | Просмотр сборок, скачивание и установка APK, обновление |
| **Viewer** | Менеджер, наблюдатель | Только просмотр сборок и метаданных, без скачивания |

---

## 3. User Flows

### 3.1. Uploader: загрузка новой сборки
```
Login → Select Project → Upload APK →
[Auto-extract metadata] → Fill release notes →
Select channel + visibility → Confirm →
Notify testers (auto) → Build available
```

### 3.2. Tester: установка сборки
```
Login → Build Feed / Project →
Find build (search/filter) → Build Detail →
[Check installed version vs available] →
Download APK → Verify checksum →
Grant "Unknown sources" permission →
Install via system installer →
App installed
```

### 3.3. Tester: обновление до новой версии
```
Push notification → Open app →
"Updates Available" screen →
Select build → Install → Done
```

### 3.4. Admin: просмотр аналитики
```
Login → Project → Analytics tab →
View: who installed, download stats,
current versions by user → Export
```

---

## 4. Функциональные требования

### MVP (Phase 1)
- [x] Email + OTP авторизация, JWT-сессии
- [x] Workspace + Projects + Builds CRUD
- [x] Загрузка APK с прогрессом и извлечением метаданных
- [x] Каталог сборок с фильтрацией (project, channel, env, date)
- [x] Карточка сборки с полными метаданными
- [x] Скачивание APK через signed URL
- [x] Установка APK через системный installer flow
- [x] Определение установленной версии на устройстве
- [x] Release notes / changelog
- [x] Роли: Admin, Uploader, Tester, Viewer
- [x] Push-уведомления о новых сборках (Firebase FCM)
- [x] Audit log основных действий

### Phase 2
- [ ] Invite-ссылки для тестировщиков
- [ ] Группы тестировщиков и автоназначение доступа
- [ ] QR-коды на сборку
- [ ] Download resume / retry
- [ ] Pinned builds + Favorite projects
- [ ] Экран "Updates Available"
- [ ] Сравнение двух сборок
- [ ] Analytics dashboard
- [ ] Build expiry + retention policy

### Phase 3
- [ ] Web admin panel
- [ ] CI/CD webhook интеграция
- [ ] Slack / Teams нотификации
- [ ] AAB + mapping files поддержка
- [ ] Multi-tenant (несколько организаций)
- [ ] Crash reporting интеграция
- [ ] Deep links + App Links

---

## 5. Нефункциональные требования

| Параметр | Требование |
|----------|-----------|
| Размер APK | До 500 МБ |
| Latency API | < 200ms (p95, без файловых операций) |
| Upload throughput | Прогрессивный, без таймаута до 10 мин |
| Offline | Просмотр кешированных данных, очередь действий |
| Security | Signed URLs (15 min TTL), JWT (access 1h + refresh 30d) |
| Availability | 99.5% uptime (self-hosted) |

---

## 6. MVP vs Future Scope

### MVP включает
- Полный auth flow (email OTP → JWT)
- Workspace → Projects → Builds иерархия
- Upload APK (multipart, auto metadata extraction)
- Catalog + filters + search
- Build detail с полными метаданными
- Download + Install flow + permission handling
- Installed version detection
- Basic push notifications
- Audit log

### Вне MVP (future)
- Invite links, groups, QR-codes
- Analytics dashboard
- Web admin panel
- CI/CD API integrations
- Crash reporting
- Multi-tenant support
