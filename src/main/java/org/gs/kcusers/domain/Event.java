package org.gs.kcusers.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.io.Serializable;
import java.util.Objects;

@Entity
@EntityListeners(EntityListener.class)
@Data
@AllArgsConstructor
@IdClass(Event.EventPK.class)
@Table(name = "events", schema = "kcusers")
public class Event {
    @Id
    String userName;
    @Id
    String realmName;
    @Id
    Long created;
    @NonNull
    String admLogin;

    String comment;

    Boolean enabled;

    public Event() {
    }

    public static class EventPK implements Serializable {
        private String userName;
        private String realmName;
        private Long created;

        public EventPK() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Event.EventPK eventPK = (Event.EventPK) o;
            return Objects.equals(userName, eventPK.userName) && Objects.equals(realmName, eventPK.realmName)
                    && Objects.equals(created, eventPK.created);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userName, realmName, created);
        }
    }


}
