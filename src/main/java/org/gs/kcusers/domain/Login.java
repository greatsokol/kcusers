package org.gs.kcusers.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

@Entity
@EntityListeners(EntityListener.class)
@Data
@AllArgsConstructor
@IdClass(Login.LoginPK.class)
@Table(name = "logins", schema = "kcusers")
public class Login {
    @Id
    String userName;

    @Id
    Long authTime;

    @Id
    String session;

    String address;

    public Login() {
    }

    public static class LoginPK implements Serializable {
        private String userName;
        private Long authTime;
        private String session;

        public LoginPK() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LoginPK loginPK = (LoginPK) o;
            return Objects.equals(userName, loginPK.userName) &&
                    Objects.equals(authTime, loginPK.authTime) &&
                    Objects.equals(session, loginPK.session);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userName, authTime, session);
        }
    }

}
