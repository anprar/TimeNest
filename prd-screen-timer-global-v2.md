# PRD — Screen Timer Global v2

## 1. Ringkasan

Buat aplikasi Android native tanpa WebView, tanpa iklan, tanpa akun, dan tanpa backend untuk membatasi waktu penggunaan HP secara global.

Aplikasi memiliki dua metode pengaturan waktu:

1. **Mode Countdown Durasi** — pengguna memilih durasi dalam menit/jam sejak tombol Mulai ditekan.
2. **Mode Jam Selesai** — pengguna memilih jam tertentu kapan sesi harus berakhir.

Kedua mode menghasilkan satu `endTime` absolut. Timer tetap berjalan walaupun anak berpindah-pindah aplikasi atau game. Ketika waktu habis, aplikasi mengunci perangkat dan mematikan layar menggunakan mekanisme Android yang tersedia dan diizinkan pengguna.

## 2. Tujuan

- Menghitung waktu penggunaan HP secara global.
- Mendukung pengaturan berdasarkan durasi atau jam selesai.
- Memungkinkan orang tua berkata “main 45 menit” atau “boleh sampai jam 8 malam”.
- Tetap berjalan ketika aplikasi lain dibuka.
- Mengunci perangkat saat waktu berakhir.
- Menyediakan fitur lengkap yang sekelas aplikasi timer/screen timeout, tanpa iklan.

## 3. Mode Waktu

### 3.1 Mode Countdown Durasi

Label UI: **Hitung Mundur**.

Pengguna mengatur:

- Jam.
- Menit.
- Detik opsional.

Contoh:

- 15 menit.
- 45 menit.
- 1 jam 30 menit.
- 2 jam.

Preset:

- 5 menit.
- 15 menit.
- 30 menit.
- 45 menit.
- 60 menit.
- 90 menit.
- 120 menit.

Sediakan input durasi manual:

- Minimum 1 menit.
- Maksimum 24 jam.
- Validasi agar durasi tidak nol.
- Tombol tambah/kurang atau input angka langsung.

Ketika pengguna menekan Mulai:

```text
startTime = waktu sekarang
endTime = startTime + durasi
```

Timer menampilkan sisa waktu hingga `endTime`.

### 3.2 Mode Jam Selesai

Label UI: **Selesai Pada Jam**.

Pengguna mengatur:

- Jam selesai.
- Menit selesai.
- Opsional tanggal selesai jika aplikasi mendukung sesi lintas hari.

Contoh:

- Sekarang pukul 16:30, pilih selesai pukul 18:00.
- Sekarang pukul 21:00, pilih selesai pukul 22:30.

Ketika pengguna menekan Mulai:

```text
startTime = waktu sekarang
endTime = tanggal dan jam selesai yang dipilih
```

Jika jam selesai lebih awal daripada waktu sekarang:

- Tampilkan pilihan “besok” jika fitur lintas tengah malam didukung.
- Jangan langsung memulai tanpa konfirmasi.
- Tampilkan ringkasan durasi yang akan berjalan.

Contoh konfirmasi:

```text
Timer akan berjalan selama 1 jam 30 menit
dan selesai pukul 18:00.

[Batalkan] [Mulai Timer]
```

### 3.3 Pemilihan Mode

Di dashboard sediakan tab atau segmented control:

```text
[Hitung Mundur] [Selesai Pada Jam]
```

Mode terakhir yang dipakai disimpan secara lokal.

Sediakan pengaturan mode default:

- Selalu buka Hitung Mundur.
- Selalu buka Selesai Pada Jam.
- Ingat mode terakhir.

## 4. Tampilan Dashboard

Dashboard menampilkan:

- Mode waktu aktif.
- Input waktu sesuai mode.
- Ringkasan waktu mulai.
- Ringkasan waktu selesai.
- Perkiraan durasi sesi.
- Tombol Mulai.
- Status izin penguncian.

Contoh mode countdown:

```text
Mode: Hitung Mundur
Durasi: 01:30:00
Selesai pada: 18:00

[ MULAI ]
```

Contoh mode jam selesai:

```text
Mode: Selesai Pada Jam
Selesai pada: 18:00
Durasi sesi: 01:30:00

[ MULAI ]
```

