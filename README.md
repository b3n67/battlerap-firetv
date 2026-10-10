# Battlerap für FireTV

Inoffizielle Fan-App (kein offizielles DLTLLY-Release). Zeigt Battles und Livestreams
von battlerap.de auf FireTV Cube / FireTV Stick. Es wird ein battlerap.de-Account
mit Abo bzw. gekauftem Event benötigt.

**Download (v1.0):** `https://tinyurl.com/22lr9q6g`
(Vollständiger Link: `https://github.com/b3n67/battlerap-firetv/releases/download/v1.0/Battlerap-FireTV.apk`)

## Vorbereitung (einmalig)

1. Am FireTV: **Einstellungen → Mein Fire TV → Entwickleroptionen**
   - Falls „Entwickleroptionen" fehlt: **Einstellungen → Mein Fire TV → Info →
     das eigene Gerät 7-mal anklicken**, danach ist der Menüpunkt da.
2. Dort einschalten:
   - **Apps unbekannter Herkunft → AN** (für Methode A)
   - **ADB-Debugging → AN** (nur für Methode B)

## Methode A: ohne PC (empfohlen)

1. App **Downloader** aus dem Amazon Appstore installieren und öffnen.
2. Oben den Kurz-Link eintragen: `https://tinyurl.com/22lr9q6g`
3. Download abwarten und auf **Installieren** tippen.
4. Bei der Nachfrage „Apps unbekannter Herkunft" mit **Zulassen** bestätigen.
5. Fertig — die App liegt danach in **Meine Apps und Sender**.

## Methode B: per Smartphone übertragen

Beide Geräte müssen im selben WLAN sein.

**Variante 1 – Send Files to TV (Android + iPhone):**
1. App „Send Files to TV" auf dem FireTV (Amazon Appstore) und auf dem
   Smartphone (Play Store / App Store) installieren.
2. Am FireTV in der App auf **Receive**, am Smartphone auf **Send** tippen.
3. Am Smartphone die **Battlerap-FireTV.apk** wählen (vorher z. B. per Browser
   über den Kurz-Link oben herunterladen) und senden.
4. Nach Empfang am FireTV auf **Installieren** tippen (ggf. „Apps unbekannter
   Herkunft" zulassen).

**Variante 2 – Apps2Fire (nur Android, keine TV-App nötig):**
1. Am FireTV **ADB-Debugging** einschalten (siehe Vorbereitung).
2. App **Apps2Fire** aus dem Play Store installieren und das FireTV per IP
   hinzufügen (IP steht unter Einstellungen → Mein Fire TV → Info → Netzwerk).
3. In Apps2Fire die APK wählen und per Upload-Symbol aufs FireTV schieben.

## Methode C: vom PC per ADB

1. Am PC: [Android platform-tools](https://developer.android.com/tools/releases/platform-tools)
   herunterladen und entpacken.
2. FireTV-IP herausfinden: **Einstellungen → Mein Fire TV → Info → Netzwerk**.
3. Am PC im platform-tools-Ordner:
   ```
   adb connect IP-DES-FIRETV:5555
   adb install Battlerap-FireTV.apk
   ```
4. Am TV erscheinende ADB-Bestätigung **zulassen** (Haken bei „immer vertrauen").

## Erster Start

1. App **Battlerap** öffnen und einmal mit dem battlerap.de-Account **einloggen**
   (bleibt danach gespeichert).
2. Bedienung per Fernbedienung:
   - **Pfeiltasten** = roter Zeiger bewegen (gedrückt halten = schneller,
     am Rand scrollt die Seite)
   - **OK** = Klick · **Zurück** = zurück
   - Zeiger blendet sich nach 4 Sekunden ohne Eingabe von selbst aus.
3. Livestream/Videos lassen sich per Player-Schaltfläche auf **Vollbild** stellen.

## Selbst bauen

Android SDK + Java 17 genügen, kein installiertes Gradle nötig (siehe
`settings.gradle` / `app/build.gradle`). Bauen z. B. mit portablem Gradle:
`gradle assembleDebug` — die APK landet unter
`app/build/outputs/apk/debug/app-debug.apk`.
