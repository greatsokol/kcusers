/*
 * Created by Eugene Sokolov 19.08.2024, 14:43.
 */

package org.gs.kcusers.controller;

import org.gs.kcusers.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/metrics")
public class MetricsController {
    @Autowired
    UserRepository userRepository;


    @GetMapping
    public String metrics() {
        long all = userRepository.count();
        long enabled = userRepository.countByEnabled(true);
        long disabled = all - enabled;

        long time = Instant.now().toEpochMilli();
        return "# HELP health Health OK if equals 1.\n" +
                "# TYPE health gauge\n" +
                "health 1 " + time + "\n" +
                "# HELP users User counters (all, enabled, disabled).\n" +
                "# TYPE users gauge\n" +
                "users {mode=\"all\"} " + all + " " + time + "\n" +
                "users {mode=\"enabled\"} " + enabled + " " + time + "\n" +
                "users {mode=\"disabled\"} " + disabled + " " + time + "\n";
    }
}