## 5. Timer Aktif

Saat timer aktif, tampilkan:

- Waktu tersisa besar.
- Jam selesai.
- Durasi awal.
- Mode yang digunakan.
- Progress bar atau progress ring.
- Status timer.

Format waktu:

- Sisa lebih dari 1 jam: `01:30:00`.
- Sisa kurang dari 1 jam: `45:20`.
- Sisa kurang dari 1 menit: `00:45`.

Gunakan `endTime` sebagai sumber kebenaran, bukan hanya mengurangi angka setiap detik. Dengan demikian timer tetap benar setelah:

- Aplikasi ditutup.
- Activity dibuat ulang.
- Layar mati.
- Perangkat masuk sleep.
- Service dimulai kembali.
- Perangkat reboot jika recovery diaktifkan.

## 6. Perilaku Global

Timer tidak terkait dengan satu game atau satu aplikasi.

Contoh:

```text
Sesi mulai pukul 16:00
Game A       20 menit
YouTube      15 menit
Game B       25 menit
Browser      10 menit
Timer habis  pukul 17:30
```

Aplikasi tidak perlu mengetahui aplikasi yang sedang dibuka. Foreground service harus menjaga timer tetap berjalan di background.

## 7. Fitur Wajib

### 7.1 Preset

Preset durasi:

- 5 menit.
- 15 menit.
- 30 menit.
- 45 menit.
- 60 menit.
- 90 menit.
- 120 menit.

Preset jam selesai:

- Selesai 30 menit dari sekarang.
- Selesai 1 jam dari sekarang.
- Selesai pukul tertentu.

Pengguna dapat mengubah urutan, menambah, atau menyembunyikan preset.

### 7.2 Durasi Manual

Input durasi manual mendukung:

- Jam.
- Menit.
- Detik opsional.

Validasi:

- Tidak boleh semua nilai nol.
- Menit dan detik boleh dinormalisasi otomatis.
- Contoh `90 menit` dapat ditampilkan sebagai `1 jam 30 menit`.
- Tampilkan ringkasan sebelum timer dimulai.

### 7.3 Jam Selesai Manual

Time picker harus mendukung:

- Format 24 jam.
- Format 12 jam jika mengikuti sistem.
- Pemilihan menit.
- Sesi melewati tengah malam.
- Konfirmasi jika waktu selesai kurang dari 1 menit dari sekarang.

Jika tanggal tidak dipilih dan jam selesai sudah lewat:

- Tampilkan pilihan “hari ini” atau “besok”.
- Default jangan diam-diam memilih besok tanpa memberi tahu pengguna.

### 7.4 Pause dan Resume

Jika pause aktif:

- Simpan sisa waktu ketika pause.
- Timer tidak berkurang saat pause.
- Saat resume, buat `endTime` baru berdasarkan waktu sekarang + sisa waktu.
- Pause/resume memerlukan PIN jika pengaturan keamanan mengharuskannya.

### 7.5 Batalkan

- Batalkan timer meminta PIN orang tua.
- Setelah berhasil, hentikan service.
- Hapus notifikasi ongoing.
- Simpan riwayat sebagai `DIBATALKAN`.
- Jangan mengunci perangkat ketika timer dibatalkan oleh orang tua.

### 7.6 Peringatan

Peringatan dapat diaktifkan berdasarkan sisa waktu:

- 10 menit.
- 5 menit.
- 1 menit.
- Waktu habis.

Pilihan:

- Suara.
- Getar.
- Notifikasi.
- Kombinasi.

Jangan menjalankan peringatan yang sama lebih dari sekali dalam satu sesi.

### 7.7 Aksi Saat Waktu Habis

Ketika waktu sekarang >= `endTime`:

1. Tandai sesi selesai secara atomik.
2. Hentikan countdown aktif.
3. Tampilkan notifikasi waktu habis.
4. Jalankan suara/getar jika aktif.
5. Panggil mekanisme lock Android jika diizinkan.
6. Matikan/akhiri foreground service setelah aksi selesai.
7. Simpan sesi ke riwayat.

Gunakan Device Administrator dan `DevicePolicyManager.lockNow()` atau mekanisme resmi Android yang kompatibel. Jangan mengklaim berhasil mengunci jika izin belum aktif.

