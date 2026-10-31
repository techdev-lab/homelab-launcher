# Ramadan and prayer: batch 3 design note

[English](RAMADAN_PRAYER.md) · [العربية](RAMADAN_PRAYER.ar.md)

Status: **implemented in 3.0.0**, tried on a Chromecast 4K. Written before the code, as the other batches.

Three small features that belong together, because they all read the same two things: the Hijri date and today's
prayer times. Nothing here needs the network, a permission or a sound. The offline build gets all three.

## 1. Hijri date adjustment (`hijri_offset_days`)

The Hijri date comes from the platform's Umm al-Qura calendar, which is the Saudi civil calendar. Other countries
start a month on a different day (moon sighting), so the date on the home screen can be one day away from the date
the family follows, and that moves the start of Ramadan with it.

- Option: a whole number of days, `-2` to `+2`, `0` by default.
- Picked on the TV (Launcher settings, a row of five chips) or in `config.json` as `hijri_offset_days`.
  The TV choice wins over the theme, like every other look option, and a theme push does not clear it.
  A value outside the range, or not a whole number, is ignored (not clamped): a typo must not turn into a
  different date quietly.
- It moves the date everywhere the launcher reads it: the header, the Ramadan flag (which also switches the
  Umm al-Qura "Isha +120 minutes" rule), the Ramadan card and the screensaver.
- It does not move prayer times by itself. Only the Ramadan flag follows the shifted date.
- The calendar call stays on the device (ICU). The pure parts (range, which source wins) are unit tested on the JVM.

A shift cannot reach Ramadan from months away, so the Ramadan card is tried on the TV with a separate throwaway
preview build (a forced Ramadan date and a shifted clock), which is never part of a release.

## 2. Prayer time alert (`prayer_alert_minutes`)

For a few minutes after a prayer time, the prayer card says it is time for that prayer, instead of counting down to
the next one.

- Visual only. No sound, no notification, no new permission. The card already rebuilds every minute, so the alert
  shows within a minute of the time and goes away by itself.
- Option: minutes `0` to `30`; `0` turns it off; default `10`. TV chips: Off, 5, 10, 15 minutes. Same priority rule
  as above (TV choice, then theme, then default; out of range is ignored).
- Applies to the five prayers (Fajr, Dhuhr, Asr, Maghrib, Isha). Sunrise is not a prayer, so it never alerts.
- The card shows "Asr time" and "Now" where the countdown was; the five times line stays.
- If the prayer card is switched off, there is no alert. Making a card out of nothing would break the card switches.

## 3. Ramadan countdown card

Only while it is Ramadan (by the Hijri date, with the adjustment above), the strip gets one more card right after
the prayer card, with its own on/off chip in the card row ("Ramadan", on by default):

- from Fajr until Maghrib: "Iftar in h:mm";
- from Maghrib until the next Fajr: "Suhoor ends in h:mm", but only if tomorrow is also a day of Ramadan, so the
  card is gone after Maghrib on the last day (the next morning is Eid, there is no suhoor);
- before Fajr: "Suhoor ends in h:mm".
- The meta line says "Ramadan · day N" (N is today's Hijri day).
- A line under it: "Suhoor ends 4:32 · Iftar 18:12", the day's Fajr and Maghrib in the TV's clock format.

Rules we chose and why:

- Suhoor ends at Fajr. There is no "imsak" margin invented here. Whoever wants a margin already has
  `prayer_adjust` (minutes per time) for Fajr.
- The date changes at midnight (as the Umm al-Qura calendar does), not at Maghrib. So the night before the first
  fast is the last evening of Shaaban: the card already appears at Maghrib that evening, because tomorrow is
  Ramadan, and shows the first suhoor. The night before Eid has no card.
- The card is not shown outside Ramadan, whatever the switch says. It needs the prayer place (latitude,
  longitude) like the prayer card, and is off when the prayer card is unavailable.
- Unknown prayer method: no times, no card (the prayer card already says so).

## Where the code goes

- `HijriShift` (pure): range, which source wins. `Hijri.today(shift)` applies it (ICU, device only).
- `PrayerAlert` (pure): which prayer, if any, is inside its alert window; the range and priority of the option.
- `RamadanCountdown` (pure): which phase it is and the moment to count down to.
- `Interests.Settings` carries the three values (like the prayer method), `Config` reads them.
- `MainActivity.prayerCard`, a new `ramadanCard`, the header, and `ClockDream` use them.
- Launcher settings: two chip rows (Hijri date, Prayer alert) and one more card chip.

## Test plan

JVM tests are written first and shown failing, then the code. They cover: the range and priority rules, the edges of
the alert window (just before, at the time, last minute, after, zero minutes, each of the five), the phases of
the Ramadan card (before Fajr, at Fajr, between, at Maghrib, after Maghrib, last day, not Ramadan), Sunrise never
alerting, and the config parsing. On the Chromecast 4K: the settings chips, the header with a shift, and a throwaway
preview build (not published) with a forced Ramadan date and a shifted clock to see the two cards.
