package org.gs.kcusers.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Data
@AllArgsConstructor
@IdClass(Event.EventPK.class)
@Table(name = "events")
public class Event {

    @Id
    String userName;
    @Id
    String realmName;
    @Id
    Long created;
    @NonNull
    String admLogin;
    @NonNull
    String comment;

    Boolean enabled;

    public Event() {}

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
