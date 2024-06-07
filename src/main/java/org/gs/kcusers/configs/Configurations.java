package org.gs.kcusers.configs;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Configurations {
    public static int INACTIVITY_DAYS;
    public static int IMMUNITY_PERIOD_MINUTES;

    @Value("${service.keycloakclient.inactivity.days}")
    public void SetInactivityDays(int days) {
        INACTIVITY_DAYS = days;
    }

    @Value("${service.keycloakclient.inactivity.immunityperiodminutes}")
    public void SetImmunityMinutes(int minutes) {
        IMMUNITY_PERIOD_MINUTES = minutes;
    }
}
