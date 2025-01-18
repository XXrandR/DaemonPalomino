package com.gpal.DaemonPalomino.utils;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChronoUtils {

    public static void scheduleFixedTime(Runnable runnable, Date time) {
        class Helper extends TimerTask {
            public static int i = 0;

            @Override
            public void run() {
                log.info("Timer ran " + ++i);
                runnable.run();
            }
        }
        Timer timer = new Timer();
        TimerTask timerTask = new Helper();
        timer.schedule(timerTask, time, 24 * 60 * 60 * 1000); // to execute every 24 hours
    }

}
