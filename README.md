# Reha-Coach

Offline-Android-App: geführte Kräftigungs-Einheiten für den Ellbogen zu
Hause, mit Schmerzprotokoll und Export für Daniels Health-OS-Chat.

Die App führt durch den Plan, den Daniel selbst einträgt, und hält fest,
wie der Ellbogen darauf reagiert. **Sie stellt keine Diagnose, kennt keinen
Befund und ersetzt keine Physiotherapie. Kein Medizinprodukt.**

APK: [dist/rehacoach-1.0.apk](dist/rehacoach-1.0.apk)

## Ablauf einer Einheit

1. Schmerz vorher (0–10).
2. Je Übung: Ziel und Hinweis, dann geführt nach Art der Übung:
   - **Halten:** Vorzählen, Haltezeit als Countdown, Pause zwischen Sätzen.
   - **Langsames Ablassen:** je Wiederholung eine Phase "ablassen" und eine
     Phase "zurückführen" mit eigener Dauer, dazu Ton und Vibration.
   - **Wiederholungen:** ohne Uhr, Satz per Knopf abhaken.
   Danach der Schmerz bei dieser Übung.
3. Schmerz danach und eine Notiz.
4. Am nächsten Morgen fragt die Startseite nach dem Ellbogen. Dieser Wert
   wird nur am Folgetag abgefragt.

Bricht man ab (Knopf zweimal oder Zurück-Geste), wird gespeichert, was bis
dahin geschafft ist, mit dem Vermerk "abgebrochen".

## Übungen

Frei editierbar: Name, Art, Sätze, Haltezeit oder Wiederholungen, Dauer für
Ablassen und Zurückführen, Pause, Last als freier Text, Hinweis,
Reihenfolge. Beim ersten Start stehen vier übliche Vorschläge bei Tennisarm
drin (Handgelenk strecken und halten 5 × 45 s, langsam ablassen 3 × 15,
Unterarm drehen 3 × 12, Greifen und halten 3 × 30 s). Das sind allgemeine
Vorschläge, kein Plan für Daniel; die App weist beim ersten Start darauf
hin.

## Schmerz-Regel (`advice`)

Faustregel nach dem in der Sehnen-Reha üblichen Schmerz-Monitoring. Die
Zahlen sind Erfahrungswerte, keine für Daniel geprüften Grenzen; Vorgaben
von Arzt oder Physio gehen vor.

| Stufe | Bedingung (letzte Einheit) | Aussage |
|---|---|---|
| stop | Schmerz bei einer Übung ≥ 7, oder Morgen danach ≥ 3 über "vorher" | aussetzen oder klar reduzieren, sonst Arzt/Physio |
| warn | Schmerz ≥ 5, oder Morgen danach ≥ 2 über "vorher" | etwas weniger Last oder ein Satz weniger |
| up | die letzten drei Einheiten alle ≤ 3, mindestens zwei bekannte Morgenwerte, keiner über "vorher" | Last um den kleinsten Schritt erhöhen |
| ok | sonst | Last beibehalten |

Die App rechnet nicht mit der Last (freier Text) und ändert sie nie selbst.

## Exportformat

Zwei Texte, beide für den Skill `health-os-debrief` (Abschnitt
"Ellbogen-Reha: Reha-Coach-App"):

```
REHA 2026-11-14 (Sa) | 14 min | 4 Übungen
Schmerz vorher 2 | bei den Übungen max 3 | danach 2
Handgelenk strecken, halten: 5 × 45 s halten | Last 3 kg | Schmerz 3
…
REHA-JSON-START
{"datum":"2026-11-14","reha_ok":true,"reha_min":14,"reha_schmerz_max":3,"reha_schmerz_vor":2,"reha_schmerz_nach":2}
REHA-JSON-ENDE
```

```
REHA 2026-11-15 (So) | Morgen-Check
Ellbogen am Morgen nach der Einheit vom 14.11.: 1/10 (vor der Einheit 2)
REHA-JSON-START
{"datum":"2026-11-15","ellbogen_n":1}
REHA-JSON-ENDE
```

Der Morgenwert gehört zum Folgetag und ist dort das schon bestehende Feld
`ellbogen_n` (dasselbe, das HealthLog füllt).

## Datenmodell (localStorage)

- `rehacoach.exercises.v1`: `[{id, name, typ: 'iso'|'exz'|'wdh', sets, hold,
  reps, ab, hoch, rest, last, note}]`
- `rehacoach.sessions.v1`: `[{id, date, startedAt, endedAt, pre, post,
  items: [{exId, name, typ, sets, setsDone, ziel, last, pain}], note,
  morgen, morgenDatum, abgebrochen}]`
- `rehacoach.settings.v1`: `sound`, `vibrate`, `seen`, `lastExport`, `schema`