## 8. Screen Timeout

Sediakan pengaturan terpisah dari timer global:

- 15 detik.
- 30 detik.
- 1 menit.
- 2 menit.
- 5 menit.
- 10 menit.
- Tidak pernah.
- Gunakan pengaturan sistem.

Pilihan perilaku:

- Jangan ubah screen timeout.
- Ubah selama sesi aktif.
- Kembalikan nilai sebelum sesi dimulai setelah sesi selesai.

Perubahan ini harus meminta konfirmasi dan dijelaskan kepada pengguna.

## 9. PIN Orang Tua

PIN diperlukan untuk:

- Membatalkan sesi.
- Pause/resume jika dikonfigurasi.
- Mengubah mode waktu saat timer aktif.
- Mengubah durasi atau jam selesai saat timer aktif.
- Mengubah aksi waktu habis.
- Menonaktifkan lock otomatis.
- Mengubah pengaturan keamanan.

Simpan PIN secara aman dan jangan menyimpan teks asli.

## 10. Notifikasi

Saat sesi aktif:

```text
Screen Timer aktif
Selesai pukul 18:00
Sisa waktu 01:12:43
```

Notifikasi harus memperbarui sisa waktu dan tetap aktif selama service berjalan.

Action notifikasi:

- Buka aplikasi.
- Lihat timer.
- Pause hanya jika PIN dapat diminta dengan aman.
- Jangan menyediakan pembatalan bebas.

## 11. Jadwal

Sediakan dua jenis jadwal:

### Jadwal durasi

```text
Setiap hari pukul 16:00, mulai timer 60 menit.
```

### Jadwal jam selesai

```text
Setiap hari pukul 16:00, izinkan penggunaan sampai pukul 18:00.
```

Jika jadwal melewati tengah malam, minta konfirmasi.

Jadwal tidak boleh memulai dua sesi bersamaan.

## 12. Riwayat

Simpan:

- Mode: durasi atau jam selesai.
- Waktu mulai.
- Waktu selesai yang direncanakan.
- Waktu selesai aktual.
- Durasi awal.
- Status: selesai, dibatalkan, dijeda, gagal mengunci.
- Penyebab kegagalan jika ada.

Contoh:

```text
30 Sep 2026
Mode: Selesai Pada Jam
Mulai: 16:00
Target selesai: 18:00
Status: Selesai dan perangkat dikunci
```

## 13. Quick Settings dan Widget

### Quick Settings tile

- Menampilkan status timer.
- Memulai preset terakhir.
- Membuka aplikasi.
- Tidak boleh membatalkan timer tanpa PIN.

### Widget

- Menampilkan sisa waktu.
- Menampilkan jam selesai.
- Menampilkan mode aktif.
- Membuka dashboard.

Jika perangkat lama tidak mendukung fitur tertentu, aplikasi harus tetap berfungsi dengan fitur utama.

## 14. Reboot dan Background

Gunakan foreground service sesuai aturan Android.

Simpan sesi aktif sebelum proses dimatikan.

Jika recovery setelah reboot aktif:

- Baca `endTime` tersimpan.
- Hitung ulang sisa waktu dari jam sekarang.
- Lanjutkan service jika waktu belum habis.
- Jalankan aksi selesai jika waktu telah habis.

Informasikan kepada pengguna bahwa OPPO/ColorOS dapat menghentikan proses background. Sediakan halaman diagnosa untuk membantu menonaktifkan pembatasan baterai dan mengaktifkan autostart jika tersedia.

## 15. Layar Aplikasi

### Setup awal

- Penjelasan aplikasi.
- Buat PIN.
- Aktifkan izin Device Administrator.
- Aktifkan notifikasi.
- Panduan baterai OPPO.

### Dashboard

- Tab Hitung Mundur.
- Tab Selesai Pada Jam.
- Preset.
- Input manual.
- Ringkasan sesi.
- Tombol Mulai.

### Timer aktif

- Countdown.
- Jam selesai.
- Mode.
- Status.
- Tombol pause/resume.
- Tombol batal.

### Riwayat

- Daftar sesi.
- Detail sesi.
- Hapus riwayat dengan PIN.

