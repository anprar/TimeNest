# TimeNest — Screen Timer Global v2

Aplikasi Android native (Kotlin, tanpa WebView/backend/iklan) untuk membatasi waktu HP global.

## Fitur Tahap 1 (sudah jadi, bisa build)
- Mode Hitung Mundur (durasi 1 mnt–24 jam, preset 5/15/30/45/60/90/120)
- Mode Selesai Pada Jam (time picker, konfirmasi, lintas tengah malam -> besok)
- endTime absolut sebagai sumber kebenaran, progress + countdown
- ForegroundService + notifikasi ongoing (update tiap 5 dtk)
- DeviceAdmin lockNow() saat waktu habis, peringatan jika izin belum aktif
- PIN orang tua (SHA-256) untuk batal/pause, riwayat lokal (DataStore)
- Peringatan suara+getar 10/5/1 mnt, boot recovery, diagnosa OPPO/ColorOS
- Unit test TimeCalc (countdown, jam, tengah malam, pause/resume, format)

## Build APK (GitHub Actions)
Push ke GitHub -> Actions "Build APK" -> artifact `timenest-debug-apk`.

## Build lokal
1. Android Studio Ladybug+, SDK 34, JDK 17
2. Open folder ini, sync Gradle, Run
3. Atau: `gradle :app:assembleDebug`

## Instal ke OPPO A5 2020
1. Install APK debug, buka aplikasi
2. Pengaturan > Buat PIN > Aktifkan izin kunci (Device Admin)
3. Izinkan notifikasi
4. Baterai: Pengaturan OPPO > Baterai > TimeNest > izinkan autostart + jalan background, matikan optimasi baterai, kunci di recent-apps

## Fitur Tahap 4 (selesai)
- Screen timeout sesi: Jangan ubah / 15 dtk–10 mnt, restore otomatis (butuh izin WRITE_SETTINGS)
- Jadwal harian via AlarmManager (durasi / sampai jam), anti dobel sesi, arm ulang tiap hari
- QS Tile: status + sisa waktu, klik buka app (tanpa batal bebas)
- Widget: sisa waktu + jam selesai + tombol buka

## Batasan jujur
- Lock memakai DeviceAdmin resmi; jika OEM menolak, dicatat "gagal kunci" di riwayat.
- Exact alarm & background bisa ditolak ColorOS — gunakan diagnosa + autostart.
