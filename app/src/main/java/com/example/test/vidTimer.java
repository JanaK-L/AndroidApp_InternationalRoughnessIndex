package com.example.test;

import android.widget.ProgressBar;
import android.widget.VideoView;
import java.util.Timer;
import java.util.TimerTask;

public class vidTimer {

    private Timer timer;
    private int duration;
    private VideoView vid;
    private ProgressBar prog;

    /**
     * Konstruktor
     * @param video VideoView
     * @param progress Progressbar
     */
    public vidTimer(VideoView video, ProgressBar progress, int dura) {
        this.vid = video;
        this.prog = progress;
        this.duration = dura * 1000;
    }


    /**
     * Methode zum starten eines neuen TimerTask.
     */
    public void start(){
        timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                updateProgressbar();
            }
        };
        timer.schedule(task, 0, 1000);
    }


    /**
     * Methode zum beenden des aktuellen TimerTask.
     */
    public void stop() {
        this.timer.cancel();
        this.timer.purge();
    }


    /**
     * Methode zum updaten der Progressbar.
     */
    private void updateProgressbar(){
        // wenn Bar voll ist, dann wieder zurück setzen
        if (prog.getProgress() >= 100) {
            prog.setProgress(0);
            this.stop();
        } else {
            // wenn Bar nicht voll ist, den aktuellen Stand berechnen
            int current = vid.getCurrentPosition();
            int progress = current * 100 / duration;
            prog.setProgress(progress);
        }
    }
}