### Pengaturan

- Mode default.
- Preset.
- Peringatan.
- Screen timeout.
- Aksi waktu habis.
- PIN.
- Jadwal.
- Recovery setelah reboot.
- Diagnostik perangkat.

## 16. Teknologi

- Kotlin native.
- Gradle.
- Android SDK.
- Jetpack/AndroidX resmi jika diperlukan.
- Foreground Service.
- DeviceAdminReceiver.
- DevicePolicyManager.
- DataStore atau SharedPreferences.
- Room opsional untuk riwayat.
- AlarmManager/WorkManager sesuai kebutuhan jadwal.
- Tidak memakai WebView.
- Tidak memakai backend.
- Tidak memakai SDK iklan atau analytics.

## 17. Build APK

Proyek harus dapat dibuild menjadi APK debug untuk instalasi pribadi.

Sediakan:

- Source code lengkap.
- Gradle wrapper.
- README Bahasa Indonesia.
- Workflow GitHub Actions.
- Artifact APK hasil build.
- Instruksi Android Studio.
- Instruksi instalasi ke OPPO A5 2020.
- Instruksi pemberian izin Device Administrator.
- Instruksi pengaturan baterai ColorOS.

GitHub Actions harus menjalankan build APK dan menyimpan hasilnya sebagai artifact.

## 18. Acceptance Criteria

### Mode countdown

1. Pilih Hitung Mundur.
2. Pilih 45 menit.
3. Tekan Mulai.
4. Aplikasi menampilkan waktu selesai.
5. Buka tiga aplikasi lain.
6. Countdown tetap berjalan.
7. Setelah 45 menit, perangkat dikunci.

### Mode jam selesai

1. Pilih Selesai Pada Jam.
2. Pilih pukul 18:00.
3. Aplikasi menghitung durasi berdasarkan jam sekarang.
4. Aplikasi menampilkan ringkasan.
5. Timer selesai tepat pada waktu target dengan toleransi sistem Android.
6. Perangkat dikunci jika izin aktif.

### Tengah malam

1. Waktu sekarang 23:00.
2. Pilih selesai pukul 01:00.
3. Aplikasi menanyakan apakah targetnya besok pukul 01:00.
4. Setelah dikonfirmasi, durasi menjadi 2 jam.

### Pause

1. Timer aktif.
2. Pause ditekan.
3. Sisa waktu tidak berkurang.
4. Resume membuat `endTime` baru berdasarkan sisa waktu.

### Aplikasi ditutup

1. Timer aktif.
2. Activity ditutup.
3. Aplikasi lain dibuka.
4. Timer tetap berjalan melalui service.

### Reboot

1. Timer aktif.
2. Perangkat restart.
3. Jika recovery aktif, timer dilanjutkan dari `endTime`.

### Izin lock belum aktif

1. Pengguna belum memberi izin admin.
2. Aplikasi memberi peringatan jelas.
3. Aplikasi tidak mengklaim perangkat akan terkunci.
4. Pengguna diarahkan ke halaman aktivasi izin.

## 19. Output untuk AI Coding Agent

Jangan menghasilkan PRD baru. Buat proyek Android native yang dapat dibuild.

Output wajib:

- Proyek Kotlin lengkap.
- UI Bahasa Indonesia.
- Mode Countdown Durasi.
- Mode Selesai Pada Jam.
- Perhitungan `startTime`, `endTime`, dan sisa waktu.
- Foreground service.
- Notifikasi.
- Device Administrator.
- Lock screen otomatis.
- PIN orang tua.
- Penyimpanan lokal.
- Riwayat.
- Screen timeout.
- Peringatan suara/getar.
- Quick Settings tile jika kompatibel.
- Widget jika kompatibel.
- Jadwal jika kompatibel.
- Boot recovery.
- Diagnostik OPPO/ColorOS.
- Unit test untuk perhitungan waktu, tengah malam, pause, dan resume.
- README build dan instalasi.
- GitHub Actions untuk menghasilkan APK.

Jika suatu fitur tidak dapat dilakukan pada versi Android tertentu, implementasikan fallback yang aman, jelaskan keterbatasannya dalam UI, dan jangan membuat klaim palsu.
