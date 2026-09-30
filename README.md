# TimeNest — Screen Timer Global v2

Aplikasi Android native (Kotlin, tanpa WebView/backend/iklan) untuk membatasi waktu HP global.

## Fitur Tahap 1 (sudah jadi, bisa build)
- Mode Hitung Mundur (durasi 1 mnt–24 jam, preset 5/15/30/45/60/90/120)
- Mode Selesai Pada Jam (time picker, konfirmasi, lintas tengah malam -> besok)
- endTime absolut sebagai sumber kebenaran, progress + countdown
- ForegroundService + notifikasi ongoing (update tiap 5 dtk)
- DeviceAdmin lockNow() saat waktu habis, peringatan jika izin belum aktif
- PIN orang tua (SHA-256) untuk batal/pause, riwayat lokal (DataStore)
- Peringatan suara+getar 10/5/1 mnt, boot recovery, diagnosa baterai
- Unit test TimeCalc (countdown, jam, tengah malam, pause/resume, format)

## Build APK (GitHub Actions)
Push ke GitHub -> Actions "Build APK" -> artifact `timenest-debug-apk`.

## Build lokal
1. Android Studio Ladybug+, SDK 34, JDK 17
2. Open folder ini, sync Gradle, Run
3. Atau: `gradle :app:assembleDebug`

## Instal ke HP (semua merek)
1. Install **TimeNest-release.apk** (signed, bukan debug), buka aplikasi
2. Ikuti setup: buat PIN (atau lewati), aktifkan semua izin
3. Izinkan notifikasi
4. Baterai: Pengaturan > Aplikasi > TimeNest > Baterai > Tanpa pembatasan; izinkan autostart; kunci di recent-apps

## Jika Play Protect memblokir
Itu normal untuk aplikasi luar Play Store yang memakai izin kunci layar.
1. Pada peringatan, ketuk **Detail lainnya / More details > Tetap instal / Install anyway**.
2. Bila tetap diblokir: Play Store > profil > Play Protect > roda gigi > matikan sementara "Pindai aplikasi", instal, lalu nyalakan lagi.
3. Jangan pakai versi debug (`com.timenest.debug`) — versi itu `debuggable` sehingga lebih dicurigai.

## Kunci keystore (penting)
File `timenest-release.keystore` + password di `D:\dokumentasi\Project\` (di luar repo) dan password di GitHub Secrets.
JANGAN hilang — update APK berikutnya wajib kunci yang sama, atau HP menolak update.

## Fitur Tahap 4 (selesai)
- Screen timeout sesi: Jangan ubah / 15 dtk–10 mnt, restore otomatis (butuh izin WRITE_SETTINGS)
- Jadwal harian via AlarmManager (durasi / sampai jam), anti dobel sesi, arm ulang tiap hari
- QS Tile: status + sisa waktu, klik buka app (tanpa batal bebas)
- Widget: sisa waktu + jam selesai + tombol buka

## Batasan jujur
- Lock memakai DeviceAdmin resmi; jika OEM menolak, dicatat "gagal kunci" di riwayat.
- Exact alarm & background bisa ditolak ColorOS — gunakan diagnosa + autostart.
