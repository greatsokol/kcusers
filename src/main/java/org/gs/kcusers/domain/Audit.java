package org.gs.kcusers.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static org.gs.kcusers.utils.Utils.*;

@Entity
@Data
@IdClass(Audit.AuditPK.class)
@Table(name = "audit", schema = "kcusers")
public class Audit {
    public static String RS_OI = "РС_ОИ";
    public static String RS_DS = "РС_ДС";
    public static String ENT_DB = "db";
    public static String ENT_TOKEN = "token";
    public static String ENT_API = "api";
    public static String ENT_KEYCLOAK = "keycloak";
    public static String SUBTYPE_DELETE = "delete";
    public static String SUBTYPE_INSERT = "insert";
    public static String SUBTYPE_UPDATE = "update";
    public static String SUBTYPE_CONNECT = "connect";
    public static String SUBTYPE_ERR = "error";
    public static String SUBTYPE_SUCCESS = "success";


//    @Id
//    @NonNull
//    @GeneratedValue(generator = "agent")
//    @Column(name = "id", columnDefinition = "BIGINT", nullable = false)
//    Long id;

    @Id
    @NonNull
    @Column(name = "audit_event_id", columnDefinition = "VARCHAR(255)", nullable = false)
    String auditEventId = UUID.randomUUID().toString();

    @Id
    @NonNull
    @Column(name = "audit_event_created", columnDefinition = "BIGINT", nullable = false)
    Long auditEventCreated = Instant.now().toEpochMilli();

    @Id
    @NonNull
    @Column(name = "audit_event_type", columnDefinition = "VARCHAR(10)", nullable = false)
    @Size(min = 1, max = 10)
    String auditEventType;

    @Id
    @NonNull
    @Column(name = "audit_event_sub_type", columnDefinition = "VARCHAR(10)", nullable = false)
    @Size(min = 1, max = 10)
    String auditEventSubType;

    @Id
    @NonNull
    @Column(name = "audit_event_entity", columnDefinition = "VARCHAR(10)", nullable = false)
    @Size(min = 1, max = 10)
    String auditEventEntity;

    @Size(max = 255)
    @Column(name = "authorized_user", columnDefinition = "VARCHAR(255)")
    String authorizedUser;

    @Size(max = 255)
    @Column(name = "token_user", columnDefinition = "VARCHAR(255)")
    String tokenUser;

    @Size(max = 255)
    @Column(name = "token_sid", columnDefinition = "VARCHAR(255)")
    String tokenSid;

    @Column(name = "token_iat", columnDefinition = "BIGINT")
    Long tokenIat;

    @Column(name = "token_exp", columnDefinition = "BIGINT")
    Long tokenExp;

    @Size(max = 255)
    @Column(name = "token_groups", columnDefinition = "VARCHAR(255)")
    String tokenGroups;

    @Column(name = "api_http_code", columnDefinition = "INT")
    Integer apiHttpCode;

    @Size(max = 20)
    @Column(name = "api_method", columnDefinition = "VARCHAR(20)")
    String apiMethod;

    @Size(max = 255)
    @Column(name = "api_path", columnDefinition = "VARCHAR(255)")
    String apiPath;

    @Size(max = 255)
    @Column(name = "table_name", columnDefinition = "VARCHAR(255)")
    String tableName;

    @Size(max = 255)
    @Column(name = "table_fields", columnDefinition = "VARCHAR(255)")
    String tableFields;

    @Size(max = 255)
    @Column(name = "kc_user", columnDefinition = "VARCHAR(255)")
    String kcUser;

    @Size(max = 255)
    @Column(name = "kc_realm", columnDefinition = "VARCHAR(255)")
    String kcRealm;

    @Column(name = "kc_enabled", columnDefinition = "BOOLEAN")
    Boolean kcEnabled;

    @Size(max = 255)
    @Column(name = "description", columnDefinition = "VARCHAR(255)")
    String description;

    private String cutString(String s, int l) {
        if (s != null && s.length() > 255) {
            return s.substring(0, l);
        }
        return s;
    }

