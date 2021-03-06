/*
 * ObjectLab, http://www.objectlab.co.uk/open is sponsoring the ObjectLab Kit.
 *
 * Based in London, we are world leaders in the design and development
 * of bespoke applications for the securities financing markets.
 *
 * <a href="http://www.objectlab.co.uk/open">Click here to learn more</a>
 *           ___  _     _           _   _          _
 *          / _ \| |__ (_) ___  ___| |_| |    __ _| |__
 *         | | | | '_ \| |/ _ \/ __| __| |   / _` | '_ \
 *         | |_| | |_) | |  __/ (__| |_| |__| (_| | |_) |
 *          \___/|_.__// |\___|\___|\__|_____\__,_|_.__/
 *                   |__/
 *
 *                     www.ObjectLab.co.uk
 *
 * $Id$
 *
 * Copyright 2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package net.objectlab.kit.datecalc.joda;

import static net.objectlab.kit.datecalc.common.IMMPeriod.QUARTERLY;
import static org.joda.time.DateTimeConstants.DECEMBER;
import static org.joda.time.DateTimeConstants.JUNE;
import static org.joda.time.DateTimeConstants.MARCH;
import static org.joda.time.DateTimeConstants.SEPTEMBER;

import java.util.ArrayList;
import java.util.List;

import net.objectlab.kit.datecalc.common.AbstractIMMDateCalculator;
import net.objectlab.kit.datecalc.common.IMMPeriod;

import org.joda.time.LocalDate;

/**
 * Joda <code>LocalDate</code> based implementation of the
 * {@link net.objectlab.kit.datecalc.common.IMMDateCalculator}.
 *
 * @author Benoit Xhenseval
 *
 */
public class LocalDateIMMDateCalculator extends AbstractIMMDateCalculator<LocalDate> {
    protected static final int MONTHS_IN_QUARTER = 3;

    protected static final int MONTH_IN_YEAR = 12;

    protected static final int DAYS_IN_WEEK = 7;

    /**
     * Returns a list of IMM dates between 2 dates, it will exclude the start
     * date if it is an IMM date but would include the end date if it is an IMM.
     *
     * @param start
     *            start of the interval, excluded
     * @param end
     *            end of the interval, may be included.
     * @param period
     *            specify when the "next" IMM is, if quarterly then it is the
     *            conventional algorithm.
     * @return list of IMM dates
     */
    public List<LocalDate> getIMMDates(final LocalDate start, final LocalDate end, final IMMPeriod period) {
        final List<LocalDate> dates = new ArrayList<LocalDate>();

        LocalDate date = start;
        while (true) {
            date = getNextIMMDate(true, date, period);
            if (!date.isAfter(end)) {
                dates.add(date);
            } else {
                break;
            }
        }

        return dates;
    }

    @Override
    protected LocalDate getNextIMMDate(final boolean requestNextIMM, final LocalDate start, final IMMPeriod period) {
        LocalDate date = start;

        final int month = date.getMonthOfYear();
        date = calculateIMMMonth(requestNextIMM, date, month);

        LocalDate imm = calculate3rdWednesday(date);
        final int immMonth = imm.getMonthOfYear();
        final boolean isMarchSept = immMonth == MARCH || immMonth == SEPTEMBER;

        switch (period) {

        case BI_ANNUALY_JUN_DEC:
            if (isMarchSept) {
                imm = getNextIMMDate(requestNextIMM, imm, period);
            }
            break;

        case BI_ANNUALY_MAR_SEP:
            if (!isMarchSept) {
                imm = getNextIMMDate(requestNextIMM, imm, period);
            }
            break;

        case ANNUALLY:
            // second jump
            imm = getNextIMMDate(requestNextIMM, imm, QUARTERLY);
            // third jump
            imm = getNextIMMDate(requestNextIMM, imm, QUARTERLY);
            // fourth jump
            imm = getNextIMMDate(requestNextIMM, imm, QUARTERLY);
            // fifth jump
            imm = getNextIMMDate(requestNextIMM, imm, QUARTERLY);
            break;

        case QUARTERLY:
        default:
            break;
        }

        return imm;
    }

    // -----------------------------------------------------------------------
    //
    // ObjectLab, world leaders in the design and development of bespoke
    // applications for the securities financing markets.
    // www.ObjectLab.co.uk
    //
    // -----------------------------------------------------------------------

    private LocalDate calculateIMMMonth(final boolean requestNextIMM, final LocalDate startDate, final int month) {
        int monthOffset = 0;
        LocalDate date = startDate;
        switch (month) {
        case MARCH:
        case JUNE:
        case SEPTEMBER:
        case DECEMBER:
            final LocalDate immDate = calculate3rdWednesday(date);
            if (requestNextIMM && !date.isBefore(immDate)) {
                date = date.plusMonths(MONTHS_IN_QUARTER);
            } else if (!requestNextIMM && !date.isAfter(immDate)) {
                date = date.minusMonths(MONTHS_IN_QUARTER);
            }
            break;

        default:
            if (requestNextIMM) {
                monthOffset = (MONTH_IN_YEAR - month) % MONTHS_IN_QUARTER;
                date = date.plusMonths(monthOffset);
            } else {
                monthOffset = month % MONTHS_IN_QUARTER;
                date = date.minusMonths(monthOffset);
            }
            break;
        }
        return date;
    }

    /**
     * Assumes that the month is correct, get the day for the 2rd wednesday.
     *
     * @param original
     *            the start date
     * @return the 3rd Wednesday of the month
     */
    private LocalDate calculate3rdWednesday(final LocalDate original) {
        final LocalDate firstOfMonth = original.withDayOfMonth(1);
        LocalDate firstWed = firstOfMonth.withDayOfWeek(MONTHS_IN_QUARTER);
        if (firstWed.isBefore(firstOfMonth)) {
            firstWed = firstWed.plusWeeks(1);
        }
        return firstWed.plusWeeks(2);
    }

    /**
     * Checks if a given date is an official IMM Date (3rd Wednesdays of
     * March/June/Sept/Dec.
     *
     * @param date
     * @return true if that date is an IMM date.
     */
    public boolean isIMMDate(final LocalDate date) {
        boolean same = false;

        final List<LocalDate> dates = getIMMDates(date.minusDays(1), date, QUARTERLY);

        if (!dates.isEmpty()) {
            same = date.equals(dates.get(0));
        }

        return same;
    }
}

/*
 * ObjectLab, http://www.objectlab.co.uk/open is sponsoring the ObjectLab Kit.
 *
 * Based in London, we are world leaders in the design and development
 * of bespoke applications for the securities financing markets.
 *
 * <a href="http://www.objectlab.co.uk/open">Click here to learn more about us</a>
 *           ___  _     _           _   _          _
 *          / _ \| |__ (_) ___  ___| |_| |    __ _| |__
 *         | | | | '_ \| |/ _ \/ __| __| |   / _` | '_ \
 *         | |_| | |_) | |  __/ (__| |_| |__| (_| | |_) |
 *          \___/|_.__// |\___|\___|\__|_____\__,_|_.__/
 *                   |__/
 *
 *                     www.ObjectLab.co.uk
 */
