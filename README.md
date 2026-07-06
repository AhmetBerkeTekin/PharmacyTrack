# PharmacyTrack

PharmacyTrack, şehir ve ilçe bazında nöbetçi eczaneleri listeleyen Android uygulamasıdır.

Uygulama; nöbetçi eczaneleri görüntüleme, telefonla arama, haritada açma, favorilere ekleme ve son alınan verileri çevrimdışı görüntüleme özelliklerini içerir.

## Proje Yapısı

```text
PharmacyTrack/
├── app/                    # Android uygulaması
├── backend/                # FastAPI backend
│   ├── main.py
│   └── requirements.txt
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Android

* Kotlin
* XML Views
* MVVM
* Hilt
* Retrofit
* Room
* DataStore
* Navigation Component

### Gereksinimler

* Android Studio
* JDK 17
* Minimum Android sürümü: Android 7.0 — API 24

### Yerel API Adresi

Debug API adresi `app/build.gradle.kts` içinde tanımlanır:

```kotlin
buildConfigField(
    "String",
    "BASE_URL",
    "\"http://192.168.1.32:8000/\""
)
```

## Backend

Backend, FastAPI kullanılarak geliştirilmiştir.

### Kurulum

```bash
cd backend
python -m venv .venv
```

Windows:

```powershell
.\.venv\Scripts\Activate.ps1
```

macOS / Linux:

```bash
source .venv/bin/activate
```

Bağımlılıkları yükleyin:

```bash
pip install -r requirements.txt
```

Backend’i çalıştırın:

```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

### API

```http
GET /pharmacies/{city}
```

Örnek:

```http
GET /pharmacies/adana
```

Backend:

* Güncel nöbetçi eczane listesini alır.
* Şehir bazlı RAM cache kullanır.
* Cache’teki aktif şehirleri belirlenen saatlerde yeniler.
* Kullanıcıya bugünün nöbet listesini döndürür.

## Çevrimdışı Kullanım

Başarılı API cevapları Android tarafında Room veritabanına kaydedilir.

İnternet bağlantısı veya backend erişimi olmadığında uygulama, şehir için son kaydedilen listeyi gösterebilir. Eski tarihli veriler kullanıcıya güncellik uyarısıyla sunulur.

## Uyarı

Bu uygulama tıbbi tavsiye, teşhis veya tedavi hizmeti sunmaz.

Nöbetçi eczane bilgileri bilgilendirme amaçlıdır. Eczaneye gitmeden önce telefonla arayarak bilgileri doğrulamanız önerilir.

## Durum

Proje geliştirme aşamasındadır. Production API adresi, uygulama kimliği, ikonlar, splash ekranı ve mağaza hazırlıkları henüz tamamlanmamıştır.
