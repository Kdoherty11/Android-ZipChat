package com.kdoherty.zipchat.utils;

import java.util.Arrays;

/**
 * Created by kdoherty on 9/2/15.
 */
public class MyObjects {

    private MyObjects() {
    }

    public static int hash(Object... values) {
        return Arrays.hashCode(values);
    }

    public static boolean equals(Object a, Object b) {
        return (a == null) ? (b == null) : a.equals(b);
    }
}
