package org.pmcsn.utils;

import org.pmcsn.libraries.Rngs;

public class Distributions {

    public static double erlang(long n, double b, Rngs rngs)
        /* ==================================================
         * Returns an Erlang distributed positive real number.
         * NOTE: use n > 0 and b > 0.0
         * ==================================================
         */
    {
        long   i;
        double x = 0.0;

        for (i = 0; i < n; i++)
            x += exponential(b, rngs);
        return (x);
    }

    public static double exponential(double m, Rngs rngs)
        /* =========================================================
         * Returns an exponentially distributed positive real number.
         * NOTE: use m > 0.0
         * =========================================================
         */
    {
        return (-m * Math.log(1.0 - rngs.random()));
    }

    public static double uniform(double a, double b, Rngs rngs) {
        /* --------------------------------------------
         * generate a Uniform random variate, use a < b
         * --------------------------------------------
         */
        return (a + (b - a) * rngs.random());
    }


}