    private void fillTokenData() {
        this.tokenUser = cutString(getAuthorizedUserName(), 255);
        this.tokenGroups = cutString(grantedAuthoritiesListAsString(), 255);
        this.tokenSid = cutString(getAuthorizedUserJwtSessionId(), 255);
        this.tokenIat = getAuthorizedUserJwtIat();
        this.tokenExp = getAuthorizedUserJwtExp();
    }

    // database event
    public Audit(@NonNull String subType, Object dbEntity) {
        this.auditEventType = RS_OI;
        this.auditEventSubType = subType;
        this.auditEventEntity = ENT_DB;
        this.authorizedUser = getAuthorizedUserOrServiceName();
        this.tableName = dbEntity.getClass().getSimpleName().toLowerCase();
        this.tableFields = cutString(dbEntity.toString(), 255);
        if (dbEntity instanceof User) {
            this.description = ((User)dbEntity).getComment();
            this.kcEnabled = ((User)dbEntity).getEnabled();
        }
        if(dbEntity instanceof Event){
            this.description = ((Event)dbEntity).getComment();
            this.kcEnabled = ((Event)dbEntity).getEnabled();
        }
        fillTokenData();
    }

    // api/token event
    public Audit(@NotNull String entity, @NonNull String subType, Integer apiHttpCode, String apiMethod, String apiPath, String apiDescription) {
        this.auditEventType = RS_DS;
        this.auditEventSubType = subType;
        this.auditEventEntity = entity;
        this.authorizedUser = getAuthorizedUserName();
        this.apiHttpCode = apiHttpCode;
        this.apiMethod = cutString(apiMethod, 20);
        this.apiPath = cutString(apiPath, 255);
        this.description = cutString(apiDescription, 255);
        fillTokenData();
    }

    // keycloak event
    public Audit(@NotNull String entity, @NonNull String subType, Integer apiHttpCode, String apiDescription) {
        this.auditEventType = RS_DS;
        this.auditEventSubType = subType;
        this.auditEventEntity = entity;
        this.authorizedUser = getAuthorizedUserName();
        this.apiHttpCode = apiHttpCode;
        this.description = cutString(apiDescription, 255);
        fillTokenData();
    }

    // keycloak event
    public Audit(@NotNull String entity, @NotNull String subType, String kcRealm, String kcUser, Boolean kcEnabled, String description) {
        this.auditEventType = RS_OI;
        this.auditEventSubType = subType;
        this.auditEventEntity = entity;
        this.authorizedUser = getAuthorizedUserOrServiceName();
        this.kcRealm = kcRealm;
        this.kcUser = kcUser;
        this.description = cutString(description, 255);
        this.kcEnabled = kcEnabled;
        fillTokenData();
    }

    // keycloak event error
    public Audit(@NotNull String entity, String kcRealm, String description) {
        this.auditEventType = RS_OI;
        this.auditEventSubType = SUBTYPE_ERR;
        this.auditEventEntity = entity;
        this.authorizedUser = getAuthorizedUserOrServiceName();
        this.kcRealm = kcRealm;
        this.description = cutString(description, 255);
        fillTokenData();
    }

    public Audit() {

    }

    public static class AuditPK implements Serializable {
        //private Long id;
        private String auditEventId;
        private Long auditEventCreated;
        private String auditEventType;
        private String auditEventSubType;
        private String auditEventEntity;

        public AuditPK() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Audit.AuditPK auditPK = (Audit.AuditPK) o;
            return Objects.equals(auditEventId, auditPK.auditEventId) &&
                    Objects.equals(auditEventCreated, auditPK.auditEventCreated) &&
                    Objects.equals(auditEventType, auditPK.auditEventType) &&
                    Objects.equals(auditEventSubType, auditPK.auditEventSubType) &&
                    Objects.equals(auditEventEntity, auditPK.auditEventEntity);
        }

        @Override
        public int hashCode() {
            return Objects.hash(auditEventId, auditEventCreated, auditEventType, auditEventSubType, auditEventEntity);
        }
    }
}
