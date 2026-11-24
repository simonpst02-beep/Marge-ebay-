package fr.margeo.app;

import android.app.job.JobParameters;
import android.app.job.JobService;

/** Tâche lancée par Android toutes les heures pour vérifier les alertes. */
public class AlertJob extends JobService {

    @Override
    public boolean onStartJob(final JobParameters params) {
        new Thread(() -> {
            Alertes.lancer(getApplicationContext());
            jobFinished(params, false);
        }).start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
