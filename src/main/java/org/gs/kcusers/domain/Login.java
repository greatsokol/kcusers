package org.gs.kcusers.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Data
@AllArgsConstructor
@IdClass(Login.LoginPK.class)
@Table(name = "logins")
public class Login {
    @Id
    String userName;

    @Id
    Long authTime;

    String session;

    String address;

    public Login()
    {}

    public static class LoginPK implements Serializable{
        private String userName;
        private Long authTime;

        public LoginPK() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Login.LoginPK loginPK = (Login.LoginPK) o;
            return Objects.equals(userName, loginPK.userName) &&
                    Objects.equals(authTime, loginPK.authTime);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userName, authTime);
        }
    }

}
