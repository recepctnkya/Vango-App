# VanGO Tablet / Telefon sürüm yönetimi (makro / mikro)

Tek KMP kaynak (`VanGO-v0.91-001`); Android tablet, telefon ve iOS aynı makro/mikro’yu paylaşır.

| Kavram | Örnek | Anlam |
|--------|--------|--------|
| **Makro** | `v0.91` | Ürün çizgisi |
| **Mikro** | `v0.91-0001` | Anlamlı değişiklik takip |
| **versionName** | `0.91-001` | Android / iOS görünen sürüm |
| **versionCode** | `91001` | Store / yükleme kodu |

## Dosyalar

- `VERSION.json`, `changelog.json`
- `composeApp/.../HexnetAppVersion.kt` (Sync üretir)
- `scripts/Sync-VersionFiles.ps1`, `scripts/Bump-Version.ps1`
- `build-debug-apk.ps1` Sync’i otomatik çağırır

```powershell
.\scripts\Bump-Version.ps1 -Bump micro -Notes "…"
.\build-debug-apk.ps1
```

## Codemagic

`codemagic.yaml`, Android ve iOS Simulator için ayrı iş akışları tanımlar.
Android debug APK, iOS ise ziplenmiş bir Simulator uygulaması üretir. iOS iş
akışı imzasızdır ve cihaza kurulabilir IPA üretmez; bunun için Apple imzalama
sertifikalarının ve provisioning profile'ların Codemagic'de yapılandırılması
gerekir.

Not: iOS'taki BLE bağlantısı ve QR tarayıcı şu an yer tutucudur; bu derleme
iOS özelliklerinin Android ile eşdeğer olduğunu göstermez.
HTTP istemcisi iOS'ta platformun `NSURLSession` tabanlı Ktor Darwin motorunu
kullanır.

Android iş akışı depodaki sürüm değerlerini kullanır ve bu depoda bulunmayan
harici PowerShell modülüne bağlı `syncHexnetVersion` görevini atlar.

KMQTT 1.0.0, iOS Simulator için gereken OpenSSL KLIB'ini Maven yayımına dahil
etmediğinden iOS iş akışı dosyayı sabit sürüm etiketinden indirip SHA-256
checksum'ını doğrular.
