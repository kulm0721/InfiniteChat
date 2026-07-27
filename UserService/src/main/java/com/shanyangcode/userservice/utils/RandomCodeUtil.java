package com.shanyangcode.userservice.utils;

import java.util.Random;

public final class RandomCodeUtil {
    private static final Random random = new Random();

    public static String getRandomCode() {
        int randomNum = random.nextInt(900000) + 100000;
        return String.format("%06d", randomNum);
    }
}