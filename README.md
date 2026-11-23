# ROFOFAQ
App for daily expenditure 
# ROFOFAQ

**App for daily expenditure** — تطبيق مفتوح المصدر لتتبع الإنفاق اليومي والمصروفات والميزانيات الشخصية.

[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Status](https://img.shields.io/badge/status-in%20development-yellow)]()

---

## نظرة عامة

ROFOFAQ تطبيق بسيط وقوي لتنظيم الإنفاق اليومي. يجمع أفضل أفكار المشاريع المفتوحة المصدر في مجال التمويل الشخصي مع التركيز على:

- سهولة الاستخدام والسرعة
- الخصوصية (Self-hosted أولاً)
- تحليلات واضحة
- دعم الموبايل والويب
- دعم اللغة العربية بشكل أساسي

---

## المميزات المستهدفة (Feature Roadmap)

### Core Tracking
- [x] تسجيل سريع للمعاملات (تاريخ + مبلغ + فئة)
- [ ] حسابات متعددة (نقدي، بطاقة، ادخار...)
- [ ] فئات وفئات فرعية (Two-level categories)
- [ ] تحويلات بين الحسابات
- [ ] معاملات متكررة (Recurring) + قوالب
- [ ] تقسيم المعاملات (Splits)
- [ ] مرفقات صور للإيصالات
- [ ] علامات (Tags) اختيارية
- [ ] تتبع الموقع الجغرافي (اختياري)

### Budgeting & Goals
- [ ] ميزانيات حسب الفئة
- [ ] أهداف ادخار مع تتبع التقدم
- [ ] تتبع القروض والتمويل
- [ ] تنبيهات عند تجاوز الميزانية

### Insights & Reports
- [ ] مخطط دائري شهري حسب الفئات
- [ ] تدفق نقدي (Income vs Expenses)
- [ ] صافي الثروة (Net Worth)
- [ ] رسوم بيانية قابلة للتخصيص
- [ ] فلترة وبحث متقدم
- [ ] تقديرات إنفاق مستقبلية

### Import / Export & Sync
- [ ] استيراد: CSV, Excel, OFX, QFX, QIF
- [ ] تصدير CSV
- [ ] مزامنة بنكية (اختياري لاحقاً)
- [ ] قواعد تصنيف تلقائي

### Security & Multi-user
- [ ] 2FA (TOTP)
- [ ] قفل التطبيق (PIN)
- [ ] دعم متعدد المستخدمين (اختياري)
- [ ] Rate limiting على تسجيل الدخول

### UX & Platforms
- [ ] واجهة ويب متجاوبة (Desktop + Mobile)
- [ ] PWA (يمكن تثبيتها كتطبيق)
- [ ] وضع داكن / فاتح
- [ ] دعم متعدد اللغات والعملات
- [ ] دعم العربية بشكل أساسي

### AI (مرحلة لاحقة)
- [ ] التعرف على نص الإيصال من الصورة
- [ ] إنشاء رسوم بيانية مخصصة بالذكاء الاصطناعي

---

## البنية المقترحة (Architecture)
Clients                           │
│         Web (PWA)  │  Mobile (Flutter)              │
└─────────────────────┬───────────────────────────────┘
│ HTTPS / REST
┌─────────────────────▼───────────────────────────────┐
│              Backend API                            │
│  - Auth (JWT + 2FA)                                 │
│  - Transactions / Accounts / Categories / Budgets   │
│  - Import/Export Engine                             │
│  - Rules Engine (auto-categorization)               │
└─────────────┬───────────────────────┬───────────────┘
│                       │
┌────────▼────────┐     ┌────────▼────────┐
│  SQLite /       │     │  File Storage   │
│  PostgreSQL     │     │  (Local / S3)   │
└─────────────────┘     └─────────────────┘
---

## Tech Stack المقترح

| المكون              | التقنية المقترحة              | ملاحظات |
|---------------------|-------------------------------|---------|
| **Backend**         | Go أو Python (FastAPI)        | خفيف وسريع |
| **Frontend**        | Vue 3 + TypeScript أو Flutter | PWA أو تطبيقات أصلية |
| **قاعدة البيانات**  | SQLite (افتراضي) + PostgreSQL | سهولة النشر |
| **التخزين**         | Local disk أو S3-compatible   | للمرفقات |
| **الحاويات**        | Docker + Docker Compose       | نشر سهل |
| **المصادقة**        | JWT + 2FA (TOTP)              | أمان أساسي |

### خيارات تقنية مفصلة

**الخيار أ (موصى به للبداية السريعة):**
- Backend: **Go**
- Frontend: **Vue 3 + TypeScript**
- DB: **SQLite** (يمكن الترقية لاحقاً لـ PostgreSQL)
- UI: متجاوب + PWA

**الخيار ب (للموبايل أولاً):**
- Backend: **Python FastAPI**
- Frontend: **Flutter** (Android + iOS + Web)
- DB: **SQLite / PostgreSQL**

**الخيار ج (أبسط ما يمكن):**
- Backend + Frontend: **Go** مع HTML/CSS/JS خام
- تخزين: **JSON** أو SQLite
- مناسب للـ Homelab

---

## هيكل المشروع المقترح
ROFOFAQ/
├── backend/                 # API server
│   ├── cmd/
│   ├── internal/
│   │   ├── auth/
│   │   ├── transactions/
│   │   ├── accounts/
│   │   ├── categories/
│   │   ├── budgets/
│   │   └── import/
│   ├── migrations/
│   └── go.mod / requirements.txt
├── frontend/                # Web / PWA
│   ├── src/
│   │   ├── components/
│   │   ├── views/
│   │   ├── stores/
│   │   └── api/
│   └── package.json
├── mobile/                  # (اختياري) Flutter
├── docker/
│   ├── Dockerfile
│   └── docker-compose.yml
├── docs/
├── .gitignore
├── LICENSE
└── README.md
