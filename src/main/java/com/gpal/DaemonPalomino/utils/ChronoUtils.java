package com.gpal.DaemonPalomino.utils;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChronoUtils {

    public static void scheduleFixedTime(Runnable runnable, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        Date scheduledTime = calendar.getTime();

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
        timer.schedule(timerTask, scheduledTime, 24 * 60 * 60 * 1000); // to execute every 24 hours
    }

}
