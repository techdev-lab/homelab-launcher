package com.gathin.tvhome;

/**
 * What the Ramadan card counts down to (docs/RAMADAN_PRAYER.md): the end of suhoor (Fajr) or the iftar (Maghrib).
 * The Hijri date turns over at midnight, so the evening before the first fast already shows the first suhoor, and
 * the evening of the last day shows nothing (the next morning is Eid). Pure Java, unit tested on the JVM.
 */
final class RamadanCountdown {
    static final int NONE = 0, SUHOOR = 1, IFTAR = 2;

    private RamadanCountdown() {
    }

    /** {@code kind} is NONE, SUHOOR or IFTAR; {@code target} is in hours of today's clock (past 24 for tomorrow's Fajr). */
    static final class Result {
        final int kind;
        final double target;

        Result(int kind, double target) {
            this.kind = kind;
            this.target = target;
        }
    }

    private static final Result NO_CARD = new Result(NONE, 0);

    /**
     * @param ramadanToday     today's Hijri date is in Ramadan
     * @param ramadanTomorrow  tomorrow's is
     * @param fajrNext         tomorrow's Fajr on today's clock, plus 24 (see {@link Prayer#onTodaysClock})
     */
    static Result of(boolean ramadanToday, boolean ramadanTomorrow, double nowHours, double fajr, double maghrib,
                     double fajrNext) {
        if (Double.isNaN(nowHours) || Double.isNaN(fajr) || Double.isNaN(maghrib) || Double.isNaN(fajrNext)) return NO_CARD;
        if (nowHours < fajr) return ramadanToday ? new Result(SUHOOR, fajr) : NO_CARD;
        if (nowHours < maghrib) return ramadanToday ? new Result(IFTAR, maghrib) : NO_CARD;
        return ramadanTomorrow ? new Result(SUHOOR, fajrNext) : NO_CARD;
    }
}
