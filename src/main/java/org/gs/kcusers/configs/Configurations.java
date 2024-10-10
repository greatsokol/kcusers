package org.gs.kcusers.configs;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Configurations {
    public static int INACTIVITY_DAYS(){
        return inactivity_days;
    };
    public static int INACTIVITY_DAYS(int i){
        // for testing purpose
        return inactivity_days;
    };
    public static int IMMUNITY_PERIOD_MINUTES(){
        return immune_minutes;
    };

    private static int inactivity_days;
    private static int immune_minutes;

    @Value("${service.keycloakclient.inactivity.days}")
    public void SetInactivityDays(int days) {
        inactivity_days = days;
    }

    @Value("${service.keycloakclient.inactivity.immunityperiodminutes}")
    public void SetImmunityMinutes(int minutes) {
        immune_minutes = minutes;
    }

    public static String ROLES_TOKEN_CLAIM_NAME = "realm_access.roles";
}
