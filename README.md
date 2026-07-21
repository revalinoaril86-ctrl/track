# Sales Tracker (Android)

App sederhana untuk sales: input nama + URL server, tekan "Mulai tracking",
lalu HP akan mengirim lokasi ke `track_api.php` Anda tiap 15 detik lewat
foreground service (tetap jalan walau app diminimize / layar mati).

## 1. Push project ini ke GitHub

```bash
cd sales-tracker-app
git init
git add .
git commit -m "Sales tracker app"
git branch -M main
git remote add origin https://github.com/USERNAME/sales-tracker-app.git
git push -u origin main
```

## 2. Build APK otomatis

Setelah push, buka tab **Actions** di repo GitHub Anda. Workflow
"Build APK" akan berjalan otomatis. Setelah selesai (~3-5 menit):

1. Klik run yang barusan selesai
2. Scroll ke bagian **Artifacts**
3. Download `sales-tracker-apk.zip` → isinya `app-debug.apk`

Anda juga bisa trigger build manual lewat tab Actions > Build APK > Run workflow.

## 3. Install di HP sales

1. Kirim file `app-debug.apk` ke HP (via WhatsApp, Google Drive, dsb)
2. Buka file APK di HP → izinkan "install dari sumber tidak dikenal" kalau diminta
3. Install, buka app, isi nama sales dan URL server
   (default sudah diisi `http://172.26.47.101/SPIBJN4A/track/track_api.php`)
4. Tekan **Mulai tracking**, izinkan semua permission lokasi yang diminta
   (termasuk "Izinkan sepanjang waktu" untuk lokasi latar belakang)

## Menyesuaikan field yang dikirim ke server

Service mengirim POST `x-www-form-urlencoded` ke URL server dengan field:
`username`, `latitude`, `longitude`, `accuracy`.

Kalau `track_api.php` Anda pakai nama field lain, edit bagian `params` di
`app/src/main/java/com/spibjn4a/salestracker/LocationService.kt`.

## Mengubah interval kirim lokasi

Ubah nilai `UPDATE_INTERVAL_MS` di `LocationService.kt` (dalam milidetik,
default 15000 = 15 detik).

## Catatan baterai (Android)

Beberapa merk HP (Xiaomi, Oppo, Vivo, dll) punya battery optimizer agresif
yang bisa mematikan service latar belakang. Minta sales untuk:
- Matikan "Battery optimization" untuk app ini di Settings > Apps > Sales Tracker > Battery
- Kunci app dari recent apps (biar tidak ke-swipe otomatis)
