package com.spyfall.game;

import java.util.Random;

public class Utils {

    private static final Random random = new Random();

    public static int randomInt(){
        return random.nextInt();
    }
}
