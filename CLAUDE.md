# Reha-Coach

Offline-Android-App für geführte Ellbogen-Kräftigung zu Hause mit
Schmerzprotokoll. Reine Web-App in einer Datei, die per
[apk-builder](../apk-builder) zur APK wird. Ablauf, Schmerz-Regel,
Exportformat und Datenmodell stehen in [README.md](README.md), Versionen in
[CHANGELOG.md](CHANGELOG.md).

## Build & Test

Kein Bundler, kein Build-Step. Testserver `reha-coach` aus der
Workspace-`.claude/launch.json` (Port 8190), dann
`http://localhost:8190/web/`. `?debug=1` führt den Selbsttest aus (Konsole
und Zeile auf der Startseite, auch per `rcTests()`). Er muss komplett `OK`
zeigen.

Eine Einheit im Browser schnell durchspielen: in der Konsole `db.exercises`
durch Übungen mit sehr kurzen Zeiten ersetzen (z. B. 2 × 2 s halten), dann
per Intervall die Schmerz-Chips, `x-go` und `x-done` bedienen. Die Uhr läuft
in Echtzeit über `performance.now()`.

APK bauen, **immer über den eigenen Wrapper** (Brücke `RcAndroid`,
Anzeigename "Reha-Coach"):

```powershell
cd "D:\claude code projects\reha-coach"
.\build.ps1 -VersionName "1.1" -VersionCode 2 -Release   # signiert -> dist\rehacoach-<v>.apk
```

## Konventionen

- Alles in `web/index.html`, kein Framework, kein Netzwerk.
- Der Ablauf einer Übung ist eine reine Liste von Phasen (`plan(ex)`); der
  Ablauf in der App arbeitet diese Liste nur ab. Neue Übungsarten zuerst in
  `plan` und im Selbsttest, dann in der Anzeige.
- Die Schmerz-Regel (`advice`) ist die einzige Stelle, an der die App etwas
  einschätzt. Jede Schwelle steht in der Tabelle im README und hat einen
  Fall im Selbsttest. Schwellen nur ändern, wenn Daniel es will oder sein
  Physio andere vorgibt.
- Die App schlägt keine Lasten vor und ändert keine. Die Seed-Übungen sind
  Vorschläge und greifen nur beim ersten Start.
- Texte bleiben vorsichtig: kein Heilversprechen, immer der Verweis auf
  Arzt oder Physio bei Warnzeichen.
- Kein `confirm()`/`alert()`: eigene Dialoge über `openModal`.
- Exportfelder nur zusammen mit dem Skill `health-os-debrief` ändern
  (Arbeitskopie in `..\health-os-skills\`).

## Aktueller Stand

v1.0 (code 1, 2026-11-15): Einheit mit drei Übungsarten, Schmerz vorher /
je Übung / danach / am Morgen danach, Einschätzung, Übungen verwalten,
Verlauf mit Kurve, Backup, Export. Selbsttest 26/26; eine ganze Einheit mit
allen drei Übungsarten, die Abfrage am Morgen danach und der Übungsdialog
liefen im Browser. Signierte APK `dist/rehacoach-1.0.apk`.

**Am Gerät noch nichts getestet.** Dort zu prüfen: Töne und Vibration bei
den Phasenwechseln (auch mit Handy auf dem Tisch hörbar?), Zurück-Geste
während der Einheit, Zwischenablage.

Offen bei Daniel: Welche Übungen und Lasten er wirklich macht, weiß ich
nicht (sie stammen aus einem anderen Chat). Er muss die Vorschläge ersetzen.
Gym-Coach enthält bewusst keine Ellbogen-Reha-Übung, die Reha läuft nur hier.
