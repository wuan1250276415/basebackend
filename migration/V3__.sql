CREATE TABLE act_ge_bytearray
(
    ID_                VARCHAR(64) NOT NULL,
    REV_               INT NULL,
    NAME_              VARCHAR(255) NULL,
    DEPLOYMENT_ID_     VARCHAR(64) NULL,
    BYTES_             BLOB NULL,
    GENERATED_         TINYINT NULL,
    TENANT_ID_         VARCHAR(64) NULL,
    TYPE_              INT NULL,
    CREATE_TIME_       datetime NULL,
    ROOT_PROC_INST_ID_ VARCHAR(64) NULL,
    REMOVAL_TIME_      datetime NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_ge_property
(
    NAME_  VARCHAR(64) NOT NULL,
    VALUE_ VARCHAR(300) NULL,
    REV_   INT NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (NAME_)
);

CREATE TABLE act_ge_schema_log
(
    ID_        VARCHAR(64) NOT NULL,
    TIMESTAMP_ datetime NULL,
    VERSION_   VARCHAR(255) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_hi_actinst
(
    ID_                 VARCHAR(64)  NOT NULL,
    PARENT_ACT_INST_ID_ VARCHAR(64) NULL,
    PROC_DEF_KEY_       VARCHAR(255) NULL,
    PROC_DEF_ID_        VARCHAR(64)  NOT NULL,
    ROOT_PROC_INST_ID_  VARCHAR(64) NULL,
    PROC_INST_ID_       VARCHAR(64)  NOT NULL,
    EXECUTION_ID_       VARCHAR(64)  NOT NULL,
    ACT_ID_             VARCHAR(255) NOT NULL,
    TASK_ID_            VARCHAR(64) NULL,
    CALL_PROC_INST_ID_  VARCHAR(64) NULL,
    CALL_CASE_INST_ID_  VARCHAR(64) NULL,
    ACT_NAME_           VARCHAR(255) NULL,
    ACT_TYPE_           VARCHAR(255) NOT NULL,
    ASSIGNEE_           VARCHAR(255) NULL,
    START_TIME_         datetime     NOT NULL,
    END_TIME_           datetime NULL,
    DURATION_           BIGINT NULL,
    ACT_INST_STATE_     INT NULL,
    SEQUENCE_COUNTER_   BIGINT NULL,
    TENANT_ID_          VARCHAR(64) NULL,
    REMOVAL_TIME_       datetime NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_hi_attachment
(
    ID_                VARCHAR(64) NOT NULL,
    REV_               INT NULL,
    USER_ID_           VARCHAR(255) NULL,
    NAME_              VARCHAR(255) NULL,
    DESCRIPTION_       VARCHAR(4000) NULL,
    TYPE_              VARCHAR(255) NULL,
    TASK_ID_           VARCHAR(64) NULL,
    ROOT_PROC_INST_ID_ VARCHAR(64) NULL,
    PROC_INST_ID_      VARCHAR(64) NULL,
    URL_               VARCHAR(4000) NULL,
    CONTENT_ID_        VARCHAR(64) NULL,
    TENANT_ID_         VARCHAR(64) NULL,
    CREATE_TIME_       datetime NULL,
    REMOVAL_TIME_      datetime NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_hi_batch
(
    ID_                  VARCHAR(64) NOT NULL,
    TYPE_                VARCHAR(255) NULL,
    TOTAL_JOBS_          INT NULL,
    JOBS_PER_SEED_       INT NULL,
    INVOCATIONS_PER_JOB_ INT NULL,
    SEED_JOB_DEF_ID_     VARCHAR(64) NULL,
    MONITOR_JOB_DEF_ID_  VARCHAR(64) NULL,
    BATCH_JOB_DEF_ID_    VARCHAR(64) NULL,
    TENANT_ID_           VARCHAR(64) NULL,
    CREATE_USER_ID_      VARCHAR(255) NULL,
    START_TIME_          datetime    NOT NULL,
    END_TIME_            datetime NULL,
    REMOVAL_TIME_        datetime NULL,
    EXEC_START_TIME_     datetime NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_hi_caseactinst
(
    ID_                 VARCHAR(64)  NOT NULL,
    PARENT_ACT_INST_ID_ VARCHAR(64) NULL,
    CASE_DEF_ID_        VARCHAR(64)  NOT NULL,
    CASE_INST_ID_       VARCHAR(64)  NOT NULL,
    CASE_ACT_ID_        VARCHAR(255) NOT NULL,
    TASK_ID_            VARCHAR(64) NULL,
    CALL_PROC_INST_ID_  VARCHAR(64) NULL,
    CALL_CASE_INST_ID_  VARCHAR(64) NULL,
    CASE_ACT_NAME_      VARCHAR(255) NULL,
    CASE_ACT_TYPE_      VARCHAR(255) NULL,
    CREATE_TIME_        datetime     NOT NULL,
    END_TIME_           datetime NULL,
    DURATION_           BIGINT NULL,
    STATE_              INT NULL,
    REQUIRED_           TINYINT(1)   NULL,
    TENANT_ID_          VARCHAR(64) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_hi_caseinst
(
    ID_                        VARCHAR(64) NOT NULL,
    CASE_INST_ID_              VARCHAR(64) NOT NULL,
    BUSINESS_KEY_              VARCHAR(255) NULL,
    CASE_DEF_ID_               VARCHAR(64) NOT NULL,
    CREATE_TIME_               datetime    NOT NULL,
    CLOSE_TIME_                datetime NULL,
    DURATION_                  BIGINT NULL,
    STATE_                     INT NULL,
    CREATE_USER_ID_            VARCHAR(255) NULL,
    SUPER_CASE_INSTANCE_ID_    VARCHAR(64) NULL,
    SUPER_PROCESS_INSTANCE_ID_ VARCHAR(64) NULL,
    TENANT_ID_                 VARCHAR(64) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_hi_comment
(
    ID_                VARCHAR(64) NOT NULL,
    TYPE_              VARCHAR(255) NULL,
    TIME_              datetime    NOT NULL,
    USER_ID_           VARCHAR(255) NULL,
    TASK_ID_           VARCHAR(64) NULL,
    ROOT_PROC_INST_ID_ VARCHAR(64) NULL,
    PROC_INST_ID_      VARCHAR(64) NULL,
    ACTION_            VARCHAR(255) NULL,
    MESSAGE_           VARCHAR(4000) NULL,
    FULL_MSG_          BLOB NULL,
    TENANT_ID_         VARCHAR(64) NULL,
    REMOVAL_TIME_      datetime NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_hi_dec_in
(
    ID_                VARCHAR(64) NOT NULL,
    DEC_INST_ID_       VARCHAR(64) NOT NULL,
    CLAUSE_ID_         VARCHAR(64) NULL,
    CLAUSE_NAME_       VARCHAR(255) NULL,
    VAR_TYPE_          VARCHAR(100) NULL,
    BYTEARRAY_ID_      VARCHAR(64) NULL,
    DOUBLE_ DOUBLE NULL,
    LONG_              BIGINT NULL,
    TEXT_              VARCHAR(4000) NULL,
    TEXT2_             VARCHAR(4000) NULL,
    TENANT_ID_         VARCHAR(64) NULL,
    CREATE_TIME_       datetime NULL,
    ROOT_PROC_INST_ID_ VARCHAR(64) NULL,
    REMOVAL_TIME_      datetime NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_hi_dec_out
(
    ID_                VARCHAR(64) NOT NULL,
    DEC_INST_ID_       VARCHAR(64) NOT NULL,
    CLAUSE_ID_         VARCHAR(64) NULL,
    CLAUSE_NAME_       VARCHAR(255) NULL,
    RULE_ID_           VARCHAR(64) NULL,
    RULE_ORDER_        INT NULL,
    VAR_NAME_          VARCHAR(255) NULL,
    VAR_TYPE_          VARCHAR(100) NULL,
    BYTEARRAY_ID_      VARCHAR(64) NULL,
    DOUBLE_ DOUBLE NULL,
    LONG_              BIGINT NULL,
    TEXT_              VARCHAR(4000) NULL,
    TEXT2_             VARCHAR(4000) NULL,
    TENANT_ID_         VARCHAR(64) NULL,
    CREATE_TIME_       datetime NULL,
    ROOT_PROC_INST_ID_ VARCHAR(64) NULL,
    REMOVAL_TIME_      datetime NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_hi_decinst
(
    ID_                VARCHAR(64)  NOT NULL,
    DEC_DEF_ID_        VARCHAR(64)  NOT NULL,
    DEC_DEF_KEY_       VARCHAR(255) NOT NULL,
    DEC_DEF_NAME_      VARCHAR(255) NULL,
    PROC_DEF_KEY_      VARCHAR(255) NULL,
    PROC_DEF_ID_       VARCHAR(64) NULL,
    PROC_INST_ID_      VARCHAR(64) NULL,
    CASE_DEF_KEY_      VARCHAR(255) NULL,
    CASE_DEF_ID_       VARCHAR(64) NULL,
    CASE_INST_ID_      VARCHAR(64) NULL,
    ACT_INST_ID_       VARCHAR(64) NULL,
    ACT_ID_            VARCHAR(255) NULL,
    EVAL_TIME_         datetime     NOT NULL,
    REMOVAL_TIME_      datetime NULL,
    COLLECT_VALUE_ DOUBLE NULL,
    USER_ID_           VARCHAR(255) NULL,
    ROOT_DEC_INST_ID_  VARCHAR(64) NULL,
    ROOT_PROC_INST_ID_ VARCHAR(64) NULL,
    DEC_REQ_ID_        VARCHAR(64) NULL,
    DEC_REQ_KEY_       VARCHAR(255) NULL,
    TENANT_ID_         VARCHAR(64) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_hi_detail
(
    ID_                VARCHAR(64)  NOT NULL,
    TYPE_              VARCHAR(255) NOT NULL,
    PROC_DEF_KEY_      VARCHAR(255) NULL,
    PROC_DEF_ID_       VARCHAR(64) NULL,
    ROOT_PROC_INST_ID_ VARCHAR(64) NULL,
    PROC_INST_ID_      VARCHAR(64) NULL,
    EXECUTION_ID_      VARCHAR(64) NULL,
    CASE_DEF_KEY_      VARCHAR(255) NULL,
    CASE_DEF_ID_       VARCHAR(64) NULL,
    CASE_INST_ID_      VARCHAR(64) NULL,
    CASE_EXECUTION_ID_ VARCHAR(64) NULL,
    TASK_ID_           VARCHAR(64) NULL,
    ACT_INST_ID_       VARCHAR(64) NULL,
    VAR_INST_ID_       VARCHAR(64) NULL,
    NAME_              VARCHAR(255) NOT NULL,
    VAR_TYPE_          VARCHAR(255) NULL,
    REV_               INT NULL,
    TIME_              datetime     NOT NULL,
    BYTEARRAY_ID_      VARCHAR(64) NULL,
    DOUBLE_ DOUBLE NULL,
    LONG_              BIGINT NULL,
    TEXT_              VARCHAR(4000) NULL,
    TEXT2_             VARCHAR(4000) NULL,
    SEQUENCE_COUNTER_  BIGINT NULL,
    TENANT_ID_         VARCHAR(64) NULL,
    OPERATION_ID_      VARCHAR(64) NULL,
    REMOVAL_TIME_      datetime NULL,
    INITIAL_           TINYINT(1)    NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_hi_ext_task_log
(
    ID_                VARCHAR(64)      NOT NULL,
    TIMESTAMP_         timestamp        NOT NULL,
    EXT_TASK_ID_       VARCHAR(64)      NOT NULL,
    RETRIES_           INT NULL,
    TOPIC_NAME_        VARCHAR(255) NULL,
    WORKER_ID_         VARCHAR(255) NULL,
    PRIORITY_          BIGINT DEFAULT 0 NOT NULL,
    ERROR_MSG_         VARCHAR(4000) NULL,
    ERROR_DETAILS_ID_  VARCHAR(64) NULL,
    ACT_ID_            VARCHAR(255) NULL,
    ACT_INST_ID_       VARCHAR(64) NULL,
    EXECUTION_ID_      VARCHAR(64) NULL,
    ROOT_PROC_INST_ID_ VARCHAR(64) NULL,
    PROC_INST_ID_      VARCHAR(64) NULL,
    PROC_DEF_ID_       VARCHAR(64) NULL,
    PROC_DEF_KEY_      VARCHAR(255) NULL,
    TENANT_ID_         VARCHAR(64) NULL,
    STATE_             INT NULL,
    REV_               INT NULL,
    REMOVAL_TIME_      datetime NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_hi_identitylink
(
    ID_                VARCHAR(64) NOT NULL,
    TIMESTAMP_         timestamp   NOT NULL,
    TYPE_              VARCHAR(255) NULL,
    USER_ID_           VARCHAR(255) NULL,
    GROUP_ID_          VARCHAR(255) NULL,
    TASK_ID_           VARCHAR(64) NULL,
    ROOT_PROC_INST_ID_ VARCHAR(64) NULL,
    PROC_DEF_ID_       VARCHAR(64) NULL,
    OPERATION_TYPE_    VARCHAR(64) NULL,
    ASSIGNER_ID_       VARCHAR(64) NULL,
    PROC_DEF_KEY_      VARCHAR(255) NULL,
    TENANT_ID_         VARCHAR(64) NULL,
    REMOVAL_TIME_      datetime NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_hi_incident
(
    ID_                     VARCHAR(64)  NOT NULL,
    PROC_DEF_KEY_           VARCHAR(255) NULL,
    PROC_DEF_ID_            VARCHAR(64) NULL,
    ROOT_PROC_INST_ID_      VARCHAR(64) NULL,
    PROC_INST_ID_           VARCHAR(64) NULL,
    EXECUTION_ID_           VARCHAR(64) NULL,
    CREATE_TIME_            timestamp    NOT NULL,
    END_TIME_               timestamp NULL,
    INCIDENT_MSG_           VARCHAR(4000) NULL,
    INCIDENT_TYPE_          VARCHAR(255) NOT NULL,
    ACTIVITY_ID_            VARCHAR(255) NULL,
    FAILED_ACTIVITY_ID_     VARCHAR(255) NULL,
    CAUSE_INCIDENT_ID_      VARCHAR(64) NULL,
    ROOT_CAUSE_INCIDENT_ID_ VARCHAR(64) NULL,
    CONFIGURATION_          VARCHAR(255) NULL,
    HISTORY_CONFIGURATION_  VARCHAR(255) NULL,
    INCIDENT_STATE_         INT NULL,
    TENANT_ID_              VARCHAR(64) NULL,
    JOB_DEF_ID_             VARCHAR(64) NULL,
    ANNOTATION_             VARCHAR(4000) NULL,
    REMOVAL_TIME_           datetime NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_hi_job_log
(
    ID_                     VARCHAR(64)      NOT NULL,
    TIMESTAMP_              datetime         NOT NULL,
    JOB_ID_                 VARCHAR(64)      NOT NULL,
    JOB_DUEDATE_            datetime NULL,
    JOB_RETRIES_            INT NULL,
    JOB_PRIORITY_           BIGINT DEFAULT 0 NOT NULL,
    JOB_EXCEPTION_MSG_      VARCHAR(4000) NULL,
    JOB_EXCEPTION_STACK_ID_ VARCHAR(64) NULL,
    JOB_STATE_              INT NULL,
    JOB_DEF_ID_             VARCHAR(64) NULL,
    JOB_DEF_TYPE_           VARCHAR(255) NULL,
    JOB_DEF_CONFIGURATION_  VARCHAR(255) NULL,
    ACT_ID_                 VARCHAR(255) NULL,
    FAILED_ACT_ID_          VARCHAR(255) NULL,
    EXECUTION_ID_           VARCHAR(64) NULL,
    ROOT_PROC_INST_ID_      VARCHAR(64) NULL,
    PROCESS_INSTANCE_ID_    VARCHAR(64) NULL,
    PROCESS_DEF_ID_         VARCHAR(64) NULL,
    PROCESS_DEF_KEY_        VARCHAR(255) NULL,
    DEPLOYMENT_ID_          VARCHAR(64) NULL,
    SEQUENCE_COUNTER_       BIGINT NULL,
    TENANT_ID_              VARCHAR(64) NULL,
    HOSTNAME_               VARCHAR(255) NULL,
    REMOVAL_TIME_           datetime NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_hi_op_log
(
    ID_                VARCHAR(64) NOT NULL,
    DEPLOYMENT_ID_     VARCHAR(64) NULL,
    PROC_DEF_ID_       VARCHAR(64) NULL,
    PROC_DEF_KEY_      VARCHAR(255) NULL,
    ROOT_PROC_INST_ID_ VARCHAR(64) NULL,
    PROC_INST_ID_      VARCHAR(64) NULL,
    EXECUTION_ID_      VARCHAR(64) NULL,
    CASE_DEF_ID_       VARCHAR(64) NULL,
    CASE_INST_ID_      VARCHAR(64) NULL,
    CASE_EXECUTION_ID_ VARCHAR(64) NULL,
    TASK_ID_           VARCHAR(64) NULL,
    JOB_ID_            VARCHAR(64) NULL,
    JOB_DEF_ID_        VARCHAR(64) NULL,
    BATCH_ID_          VARCHAR(64) NULL,
    USER_ID_           VARCHAR(255) NULL,
    TIMESTAMP_         timestamp   NOT NULL,
    OPERATION_TYPE_    VARCHAR(64) NULL,
    OPERATION_ID_      VARCHAR(64) NULL,
    ENTITY_TYPE_       VARCHAR(30) NULL,
    PROPERTY_          VARCHAR(64) NULL,
    ORG_VALUE_         VARCHAR(4000) NULL,
    NEW_VALUE_         VARCHAR(4000) NULL,
    TENANT_ID_         VARCHAR(64) NULL,
    REMOVAL_TIME_      datetime NULL,
    CATEGORY_          VARCHAR(64) NULL,
    EXTERNAL_TASK_ID_  VARCHAR(64) NULL,
    ANNOTATION_        VARCHAR(4000) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_hi_procinst
(
    ID_                        VARCHAR(64) NOT NULL,
    PROC_INST_ID_              VARCHAR(64) NOT NULL,
    BUSINESS_KEY_              VARCHAR(255) NULL,
    PROC_DEF_KEY_              VARCHAR(255) NULL,
    PROC_DEF_ID_               VARCHAR(64) NOT NULL,
    START_TIME_                datetime    NOT NULL,
    END_TIME_                  datetime NULL,
    REMOVAL_TIME_              datetime NULL,
    DURATION_                  BIGINT NULL,
    START_USER_ID_             VARCHAR(255) NULL,
    START_ACT_ID_              VARCHAR(255) NULL,
    END_ACT_ID_                VARCHAR(255) NULL,
    SUPER_PROCESS_INSTANCE_ID_ VARCHAR(64) NULL,
    ROOT_PROC_INST_ID_         VARCHAR(64) NULL,
    SUPER_CASE_INSTANCE_ID_    VARCHAR(64) NULL,
    CASE_INST_ID_              VARCHAR(64) NULL,
    DELETE_REASON_             VARCHAR(4000) NULL,
    TENANT_ID_                 VARCHAR(64) NULL,
    STATE_                     VARCHAR(255) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_hi_taskinst
(
    ID_                VARCHAR(64) NOT NULL,
    TASK_DEF_KEY_      VARCHAR(255) NULL,
    PROC_DEF_KEY_      VARCHAR(255) NULL,
    PROC_DEF_ID_       VARCHAR(64) NULL,
    ROOT_PROC_INST_ID_ VARCHAR(64) NULL,
    PROC_INST_ID_      VARCHAR(64) NULL,
    EXECUTION_ID_      VARCHAR(64) NULL,
    CASE_DEF_KEY_      VARCHAR(255) NULL,
    CASE_DEF_ID_       VARCHAR(64) NULL,
    CASE_INST_ID_      VARCHAR(64) NULL,
    CASE_EXECUTION_ID_ VARCHAR(64) NULL,
    ACT_INST_ID_       VARCHAR(64) NULL,
    NAME_              VARCHAR(255) NULL,
    PARENT_TASK_ID_    VARCHAR(64) NULL,
    DESCRIPTION_       VARCHAR(4000) NULL,
    OWNER_             VARCHAR(255) NULL,
    ASSIGNEE_          VARCHAR(255) NULL,
    START_TIME_        datetime    NOT NULL,
    END_TIME_          datetime NULL,
    DURATION_          BIGINT NULL,
    DELETE_REASON_     VARCHAR(4000) NULL,
    PRIORITY_          INT NULL,
    DUE_DATE_          datetime NULL,
    FOLLOW_UP_DATE_    datetime NULL,
    TENANT_ID_         VARCHAR(64) NULL,
    REMOVAL_TIME_      datetime NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_hi_varinst
(
    ID_                VARCHAR(64)  NOT NULL,
    PROC_DEF_KEY_      VARCHAR(255) NULL,
    PROC_DEF_ID_       VARCHAR(64) NULL,
    ROOT_PROC_INST_ID_ VARCHAR(64) NULL,
    PROC_INST_ID_      VARCHAR(64) NULL,
    EXECUTION_ID_      VARCHAR(64) NULL,
    ACT_INST_ID_       VARCHAR(64) NULL,
    CASE_DEF_KEY_      VARCHAR(255) NULL,
    CASE_DEF_ID_       VARCHAR(64) NULL,
    CASE_INST_ID_      VARCHAR(64) NULL,
    CASE_EXECUTION_ID_ VARCHAR(64) NULL,
    TASK_ID_           VARCHAR(64) NULL,
    NAME_              VARCHAR(255) NOT NULL,
    VAR_TYPE_          VARCHAR(100) NULL,
    CREATE_TIME_       datetime NULL,
    REV_               INT NULL,
    BYTEARRAY_ID_      VARCHAR(64) NULL,
    DOUBLE_ DOUBLE NULL,
    LONG_              BIGINT NULL,
    TEXT_              VARCHAR(4000) NULL,
    TEXT2_             VARCHAR(4000) NULL,
    TENANT_ID_         VARCHAR(64) NULL,
    STATE_             VARCHAR(20) NULL,
    REMOVAL_TIME_      datetime NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_id_group
(
    ID_   VARCHAR(64) NOT NULL,
    REV_  INT NULL,
    NAME_ VARCHAR(255) NULL,
    TYPE_ VARCHAR(255) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_id_info
(
    ID_        VARCHAR(64) NOT NULL,
    REV_       INT NULL,
    USER_ID_   VARCHAR(64) NULL,
    TYPE_      VARCHAR(64) NULL,
    KEY_       VARCHAR(255) NULL,
    VALUE_     VARCHAR(255) NULL,
    PASSWORD_  BLOB NULL,
    PARENT_ID_ VARCHAR(255) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_id_membership
(
    USER_ID_  VARCHAR(64) NOT NULL,
    GROUP_ID_ VARCHAR(64) NOT NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (USER_ID_, GROUP_ID_)
);

CREATE TABLE act_id_tenant
(
    ID_   VARCHAR(64) NOT NULL,
    REV_  INT NULL,
    NAME_ VARCHAR(255) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_id_tenant_member
(
    ID_        VARCHAR(64) NOT NULL,
    TENANT_ID_ VARCHAR(64) NOT NULL,
    USER_ID_   VARCHAR(64) NULL,
    GROUP_ID_  VARCHAR(64) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_id_user
(
    ID_            VARCHAR(64) NOT NULL,
    REV_           INT NULL,
    FIRST_         VARCHAR(255) NULL,
    LAST_          VARCHAR(255) NULL,
    EMAIL_         VARCHAR(255) NULL,
    PWD_           VARCHAR(255) NULL,
    SALT_          VARCHAR(255) NULL,
    LOCK_EXP_TIME_ datetime NULL,
    ATTEMPTS_      INT NULL,
    PICTURE_ID_    VARCHAR(64) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_re_camformdef
(
    ID_            VARCHAR(64)  NOT NULL,
    REV_           INT NULL,
    KEY_           VARCHAR(255) NOT NULL,
    VERSION_       INT          NOT NULL,
    DEPLOYMENT_ID_ VARCHAR(64) NULL,
    RESOURCE_NAME_ VARCHAR(4000) NULL,
    TENANT_ID_     VARCHAR(64) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_re_case_def
(
    ID_                 VARCHAR(64)  NOT NULL,
    REV_                INT NULL,
    CATEGORY_           VARCHAR(255) NULL,
    NAME_               VARCHAR(255) NULL,
    KEY_                VARCHAR(255) NOT NULL,
    VERSION_            INT          NOT NULL,
    DEPLOYMENT_ID_      VARCHAR(64) NULL,
    RESOURCE_NAME_      VARCHAR(4000) NULL,
    DGRM_RESOURCE_NAME_ VARCHAR(4000) NULL,
    TENANT_ID_          VARCHAR(64) NULL,
    HISTORY_TTL_        INT NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_re_decision_def
(
    ID_                 VARCHAR(64)  NOT NULL,
    REV_                INT NULL,
    CATEGORY_           VARCHAR(255) NULL,
    NAME_               VARCHAR(255) NULL,
    KEY_                VARCHAR(255) NOT NULL,
    VERSION_            INT          NOT NULL,
    DEPLOYMENT_ID_      VARCHAR(64) NULL,
    RESOURCE_NAME_      VARCHAR(4000) NULL,
    DGRM_RESOURCE_NAME_ VARCHAR(4000) NULL,
    DEC_REQ_ID_         VARCHAR(64) NULL,
    DEC_REQ_KEY_        VARCHAR(255) NULL,
    TENANT_ID_          VARCHAR(64) NULL,
    HISTORY_TTL_        INT NULL,
    VERSION_TAG_        VARCHAR(64) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_re_decision_req_def
(
    ID_                 VARCHAR(64)  NOT NULL,
    REV_                INT NULL,
    CATEGORY_           VARCHAR(255) NULL,
    NAME_               VARCHAR(255) NULL,
    KEY_                VARCHAR(255) NOT NULL,
    VERSION_            INT          NOT NULL,
    DEPLOYMENT_ID_      VARCHAR(64) NULL,
    RESOURCE_NAME_      VARCHAR(4000) NULL,
    DGRM_RESOURCE_NAME_ VARCHAR(4000) NULL,
    TENANT_ID_          VARCHAR(64) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_re_deployment
(
    ID_          VARCHAR(64) NOT NULL,
    NAME_        VARCHAR(255) NULL,
    DEPLOY_TIME_ datetime NULL,
    SOURCE_      VARCHAR(255) NULL,
    TENANT_ID_   VARCHAR(64) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_re_procdef
(
    ID_                 VARCHAR(64)  NOT NULL,
    REV_                INT NULL,
    CATEGORY_           VARCHAR(255) NULL,
    NAME_               VARCHAR(255) NULL,
    KEY_                VARCHAR(255) NOT NULL,
    VERSION_            INT          NOT NULL,
    DEPLOYMENT_ID_      VARCHAR(64) NULL,
    RESOURCE_NAME_      VARCHAR(4000) NULL,
    DGRM_RESOURCE_NAME_ VARCHAR(4000) NULL,
    HAS_START_FORM_KEY_ TINYINT NULL,
    SUSPENSION_STATE_   INT NULL,
    TENANT_ID_          VARCHAR(64) NULL,
    VERSION_TAG_        VARCHAR(64) NULL,
    HISTORY_TTL_        INT NULL,
    STARTABLE_          TINYINT(1) DEFAULT 1 NOT NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_ru_authorization
(
    ID_                VARCHAR(64) NOT NULL,
    REV_               INT         NOT NULL,
    TYPE_              INT         NOT NULL,
    GROUP_ID_          VARCHAR(255) NULL,
    USER_ID_           VARCHAR(255) NULL,
    RESOURCE_TYPE_     INT         NOT NULL,
    RESOURCE_ID_       VARCHAR(255) NULL,
    PERMS_             INT NULL,
    REMOVAL_TIME_      datetime NULL,
    ROOT_PROC_INST_ID_ VARCHAR(64) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_ru_batch
(
    ID_                  VARCHAR(64) NOT NULL,
    REV_                 INT         NOT NULL,
    TYPE_                VARCHAR(255) NULL,
    TOTAL_JOBS_          INT NULL,
    JOBS_CREATED_        INT NULL,
    JOBS_PER_SEED_       INT NULL,
    INVOCATIONS_PER_JOB_ INT NULL,
    SEED_JOB_DEF_ID_     VARCHAR(64) NULL,
    BATCH_JOB_DEF_ID_    VARCHAR(64) NULL,
    MONITOR_JOB_DEF_ID_  VARCHAR(64) NULL,
    SUSPENSION_STATE_    INT NULL,
    CONFIGURATION_       VARCHAR(255) NULL,
    TENANT_ID_           VARCHAR(64) NULL,
    CREATE_USER_ID_      VARCHAR(255) NULL,
    START_TIME_          datetime NULL,
    EXEC_START_TIME_     datetime NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_ru_case_execution
(
    ID_              VARCHAR(64) NOT NULL,
    REV_             INT NULL,
    CASE_INST_ID_    VARCHAR(64) NULL,
    SUPER_CASE_EXEC_ VARCHAR(64) NULL,
    SUPER_EXEC_      VARCHAR(64) NULL,
    BUSINESS_KEY_    VARCHAR(255) NULL,
    PARENT_ID_       VARCHAR(64) NULL,
    CASE_DEF_ID_     VARCHAR(64) NULL,
    ACT_ID_          VARCHAR(255) NULL,
    PREV_STATE_      INT NULL,
    CURRENT_STATE_   INT NULL,
    REQUIRED_        TINYINT(1)   NULL,
    TENANT_ID_       VARCHAR(64) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_ru_case_sentry_part
(
    ID_                  VARCHAR(64) NOT NULL,
    REV_                 INT NULL,
    CASE_INST_ID_        VARCHAR(64) NULL,
    CASE_EXEC_ID_        VARCHAR(64) NULL,
    SENTRY_ID_           VARCHAR(255) NULL,
    TYPE_                VARCHAR(255) NULL,
    SOURCE_CASE_EXEC_ID_ VARCHAR(64) NULL,
    STANDARD_EVENT_      VARCHAR(255) NULL,
    SOURCE_              VARCHAR(255) NULL,
    VARIABLE_EVENT_      VARCHAR(255) NULL,
    VARIABLE_NAME_       VARCHAR(255) NULL,
    SATISFIED_           TINYINT(1)   NULL,
    TENANT_ID_           VARCHAR(64) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_ru_event_subscr
(
    ID_            VARCHAR(64)  NOT NULL,
    REV_           INT NULL,
    EVENT_TYPE_    VARCHAR(255) NOT NULL,
    EVENT_NAME_    VARCHAR(255) NULL,
    EXECUTION_ID_  VARCHAR(64) NULL,
    PROC_INST_ID_  VARCHAR(64) NULL,
    ACTIVITY_ID_   VARCHAR(255) NULL,
    CONFIGURATION_ VARCHAR(255) NULL,
    CREATED_       datetime     NOT NULL,
    TENANT_ID_     VARCHAR(64) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_ru_execution
(
    ID_                VARCHAR(64) NOT NULL,
    REV_               INT NULL,
    ROOT_PROC_INST_ID_ VARCHAR(64) NULL,
    PROC_INST_ID_      VARCHAR(64) NULL,
    BUSINESS_KEY_      VARCHAR(255) NULL,
    PARENT_ID_         VARCHAR(64) NULL,
    PROC_DEF_ID_       VARCHAR(64) NULL,
    SUPER_EXEC_        VARCHAR(64) NULL,
    SUPER_CASE_EXEC_   VARCHAR(64) NULL,
    CASE_INST_ID_      VARCHAR(64) NULL,
    ACT_ID_            VARCHAR(255) NULL,
    ACT_INST_ID_       VARCHAR(64) NULL,
    IS_ACTIVE_         TINYINT NULL,
    IS_CONCURRENT_     TINYINT NULL,
    IS_SCOPE_          TINYINT NULL,
    IS_EVENT_SCOPE_    TINYINT NULL,
    SUSPENSION_STATE_  INT NULL,
    CACHED_ENT_STATE_  INT NULL,
    SEQUENCE_COUNTER_  BIGINT NULL,
    TENANT_ID_         VARCHAR(64) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_ru_ext_task
(
    ID_                  VARCHAR(64)      NOT NULL,
    REV_                 INT              NOT NULL,
    WORKER_ID_           VARCHAR(255) NULL,
    TOPIC_NAME_          VARCHAR(255) NULL,
    RETRIES_             INT NULL,
    ERROR_MSG_           VARCHAR(4000) NULL,
    ERROR_DETAILS_ID_    VARCHAR(64) NULL,
    LOCK_EXP_TIME_       datetime NULL,
    CREATE_TIME_         datetime NULL,
    SUSPENSION_STATE_    INT NULL,
    EXECUTION_ID_        VARCHAR(64) NULL,
    PROC_INST_ID_        VARCHAR(64) NULL,
    PROC_DEF_ID_         VARCHAR(64) NULL,
    PROC_DEF_KEY_        VARCHAR(255) NULL,
    ACT_ID_              VARCHAR(255) NULL,
    ACT_INST_ID_         VARCHAR(64) NULL,
    TENANT_ID_           VARCHAR(64) NULL,
    PRIORITY_            BIGINT DEFAULT 0 NOT NULL,
    LAST_FAILURE_LOG_ID_ VARCHAR(64) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_ru_filter
(
    ID_            VARCHAR(64)  NOT NULL,
    REV_           INT          NOT NULL,
    RESOURCE_TYPE_ VARCHAR(255) NOT NULL,
    NAME_          VARCHAR(255) NOT NULL,
    OWNER_         VARCHAR(255) NULL,
    QUERY_         LONGTEXT     NOT NULL,
    PROPERTIES_    LONGTEXT NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_ru_identitylink
(
    ID_          VARCHAR(64) NOT NULL,
    REV_         INT NULL,
    GROUP_ID_    VARCHAR(255) NULL,
    TYPE_        VARCHAR(255) NULL,
    USER_ID_     VARCHAR(255) NULL,
    TASK_ID_     VARCHAR(64) NULL,
    PROC_DEF_ID_ VARCHAR(64) NULL,
    TENANT_ID_   VARCHAR(64) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_ru_incident
(
    ID_                     VARCHAR(64)  NOT NULL,
    REV_                    INT          NOT NULL,
    INCIDENT_TIMESTAMP_     datetime     NOT NULL,
    INCIDENT_MSG_           VARCHAR(4000) NULL,
    INCIDENT_TYPE_          VARCHAR(255) NOT NULL,
    EXECUTION_ID_           VARCHAR(64) NULL,
    ACTIVITY_ID_            VARCHAR(255) NULL,
    FAILED_ACTIVITY_ID_     VARCHAR(255) NULL,
    PROC_INST_ID_           VARCHAR(64) NULL,
    PROC_DEF_ID_            VARCHAR(64) NULL,
    CAUSE_INCIDENT_ID_      VARCHAR(64) NULL,
    ROOT_CAUSE_INCIDENT_ID_ VARCHAR(64) NULL,
    CONFIGURATION_          VARCHAR(255) NULL,
    TENANT_ID_              VARCHAR(64) NULL,
    JOB_DEF_ID_             VARCHAR(64) NULL,
    ANNOTATION_             VARCHAR(4000) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_ru_job
(
    ID_                  VARCHAR(64)      NOT NULL,
    REV_                 INT NULL,
    TYPE_                VARCHAR(255)     NOT NULL,
    LOCK_EXP_TIME_       datetime NULL,
    LOCK_OWNER_          VARCHAR(255) NULL,
    EXCLUSIVE_           TINYINT(1)       NULL,
    EXECUTION_ID_        VARCHAR(64) NULL,
    ROOT_PROC_INST_ID_   VARCHAR(64) NULL,
    PROCESS_INSTANCE_ID_ VARCHAR(64) NULL,
    PROCESS_DEF_ID_      VARCHAR(64) NULL,
    PROCESS_DEF_KEY_     VARCHAR(255) NULL,
    RETRIES_             INT NULL,
    EXCEPTION_STACK_ID_  VARCHAR(64) NULL,
    EXCEPTION_MSG_       VARCHAR(4000) NULL,
    FAILED_ACT_ID_       VARCHAR(255) NULL,
    DUEDATE_             datetime NULL,
    REPEAT_              VARCHAR(255) NULL,
    REPEAT_OFFSET_       BIGINT DEFAULT 0 NULL,
    HANDLER_TYPE_        VARCHAR(255) NULL,
    HANDLER_CFG_         VARCHAR(4000) NULL,
    DEPLOYMENT_ID_       VARCHAR(64) NULL,
    SUSPENSION_STATE_    INT    DEFAULT 1 NOT NULL,
    JOB_DEF_ID_          VARCHAR(64) NULL,
    PRIORITY_            BIGINT DEFAULT 0 NOT NULL,
    SEQUENCE_COUNTER_    BIGINT NULL,
    TENANT_ID_           VARCHAR(64) NULL,
    CREATE_TIME_         datetime NULL,
    LAST_FAILURE_LOG_ID_ VARCHAR(64) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_ru_jobdef
(
    ID_                VARCHAR(64)  NOT NULL,
    REV_               INT NULL,
    PROC_DEF_ID_       VARCHAR(64) NULL,
    PROC_DEF_KEY_      VARCHAR(255) NULL,
    ACT_ID_            VARCHAR(255) NULL,
    JOB_TYPE_          VARCHAR(255) NOT NULL,
    JOB_CONFIGURATION_ VARCHAR(255) NULL,
    SUSPENSION_STATE_  INT NULL,
    JOB_PRIORITY_      BIGINT NULL,
    TENANT_ID_         VARCHAR(64) NULL,
    DEPLOYMENT_ID_     VARCHAR(64) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_ru_meter_log
(
    ID_           VARCHAR(64) NOT NULL,
    NAME_         VARCHAR(64) NOT NULL,
    REPORTER_     VARCHAR(255) NULL,
    VALUE_        BIGINT NULL,
    TIMESTAMP_    datetime NULL,
    MILLISECONDS_ BIGINT DEFAULT 0 NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_ru_task
(
    ID_                VARCHAR(64) NOT NULL,
    REV_               INT NULL,
    EXECUTION_ID_      VARCHAR(64) NULL,
    PROC_INST_ID_      VARCHAR(64) NULL,
    PROC_DEF_ID_       VARCHAR(64) NULL,
    CASE_EXECUTION_ID_ VARCHAR(64) NULL,
    CASE_INST_ID_      VARCHAR(64) NULL,
    CASE_DEF_ID_       VARCHAR(64) NULL,
    NAME_              VARCHAR(255) NULL,
    PARENT_TASK_ID_    VARCHAR(64) NULL,
    DESCRIPTION_       VARCHAR(4000) NULL,
    TASK_DEF_KEY_      VARCHAR(255) NULL,
    OWNER_             VARCHAR(255) NULL,
    ASSIGNEE_          VARCHAR(255) NULL,
    DELEGATION_        VARCHAR(64) NULL,
    PRIORITY_          INT NULL,
    CREATE_TIME_       datetime NULL,
    LAST_UPDATED_      datetime NULL,
    DUE_DATE_          datetime NULL,
    FOLLOW_UP_DATE_    datetime NULL,
    SUSPENSION_STATE_  INT NULL,
    TENANT_ID_         VARCHAR(64) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_ru_task_meter_log
(
    ID_            VARCHAR(64) NOT NULL,
    ASSIGNEE_HASH_ BIGINT NULL,
    TIMESTAMP_     datetime NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE act_ru_variable
(
    ID_                  VARCHAR(64)  NOT NULL,
    REV_                 INT NULL,
    TYPE_                VARCHAR(255) NOT NULL,
    NAME_                VARCHAR(255) NOT NULL,
    EXECUTION_ID_        VARCHAR(64) NULL,
    PROC_INST_ID_        VARCHAR(64) NULL,
    PROC_DEF_ID_         VARCHAR(64) NULL,
    CASE_EXECUTION_ID_   VARCHAR(64) NULL,
    CASE_INST_ID_        VARCHAR(64) NULL,
    TASK_ID_             VARCHAR(64) NULL,
    BATCH_ID_            VARCHAR(64) NULL,
    BYTEARRAY_ID_        VARCHAR(64) NULL,
    DOUBLE_ DOUBLE NULL,
    LONG_                BIGINT NULL,
    TEXT_                VARCHAR(4000) NULL,
    TEXT2_               VARCHAR(4000) NULL,
    VAR_SCOPE_           VARCHAR(64)  NOT NULL,
    SEQUENCE_COUNTER_    BIGINT NULL,
    IS_CONCURRENT_LOCAL_ TINYINT NULL,
    TENANT_ID_           VARCHAR(64) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (ID_)
);

CREATE TABLE alert_history
(
    id           BIGINT AUTO_INCREMENT        NOT NULL,
    alert_id     VARCHAR(64)  NOT NULL,
    rule_id      BIGINT NULL,
    alert_level  VARCHAR(20)  NOT NULL,
    alert_type   VARCHAR(50)  NOT NULL,
    title        VARCHAR(255) NOT NULL,
    message      LONGTEXT     NOT NULL,
    metric_value DOUBLE NULL,
    threshold_value DOUBLE NULL,
    service_name VARCHAR(100) NULL,
    instance_id  VARCHAR(100) NULL,
    trace_id     VARCHAR(64) NULL,
    status       VARCHAR(20) DEFAULT 'FIRING' NULL,
    fired_at     timestamp   DEFAULT NOW() NULL,
    resolved_at  timestamp NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='告警历史记录表';

CREATE TABLE alert_rule_config
(
    id                  BIGINT AUTO_INCREMENT         NOT NULL,
    rule_name           VARCHAR(100) NOT NULL,
    rule_type           VARCHAR(50)  NOT NULL,
    metric_name         VARCHAR(100) NULL,
    threshold_value DOUBLE NULL,
    comparison_operator VARCHAR(20) NULL,
    duration_seconds    INT NULL,
    severity            VARCHAR(20) DEFAULT 'WARNING' NULL,
    enabled             TINYINT(1)  DEFAULT 1         NULL,
    alert_channels      VARCHAR(255) NULL,
    create_time         timestamp   DEFAULT NOW() NULL,
    update_time         timestamp   DEFAULT NOW() NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='告警规则配置表';

CREATE TABLE breakpoint_config
(
    id             BIGINT AUTO_INCREMENT    NOT NULL,
    breakpoint_id  VARCHAR(64)  NOT NULL,
    class_name     VARCHAR(255) NOT NULL,
    method_name    VARCHAR(100) NOT NULL,
    condition_expr LONGTEXT NULL,
    max_hits       INT       DEFAULT 100 NULL,
    enabled        TINYINT(1) DEFAULT 1     NULL,
    hit_count      INT       DEFAULT 0 NULL,
    create_time    timestamp DEFAULT NOW() NULL,
    update_time    timestamp DEFAULT NOW() NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='断点配置表';

CREATE TABLE exception_aggregation
(
    id                BIGINT AUTO_INCREMENT        NOT NULL,
    exception_class   VARCHAR(255) NOT NULL,
    exception_message LONGTEXT NULL,
    stack_trace_hash  VARCHAR(64)  NOT NULL,
    occurrence_count  BIGINT      DEFAULT 1 NULL,
    first_seen        timestamp    NOT NULL,
    last_seen         timestamp    NOT NULL,
    sample_log_id     VARCHAR(64) NULL,
    service_name      VARCHAR(100) NULL,
    status            VARCHAR(20) DEFAULT 'OPEN' NULL,
    severity          VARCHAR(20) DEFAULT 'MEDIUM' NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='异常聚合表';

CREATE TABLE file_metadata
(
    id                BIGINT AUTO_INCREMENT       NOT NULL COMMENT '主键ID',
    file_id           VARCHAR(64)                 NOT NULL COMMENT '文件唯一标识',
    file_name         VARCHAR(255)                NOT NULL COMMENT '文件名',
    original_name     VARCHAR(255)                NOT NULL COMMENT '原始文件名',
    file_path         VARCHAR(500)                NOT NULL COMMENT '存储路径',
    file_size         BIGINT                      NOT NULL COMMENT '文件大小(字节)',
    content_type      VARCHAR(100) NULL COMMENT '文件MIME类型',
    file_extension    VARCHAR(50) NULL COMMENT '文件扩展名',
    md5               VARCHAR(64) NULL COMMENT '文件MD5值',
    sha256            VARCHAR(128) NULL COMMENT '文件SHA256值',
    storage_type      VARCHAR(20) DEFAULT 'LOCAL' NOT NULL COMMENT '存储类型:LOCAL,MINIO,ALIYUN_OSS,AWS_S3',
    bucket_name       VARCHAR(100) NULL COMMENT '存储桶名称',
    folder_id         BIGINT NULL COMMENT '所属文件夹ID',
    folder_path       VARCHAR(500) NULL COMMENT '文件夹路径',
    is_folder         TINYINT(1)  DEFAULT 0       NULL COMMENT '是否为文件夹',
    owner_id          BIGINT                      NOT NULL COMMENT '所有者ID',
    owner_name        VARCHAR(100) NULL COMMENT '所有者名称',
    is_public         TINYINT(1)  DEFAULT 0       NULL COMMENT '是否公开',
    is_deleted        TINYINT(1)  DEFAULT 0       NULL COMMENT '是否删除(软删除)',
    deleted_at        datetime NULL COMMENT '删除时间',
    deleted_by        BIGINT NULL COMMENT '删除人ID',
    version           INT         DEFAULT 1 NULL COMMENT '当前版本号',
    latest_version_id BIGINT NULL COMMENT '最新版本ID',
    download_count    INT         DEFAULT 0 NULL COMMENT '下载次数',
    view_count        INT         DEFAULT 0 NULL COMMENT '浏览次数',
    thumbnail_path    VARCHAR(500) NULL COMMENT '缩略图路径',
    tags              VARCHAR(500) NULL COMMENT '标签(JSON数组)',
    `description`     LONGTEXT NULL COMMENT '文件描述',
    metadata          JSON NULL COMMENT '扩展元数据',
    create_time       datetime    DEFAULT NOW() NULL COMMENT '创建时间',
    update_time       datetime    DEFAULT NOW() NULL COMMENT '更新时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='文件元数据表';

CREATE TABLE file_operation_log
(
    id               BIGINT AUTO_INCREMENT  NOT NULL COMMENT '主键ID',
    file_id          VARCHAR(64) NOT NULL COMMENT '文件ID',
    operation_type   VARCHAR(50) NOT NULL COMMENT '操作类型:UPLOAD,DOWNLOAD,DELETE,RENAME,MOVE,SHARE,RECOVER',
    operator_id      BIGINT NULL COMMENT '操作人ID',
    operator_name    VARCHAR(100) NULL COMMENT '操作人名称',
    ip_address       VARCHAR(50) NULL COMMENT 'IP地址',
    user_agent       VARCHAR(500) NULL COMMENT '用户代理',
    operation_detail LONGTEXT NULL COMMENT '操作详情',
    operation_time   datetime DEFAULT NOW() NULL COMMENT '操作时间',
    create_time      datetime NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='文件操作日志表';

CREATE TABLE file_permission
(
    id              BIGINT AUTO_INCREMENT  NOT NULL COMMENT '主键ID',
    file_id         VARCHAR(64) NOT NULL COMMENT '文件ID',
    user_id         BIGINT NULL COMMENT '用户ID',
    role_id         BIGINT NULL COMMENT '角色ID',
    dept_id         BIGINT NULL COMMENT '部门ID',
    permission_type VARCHAR(20) NOT NULL COMMENT '权限类型:READ,WRITE,DELETE,SHARE',
    expire_time     datetime NULL COMMENT '过期时间',
    create_time     datetime DEFAULT NOW() NULL COMMENT '创建时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='文件权限表';

CREATE TABLE file_recycle_bin
(
    id                BIGINT AUTO_INCREMENT  NOT NULL COMMENT '主键ID',
    file_id           VARCHAR(64)  NOT NULL COMMENT '文件ID',
    file_name         VARCHAR(255) NOT NULL COMMENT '文件名',
    file_path         VARCHAR(500) NOT NULL COMMENT '原始路径',
    file_size         BIGINT       NOT NULL COMMENT '文件大小',
    deleted_by        BIGINT       NOT NULL COMMENT '删除人ID',
    deleted_by_name   VARCHAR(100) NULL COMMENT '删除人名称',
    deleted_at        datetime DEFAULT NOW() NULL COMMENT '删除时间',
    expire_at         datetime NULL COMMENT '过期时间(自动清理时间)',
    original_metadata JSON NULL COMMENT '原始元数据',
    create_time       datetime NULL,
    update_time       datetime NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='回收站表';

CREATE TABLE file_share
(
    id             BIGINT AUTO_INCREMENT      NOT NULL COMMENT '主键ID',
    share_code     VARCHAR(64)                NOT NULL COMMENT '分享码',
    file_id        VARCHAR(64)                NOT NULL COMMENT '文件ID',
    share_type     VARCHAR(20) DEFAULT 'LINK' NOT NULL COMMENT '分享类型:LINK,PASSWORD',
    share_password VARCHAR(20) NULL COMMENT '分享密码',
    share_by       BIGINT                     NOT NULL COMMENT '分享人ID',
    share_by_name  VARCHAR(100) NULL COMMENT '分享人名称',
    expire_time    datetime NULL COMMENT '过期时间',
    download_limit INT         DEFAULT 0 NULL COMMENT '下载次数限制(0表示不限制)',
    download_count INT         DEFAULT 0 NULL COMMENT '已下载次数',
    view_count     INT         DEFAULT 0 NULL COMMENT '查看次数',
    is_enabled     TINYINT(1)  DEFAULT 1      NULL COMMENT '是否启用',
    create_time    datetime    DEFAULT NOW() NULL COMMENT '创建时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='文件分享表';

CREATE TABLE file_tag
(
    id          BIGINT AUTO_INCREMENT  NOT NULL COMMENT '主键ID',
    tag_name    VARCHAR(50) NOT NULL COMMENT '标签名称',
    tag_color   VARCHAR(20) NULL COMMENT '标签颜色',
    created_by  BIGINT NULL COMMENT '创建人ID',
    create_time datetime DEFAULT NOW() NULL COMMENT '创建时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='文件标签表';

CREATE TABLE file_tag_relation
(
    id          BIGINT AUTO_INCREMENT  NOT NULL COMMENT '主键ID',
    file_id     VARCHAR(64) NOT NULL COMMENT '文件ID',
    tag_id      BIGINT      NOT NULL COMMENT '标签ID',
    create_time datetime DEFAULT NOW() NULL COMMENT '创建时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='文件标签关联表';

CREATE TABLE file_version
(
    id                 BIGINT AUTO_INCREMENT    NOT NULL COMMENT '主键ID',
    file_id            VARCHAR(64)  NOT NULL COMMENT '文件ID',
    version_number     INT          NOT NULL COMMENT '版本号',
    file_path          VARCHAR(500) NOT NULL COMMENT '存储路径',
    file_size          BIGINT       NOT NULL COMMENT '文件大小',
    md5                VARCHAR(64) NULL COMMENT '文件MD5值',
    change_description VARCHAR(500) NULL COMMENT '变更说明',
    created_by         BIGINT       NOT NULL COMMENT '创建人ID',
    created_by_name    VARCHAR(100) NULL COMMENT '创建人名称',
    create_time        datetime DEFAULT NOW() NULL COMMENT '创建时间',
    is_current         TINYINT(1) DEFAULT 0     NULL COMMENT '是否当前版本',
    update_time        datetime NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='文件版本表';

CREATE TABLE flyway_schema_history
(
    installed_rank INT                     NOT NULL,
    version        VARCHAR(50) NULL,
    `description`  VARCHAR(200)            NOT NULL,
    type           VARCHAR(20)             NOT NULL,
    script         VARCHAR(1000)           NOT NULL,
    checksum       INT NULL,
    installed_by   VARCHAR(100)            NOT NULL,
    installed_on   timestamp DEFAULT NOW() NOT NULL,
    execution_time INT                     NOT NULL,
    success        TINYINT(1)              NOT NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (installed_rank)
);

CREATE TABLE gen_datasource
(
    id                BIGINT       NOT NULL COMMENT '主键ID',
    name              VARCHAR(100) NOT NULL COMMENT '数据源名称',
    db_type           VARCHAR(20)  NOT NULL COMMENT '数据库类型：MYSQL/POSTGRESQL/ORACLE',
    host              VARCHAR(255) NOT NULL COMMENT '主机地址',
    port              INT          NOT NULL COMMENT '端口',
    database_name     VARCHAR(100) NOT NULL COMMENT '数据库名',
    username          VARCHAR(100) NOT NULL COMMENT '用户名',
    password          VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    connection_params LONGTEXT NULL COMMENT '连接参数JSON',
    status            TINYINT  DEFAULT 1 NULL COMMENT '状态：0-禁用，1-启用',
    create_time       datetime DEFAULT NOW() NULL COMMENT '创建时间',
    update_time       datetime DEFAULT NOW() NULL COMMENT '更新时间',
    create_by         BIGINT NULL COMMENT '创建人',
    update_by         BIGINT NULL COMMENT '更新人',
    deleted           TINYINT  DEFAULT 0 NULL COMMENT '删除标记：0-未删除，1-已删除',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='代码生成器-数据源配置表';

CREATE TABLE gen_history
(
    id                BIGINT      NOT NULL COMMENT '主键ID',
    project_id        BIGINT NULL COMMENT '项目ID',
    datasource_id     BIGINT      NOT NULL COMMENT '数据源ID',
    table_names       LONGTEXT    NOT NULL COMMENT '生成的表名（逗号分隔）',
    template_group_id BIGINT      NOT NULL COMMENT '使用的模板分组',
    generate_type     VARCHAR(20) NOT NULL COMMENT '生成类型：DOWNLOAD/PREVIEW/INCREMENT',
    file_path         VARCHAR(500) NULL COMMENT '生成文件路径',
    file_count        INT NULL COMMENT '生成文件数',
    status            VARCHAR(20) NOT NULL COMMENT '状态：SUCCESS/FAILED/PARTIAL',
    error_message     LONGTEXT NULL COMMENT '错误信息',
    generate_config   LONGTEXT NULL COMMENT '生成配置JSON',
    create_time       datetime DEFAULT NOW() NULL COMMENT '创建时间',
    create_by         BIGINT NULL COMMENT '创建人',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='代码生成器-生成历史表';

CREATE TABLE gen_history_detail
(
    id            BIGINT       NOT NULL COMMENT '主键ID',
    history_id    BIGINT       NOT NULL COMMENT '历史记录ID',
    table_name    VARCHAR(100) NOT NULL COMMENT '表名',
    template_code VARCHAR(50)  NOT NULL COMMENT '模板编码',
    file_path     VARCHAR(500) NULL COMMENT '文件路径',
    file_content  LONGTEXT NULL COMMENT '文件内容',
    status        VARCHAR(20)  NOT NULL COMMENT '状态：SUCCESS/FAILED',
    error_message LONGTEXT NULL COMMENT '错误信息',
    create_time   datetime DEFAULT NOW() NULL COMMENT '创建时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='代码生成器-生成文件明细表';

CREATE TABLE gen_project
(
    id                BIGINT       NOT NULL COMMENT '主键ID',
    name              VARCHAR(100) NOT NULL COMMENT '项目名称',
    package_name      VARCHAR(255) NOT NULL COMMENT '包名',
    author            VARCHAR(50) NULL COMMENT '作者',
    version           VARCHAR(20) DEFAULT '1.0.0' NULL COMMENT '版本',
    base_path         VARCHAR(500) NULL COMMENT '基础路径',
    module_name       VARCHAR(100) NULL COMMENT '模块名',
    table_prefix      VARCHAR(50) NULL COMMENT '表前缀（生成时去除）',
    template_group_id BIGINT NULL COMMENT '使用的模板分组',
    config_json       LONGTEXT NULL COMMENT '其他配置JSON',
    create_time       datetime    DEFAULT NOW() NULL COMMENT '创建时间',
    update_time       datetime    DEFAULT NOW() NULL COMMENT '更新时间',
    create_by         BIGINT NULL COMMENT '创建人',
    update_by         BIGINT NULL COMMENT '更新人',
    deleted           TINYINT     DEFAULT 0 NULL COMMENT '删除标记：0-未删除，1-已删除',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='代码生成器-项目配置表';

CREATE TABLE gen_template
(
    id               BIGINT       NOT NULL COMMENT '主键ID',
    group_id         BIGINT       NOT NULL COMMENT '分组ID',
    name             VARCHAR(100) NOT NULL COMMENT '模板名称',
    code             VARCHAR(50)  NOT NULL COMMENT '模板编码：entity/mapper/service/controller等',
    template_content LONGTEXT     NOT NULL COMMENT '模板内容',
    output_path      VARCHAR(255) NULL COMMENT '输出路径模板',
    file_suffix      VARCHAR(20) NULL COMMENT '文件后缀：.java/.tsx/.ts等',
    `description`    VARCHAR(500) NULL COMMENT '描述',
    is_builtin       TINYINT  DEFAULT 0 NULL COMMENT '是否内置：0-否，1-是',
    enabled          TINYINT  DEFAULT 1 NULL COMMENT '是否启用：0-禁用，1-启用',
    sort_order       INT      DEFAULT 0 NULL COMMENT '排序',
    create_time      datetime DEFAULT NOW() NULL COMMENT '创建时间',
    update_time      datetime DEFAULT NOW() NULL COMMENT '更新时间',
    create_by        BIGINT NULL COMMENT '创建人',
    update_by        BIGINT NULL COMMENT '更新人',
    deleted          TINYINT  DEFAULT 0 NULL COMMENT '删除标记：0-未删除，1-已删除',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='代码生成器-代码模板表';

CREATE TABLE gen_template_group
(
    id            BIGINT       NOT NULL COMMENT '主键ID',
    name          VARCHAR(100) NOT NULL COMMENT '分组名称',
    code          VARCHAR(50)  NOT NULL COMMENT '分组编码',
    `description` VARCHAR(500) NULL COMMENT '描述',
    engine_type   VARCHAR(20)  NOT NULL COMMENT '模板引擎：FREEMARKER/VELOCITY/THYMELEAF',
    sort_order    INT      DEFAULT 0 NULL COMMENT '排序',
    create_time   datetime DEFAULT NOW() NULL COMMENT '创建时间',
    update_time   datetime DEFAULT NOW() NULL COMMENT '更新时间',
    create_by     BIGINT NULL COMMENT '创建人',
    update_by     BIGINT NULL COMMENT '更新人',
    deleted       TINYINT  DEFAULT 0 NULL COMMENT '删除标记：0-未删除，1-已删除',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='代码生成器-模板分组表';

CREATE TABLE gen_type_mapping
(
    id             BIGINT       NOT NULL COMMENT '主键ID',
    db_type        VARCHAR(20)  NOT NULL COMMENT '数据库类型：MYSQL/POSTGRESQL/ORACLE',
    column_type    VARCHAR(50)  NOT NULL COMMENT '数据库字段类型',
    java_type      VARCHAR(100) NOT NULL COMMENT 'Java类型',
    ts_type        VARCHAR(50) NULL COMMENT 'TypeScript类型',
    import_package VARCHAR(255) NULL COMMENT 'Java导入包',
    create_time    datetime DEFAULT NOW() NULL COMMENT '创建时间',
    update_time    datetime DEFAULT NOW() NULL COMMENT '更新时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='代码生成器-字段类型映射表';

CREATE TABLE hot_deploy_history
(
    id          BIGINT AUTO_INCREMENT   NOT NULL,
    class_name  VARCHAR(255) NOT NULL,
    deploy_time timestamp DEFAULT NOW() NULL,
    success     TINYINT(1)              NOT NULL,
    message     LONGTEXT NULL,
    user_id     BIGINT NULL,
    instance_id VARCHAR(100) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='热部署历史表';

CREATE TABLE jvm_metrics
(
    id                  BIGINT AUTO_INCREMENT NOT NULL,
    instance_id         VARCHAR(100) NOT NULL,
    timestamp           timestamp    NOT NULL,
    heap_used           BIGINT NULL,
    heap_max            BIGINT NULL,
    heap_committed      BIGINT NULL,
    non_heap_used       BIGINT NULL,
    thread_count        INT NULL,
    daemon_thread_count INT NULL,
    peak_thread_count   INT NULL,
    gc_count            INT NULL,
    gc_time             BIGINT NULL,
    cpu_usage DOUBLE NULL,
    load_average DOUBLE NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='JVM性能指标表';

CREATE TABLE log_statistics
(
    id           BIGINT AUTO_INCREMENT NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    log_level    VARCHAR(20)  NOT NULL,
    log_count    BIGINT DEFAULT 0 NULL,
    time_bucket  timestamp    NOT NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='日志统计表';

CREATE TABLE notification_template
(
    id            BIGINT AUTO_INCREMENT  NOT NULL COMMENT '模板ID',
    template_code VARCHAR(50)  NOT NULL COMMENT '模板编码',
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    template_type VARCHAR(20)  NOT NULL COMMENT '模板类型：email-邮件, system-系统通知',
    subject       VARCHAR(200) NULL COMMENT '邮件主题（邮件模板使用）',
    content       LONGTEXT     NOT NULL COMMENT '模板内容（支持变量占位符）',
    variables     VARCHAR(500) NULL COMMENT '可用变量列表（JSON数组）',
    status        TINYINT  DEFAULT 1 NULL COMMENT '状态：0-禁用，1-启用',
    remark        VARCHAR(500) NULL COMMENT '备注',
    create_time   datetime DEFAULT NOW() NULL COMMENT '创建时间',
    update_time   datetime DEFAULT NOW() NULL COMMENT '更新时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='通知模板表';

CREATE TABLE profiling_session
(
    id               BIGINT AUTO_INCREMENT         NOT NULL,
    session_id       VARCHAR(64)  NOT NULL,
    instance_id      VARCHAR(100) NOT NULL,
    profiling_type   VARCHAR(50)  NOT NULL,
    start_time       timestamp    NOT NULL,
    end_time         timestamp NULL,
    duration         BIGINT NULL,
    flame_graph_path VARCHAR(500) NULL,
    hot_methods      JSON NULL,
    status           VARCHAR(20) DEFAULT 'RUNNING' NULL,
    create_time      timestamp   DEFAULT NOW() NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='性能剖析会话表';

CREATE TABLE service_dependency
(
    id             BIGINT AUTO_INCREMENT NOT NULL,
    from_service   VARCHAR(100) NOT NULL,
    to_service     VARCHAR(100) NOT NULL,
    call_count     BIGINT DEFAULT 0 NULL,
    error_count    BIGINT DEFAULT 0 NULL,
    total_duration BIGINT DEFAULT 0 NULL,
    time_bucket    timestamp    NOT NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='服务调用依赖表';

CREATE TABLE slow_sql_record
(
    id            BIGINT AUTO_INCREMENT   NOT NULL,
    method_name   VARCHAR(255) NOT NULL,
    sql_statement LONGTEXT NULL,
    duration      BIGINT       NOT NULL,
    parameters    LONGTEXT NULL,
    trace_id      VARCHAR(64) NULL,
    service_name  VARCHAR(100) NULL,
    timestamp     timestamp DEFAULT NOW() NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='慢SQL记录表';

CREATE TABLE slow_trace_record
(
    id               BIGINT AUTO_INCREMENT   NOT NULL,
    trace_id         VARCHAR(64)  NOT NULL,
    service_name     VARCHAR(100) NOT NULL,
    operation_name   VARCHAR(255) NOT NULL,
    duration         BIGINT       NOT NULL,
    threshold        BIGINT       NOT NULL,
    bottleneck_type  VARCHAR(50) NULL,
    bottleneck_spans JSON NULL,
    create_time      timestamp DEFAULT NOW() NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='慢请求记录表';

CREATE TABLE sys_application
(
    id          BIGINT AUTO_INCREMENT    NOT NULL COMMENT '应用ID',
    app_name    VARCHAR(100)           NOT NULL COMMENT '应用名称',
    app_code    VARCHAR(50)            NOT NULL COMMENT '应用编码（唯一标识）',
    app_type    VARCHAR(20)            NOT NULL COMMENT '应用类型（web/mobile/api等）',
    app_icon    VARCHAR(255) NULL COMMENT '应用图标',
    app_url     VARCHAR(255) NULL COMMENT '应用地址',
    status      TINYINT(1) DEFAULT 1     NOT NULL COMMENT '是否启用：0-禁用，1-启用',
    order_num   INT      DEFAULT 0 NULL COMMENT '显示顺序',
    remark      VARCHAR(500) NULL COMMENT '备注',
    create_by   VARCHAR(64) NULL COMMENT '创建人',
    create_time datetime DEFAULT NOW() NOT NULL COMMENT '创建时间',
    update_by   VARCHAR(64) NULL COMMENT '更新人',
    update_time datetime DEFAULT NOW() NOT NULL COMMENT '更新时间',
    deleted     TINYINT(1) DEFAULT 0     NOT NULL COMMENT '是否删除：0-未删除，1-已删除',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='应用信息表';

CREATE TABLE sys_application_resource
(
    id            BIGINT AUTO_INCREMENT         NOT NULL COMMENT '资源ID',
    app_id        BIGINT                    NOT NULL COMMENT '所属应用ID',
    resource_name VARCHAR(100)              NOT NULL COMMENT '资源名称',
    parent_id     BIGINT      DEFAULT 0 NULL COMMENT '父资源ID（0表示顶级）',
    resource_type VARCHAR(20)               NOT NULL COMMENT '资源类型：M-目录，C-菜单，F-按钮',
    `path`        VARCHAR(255) NULL COMMENT '路由地址',
    `component`   VARCHAR(255) NULL COMMENT '组件路径',
    perms         VARCHAR(100) NULL COMMENT '权限标识',
    icon          VARCHAR(100) NULL COMMENT '菜单图标',
    `visible`     TINYINT(1)  DEFAULT 1         NOT NULL COMMENT '是否显示：0-隐藏，1-显示',
    open_type     VARCHAR(20) DEFAULT 'current' NULL COMMENT '打开方式：current-当前页，blank-新窗口',
    order_num     INT         DEFAULT 0 NULL COMMENT '显示顺序',
    status        TINYINT(1)  DEFAULT 1         NOT NULL COMMENT '状态：0-禁用，1-启用',
    remark        VARCHAR(500) NULL COMMENT '备注',
    create_by     VARCHAR(64) NULL COMMENT '创建人',
    create_time   datetime    DEFAULT NOW() NOT NULL COMMENT '创建时间',
    update_by     VARCHAR(64) NULL COMMENT '更新人',
    update_time   datetime    DEFAULT NOW() NOT NULL COMMENT '更新时间',
    deleted       TINYINT(1)  DEFAULT 0         NOT NULL COMMENT '是否删除：0-未删除，1-已删除',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='应用资源表';

CREATE TABLE sys_backup_record
(
    id              BIGINT AUTO_INCREMENT  NOT NULL COMMENT '备份ID',
    backup_code     VARCHAR(64)            NOT NULL COMMENT '备份编号',
    backup_type     VARCHAR(20)            NOT NULL COMMENT '备份类型(full/incremental)',
    status          VARCHAR(20)            NOT NULL COMMENT '备份状态(running/success/failed)',
    database_name   VARCHAR(64)            NOT NULL COMMENT '数据库名称',
    file_path       VARCHAR(512) NULL COMMENT '备份文件路径',
    file_size       BIGINT NULL COMMENT '备份文件大小(字节)',
    binlog_file     VARCHAR(128) NULL COMMENT 'Binlog文件名',
    binlog_position BIGINT NULL COMMENT 'Binlog位置',
    start_time      datetime               NOT NULL COMMENT '备份开始时间',
    end_time        datetime NULL COMMENT '备份结束时间',
    duration        BIGINT NULL COMMENT '耗时(秒)',
    error_message   LONGTEXT NULL COMMENT '错误信息',
    create_time     datetime DEFAULT NOW() NOT NULL COMMENT '创建时间',
    create_by       VARCHAR(64) NULL COMMENT '创建人',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='备份记录表';

CREATE TABLE sys_dead_letter
(
    id               BIGINT AUTO_INCREMENT         NOT NULL COMMENT '主键ID',
    message_id       VARCHAR(64)               NOT NULL COMMENT '消息ID',
    topic            VARCHAR(128)              NOT NULL COMMENT '主题/交换机',
    routing_key      VARCHAR(128) NULL COMMENT '路由键',
    tags             VARCHAR(255) NULL COMMENT 'RocketMQ 消息标签',
    message_type     VARCHAR(100) NULL COMMENT '消息类型',
    payload          LONGTEXT                  NOT NULL COMMENT '消息体(JSON格式)',
    headers          LONGTEXT NULL COMMENT '消息头(JSON格式)',
    retry_count      INT         DEFAULT 0 NULL COMMENT '重试次数',
    error_message    LONGTEXT NULL COMMENT '错误信息',
    original_queue   VARCHAR(128) NULL COMMENT '原始队列',
    status           VARCHAR(32) DEFAULT 'PENDING' NULL COMMENT '状态:PENDING-待处理,REDELIVERED-已重投,DISCARDED-已丢弃',
    create_time      datetime    DEFAULT NOW() NOT NULL COMMENT '创建时间',
    update_time      datetime    DEFAULT NOW() NULL COMMENT '更新时间',
    handled_time     datetime NULL COMMENT '处理时间',
    handled_by       BIGINT NULL COMMENT '处理人ID',
    original_message VARCHAR(200) NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='死信表（支持 RocketMQ）';

CREATE TABLE sys_dept
(
    id          BIGINT      NOT NULL COMMENT '主键ID',
    dept_name   VARCHAR(50) NOT NULL COMMENT '部门名称',
    parent_id   BIGINT   DEFAULT 0 NULL COMMENT '父部门ID',
    order_num   INT      DEFAULT 0 NULL COMMENT '显示顺序',
    leader      VARCHAR(20) NULL COMMENT '负责人',
    phone       VARCHAR(20) NULL COMMENT '联系电话',
    email       VARCHAR(50) NULL COMMENT '邮箱',
    status      TINYINT  DEFAULT 1 NULL COMMENT '部门状态：0-禁用，1-启用',
    remark      VARCHAR(500) NULL COMMENT '备注',
    create_time datetime DEFAULT NOW() NULL COMMENT '创建时间',
    update_time datetime DEFAULT NOW() NULL COMMENT '更新时间',
    create_by   BIGINT NULL COMMENT '创建人',
    update_by   BIGINT NULL COMMENT '更新人',
    deleted     TINYINT  DEFAULT 0 NULL COMMENT '逻辑删除：0-未删除，1-已删除',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='系统部门表';

CREATE TABLE sys_dict
(
    id          BIGINT       NOT NULL COMMENT '主键ID',
    app_id      BIGINT NULL COMMENT '所属应用ID（NULL表示系统字典）',
    dict_name   VARCHAR(100) NOT NULL COMMENT '字典名称',
    dict_type   VARCHAR(100) NOT NULL COMMENT '字典类型',
    status      TINYINT  DEFAULT 1 NULL COMMENT '状态：0-禁用，1-启用',
    remark      VARCHAR(500) NULL COMMENT '备注',
    create_time datetime DEFAULT NOW() NULL COMMENT '创建时间',
    update_time datetime DEFAULT NOW() NULL COMMENT '更新时间',
    create_by   BIGINT NULL COMMENT '创建人',
    update_by   BIGINT NULL COMMENT '更新人',
    deleted     TINYINT  DEFAULT 0 NULL COMMENT '逻辑删除：0-未删除，1-已删除',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='数据字典表';

CREATE TABLE sys_dict_data
(
    id          BIGINT       NOT NULL COMMENT '主键ID',
    app_id      BIGINT NULL COMMENT '所属应用ID（NULL表示系统字典）',
    dict_sort   INT      DEFAULT 0 NULL COMMENT '字典排序',
    dict_label  VARCHAR(100) NOT NULL COMMENT '字典标签',
    dict_value  VARCHAR(100) NOT NULL COMMENT '字典键值',
    dict_type   VARCHAR(100) NOT NULL COMMENT '字典类型',
    css_class   VARCHAR(100) NULL COMMENT '样式属性',
    list_class  VARCHAR(100) NULL COMMENT '表格回显样式',
    is_default  TINYINT  DEFAULT 0 NULL COMMENT '是否默认：0-否，1-是',
    status      TINYINT  DEFAULT 1 NULL COMMENT '状态：0-禁用，1-启用',
    remark      VARCHAR(500) NULL COMMENT '备注',
    create_time datetime DEFAULT NOW() NULL COMMENT '创建时间',
    update_time datetime DEFAULT NOW() NULL COMMENT '更新时间',
    create_by   BIGINT NULL COMMENT '创建人',
    update_by   BIGINT NULL COMMENT '更新人',
    deleted     TINYINT  DEFAULT 0 NULL COMMENT '逻辑删除：0-未删除，1-已删除',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='字典数据表';

CREATE TABLE sys_event_subscription
(
    id          BIGINT AUTO_INCREMENT    NOT NULL COMMENT '主键ID',
    webhook_id  BIGINT                 NOT NULL COMMENT 'Webhook配置ID',
    event_type  VARCHAR(128)           NOT NULL COMMENT '事件类型',
    enabled     TINYINT(1) DEFAULT 1     NULL COMMENT '是否启用:0-否,1-是',
    create_time datetime DEFAULT NOW() NOT NULL COMMENT '创建时间',
    update_time datetime DEFAULT NOW() NULL COMMENT '更新时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='事件订阅表';

CREATE TABLE sys_file_info
(
    id                BIGINT AUTO_INCREMENT      NOT NULL COMMENT '文件ID',
    file_code         VARCHAR(64)               NOT NULL COMMENT '文件编号',
    original_filename VARCHAR(256)              NOT NULL COMMENT '原始文件名',
    stored_filename   VARCHAR(256)              NOT NULL COMMENT '存储文件名',
    file_path         VARCHAR(512)              NOT NULL COMMENT '文件路径',
    file_url          VARCHAR(1024) NULL COMMENT '文件URL',
    file_size         BIGINT                    NOT NULL COMMENT '文件大小(字节)',
    content_type      VARCHAR(128) NULL COMMENT '文件类型(MIME)',
    file_category     VARCHAR(32) DEFAULT 'file' NULL COMMENT '文件分类(file/image/large)',
    thumbnail_url     VARCHAR(1024) NULL COMMENT '缩略图URL',
    bucket_name       VARCHAR(64)               NOT NULL COMMENT '存储桶名称',
    upload_time       datetime                  NOT NULL COMMENT '上传时间',
    upload_user_id    BIGINT NULL COMMENT '上传人ID',
    upload_username   VARCHAR(64) NULL COMMENT '上传人姓名',
    etag              VARCHAR(128) NULL COMMENT 'ETag',
    deleted           TINYINT(1)  DEFAULT 0      NOT NULL COMMENT '删除标记(0-未删除,1-已删除)',
    create_time       datetime    DEFAULT NOW() NOT NULL COMMENT '创建时间',
    create_by         VARCHAR(64) NULL COMMENT '创建人',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='文件信息表';

CREATE TABLE sys_job_dead_letter
(
    id                BIGINT AUTO_INCREMENT    NOT NULL COMMENT '死信ID',
    job_id            BIGINT                 NOT NULL COMMENT '任务ID',
    instance_id       BIGINT NULL COMMENT '实例ID',
    job_name          VARCHAR(100)           NOT NULL COMMENT '任务名称',
    params            LONGTEXT NULL COMMENT '任务参数',
    error_msg         LONGTEXT NULL COMMENT '失败原因',
    retry_times       INT      DEFAULT 0 NULL COMMENT '已重试次数',
    last_execute_time datetime NULL COMMENT '最后执行时间',
    create_time       datetime DEFAULT NOW() NOT NULL COMMENT '创建时间',
    processed         TINYINT(1) DEFAULT 0     NULL COMMENT '是否已处理',
    processed_time    datetime NULL COMMENT '处理时间',
    processed_by      VARCHAR(64) NULL COMMENT '处理人',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='死信任务表';

CREATE TABLE sys_job_info
(
    id                   BIGINT AUTO_INCREMENT            NOT NULL COMMENT '任务ID',
    power_job_id         BIGINT NULL COMMENT 'PowerJob任务ID',
    job_name             VARCHAR(100)                     NOT NULL COMMENT '任务名称',
    `description`        VARCHAR(500) NULL COMMENT '任务描述',
    job_type             VARCHAR(20)                      NOT NULL COMMENT '任务类型: SCHEDULED/DELAY/WORKFLOW/IMMEDIATE',
    execute_type         VARCHAR(20) DEFAULT 'STANDALONE' NOT NULL COMMENT '执行类型: STANDALONE/BROADCAST/MAP_REDUCE/SHARDING',
    time_expression_type VARCHAR(20)                      NOT NULL COMMENT '时间表达式类型: CRON/FIXED_RATE/FIXED_DELAY/API/WORKFLOW',
    time_expression      VARCHAR(255) NULL COMMENT '时间表达式',
    processor_type       VARCHAR(255)                     NOT NULL COMMENT '处理器类名',
    job_params           LONGTEXT NULL COMMENT '任务参数(JSON)',
    max_instance_num     INT         DEFAULT 1 NULL COMMENT '最大实例数',
    max_retry_times      INT         DEFAULT 3 NULL COMMENT '最大重试次数',
    retry_interval       INT         DEFAULT 60 NULL COMMENT '重试间隔(秒)',
    enabled              TINYINT(1)  DEFAULT 1            NULL COMMENT '是否启用',
    alert_config         LONGTEXT NULL COMMENT '告警配置(JSON)',
    app_id               BIGINT NULL COMMENT '应用ID',
    tenant_id            VARCHAR(64) NULL COMMENT '租户ID',
    create_time          datetime    DEFAULT NOW()        NOT NULL COMMENT '创建时间',
    update_time          datetime    DEFAULT NOW()        NOT NULL COMMENT '更新时间',
    create_by            VARCHAR(64) NULL COMMENT '创建人',
    update_by            VARCHAR(64) NULL COMMENT '更新人',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='任务信息表';

CREATE TABLE sys_job_instance
(
    id                    BIGINT AUTO_INCREMENT  NOT NULL COMMENT '实例ID',
    job_id                BIGINT                 NOT NULL COMMENT '任务ID',
    power_job_instance_id BIGINT NULL COMMENT 'PowerJob实例ID',
    job_name              VARCHAR(100)           NOT NULL COMMENT '任务名称',
    status                VARCHAR(20)            NOT NULL COMMENT '状态: WAITING/RUNNING/SUCCESS/FAILED/CANCELLED/TIMEOUT/STOPPED',
    params                LONGTEXT NULL COMMENT '执行参数',
    result                LONGTEXT NULL COMMENT '执行结果',
    error_msg             LONGTEXT NULL COMMENT '错误信息',
    worker_address        VARCHAR(255) NULL COMMENT '执行机器地址',
    retry_times           INT      DEFAULT 0 NULL COMMENT '重试次数',
    expected_trigger_time datetime NULL COMMENT '预期触发时间',
    actual_trigger_time   datetime NULL COMMENT '实际触发时间',
    start_time            datetime NULL COMMENT '开始执行时间',
    finish_time           datetime NULL COMMENT '完成时间',
    duration              BIGINT NULL COMMENT '执行耗时(毫秒)',
    create_time           datetime DEFAULT NOW() NOT NULL COMMENT '创建时间',
    update_time           datetime DEFAULT NOW() NOT NULL COMMENT '更新时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='任务实例表';

CREATE TABLE sys_list_operation
(
    id             BIGINT       NOT NULL COMMENT '主键ID',
    operation_code VARCHAR(50)  NOT NULL COMMENT '操作编码（唯一标识）',
    operation_name VARCHAR(100) NOT NULL COMMENT '操作名称',
    operation_type VARCHAR(20)  NOT NULL COMMENT '操作类型：view-查看，add-新增，edit-编辑，delete-删除，export-导出，import-导入',
    resource_type  VARCHAR(50) NULL COMMENT '适用资源类型（NULL表示通用）',
    icon           VARCHAR(100) NULL COMMENT '操作图标',
    order_num      INT      DEFAULT 0 NULL COMMENT '显示顺序',
    status         TINYINT  DEFAULT 1 NULL COMMENT '状态：0-禁用，1-启用',
    remark         VARCHAR(500) NULL COMMENT '备注',
    create_time    datetime DEFAULT NOW() NULL COMMENT '创建时间',
    update_time    datetime DEFAULT NOW() NULL COMMENT '更新时间',
    create_by      BIGINT NULL COMMENT '创建人',
    update_by      BIGINT NULL COMMENT '更新人',
    deleted        TINYINT  DEFAULT 0 NULL COMMENT '逻辑删除：0-未删除，1-已删除',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='列表操作定义表';

CREATE TABLE sys_login_log
(
    id             BIGINT NOT NULL COMMENT '主键ID',
    user_id        BIGINT NULL COMMENT '用户ID',
    username       VARCHAR(50) NULL COMMENT '用户名',
    ip_address     VARCHAR(50) NULL COMMENT '登录IP',
    login_location VARCHAR(255) NULL COMMENT '登录地点',
    browser        VARCHAR(50) NULL COMMENT '浏览器类型',
    os             VARCHAR(50) NULL COMMENT '操作系统',
    status         TINYINT  DEFAULT 1 NULL COMMENT '登录状态：0-失败，1-成功',
    msg            VARCHAR(255) NULL COMMENT '提示消息',
    login_time     datetime DEFAULT NOW() NULL COMMENT '登录时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='登录日志表';

CREATE TABLE sys_menu
(
    id          BIGINT      NOT NULL COMMENT '主键ID',
    app_id      BIGINT NULL COMMENT '应用ID（为空表示系统菜单）',
    menu_name   VARCHAR(50) NOT NULL COMMENT '菜单名称',
    parent_id   BIGINT   DEFAULT 0 NULL COMMENT '父菜单ID',
    order_num   INT      DEFAULT 0 NULL COMMENT '显示顺序',
    `path`      VARCHAR(200) NULL COMMENT '路由地址',
    `component` VARCHAR(255) NULL COMMENT '组件路径',
    query       VARCHAR(255) NULL COMMENT '路由参数',
    is_frame    TINYINT  DEFAULT 1 NULL COMMENT '是否为外链：0-是，1-否',
    is_cache    TINYINT  DEFAULT 0 NULL COMMENT '是否缓存：0-缓存，1-不缓存',
    menu_type   CHAR(1) NULL COMMENT '菜单类型：M-目录，C-菜单，F-按钮',
    `visible`   TINYINT  DEFAULT 1 NULL COMMENT '菜单状态：0-隐藏，1-显示',
    status      TINYINT  DEFAULT 1 NULL COMMENT '菜单状态：0-禁用，1-启用',
    perms       VARCHAR(100) NULL COMMENT '权限标识',
    icon        VARCHAR(100) NULL COMMENT '菜单图标',
    remark      VARCHAR(500) NULL COMMENT '备注',
    create_time datetime DEFAULT NOW() NULL COMMENT '创建时间',
    update_time datetime DEFAULT NOW() NULL COMMENT '更新时间',
    create_by   BIGINT NULL COMMENT '创建人',
    update_by   BIGINT NULL COMMENT '更新人',
    deleted     TINYINT  DEFAULT 0 NULL COMMENT '逻辑删除：0-未删除，1-已删除',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='系统菜单表（支持应用级隔离）';

CREATE TABLE sys_message_log
(
    id            BIGINT AUTO_INCREMENT  NOT NULL COMMENT '主键ID',
    message_id    VARCHAR(64)            NOT NULL COMMENT '消息ID',
    topic         VARCHAR(128)           NOT NULL COMMENT '主题/交换机',
    routing_key   VARCHAR(128) NULL COMMENT '路由键',
    tag           VARCHAR(64) NULL COMMENT '消息标签',
    payload       LONGTEXT               NOT NULL COMMENT '消息体(JSON格式)',
    headers       LONGTEXT NULL COMMENT '消息头(JSON格式)',
    send_time     datetime               NOT NULL COMMENT '发送时间',
    delay_millis  BIGINT NULL COMMENT '延迟时间(毫秒)',
    retry_count   INT      DEFAULT 0 NULL COMMENT '重试次数',
    max_retries   INT      DEFAULT 3 NULL COMMENT '最大重试次数',
    partition_key VARCHAR(128) NULL COMMENT '分区键(用于顺序消息)',
    status        VARCHAR(32)            NOT NULL COMMENT '状态:PENDING,SENT,DELIVERED,CONSUMING,CONSUMED,FAILED,DEAD_LETTER',
    error_message LONGTEXT NULL COMMENT '错误信息',
    create_time   datetime DEFAULT NOW() NOT NULL COMMENT '创建时间',
    update_time   datetime DEFAULT NOW() NULL COMMENT '更新时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='消息日志表（支持 RocketMQ）';

CREATE TABLE sys_message_queue_monitor
(
    id             BIGINT AUTO_INCREMENT         NOT NULL COMMENT '主键ID',
    queue_name     VARCHAR(128)              NOT NULL COMMENT '队列名称',
    message_count  BIGINT      DEFAULT 0 NULL COMMENT '消息数量',
    consumer_count INT         DEFAULT 0 NULL COMMENT '消费者数量',
    message_rate DOUBLE DEFAULT 0         NULL COMMENT '消息速率(条/秒)',
    ack_rate DOUBLE DEFAULT 0         NULL COMMENT '确认速率(条/秒)',
    state          VARCHAR(32) DEFAULT 'RUNNING' NULL COMMENT '状态:RUNNING,IDLE,FLOW',
    create_time    datetime    DEFAULT NOW() NOT NULL COMMENT '创建时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='消息队列监控表';

CREATE TABLE sys_nacos_config
(
    id            BIGINT AUTO_INCREMENT                NOT NULL COMMENT '主键ID',
    data_id       VARCHAR(255)                         NOT NULL COMMENT '配置Data ID',
    group_name    VARCHAR(128) DEFAULT 'DEFAULT_GROUP' NOT NULL COMMENT '配置分组',
    namespace     VARCHAR(128) DEFAULT 'public'        NOT NULL COMMENT '命名空间',
    content       LONGTEXT NULL COMMENT '配置内容',
    type          VARCHAR(32)  DEFAULT 'yaml' NULL COMMENT '配置类型（yaml/properties/json/xml/text）',
    environment   VARCHAR(32) NULL COMMENT '环境（dev/test/prod等）',
    tenant_id     VARCHAR(64) NULL COMMENT '租户ID',
    app_id        BIGINT NULL COMMENT '应用ID',
    version       INT          DEFAULT 1 NULL COMMENT '配置版本号',
    status        VARCHAR(32)  DEFAULT 'draft' NULL COMMENT '配置状态（draft/published/archived）',
    is_critical   TINYINT(1)   DEFAULT 0               NULL COMMENT '是否关键配置（0-否，1-是）',
    publish_type  VARCHAR(32)  DEFAULT 'auto' NULL COMMENT '发布类型（auto/manual/gray）',
    `description` VARCHAR(500) NULL COMMENT '配置描述',
    md5           VARCHAR(64) NULL COMMENT '内容MD5值',
    create_by     BIGINT NULL COMMENT '创建人ID',
    create_time   datetime     DEFAULT NOW() NULL COMMENT '创建时间',
    update_by     BIGINT NULL COMMENT '更新人ID',
    update_time   datetime     DEFAULT NOW() NULL COMMENT '更新时间',
    is_deleted    TINYINT(1)   DEFAULT 0               NULL COMMENT '删除标志（0-未删除，1-已删除）',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='Nacos配置表';

CREATE TABLE sys_nacos_config_history
(
    id             BIGINT AUTO_INCREMENT  NOT NULL COMMENT '主键ID',
    config_id      BIGINT       NOT NULL COMMENT '配置ID',
    data_id        VARCHAR(255) NOT NULL COMMENT '配置Data ID',
    group_name     VARCHAR(128) NOT NULL COMMENT '配置分组',
    namespace      VARCHAR(128) NOT NULL COMMENT '命名空间',
    content        LONGTEXT NULL COMMENT '配置内容',
    version        INT          NOT NULL COMMENT '配置版本号',
    operation_type VARCHAR(32)  NOT NULL COMMENT '操作类型（create/update/delete/rollback/publish）',
    operator       BIGINT NULL COMMENT '操作人ID',
    operator_name  VARCHAR(64) NULL COMMENT '操作人姓名',
    rollback_from  INT NULL COMMENT '回滚来源版本',
    md5            VARCHAR(64) NULL COMMENT '内容MD5值',
    create_time    datetime DEFAULT NOW() NULL COMMENT '创建时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='Nacos配置历史表';

CREATE TABLE sys_nacos_gray_config
(
    id               BIGINT AUTO_INCREMENT           NOT NULL COMMENT '主键ID',
    config_id        BIGINT      NOT NULL COMMENT '关联的配置ID',
    strategy_type    VARCHAR(32) NOT NULL COMMENT '灰度策略类型（ip/percentage/label）',
    target_instances LONGTEXT NULL COMMENT '目标实例列表（IP列表，逗号分隔）',
    percentage       INT NULL COMMENT '灰度百分比（0-100）',
    labels           VARCHAR(500) NULL COMMENT '实例标签（JSON格式）',
    status           VARCHAR(32) DEFAULT 'preparing' NULL COMMENT '灰度状态（preparing/running/completed/rollback）',
    start_time       datetime NULL COMMENT '灰度开始时间',
    end_time         datetime NULL COMMENT '灰度结束时间',
    gray_content     LONGTEXT NULL COMMENT '灰度配置内容',
    create_by        BIGINT NULL COMMENT '创建人ID',
    create_time      datetime    DEFAULT NOW() NULL COMMENT '创建时间',
    update_by        BIGINT NULL COMMENT '更新人ID',
    update_time      datetime    DEFAULT NOW() NULL COMMENT '更新时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='Nacos灰度发布配置表';

CREATE TABLE sys_nacos_publish_task
(
    id               BIGINT AUTO_INCREMENT         NOT NULL COMMENT '主键ID',
    config_id        BIGINT      NOT NULL COMMENT '配置ID',
    publish_type     VARCHAR(32) NOT NULL COMMENT '发布类型（auto/manual/gray）',
    status           VARCHAR(32) DEFAULT 'pending' NULL COMMENT '任务状态（pending/running/success/failed）',
    executor         BIGINT NULL COMMENT '执行人ID',
    executor_name    VARCHAR(64) NULL COMMENT '执行人姓名',
    target_instances LONGTEXT NULL COMMENT '目标实例列表',
    result           LONGTEXT NULL COMMENT '执行结果（JSON格式）',
    error_message    LONGTEXT NULL COMMENT '错误信息',
    create_time      datetime    DEFAULT NOW() NULL COMMENT '创建时间',
    start_time       datetime NULL COMMENT '开始时间',
    finish_time      datetime NULL COMMENT '完成时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='Nacos配置发布任务表';

CREATE TABLE sys_nacos_service
(
    id             BIGINT AUTO_INCREMENT                NOT NULL COMMENT '主键ID',
    service_name   VARCHAR(255)                         NOT NULL COMMENT '服务名',
    group_name     VARCHAR(128) DEFAULT 'DEFAULT_GROUP' NOT NULL COMMENT '分组名',
    namespace      VARCHAR(128) DEFAULT 'public'        NOT NULL COMMENT '命名空间',
    cluster_name   VARCHAR(128) NULL COMMENT '集群名',
    instance_count INT          DEFAULT 0 NULL COMMENT '实例总数',
    healthy_count  INT          DEFAULT 0 NULL COMMENT '健康实例数',
    status         VARCHAR(32)  DEFAULT 'online' NULL COMMENT '服务状态（online/offline）',
    metadata       LONGTEXT NULL COMMENT '服务元数据（JSON格式）',
    create_time    datetime     DEFAULT NOW() NULL COMMENT '创建时间',
    update_time    datetime     DEFAULT NOW() NULL COMMENT '更新时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='Nacos服务注册表';

CREATE TABLE sys_operation_log
(
    id             BIGINT NOT NULL COMMENT '主键ID',
    user_id        BIGINT NULL COMMENT '用户ID',
    username       VARCHAR(50) NULL COMMENT '用户名',
    operation      VARCHAR(50) NULL COMMENT '操作',
    method         VARCHAR(200) NULL COMMENT '请求方法',
    params         LONGTEXT NULL COMMENT '请求参数',
    time           BIGINT NULL COMMENT '执行时长(毫秒)',
    ip_address     VARCHAR(50) NULL COMMENT 'IP地址',
    location       VARCHAR(255) NULL COMMENT '操作地点',
    status         TINYINT  DEFAULT 1 NULL COMMENT '操作状态：0-失败，1-成功',
    error_msg      VARCHAR(2000) NULL COMMENT '错误消息',
    operation_time datetime DEFAULT NOW() NULL COMMENT '操作时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='操作日志表';

CREATE TABLE sys_permission
(
    id              BIGINT       NOT NULL COMMENT '主键ID',
    permission_name VARCHAR(50)  NOT NULL COMMENT '权限名称',
    permission_key  VARCHAR(100) NOT NULL COMMENT '权限标识',
    api_path        VARCHAR(200) NULL COMMENT 'API路径',
    http_method     VARCHAR(10) NULL COMMENT 'HTTP方法',
    permission_type TINYINT  DEFAULT 1 NULL COMMENT '权限类型：1-菜单权限，2-按钮权限，3-API权限',
    status          TINYINT  DEFAULT 1 NULL COMMENT '状态：0-禁用，1-启用',
    remark          VARCHAR(500) NULL COMMENT '备注',
    create_time     datetime DEFAULT NOW() NULL COMMENT '创建时间',
    update_time     datetime DEFAULT NOW() NULL COMMENT '更新时间',
    create_by       BIGINT NULL COMMENT '创建人',
    update_by       BIGINT NULL COMMENT '更新人',
    deleted         TINYINT  DEFAULT 0 NULL COMMENT '逻辑删除：0-未删除，1-已删除',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='系统权限表';

CREATE TABLE sys_role
(
    id          BIGINT      NOT NULL COMMENT '主键ID',
    parent_id   BIGINT   DEFAULT 0 NULL COMMENT '父角色ID（0表示顶级角色）',
    app_id      BIGINT NULL COMMENT '所属应用ID（NULL表示系统角色）',
    role_name   VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_key    VARCHAR(50) NOT NULL COMMENT '角色标识',
    role_sort   INT      DEFAULT 0 NULL COMMENT '显示顺序',
    data_scope  TINYINT  DEFAULT 1 NULL COMMENT '数据范围：1-全部数据权限，2-本部门数据权限，3-本部门及以下数据权限，4-仅本人数据权限',
    status      TINYINT  DEFAULT 1 NULL COMMENT '状态：0-禁用，1-启用',
    remark      VARCHAR(500) NULL COMMENT '备注',
    create_time datetime DEFAULT NOW() NULL COMMENT '创建时间',
    update_time datetime DEFAULT NOW() NULL COMMENT '更新时间',
    create_by   BIGINT NULL COMMENT '创建人',
    update_by   BIGINT NULL COMMENT '更新人',
    deleted     TINYINT  DEFAULT 0 NULL COMMENT '逻辑删除：0-未删除，1-已删除',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='系统角色表';

CREATE TABLE sys_role_data_permission
(
    id              BIGINT       NOT NULL COMMENT '主键ID',
    role_id         BIGINT       NOT NULL COMMENT '角色ID',
    resource_type   VARCHAR(50)  NOT NULL COMMENT '资源类型（如：user, dept, order等）',
    permission_name VARCHAR(100) NOT NULL COMMENT '权限名称',
    filter_type     VARCHAR(20)  NOT NULL COMMENT '过滤类型：dept-部门，field-字段，custom-自定义',
    filter_rule     LONGTEXT NULL COMMENT '过滤规则（JSON格式）',
    status          TINYINT  DEFAULT 1 NULL COMMENT '状态：0-禁用，1-启用',
    remark          VARCHAR(500) NULL COMMENT '备注',
    create_time     datetime DEFAULT NOW() NULL COMMENT '创建时间',
    update_time     datetime DEFAULT NOW() NULL COMMENT '更新时间',
    create_by       BIGINT NULL COMMENT '创建人',
    update_by       BIGINT NULL COMMENT '更新人',
    deleted         TINYINT  DEFAULT 0 NULL COMMENT '逻辑删除：0-未删除，1-已删除',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='角色数据权限配置表';

CREATE TABLE sys_role_list_operation
(
    id            BIGINT      NOT NULL COMMENT '主键ID',
    role_id       BIGINT      NOT NULL COMMENT '角色ID',
    operation_id  BIGINT      NOT NULL COMMENT '操作ID',
    resource_type VARCHAR(50) NOT NULL COMMENT '资源类型',
    create_time   datetime DEFAULT NOW() NULL COMMENT '创建时间',
    create_by     BIGINT NULL COMMENT '创建人',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='角色列表操作关联表';

CREATE TABLE sys_role_menu
(
    id          BIGINT NOT NULL COMMENT '主键ID',
    role_id     BIGINT NOT NULL COMMENT '角色ID',
    menu_id     BIGINT NOT NULL COMMENT '菜单ID',
    create_time datetime DEFAULT NOW() NULL COMMENT '创建时间',
    create_by   BIGINT NULL COMMENT '创建人',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='角色菜单关联表';

CREATE TABLE sys_role_permission
(
    id            BIGINT NOT NULL COMMENT '主键ID',
    role_id       BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    create_time   datetime DEFAULT NOW() NULL COMMENT '创建时间',
    create_by     BIGINT NULL COMMENT '创建人',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='角色权限关联表';

CREATE TABLE sys_role_resource
(
    id          BIGINT AUTO_INCREMENT  NOT NULL COMMENT '主键ID',
    role_id     BIGINT                 NOT NULL COMMENT '角色ID',
    resource_id BIGINT                 NOT NULL COMMENT '资源ID',
    create_time datetime DEFAULT NOW() NOT NULL COMMENT '创建时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='角色资源关联表';

CREATE TABLE sys_user
(
    id          BIGINT       NOT NULL COMMENT '主键ID',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(255) NOT NULL COMMENT '密码',
    nickname    VARCHAR(50) NULL COMMENT '昵称',
    email       VARCHAR(100) NULL COMMENT '邮箱',
    phone       VARCHAR(20) NULL COMMENT '手机号',
    avatar      VARCHAR(255) NULL COMMENT '头像URL',
    gender      TINYINT  DEFAULT 0 NULL COMMENT '性别：0-未知，1-男，2-女',
    birthday    date NULL COMMENT '生日',
    dept_id     BIGINT NULL COMMENT '部门ID',
    user_type   TINYINT  DEFAULT 1 NULL COMMENT '用户类型：1-系统用户，2-普通用户',
    status      TINYINT  DEFAULT 1 NULL COMMENT '状态：0-禁用，1-启用',
    login_ip    VARCHAR(50) NULL COMMENT '最后登录IP',
    login_time  datetime NULL COMMENT '最后登录时间',
    create_time datetime DEFAULT NOW() NULL COMMENT '创建时间',
    update_time datetime DEFAULT NOW() NULL COMMENT '更新时间',
    create_by   BIGINT NULL COMMENT '创建人',
    update_by   BIGINT NULL COMMENT '更新人',
    deleted     TINYINT  DEFAULT 0 NULL COMMENT '逻辑删除：0-未删除，1-已删除',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='系统用户表';

CREATE TABLE sys_user_role
(
    id          BIGINT NOT NULL COMMENT '主键ID',
    user_id     BIGINT NOT NULL COMMENT '用户ID',
    role_id     BIGINT NOT NULL COMMENT '角色ID',
    create_time datetime DEFAULT NOW() NULL COMMENT '创建时间',
    create_by   BIGINT NULL COMMENT '创建人',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='用户角色关联表';

CREATE TABLE sys_webhook_config
(
    id                BIGINT AUTO_INCREMENT      NOT NULL COMMENT '主键ID',
    name              VARCHAR(128)              NOT NULL COMMENT 'Webhook名称',
    url               VARCHAR(512)              NOT NULL COMMENT 'Webhook URL',
    event_types       VARCHAR(512)              NOT NULL COMMENT '订阅的事件类型(多个用逗号分隔,*表示所有)',
    secret            VARCHAR(128) NULL COMMENT '签名密钥',
    signature_enabled TINYINT(1)  DEFAULT 1      NULL COMMENT '是否启用签名验证:0-否,1-是',
    method            VARCHAR(16) DEFAULT 'POST' NULL COMMENT 'HTTP请求方法:POST,PUT',
    headers           LONGTEXT NULL COMMENT '自定义请求头(JSON格式)',
    timeout           INT         DEFAULT 30 NULL COMMENT '超时时间(秒)',
    max_retries       INT         DEFAULT 3 NULL COMMENT '最大重试次数',
    retry_interval    INT         DEFAULT 60 NULL COMMENT '重试间隔(秒)',
    enabled           TINYINT(1)  DEFAULT 1      NULL COMMENT '是否启用:0-否,1-是',
    create_time       datetime    DEFAULT NOW() NOT NULL COMMENT '创建时间',
    update_time       datetime    DEFAULT NOW() NULL COMMENT '更新时间',
    create_by         BIGINT NULL COMMENT '创建人ID',
    update_by         BIGINT NULL COMMENT '更新人ID',
    remark            VARCHAR(512) NULL COMMENT '备注',
    deleted           TINYINT(1)  DEFAULT 0      NULL COMMENT '删除标记:0-未删除,1-已删除',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='Webhook配置表';

CREATE TABLE sys_webhook_log
(
    id              BIGINT AUTO_INCREMENT    NOT NULL COMMENT '主键ID',
    webhook_id      BIGINT                 NOT NULL COMMENT 'Webhook配置ID',
    event_id        VARCHAR(64)            NOT NULL COMMENT '事件ID',
    event_type      VARCHAR(128)           NOT NULL COMMENT '事件类型',
    request_url     VARCHAR(512)           NOT NULL COMMENT '请求URL',
    request_method  VARCHAR(16)            NOT NULL COMMENT '请求方法',
    request_headers LONGTEXT NULL COMMENT '请求头(JSON格式)',
    request_body    LONGTEXT NULL COMMENT '请求体(JSON格式)',
    response_status INT NULL COMMENT '响应状态码',
    response_body   LONGTEXT NULL COMMENT '响应体',
    response_time   BIGINT NULL COMMENT '响应时间(毫秒)',
    success         TINYINT(1) DEFAULT 0     NULL COMMENT '是否成功:0-失败,1-成功',
    error_message   LONGTEXT NULL COMMENT '错误信息',
    retry_count     INT      DEFAULT 0 NULL COMMENT '重试次数',
    call_time       datetime               NOT NULL COMMENT '调用时间',
    create_time     datetime DEFAULT NOW() NOT NULL COMMENT '创建时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='Webhook调用日志表';

CREATE TABLE sys_workflow_definition
(
    id            BIGINT AUTO_INCREMENT    NOT NULL COMMENT '工作流ID',
    workflow_name VARCHAR(100)           NOT NULL COMMENT '工作流名称',
    `description` VARCHAR(500) NULL COMMENT '工作流描述',
    workflow_json LONGTEXT               NOT NULL COMMENT '工作流定义(JSON格式)',
    enabled       TINYINT(1) DEFAULT 1     NULL COMMENT '是否启用',
    app_id        BIGINT NULL COMMENT '应用ID',
    tenant_id     VARCHAR(64) NULL COMMENT '租户ID',
    create_time   datetime DEFAULT NOW() NOT NULL COMMENT '创建时间',
    update_time   datetime DEFAULT NOW() NOT NULL COMMENT '更新时间',
    create_by     VARCHAR(64) NULL COMMENT '创建人',
    update_by     VARCHAR(64) NULL COMMENT '更新人',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='工作流定义表';

CREATE TABLE sys_workflow_instance
(
    id            BIGINT AUTO_INCREMENT  NOT NULL COMMENT '实例ID',
    workflow_id   BIGINT                 NOT NULL COMMENT '工作流ID',
    workflow_name VARCHAR(100)           NOT NULL COMMENT '工作流名称',
    status        VARCHAR(20)            NOT NULL COMMENT '状态: WAITING/RUNNING/SUCCESS/FAILED/CANCELLED',
    params        LONGTEXT NULL COMMENT '执行参数',
    result        LONGTEXT NULL COMMENT '执行结果',
    error_msg     LONGTEXT NULL COMMENT '错误信息',
    start_time    datetime NULL COMMENT '开始时间',
    finish_time   datetime NULL COMMENT '完成时间',
    duration      BIGINT NULL COMMENT '执行耗时(毫秒)',
    create_time   datetime DEFAULT NOW() NOT NULL COMMENT '创建时间',
    update_time   datetime DEFAULT NOW() NOT NULL COMMENT '更新时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='工作流实例表';

CREATE TABLE trace_service_stats
(
    id             BIGINT AUTO_INCREMENT NOT NULL,
    service_name   VARCHAR(100) NOT NULL,
    operation_name VARCHAR(255) NOT NULL,
    time_bucket    timestamp    NOT NULL,
    call_count     BIGINT DEFAULT 0 NULL,
    success_count  BIGINT DEFAULT 0 NULL,
    error_count    BIGINT DEFAULT 0 NULL,
    p50_duration DOUBLE NULL,
    p95_duration DOUBLE NULL,
    p99_duration DOUBLE NULL,
    max_duration   BIGINT NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='追踪统计汇总表';

CREATE TABLE trace_span_ext
(
    span_id        VARCHAR(32)  NOT NULL,
    trace_id       VARCHAR(64)  NOT NULL,
    parent_span_id VARCHAR(32) NULL,
    service_name   VARCHAR(100) NOT NULL,
    operation_name VARCHAR(255) NOT NULL,
    start_time     BIGINT       NOT NULL,
    duration       BIGINT       NOT NULL,
    tags           JSON NULL,
    logs           JSON NULL,
    status         VARCHAR(20) DEFAULT 'OK' NULL,
    error_message  LONGTEXT NULL,
    stack_trace    LONGTEXT NULL,
    create_time    timestamp   DEFAULT NOW() NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (span_id)
) COMMENT ='追踪Span扩展表';

CREATE TABLE undo_log
(
    id            BIGINT AUTO_INCREMENT NOT NULL COMMENT 'Primary key',
    branch_id     BIGINT       NOT NULL COMMENT 'Branch transaction ID',
    xid           VARCHAR(128) NOT NULL COMMENT 'Global transaction ID',
    context       VARCHAR(128) NOT NULL COMMENT 'Undo log context (serialization type, etc.)',
    rollback_info BLOB         NOT NULL COMMENT 'Rollback data (before-image and after-image)',
    log_status    INT          NOT NULL COMMENT '0: normal, 1: defense (prevent dirty write)',
    log_created   datetime     NOT NULL COMMENT 'Creation timestamp',
    log_modified  datetime     NOT NULL COMMENT 'Modification timestamp',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='Seata AT mode undo log table';

CREATE TABLE user_2fa
(
    id                BIGINT       NOT NULL COMMENT '主键ID',
    user_id           BIGINT       NOT NULL COMMENT '用户ID',
    enabled           TINYINT     DEFAULT 0 NULL COMMENT '是否启用: 0-未启用, 1-已启用',
    auth_type         VARCHAR(20) DEFAULT 'TOTP' NULL COMMENT '认证类型: TOTP-基于时间的一次性密码',
    secret_key        VARCHAR(255) NOT NULL COMMENT '密钥 (加密存储)',
    qr_code_url       VARCHAR(500) NULL COMMENT '二维码URL',
    backup_codes      LONGTEXT NULL COMMENT '备用恢复码 (加密存储, 逗号分隔)',
    backup_codes_used LONGTEXT NULL COMMENT '已使用的备用码 (逗号分隔)',
    bind_time         datetime NULL COMMENT '绑定时间',
    last_verify_time  datetime NULL COMMENT '最后验证时间',
    verify_fail_count INT         DEFAULT 0 NULL COMMENT '验证失败次数',
    locked_until      datetime NULL COMMENT '锁定至 (防暴力破解)',
    create_time       datetime    DEFAULT NOW() NULL COMMENT '创建时间',
    update_time       datetime    DEFAULT NOW() NULL COMMENT '更新时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='双因素认证配置表';

CREATE TABLE user_device
(
    id                 BIGINT       NOT NULL COMMENT '主键ID',
    user_id            BIGINT       NOT NULL COMMENT '用户ID',
    session_id         VARCHAR(255) NOT NULL COMMENT '会话ID (Redis key)',
    token              VARCHAR(500) NOT NULL COMMENT 'JWT Token',
    device_type        VARCHAR(50) NULL COMMENT '设备类型: PC, Mobile, Tablet',
    os_name            VARCHAR(100) NULL COMMENT '操作系统',
    os_version         VARCHAR(50) NULL COMMENT '系统版本',
    device_name        VARCHAR(50) NULL,
    browser_name       VARCHAR(100) NULL COMMENT '浏览器名称',
    browser_version    VARCHAR(50) NULL COMMENT '浏览器版本',
    user_agent         VARCHAR(500) NULL COMMENT 'User-Agent',
    ip_address         VARCHAR(50) NULL COMMENT 'IP地址',
    location           VARCHAR(255) NULL COMMENT '登录地点',
    is_current         TINYINT  DEFAULT 0 NULL COMMENT '是否当前设备: 0-否, 1-是',
    status             TINYINT  DEFAULT 1 NULL COMMENT '状态: 0-已注销, 1-在线',
    last_active_time   datetime DEFAULT NOW() NULL COMMENT '最后活跃时间',
    first_login_time   datetime NULL,
    create_time        datetime NULL,
    update_time        datetime NULL,
    login_time         datetime DEFAULT NOW() NULL COMMENT '登录时间',
    logout_time        datetime NULL COMMENT '注销时间',
    expired_time       datetime NULL COMMENT '过期时间',
    browser            VARCHAR(200) NULL,
    os                 VARCHAR(50) NULL,
    device_fingerprint VARCHAR(200) NULL,
    is_trusted         INT NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='在线设备会话表';

CREATE TABLE user_notification
(
    id          BIGINT AUTO_INCREMENT        NOT NULL COMMENT '通知ID',
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    title       VARCHAR(200) NOT NULL COMMENT '通知标题',
    content     LONGTEXT NULL COMMENT '通知内容',
    type        VARCHAR(50) DEFAULT 'system' NULL COMMENT '通知类型：system-系统通知, announcement-公告, reminder-提醒',
    level       VARCHAR(20) DEFAULT 'info' NULL COMMENT '通知级别：info, warning, error, success',
    is_read     TINYINT     DEFAULT 0 NULL COMMENT '是否已读：0-未读，1-已读',
    link_url    VARCHAR(500) NULL COMMENT '关联链接',
    extra_data  JSON NULL COMMENT '扩展数据（JSON格式）',
    create_time datetime    DEFAULT NOW() NULL COMMENT '创建时间',
    read_time   datetime NULL COMMENT '阅读时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='用户通知表';

CREATE TABLE user_operation_log
(
    id               BIGINT      NOT NULL COMMENT '主键ID',
    user_id          BIGINT      NOT NULL COMMENT '用户ID',
    username         VARCHAR(50) NULL COMMENT '用户名',
    operation_type   VARCHAR(50) NOT NULL COMMENT '操作类型: UPDATE_PROFILE-更新资料, CHANGE_PASSWORD-修改密码, ENABLE_2FA-启用2FA, DISABLE_2FA-禁用2FA, etc',
    operation_module VARCHAR(50) NOT NULL COMMENT '操作模块: PROFILE-个人资料, SECURITY-安全设置, PREFERENCE-偏好设置',
    operation_desc   VARCHAR(500) NULL COMMENT '操作描述',
    operation_detail LONGTEXT NULL COMMENT '操作详情 (JSON格式)',
    request_method   VARCHAR(10) NULL COMMENT '请求方法: GET, POST, PUT, DELETE',
    request_url      VARCHAR(500) NULL COMMENT '请求URL',
    request_params   LONGTEXT NULL COMMENT '请求参数 (JSON格式)',
    ip_address       VARCHAR(50) NULL COMMENT 'IP地址',
    location         VARCHAR(255) NULL COMMENT '操作地点',
    browser          VARCHAR(100) NULL COMMENT '浏览器',
    os               VARCHAR(100) NULL COMMENT '操作系统',
    user_agent       VARCHAR(500) NULL COMMENT 'User-Agent',
    status           TINYINT  DEFAULT 1 NULL COMMENT '状态: 0-失败, 1-成功',
    error_msg        VARCHAR(500) NULL COMMENT '错误消息',
    execution_time   INT NULL COMMENT '执行时长(毫秒)',
    create_time      datetime DEFAULT NOW() NULL COMMENT '操作时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='用户操作日志表';

CREATE TABLE user_preference
(
    id                  BIGINT NOT NULL COMMENT '主键ID',
    user_id             BIGINT NOT NULL COMMENT '用户ID',
    theme               VARCHAR(20) DEFAULT 'light' NULL COMMENT '主题: light-浅色, dark-深色',
    primary_color       VARCHAR(20) DEFAULT '#1890ff' NULL COMMENT '主题色',
    layout              VARCHAR(20) DEFAULT 'side' NULL COMMENT '布局: side-侧边, top-顶部',
    menu_collapse       TINYINT     DEFAULT 0 NULL COMMENT '菜单收起状态: 0-展开, 1-收起',
    language            VARCHAR(10) DEFAULT 'zh-CN' NULL COMMENT '语言: zh-CN-简体中文, en-US-English',
    timezone            VARCHAR(50) DEFAULT 'Asia/Shanghai' NULL COMMENT '时区',
    date_format         VARCHAR(20) DEFAULT 'YYYY-MM-DD' NULL COMMENT '日期格式',
    time_format         VARCHAR(20) DEFAULT 'HH:mm:ss' NULL COMMENT '时间格式',
    email_notification  TINYINT     DEFAULT 1 NULL COMMENT '邮件通知: 0-关闭, 1-开启',
    sms_notification    TINYINT     DEFAULT 0 NULL COMMENT '短信通知: 0-关闭, 1-开启',
    system_notification TINYINT     DEFAULT 1 NULL COMMENT '系统通知: 0-关闭, 1-开启',
    page_size           INT         DEFAULT 20 NULL COMMENT '分页大小',
    dashboard_layout    JSON NULL COMMENT '仪表板布局配置（JSON格式）',
    auto_save           TINYINT     DEFAULT 1 NULL COMMENT '自动保存: 0-关闭, 1-开启',
    create_time         datetime    DEFAULT NOW() NULL COMMENT '创建时间',
    update_time         datetime    DEFAULT NOW() NULL COMMENT '更新时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='用户偏好设置表';

CREATE TABLE workflow_form_template
(
    id                     BIGINT       NOT NULL COMMENT '主键ID',
    name                   VARCHAR(100) NOT NULL COMMENT '模板名称',
    `description`          VARCHAR(500) NULL COMMENT '模板描述',
    form_key               VARCHAR(100) NOT NULL COMMENT '表单Key（唯一标识）',
    process_definition_key VARCHAR(100) NULL COMMENT '关联的流程定义Key',
    schema_json            LONGTEXT     NOT NULL COMMENT '表单Schema JSON（JSON Schema格式）',
    status                 TINYINT(1) DEFAULT 1     NULL COMMENT '状态：0-禁用，1-启用',
    version                INT      DEFAULT 1 NULL COMMENT '版本号',
    create_time            datetime DEFAULT NOW() NULL COMMENT '创建时间',
    update_time            datetime DEFAULT NOW() NULL COMMENT '更新时间',
    create_by              BIGINT NULL COMMENT '创建人ID',
    update_by              BIGINT NULL COMMENT '更新人ID',
    deleted                TINYINT(1) DEFAULT 0     NULL COMMENT '删除标记：0-未删除，1-已删除',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='工作流表单模板表';

ALTER TABLE act_ru_authorization
    ADD CONSTRAINT ACT_UNIQ_AUTH_GROUP UNIQUE (GROUP_ID_, TYPE_, RESOURCE_TYPE_, RESOURCE_ID_);

ALTER TABLE act_ru_authorization
    ADD CONSTRAINT ACT_UNIQ_AUTH_USER UNIQUE (USER_ID_, TYPE_, RESOURCE_TYPE_, RESOURCE_ID_);

ALTER TABLE act_id_tenant_member
    ADD CONSTRAINT ACT_UNIQ_TENANT_MEMB_GROUP UNIQUE (TENANT_ID_, GROUP_ID_);

ALTER TABLE act_id_tenant_member
    ADD CONSTRAINT ACT_UNIQ_TENANT_MEMB_USER UNIQUE (TENANT_ID_, USER_ID_);

ALTER TABLE act_ru_variable
    ADD CONSTRAINT ACT_UNIQ_VARIABLE UNIQUE (VAR_SCOPE_, NAME_);

ALTER TABLE act_hi_caseinst
    ADD CONSTRAINT CASE_INST_ID_ UNIQUE (CASE_INST_ID_);

ALTER TABLE act_hi_procinst
    ADD CONSTRAINT PROC_INST_ID_ UNIQUE (PROC_INST_ID_);

ALTER TABLE alert_history
    ADD CONSTRAINT alert_id UNIQUE (alert_id);

ALTER TABLE breakpoint_config
    ADD CONSTRAINT breakpoint_id UNIQUE (breakpoint_id);

ALTER TABLE file_metadata
    ADD CONSTRAINT file_id UNIQUE (file_id);

ALTER TABLE alert_rule_config
    ADD CONSTRAINT rule_name UNIQUE (rule_name);

ALTER TABLE profiling_session
    ADD CONSTRAINT session_id UNIQUE (session_id);

ALTER TABLE user_device
    ADD CONSTRAINT session_id UNIQUE (session_id);

ALTER TABLE file_share
    ADD CONSTRAINT share_code UNIQUE (share_code);

ALTER TABLE notification_template
    ADD CONSTRAINT template_code UNIQUE (template_code);

ALTER TABLE slow_trace_record
    ADD CONSTRAINT trace_id UNIQUE (trace_id);

ALTER TABLE sys_application
    ADD CONSTRAINT uk_app_code UNIQUE (app_code);

ALTER TABLE sys_backup_record
    ADD CONSTRAINT uk_backup_code UNIQUE (backup_code);

ALTER TABLE gen_template_group
    ADD CONSTRAINT uk_code UNIQUE (code, deleted);

ALTER TABLE sys_nacos_config
    ADD CONSTRAINT uk_config UNIQUE (data_id, group_name, namespace, environment, tenant_id, app_id, is_deleted);

ALTER TABLE gen_type_mapping
    ADD CONSTRAINT uk_db_column_type UNIQUE (db_type, column_type);

ALTER TABLE sys_dict
    ADD CONSTRAINT uk_dict_type UNIQUE (dict_type);

ALTER TABLE sys_file_info
    ADD CONSTRAINT uk_file_code UNIQUE (file_code);

ALTER TABLE file_tag_relation
    ADD CONSTRAINT uk_file_tag UNIQUE (file_id, tag_id);

ALTER TABLE file_version
    ADD CONSTRAINT uk_file_version UNIQUE (file_id, version_number);

ALTER TABLE workflow_form_template
    ADD CONSTRAINT uk_form_key UNIQUE (form_key, deleted);

ALTER TABLE exception_aggregation
    ADD CONSTRAINT uk_hash UNIQUE (stack_trace_hash);

ALTER TABLE sys_dead_letter
    ADD CONSTRAINT uk_message_id UNIQUE (message_id);

ALTER TABLE sys_message_log
    ADD CONSTRAINT uk_message_id UNIQUE (message_id);

ALTER TABLE sys_list_operation
    ADD CONSTRAINT uk_operation_code UNIQUE (operation_code);

ALTER TABLE sys_permission
    ADD CONSTRAINT uk_permission_key UNIQUE (permission_key);

ALTER TABLE sys_role
    ADD CONSTRAINT uk_role_key UNIQUE (role_key);

ALTER TABLE sys_role_menu
    ADD CONSTRAINT uk_role_menu UNIQUE (role_id, menu_id);

ALTER TABLE sys_role_list_operation
    ADD CONSTRAINT uk_role_operation_resource UNIQUE (role_id, operation_id, resource_type);

ALTER TABLE sys_role_permission
    ADD CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id);

ALTER TABLE sys_role_resource
    ADD CONSTRAINT uk_role_resource UNIQUE (role_id, resource_id);

ALTER TABLE sys_nacos_service
    ADD CONSTRAINT uk_service UNIQUE (service_name, group_name, namespace);

ALTER TABLE log_statistics
    ADD CONSTRAINT uk_service_level_time UNIQUE (service_name, log_level, time_bucket);

ALTER TABLE trace_service_stats
    ADD CONSTRAINT uk_service_op_time UNIQUE (service_name, operation_name, time_bucket);

ALTER TABLE service_dependency
    ADD CONSTRAINT uk_service_time UNIQUE (from_service, to_service, time_bucket);

ALTER TABLE file_tag
    ADD CONSTRAINT uk_tag_name UNIQUE (tag_name);

ALTER TABLE sys_user_role
    ADD CONSTRAINT uk_user_role UNIQUE (user_id, role_id);

ALTER TABLE sys_user
    ADD CONSTRAINT uk_username UNIQUE (username);

ALTER TABLE sys_event_subscription
    ADD CONSTRAINT uk_webhook_event UNIQUE (webhook_id, event_type);

ALTER TABLE user_2fa
    ADD CONSTRAINT user_id UNIQUE (user_id);

ALTER TABLE user_preference
    ADD CONSTRAINT user_id UNIQUE (user_id);

ALTER TABLE undo_log
    ADD CONSTRAINT ux_undo_log UNIQUE (xid, branch_id);

CREATE INDEX ACT_HI_BAT_RM_TIME ON act_hi_batch (REMOVAL_TIME_);

CREATE INDEX ACT_HI_EXT_TASK_LOG_PROCDEF ON act_hi_ext_task_log (PROC_DEF_ID_);

CREATE INDEX ACT_HI_EXT_TASK_LOG_PROCINST ON act_hi_ext_task_log (PROC_INST_ID_);

CREATE INDEX ACT_HI_EXT_TASK_LOG_PROC_DEF_KEY ON act_hi_ext_task_log (PROC_DEF_KEY_);

CREATE INDEX ACT_HI_EXT_TASK_LOG_RM_TIME ON act_hi_ext_task_log (REMOVAL_TIME_);

CREATE INDEX ACT_HI_EXT_TASK_LOG_ROOT_PI ON act_hi_ext_task_log (ROOT_PROC_INST_ID_);

CREATE INDEX ACT_HI_EXT_TASK_LOG_TENANT_ID ON act_hi_ext_task_log (TENANT_ID_);

CREATE INDEX ACT_IDX_AUTH_GROUP_ID ON act_ru_authorization (GROUP_ID_);

CREATE INDEX ACT_IDX_AUTH_RESOURCE_ID ON act_ru_authorization (RESOURCE_ID_);

CREATE INDEX ACT_IDX_AUTH_RM_TIME ON act_ru_authorization (REMOVAL_TIME_);

CREATE INDEX ACT_IDX_AUTH_ROOT_PI ON act_ru_authorization (ROOT_PROC_INST_ID_);

CREATE INDEX ACT_IDX_BYTEARRAY_NAME ON act_ge_bytearray (NAME_);

CREATE INDEX ACT_IDX_BYTEARRAY_RM_TIME ON act_ge_bytearray (REMOVAL_TIME_);

CREATE INDEX ACT_IDX_BYTEARRAY_ROOT_PI ON act_ge_bytearray (ROOT_PROC_INST_ID_);

CREATE INDEX ACT_IDX_CASE_DEF_TENANT_ID ON act_re_case_def (TENANT_ID_);

CREATE INDEX ACT_IDX_CASE_EXEC_BUSKEY ON act_ru_case_execution (BUSINESS_KEY_);

CREATE INDEX ACT_IDX_CASE_EXEC_TENANT_ID ON act_ru_case_execution (TENANT_ID_);

CREATE INDEX ACT_IDX_DEC_DEF_TENANT_ID ON act_re_decision_def (TENANT_ID_);

CREATE INDEX ACT_IDX_DEC_REQ_DEF_TENANT_ID ON act_re_decision_req_def (TENANT_ID_);

CREATE INDEX ACT_IDX_DEPLOYMENT_NAME ON act_re_deployment (NAME_);

CREATE INDEX ACT_IDX_DEPLOYMENT_TENANT_ID ON act_re_deployment (TENANT_ID_);

CREATE INDEX ACT_IDX_EVENT_SUBSCR_CONFIG_ ON act_ru_event_subscr (CONFIGURATION_);

CREATE INDEX ACT_IDX_EVENT_SUBSCR_EVT_NAME ON act_ru_event_subscr (EVENT_NAME_);

CREATE INDEX ACT_IDX_EVENT_SUBSCR_TENANT_ID ON act_ru_event_subscr (TENANT_ID_);

CREATE INDEX ACT_IDX_EXEC_BUSKEY ON act_ru_execution (BUSINESS_KEY_);

CREATE INDEX ACT_IDX_EXEC_ROOT_PI ON act_ru_execution (ROOT_PROC_INST_ID_);

CREATE INDEX ACT_IDX_EXEC_TENANT_ID ON act_ru_execution (TENANT_ID_);

CREATE INDEX ACT_IDX_EXT_TASK_PRIORITY ON act_ru_ext_task (PRIORITY_);

CREATE INDEX ACT_IDX_EXT_TASK_TENANT_ID ON act_ru_ext_task (TENANT_ID_);

CREATE INDEX ACT_IDX_EXT_TASK_TOPIC ON act_ru_ext_task (TOPIC_NAME_);

CREATE INDEX ACT_IDX_HI_ACTINST_ROOT_PI ON act_hi_actinst (ROOT_PROC_INST_ID_);

CREATE INDEX ACT_IDX_HI_ACT_INST_COMP ON act_hi_actinst (EXECUTION_ID_, ACT_ID_, END_TIME_, ID_);

CREATE INDEX ACT_IDX_HI_ACT_INST_END ON act_hi_actinst (END_TIME_);

CREATE INDEX ACT_IDX_HI_ACT_INST_PROCINST ON act_hi_actinst (PROC_INST_ID_, ACT_ID_);

CREATE INDEX ACT_IDX_HI_ACT_INST_PROC_DEF_KEY ON act_hi_actinst (PROC_DEF_KEY_);

CREATE INDEX ACT_IDX_HI_ACT_INST_RM_TIME ON act_hi_actinst (REMOVAL_TIME_);

CREATE INDEX ACT_IDX_HI_ACT_INST_START_END ON act_hi_actinst (START_TIME_, END_TIME_);

CREATE INDEX ACT_IDX_HI_ACT_INST_STATS ON act_hi_actinst (PROC_DEF_ID_, PROC_INST_ID_, ACT_ID_, END_TIME_, ACT_INST_STATE_);

CREATE INDEX ACT_IDX_HI_ACT_INST_TENANT_ID ON act_hi_actinst (TENANT_ID_);

CREATE INDEX ACT_IDX_HI_AI_PDEFID_END_TIME ON act_hi_actinst (PROC_DEF_ID_, END_TIME_);

CREATE INDEX ACT_IDX_HI_ATTACHMENT_CONTENT ON act_hi_attachment (CONTENT_ID_);

CREATE INDEX ACT_IDX_HI_ATTACHMENT_PROCINST ON act_hi_attachment (PROC_INST_ID_);

CREATE INDEX ACT_IDX_HI_ATTACHMENT_RM_TIME ON act_hi_attachment (REMOVAL_TIME_);

CREATE INDEX ACT_IDX_HI_ATTACHMENT_ROOT_PI ON act_hi_attachment (ROOT_PROC_INST_ID_);

CREATE INDEX ACT_IDX_HI_ATTACHMENT_TASK ON act_hi_attachment (TASK_ID_);

CREATE INDEX ACT_IDX_HI_CASEVAR_CASE_INST ON act_hi_varinst (CASE_INST_ID_);

CREATE INDEX ACT_IDX_HI_CAS_A_I_CASEINST ON act_hi_caseactinst (CASE_INST_ID_, CASE_ACT_ID_);

CREATE INDEX ACT_IDX_HI_CAS_A_I_COMP ON act_hi_caseactinst (CASE_ACT_ID_, END_TIME_, ID_);

CREATE INDEX ACT_IDX_HI_CAS_A_I_CREATE ON act_hi_caseactinst (CREATE_TIME_);

CREATE INDEX ACT_IDX_HI_CAS_A_I_END ON act_hi_caseactinst (END_TIME_);

CREATE INDEX ACT_IDX_HI_CAS_A_I_TENANT_ID ON act_hi_caseactinst (TENANT_ID_);

CREATE INDEX ACT_IDX_HI_CAS_I_BUSKEY ON act_hi_caseinst (BUSINESS_KEY_);

CREATE INDEX ACT_IDX_HI_CAS_I_CLOSE ON act_hi_caseinst (CLOSE_TIME_);

CREATE INDEX ACT_IDX_HI_CAS_I_TENANT_ID ON act_hi_caseinst (TENANT_ID_);

CREATE INDEX ACT_IDX_HI_COMMENT_PROCINST ON act_hi_comment (PROC_INST_ID_);

CREATE INDEX ACT_IDX_HI_COMMENT_RM_TIME ON act_hi_comment (REMOVAL_TIME_);

CREATE INDEX ACT_IDX_HI_COMMENT_ROOT_PI ON act_hi_comment (ROOT_PROC_INST_ID_);

CREATE INDEX ACT_IDX_HI_COMMENT_TASK ON act_hi_comment (TASK_ID_);

CREATE INDEX ACT_IDX_HI_DEC_INST_ACT ON act_hi_decinst (ACT_ID_);

CREATE INDEX ACT_IDX_HI_DEC_INST_ACT_INST ON act_hi_decinst (ACT_INST_ID_);

CREATE INDEX ACT_IDX_HI_DEC_INST_CI ON act_hi_decinst (CASE_INST_ID_);

CREATE INDEX ACT_IDX_HI_DEC_INST_ID ON act_hi_decinst (DEC_DEF_ID_);

CREATE INDEX ACT_IDX_HI_DEC_INST_KEY ON act_hi_decinst (DEC_DEF_KEY_);

CREATE INDEX ACT_IDX_HI_DEC_INST_PI ON act_hi_decinst (PROC_INST_ID_);

CREATE INDEX ACT_IDX_HI_DEC_INST_REQ_ID ON act_hi_decinst (DEC_REQ_ID_);

CREATE INDEX ACT_IDX_HI_DEC_INST_REQ_KEY ON act_hi_decinst (DEC_REQ_KEY_);

CREATE INDEX ACT_IDX_HI_DEC_INST_RM_TIME ON act_hi_decinst (REMOVAL_TIME_);

CREATE INDEX ACT_IDX_HI_DEC_INST_ROOT_ID ON act_hi_decinst (ROOT_DEC_INST_ID_);

CREATE INDEX ACT_IDX_HI_DEC_INST_ROOT_PI ON act_hi_decinst (ROOT_PROC_INST_ID_);

CREATE INDEX ACT_IDX_HI_DEC_INST_TENANT_ID ON act_hi_decinst (TENANT_ID_);

CREATE INDEX ACT_IDX_HI_DEC_INST_TIME ON act_hi_decinst (EVAL_TIME_);

CREATE INDEX ACT_IDX_HI_DEC_IN_CLAUSE ON act_hi_dec_in (DEC_INST_ID_, CLAUSE_ID_);

CREATE INDEX ACT_IDX_HI_DEC_IN_INST ON act_hi_dec_in (DEC_INST_ID_);

CREATE INDEX ACT_IDX_HI_DEC_IN_RM_TIME ON act_hi_dec_in (REMOVAL_TIME_);

CREATE INDEX ACT_IDX_HI_DEC_IN_ROOT_PI ON act_hi_dec_in (ROOT_PROC_INST_ID_);

CREATE INDEX ACT_IDX_HI_DEC_OUT_INST ON act_hi_dec_out (DEC_INST_ID_);

CREATE INDEX ACT_IDX_HI_DEC_OUT_RM_TIME ON act_hi_dec_out (REMOVAL_TIME_);

CREATE INDEX ACT_IDX_HI_DEC_OUT_ROOT_PI ON act_hi_dec_out (ROOT_PROC_INST_ID_);

CREATE INDEX ACT_IDX_HI_DEC_OUT_RULE ON act_hi_dec_out (RULE_ORDER_, CLAUSE_ID_);

CREATE INDEX ACT_IDX_HI_DETAIL_ACT_INST ON act_hi_detail (ACT_INST_ID_);

CREATE INDEX ACT_IDX_HI_DETAIL_BYTEAR ON act_hi_detail (BYTEARRAY_ID_);

CREATE INDEX ACT_IDX_HI_DETAIL_CASE_EXEC ON act_hi_detail (CASE_EXECUTION_ID_);

CREATE INDEX ACT_IDX_HI_DETAIL_CASE_INST ON act_hi_detail (CASE_INST_ID_);

CREATE INDEX ACT_IDX_HI_DETAIL_NAME ON act_hi_detail (NAME_);

CREATE INDEX ACT_IDX_HI_DETAIL_PROC_DEF_KEY ON act_hi_detail (PROC_DEF_KEY_);

CREATE INDEX ACT_IDX_HI_DETAIL_PROC_INST ON act_hi_detail (PROC_INST_ID_);

CREATE INDEX ACT_IDX_HI_DETAIL_RM_TIME ON act_hi_detail (REMOVAL_TIME_);

CREATE INDEX ACT_IDX_HI_DETAIL_ROOT_PI ON act_hi_detail (ROOT_PROC_INST_ID_);

CREATE INDEX ACT_IDX_HI_DETAIL_TASK_BYTEAR ON act_hi_detail (BYTEARRAY_ID_, TASK_ID_);

CREATE INDEX ACT_IDX_HI_DETAIL_TASK_ID ON act_hi_detail (TASK_ID_);

CREATE INDEX ACT_IDX_HI_DETAIL_TENANT_ID ON act_hi_detail (TENANT_ID_);

CREATE INDEX ACT_IDX_HI_DETAIL_TIME ON act_hi_detail (TIME_);

CREATE INDEX ACT_IDX_HI_DETAIL_VAR_INST_ID ON act_hi_detail (VAR_INST_ID_);

CREATE INDEX ACT_IDX_HI_EXTTASKLOG_ERRORDET ON act_hi_ext_task_log (ERROR_DETAILS_ID_);

CREATE INDEX ACT_IDX_HI_IDENT_LINK_RM_TIME ON act_hi_identitylink (REMOVAL_TIME_);

CREATE INDEX ACT_IDX_HI_IDENT_LINK_TASK ON act_hi_identitylink (TASK_ID_);

CREATE INDEX ACT_IDX_HI_IDENT_LNK_GROUP ON act_hi_identitylink (GROUP_ID_);

CREATE INDEX ACT_IDX_HI_IDENT_LNK_PROC_DEF_KEY ON act_hi_identitylink (PROC_DEF_KEY_);

CREATE INDEX ACT_IDX_HI_IDENT_LNK_ROOT_PI ON act_hi_identitylink (ROOT_PROC_INST_ID_);

CREATE INDEX ACT_IDX_HI_IDENT_LNK_TENANT_ID ON act_hi_identitylink (TENANT_ID_);

CREATE INDEX ACT_IDX_HI_IDENT_LNK_TIMESTAMP ON act_hi_identitylink (TIMESTAMP_);

CREATE INDEX ACT_IDX_HI_IDENT_LNK_USER ON act_hi_identitylink (USER_ID_);

CREATE INDEX ACT_IDX_HI_INCIDENT_CREATE_TIME ON act_hi_incident (CREATE_TIME_);

CREATE INDEX ACT_IDX_HI_INCIDENT_END_TIME ON act_hi_incident (END_TIME_);

CREATE INDEX ACT_IDX_HI_INCIDENT_PROCINST ON act_hi_incident (PROC_INST_ID_);

CREATE INDEX ACT_IDX_HI_INCIDENT_PROC_DEF_KEY ON act_hi_incident (PROC_DEF_KEY_);

CREATE INDEX ACT_IDX_HI_INCIDENT_RM_TIME ON act_hi_incident (REMOVAL_TIME_);

CREATE INDEX ACT_IDX_HI_INCIDENT_ROOT_PI ON act_hi_incident (ROOT_PROC_INST_ID_);

CREATE INDEX ACT_IDX_HI_INCIDENT_TENANT_ID ON act_hi_incident (TENANT_ID_);

CREATE INDEX ACT_IDX_HI_JOB_LOG_EX_STACK ON act_hi_job_log (JOB_EXCEPTION_STACK_ID_);

CREATE INDEX ACT_IDX_HI_JOB_LOG_JOB_CONF ON act_hi_job_log (JOB_DEF_CONFIGURATION_);

CREATE INDEX ACT_IDX_HI_JOB_LOG_JOB_DEF_ID ON act_hi_job_log (JOB_DEF_ID_);

CREATE INDEX ACT_IDX_HI_JOB_LOG_PROCDEF ON act_hi_job_log (PROCESS_DEF_ID_);

CREATE INDEX ACT_IDX_HI_JOB_LOG_PROCINST ON act_hi_job_log (PROCESS_INSTANCE_ID_);

CREATE INDEX ACT_IDX_HI_JOB_LOG_PROC_DEF_KEY ON act_hi_job_log (PROCESS_DEF_KEY_);

CREATE INDEX ACT_IDX_HI_JOB_LOG_RM_TIME ON act_hi_job_log (REMOVAL_TIME_);

CREATE INDEX ACT_IDX_HI_JOB_LOG_ROOT_PI ON act_hi_job_log (ROOT_PROC_INST_ID_);

CREATE INDEX ACT_IDX_HI_JOB_LOG_TENANT_ID ON act_hi_job_log (TENANT_ID_);

CREATE INDEX ACT_IDX_HI_OP_LOG_ENTITY_TYPE ON act_hi_op_log (ENTITY_TYPE_);

CREATE INDEX ACT_IDX_HI_OP_LOG_OP_TYPE ON act_hi_op_log (OPERATION_TYPE_);

CREATE INDEX ACT_IDX_HI_OP_LOG_PROCDEF ON act_hi_op_log (PROC_DEF_ID_);

CREATE INDEX ACT_IDX_HI_OP_LOG_PROCINST ON act_hi_op_log (PROC_INST_ID_);

CREATE INDEX ACT_IDX_HI_OP_LOG_RM_TIME ON act_hi_op_log (REMOVAL_TIME_);

CREATE INDEX ACT_IDX_HI_OP_LOG_ROOT_PI ON act_hi_op_log (ROOT_PROC_INST_ID_);

CREATE INDEX ACT_IDX_HI_OP_LOG_TASK ON act_hi_op_log (TASK_ID_);

CREATE INDEX ACT_IDX_HI_OP_LOG_TIMESTAMP ON act_hi_op_log (TIMESTAMP_);

CREATE INDEX ACT_IDX_HI_OP_LOG_USER_ID ON act_hi_op_log (USER_ID_);

CREATE INDEX ACT_IDX_HI_PI_PDEFID_END_TIME ON act_hi_procinst (PROC_DEF_ID_, END_TIME_);

CREATE INDEX ACT_IDX_HI_PROCVAR_NAME_TYPE ON act_hi_varinst (NAME_, VAR_TYPE_);

CREATE INDEX ACT_IDX_HI_PROCVAR_PROC_INST ON act_hi_varinst (PROC_INST_ID_);

CREATE INDEX ACT_IDX_HI_PRO_INST_END ON act_hi_procinst (END_TIME_);

CREATE INDEX ACT_IDX_HI_PRO_INST_PROC_DEF_KEY ON act_hi_procinst (PROC_DEF_KEY_);

CREATE INDEX ACT_IDX_HI_PRO_INST_PROC_TIME ON act_hi_procinst (START_TIME_, END_TIME_);

CREATE INDEX ACT_IDX_HI_PRO_INST_RM_TIME ON act_hi_procinst (REMOVAL_TIME_);

CREATE INDEX ACT_IDX_HI_PRO_INST_ROOT_PI ON act_hi_procinst (ROOT_PROC_INST_ID_);

CREATE INDEX ACT_IDX_HI_PRO_INST_TENANT_ID ON act_hi_procinst (TENANT_ID_);

CREATE INDEX ACT_IDX_HI_PRO_I_BUSKEY ON act_hi_procinst (BUSINESS_KEY_);

CREATE INDEX ACT_IDX_HI_TASKINSTID_PROCINST ON act_hi_taskinst (ID_, PROC_INST_ID_);

CREATE INDEX ACT_IDX_HI_TASKINST_PROCINST ON act_hi_taskinst (PROC_INST_ID_);

CREATE INDEX ACT_IDX_HI_TASKINST_ROOT_PI ON act_hi_taskinst (ROOT_PROC_INST_ID_);

CREATE INDEX ACT_IDX_HI_TASK_INST_END ON act_hi_taskinst (END_TIME_);

CREATE INDEX ACT_IDX_HI_TASK_INST_PROC_DEF_KEY ON act_hi_taskinst (PROC_DEF_KEY_);

CREATE INDEX ACT_IDX_HI_TASK_INST_RM_TIME ON act_hi_taskinst (REMOVAL_TIME_);

CREATE INDEX ACT_IDX_HI_TASK_INST_START ON act_hi_taskinst (START_TIME_);

CREATE INDEX ACT_IDX_HI_TASK_INST_TENANT_ID ON act_hi_taskinst (TENANT_ID_);

CREATE INDEX ACT_IDX_HI_VARINST_ACT_INST_ID ON act_hi_varinst (ACT_INST_ID_);

CREATE INDEX ACT_IDX_HI_VARINST_BYTEAR ON act_hi_varinst (BYTEARRAY_ID_);

CREATE INDEX ACT_IDX_HI_VARINST_NAME ON act_hi_varinst (NAME_);

CREATE INDEX ACT_IDX_HI_VARINST_RM_TIME ON act_hi_varinst (REMOVAL_TIME_);

CREATE INDEX ACT_IDX_HI_VARINST_ROOT_PI ON act_hi_varinst (ROOT_PROC_INST_ID_);

CREATE INDEX ACT_IDX_HI_VAR_INST_PROC_DEF_KEY ON act_hi_varinst (PROC_DEF_KEY_);

CREATE INDEX ACT_IDX_HI_VAR_INST_TENANT_ID ON act_hi_varinst (TENANT_ID_);

CREATE INDEX ACT_IDX_HI_VAR_PI_NAME_TYPE ON act_hi_varinst (PROC_INST_ID_, NAME_, VAR_TYPE_);

CREATE INDEX ACT_IDX_IDENT_LNK_GROUP ON act_ru_identitylink (GROUP_ID_);

CREATE INDEX ACT_IDX_IDENT_LNK_USER ON act_ru_identitylink (USER_ID_);

CREATE INDEX ACT_IDX_INC_CONFIGURATION ON act_ru_incident (CONFIGURATION_);

CREATE INDEX ACT_IDX_INC_TENANT_ID ON act_ru_incident (TENANT_ID_);

CREATE INDEX ACT_IDX_JOBDEF_PROC_DEF_ID ON act_ru_jobdef (PROC_DEF_ID_);

CREATE INDEX ACT_IDX_JOBDEF_TENANT_ID ON act_ru_jobdef (TENANT_ID_);

CREATE INDEX ACT_IDX_JOB_EXECUTION_ID ON act_ru_job (EXECUTION_ID_);

CREATE INDEX ACT_IDX_JOB_HANDLER ON act_ru_job (HANDLER_TYPE_, HANDLER_CFG_);

CREATE INDEX ACT_IDX_JOB_HANDLER_TYPE ON act_ru_job (HANDLER_TYPE_);

CREATE INDEX ACT_IDX_JOB_JOB_DEF_ID ON act_ru_job (JOB_DEF_ID_);

CREATE INDEX ACT_IDX_JOB_PROCINST ON act_ru_job (PROCESS_INSTANCE_ID_);

CREATE INDEX ACT_IDX_JOB_ROOT_PROCINST ON act_ru_job (ROOT_PROC_INST_ID_);

CREATE INDEX ACT_IDX_JOB_TENANT_ID ON act_ru_job (TENANT_ID_);

CREATE INDEX ACT_IDX_METER_LOG ON act_ru_meter_log (NAME_, TIMESTAMP_);

CREATE INDEX ACT_IDX_METER_LOG_MS ON act_ru_meter_log (MILLISECONDS_);

CREATE INDEX ACT_IDX_METER_LOG_NAME_MS ON act_ru_meter_log (NAME_, MILLISECONDS_);

CREATE INDEX ACT_IDX_METER_LOG_REPORT ON act_ru_meter_log (NAME_, REPORTER_, MILLISECONDS_);

CREATE INDEX ACT_IDX_METER_LOG_TIME ON act_ru_meter_log (TIMESTAMP_);

CREATE INDEX ACT_IDX_PROCDEF_DEPLOYMENT_ID ON act_re_procdef (DEPLOYMENT_ID_);

CREATE INDEX ACT_IDX_PROCDEF_TENANT_ID ON act_re_procdef (TENANT_ID_);

CREATE INDEX ACT_IDX_PROCDEF_VER_TAG ON act_re_procdef (VERSION_TAG_);

CREATE INDEX ACT_IDX_TASK_ASSIGNEE ON act_ru_task (ASSIGNEE_);

CREATE INDEX ACT_IDX_TASK_CREATE ON act_ru_task (CREATE_TIME_);

CREATE INDEX ACT_IDX_TASK_LAST_UPDATED ON act_ru_task (LAST_UPDATED_);

CREATE INDEX ACT_IDX_TASK_METER_LOG_TIME ON act_ru_task_meter_log (TIMESTAMP_);

CREATE INDEX ACT_IDX_TASK_OWNER ON act_ru_task (OWNER_);

CREATE INDEX ACT_IDX_TASK_TENANT_ID ON act_ru_task (TENANT_ID_);

CREATE INDEX ACT_IDX_VARIABLE_TASK_ID ON act_ru_variable (TASK_ID_);

CREATE INDEX ACT_IDX_VARIABLE_TASK_NAME_TYPE ON act_ru_variable (TASK_ID_, NAME_, TYPE_);

CREATE INDEX ACT_IDX_VARIABLE_TENANT_ID ON act_ru_variable (TENANT_ID_);

CREATE INDEX flyway_schema_history_s_idx ON flyway_schema_history (success);

CREATE INDEX idx_alert_type ON alert_history (alert_type);

CREATE INDEX idx_app ON sys_nacos_config (app_id);

CREATE INDEX idx_app_id ON sys_workflow_definition (app_id);

CREATE INDEX idx_app_id ON sys_workflow_definition (app_id);

CREATE INDEX idx_app_id ON sys_workflow_definition (app_id);

CREATE INDEX idx_app_id ON sys_workflow_definition (app_id);

CREATE INDEX idx_app_id ON sys_workflow_definition (app_id);

CREATE INDEX idx_app_id ON sys_workflow_definition (app_id);

CREATE INDEX idx_app_id ON sys_workflow_definition (app_id);

CREATE INDEX idx_backup_type ON sys_backup_record (backup_type);

CREATE INDEX idx_branch_id ON undo_log (branch_id);

CREATE INDEX idx_call_time ON sys_webhook_log (call_time);

CREATE INDEX idx_class ON hot_deploy_history (class_name);

CREATE INDEX idx_class_method ON breakpoint_config (class_name, method_name);

CREATE INDEX idx_code ON gen_template (code);

CREATE INDEX idx_config_id ON sys_nacos_publish_task (config_id);

CREATE INDEX idx_config_id ON sys_nacos_publish_task (config_id);

CREATE INDEX idx_config_id ON sys_nacos_publish_task (config_id);

CREATE INDEX idx_create_time ON workflow_form_template (create_time);

CREATE INDEX idx_create_time ON workflow_form_template (create_time);

CREATE INDEX idx_create_time ON workflow_form_template (create_time);

CREATE INDEX idx_create_time ON workflow_form_template (create_time);

CREATE INDEX idx_create_time ON workflow_form_template (create_time);

CREATE INDEX idx_create_time ON workflow_form_template (create_time);

CREATE INDEX idx_create_time ON workflow_form_template (create_time);

CREATE INDEX idx_create_time ON workflow_form_template (create_time);

CREATE INDEX idx_create_time ON workflow_form_template (create_time);

CREATE INDEX idx_create_time ON workflow_form_template (create_time);

CREATE INDEX idx_create_time ON workflow_form_template (create_time);

CREATE INDEX idx_create_time ON workflow_form_template (create_time);

CREATE INDEX idx_create_time ON workflow_form_template (create_time);

CREATE INDEX idx_create_time ON workflow_form_template (create_time);

CREATE INDEX idx_create_time ON workflow_form_template (create_time);

CREATE INDEX idx_create_time ON workflow_form_template (create_time);

CREATE INDEX idx_create_time ON workflow_form_template (create_time);

CREATE INDEX idx_create_time ON workflow_form_template (create_time);

CREATE INDEX idx_create_time ON workflow_form_template (create_time);

CREATE INDEX idx_create_time ON workflow_form_template (create_time);

CREATE INDEX idx_create_time ON workflow_form_template (create_time);

CREATE INDEX idx_create_time ON workflow_form_template (create_time);

CREATE INDEX idx_create_time ON workflow_form_template (create_time);

CREATE INDEX idx_create_time ON workflow_form_template (create_time);

CREATE INDEX idx_create_time ON workflow_form_template (create_time);

CREATE INDEX idx_data_id ON sys_nacos_config_history (data_id);

CREATE INDEX idx_data_id ON sys_nacos_config_history (data_id);

CREATE INDEX idx_datasource ON gen_history (datasource_id);

CREATE INDEX idx_deleted ON sys_file_info (deleted);

CREATE INDEX idx_deleted ON sys_file_info (deleted);

CREATE INDEX idx_deleted ON sys_file_info (deleted);

CREATE INDEX idx_deleted ON sys_file_info (deleted);

CREATE INDEX idx_deleted_at ON file_recycle_bin (deleted_at);

CREATE INDEX idx_deleted_by ON file_recycle_bin (deleted_by);

CREATE INDEX idx_deploy_time ON hot_deploy_history (deploy_time);

CREATE INDEX idx_dept_id ON sys_user (dept_id);

CREATE INDEX idx_dept_id ON sys_user (dept_id);

CREATE INDEX idx_dict_type ON sys_dict_data (dict_type);

CREATE INDEX idx_duration ON trace_span_ext (duration);

CREATE INDEX idx_duration ON trace_span_ext (duration);

CREATE INDEX idx_duration ON trace_span_ext (duration);

CREATE INDEX idx_email ON sys_user (email);

CREATE INDEX idx_enabled ON user_2fa (enabled);

CREATE INDEX idx_enabled ON user_2fa (enabled);

CREATE INDEX idx_enabled ON user_2fa (enabled);

CREATE INDEX idx_enabled ON user_2fa (enabled);

CREATE INDEX idx_environment ON sys_nacos_config (environment);

CREATE INDEX idx_event_id ON sys_webhook_log (event_id);

CREATE INDEX idx_event_type ON sys_webhook_log (event_type);

CREATE INDEX idx_event_type ON sys_webhook_log (event_type);

CREATE INDEX idx_exception_class ON exception_aggregation (exception_class);

CREATE INDEX idx_expire_at ON file_recycle_bin (expire_at);

CREATE INDEX idx_file_category ON sys_file_info (file_category);

CREATE INDEX idx_file_id ON file_version (file_id);

CREATE INDEX idx_file_id ON file_version (file_id);

CREATE INDEX idx_file_id ON file_version (file_id);

CREATE INDEX idx_file_id ON file_version (file_id);

CREATE INDEX idx_file_id ON file_version (file_id);

CREATE INDEX idx_file_id ON file_version (file_id);

CREATE INDEX idx_fired_at ON alert_history (fired_at);

CREATE INDEX idx_folder_id ON file_metadata (folder_id);

CREATE INDEX idx_from_service ON service_dependency (from_service);

CREATE INDEX idx_group_id ON gen_template (group_id);

CREATE INDEX idx_group_namespace ON sys_nacos_service (group_name, namespace);

CREATE INDEX idx_group_namespace ON sys_nacos_service (group_name, namespace);

CREATE INDEX idx_history_id ON gen_history_detail (history_id);

CREATE INDEX idx_instance ON profiling_session (instance_id);

CREATE INDEX idx_instance_time ON jvm_metrics (instance_id, timestamp);

CREATE INDEX idx_is_read ON user_notification (is_read);

CREATE INDEX idx_job_id ON sys_job_instance (job_id);

CREATE INDEX idx_job_id ON sys_job_instance (job_id);

CREATE INDEX idx_job_name ON sys_job_info (job_name);

CREATE INDEX idx_job_type ON sys_job_info (job_type);

CREATE INDEX idx_last_seen ON exception_aggregation (last_seen);

CREATE INDEX idx_log_created ON undo_log (log_created);

CREATE INDEX idx_login_time ON user_device (login_time);

CREATE INDEX idx_login_time ON user_device (login_time);

CREATE INDEX idx_menu_id ON sys_role_menu (menu_id);

CREATE INDEX idx_method ON slow_sql_record (method_name);

CREATE INDEX idx_name ON gen_project (name);

CREATE INDEX idx_name ON gen_project (name);

CREATE INDEX idx_operation_id ON sys_role_list_operation (operation_id);

CREATE INDEX idx_operation_module ON user_operation_log (operation_module);

CREATE INDEX idx_operation_time ON sys_operation_log (operation_time);

CREATE INDEX idx_operation_time ON sys_operation_log (operation_time);

CREATE INDEX idx_operation_type ON user_operation_log (operation_type);

CREATE INDEX idx_operation_type ON user_operation_log (operation_type);

CREATE INDEX idx_operator_id ON file_operation_log (operator_id);

CREATE INDEX idx_owner_id ON file_metadata (owner_id);

CREATE INDEX idx_parent_id ON sys_role (parent_id);

CREATE INDEX idx_parent_id ON sys_role (parent_id);

CREATE INDEX idx_parent_id ON sys_role (parent_id);

CREATE INDEX idx_parent_id ON sys_role (parent_id);

CREATE INDEX idx_permission_id ON sys_role_permission (permission_id);

CREATE INDEX idx_phone ON sys_user (phone);

CREATE INDEX idx_process_def_key ON workflow_form_template (process_definition_key);

CREATE INDEX idx_processed ON sys_job_dead_letter (processed);

CREATE INDEX idx_project ON gen_history (project_id);

CREATE INDEX idx_queue_name ON sys_message_queue_monitor (queue_name);

CREATE INDEX idx_resource_id ON sys_role_resource (resource_id);

CREATE INDEX idx_resource_type ON sys_role_list_operation (resource_type);

CREATE INDEX idx_resource_type ON sys_role_list_operation (resource_type);

CREATE INDEX idx_resource_type ON sys_role_list_operation (resource_type);

CREATE INDEX idx_role_id ON sys_user_role (role_id);

CREATE INDEX idx_role_id ON sys_user_role (role_id);

CREATE INDEX idx_role_id ON sys_user_role (role_id);

CREATE INDEX idx_role_id ON sys_user_role (role_id);

CREATE INDEX idx_role_id ON sys_user_role (role_id);

CREATE INDEX idx_role_id ON sys_user_role (role_id);

CREATE INDEX idx_role_id ON sys_user_role (role_id);

CREATE INDEX idx_rule_id ON alert_history (rule_id);

CREATE INDEX idx_rule_type ON alert_rule_config (rule_type);

CREATE INDEX idx_service ON trace_span_ext (service_name);

CREATE INDEX idx_service ON trace_span_ext (service_name);

CREATE INDEX idx_service ON trace_span_ext (service_name);

CREATE INDEX idx_service_name ON sys_nacos_service (service_name);

CREATE INDEX idx_service_time ON trace_service_stats (service_name, time_bucket);

CREATE INDEX idx_service_time ON trace_service_stats (service_name, time_bucket);

CREATE INDEX idx_share_by ON file_share (share_by);

CREATE INDEX idx_start_time ON trace_span_ext (start_time);

CREATE INDEX idx_start_time ON trace_span_ext (start_time);

CREATE INDEX idx_status ON workflow_form_template (status);

CREATE INDEX idx_status ON workflow_form_template (status);

CREATE INDEX idx_status ON workflow_form_template (status);

CREATE INDEX idx_status ON workflow_form_template (status);

CREATE INDEX idx_status ON workflow_form_template (status);

CREATE INDEX idx_status ON workflow_form_template (status);

CREATE INDEX idx_status ON workflow_form_template (status);

CREATE INDEX idx_status ON workflow_form_template (status);

CREATE INDEX idx_status ON workflow_form_template (status);

CREATE INDEX idx_status ON workflow_form_template (status);

CREATE INDEX idx_status ON workflow_form_template (status);

CREATE INDEX idx_status ON workflow_form_template (status);

CREATE INDEX idx_status ON workflow_form_template (status);

CREATE INDEX idx_status ON workflow_form_template (status);

CREATE INDEX idx_status ON workflow_form_template (status);

CREATE INDEX idx_status ON workflow_form_template (status);

CREATE INDEX idx_status ON workflow_form_template (status);

CREATE INDEX idx_status ON workflow_form_template (status);

CREATE INDEX idx_success ON sys_webhook_log (success);

CREATE INDEX idx_tag_id ON file_tag_relation (tag_id);

CREATE INDEX idx_tenant ON sys_nacos_config (tenant_id);

CREATE INDEX idx_tenant_id ON sys_workflow_definition (tenant_id);

CREATE INDEX idx_tenant_id ON sys_workflow_definition (tenant_id);

CREATE INDEX idx_time_bucket ON log_statistics (time_bucket);

CREATE INDEX idx_timestamp ON slow_sql_record (timestamp);

CREATE INDEX idx_timestamp ON slow_sql_record (timestamp);

CREATE INDEX idx_to_service ON service_dependency (to_service);

CREATE INDEX idx_topic ON sys_message_log (topic);

CREATE INDEX idx_topic ON sys_message_log (topic);

CREATE INDEX idx_trace_id ON trace_span_ext (trace_id);

CREATE INDEX idx_upload_user_id ON sys_file_info (upload_user_id);

CREATE INDEX idx_user_id ON user_operation_log (user_id);

CREATE INDEX idx_user_id ON user_operation_log (user_id);

CREATE INDEX idx_user_id ON user_operation_log (user_id);

CREATE INDEX idx_user_id ON user_operation_log (user_id);

CREATE INDEX idx_user_id ON user_operation_log (user_id);

CREATE INDEX idx_user_id ON user_operation_log (user_id);

CREATE INDEX idx_user_id ON user_operation_log (user_id);

CREATE INDEX idx_username ON sys_operation_log (username);

CREATE INDEX idx_username ON sys_operation_log (username);

CREATE INDEX idx_version ON sys_nacos_config_history (version);

CREATE INDEX idx_webhook_id ON sys_webhook_log (webhook_id);

CREATE INDEX idx_workflow_id ON sys_workflow_instance (workflow_id);

CREATE INDEX idx_workflow_name ON sys_workflow_definition (workflow_name);

CREATE INDEX idx_xid ON undo_log (xid);

ALTER TABLE act_ru_identitylink
    ADD CONSTRAINT ACT_FK_ATHRZ_PROCEDEF FOREIGN KEY (PROC_DEF_ID_) REFERENCES act_re_procdef (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_IDX_ATHRZ_PROCEDEF ON act_ru_identitylink (PROC_DEF_ID_);

ALTER TABLE act_ru_batch
    ADD CONSTRAINT ACT_FK_BATCH_JOB_DEF FOREIGN KEY (BATCH_JOB_DEF_ID_) REFERENCES act_ru_jobdef (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_IDX_BATCH_JOB_DEF ON act_ru_batch (BATCH_JOB_DEF_ID_);

ALTER TABLE act_ru_batch
    ADD CONSTRAINT ACT_FK_BATCH_MONITOR_JOB_DEF FOREIGN KEY (MONITOR_JOB_DEF_ID_) REFERENCES act_ru_jobdef (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_IDX_BATCH_MONITOR_JOB_DEF ON act_ru_batch (MONITOR_JOB_DEF_ID_);

ALTER TABLE act_ru_batch
    ADD CONSTRAINT ACT_FK_BATCH_SEED_JOB_DEF FOREIGN KEY (SEED_JOB_DEF_ID_) REFERENCES act_ru_jobdef (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_IDX_BATCH_SEED_JOB_DEF ON act_ru_batch (SEED_JOB_DEF_ID_);

ALTER TABLE act_ge_bytearray
    ADD CONSTRAINT ACT_FK_BYTEARR_DEPL FOREIGN KEY (DEPLOYMENT_ID_) REFERENCES act_re_deployment (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_FK_BYTEARR_DEPL ON act_ge_bytearray (DEPLOYMENT_ID_);

ALTER TABLE act_ru_case_execution
    ADD CONSTRAINT ACT_FK_CASE_EXE_CASE_DEF FOREIGN KEY (CASE_DEF_ID_) REFERENCES act_re_case_def (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_FK_CASE_EXE_CASE_DEF ON act_ru_case_execution (CASE_DEF_ID_);

ALTER TABLE act_ru_case_execution
    ADD CONSTRAINT ACT_FK_CASE_EXE_CASE_INST FOREIGN KEY (CASE_INST_ID_) REFERENCES act_ru_case_execution (ID_) ON DELETE CASCADE;

CREATE INDEX ACT_IDX_CASE_EXE_CASE_INST ON act_ru_case_execution (CASE_INST_ID_);

ALTER TABLE act_ru_case_execution
    ADD CONSTRAINT ACT_FK_CASE_EXE_PARENT FOREIGN KEY (PARENT_ID_) REFERENCES act_ru_case_execution (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_FK_CASE_EXE_PARENT ON act_ru_case_execution (PARENT_ID_);

ALTER TABLE act_ru_case_sentry_part
    ADD CONSTRAINT ACT_FK_CASE_SENTRY_CASE_EXEC FOREIGN KEY (CASE_EXEC_ID_) REFERENCES act_ru_case_execution (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_FK_CASE_SENTRY_CASE_EXEC ON act_ru_case_sentry_part (CASE_EXEC_ID_);

ALTER TABLE act_ru_case_sentry_part
    ADD CONSTRAINT ACT_FK_CASE_SENTRY_CASE_INST FOREIGN KEY (CASE_INST_ID_) REFERENCES act_ru_case_execution (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_FK_CASE_SENTRY_CASE_INST ON act_ru_case_sentry_part (CASE_INST_ID_);

ALTER TABLE act_re_decision_def
    ADD CONSTRAINT ACT_FK_DEC_REQ FOREIGN KEY (DEC_REQ_ID_) REFERENCES act_re_decision_req_def (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_IDX_DEC_DEF_REQ_ID ON act_re_decision_def (DEC_REQ_ID_);

ALTER TABLE act_ru_event_subscr
    ADD CONSTRAINT ACT_FK_EVENT_EXEC FOREIGN KEY (EXECUTION_ID_) REFERENCES act_ru_execution (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_FK_EVENT_EXEC ON act_ru_event_subscr (EXECUTION_ID_);

ALTER TABLE act_ru_execution
    ADD CONSTRAINT ACT_FK_EXE_PARENT FOREIGN KEY (PARENT_ID_) REFERENCES act_ru_execution (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_FK_EXE_PARENT ON act_ru_execution (PARENT_ID_);

ALTER TABLE act_ru_execution
    ADD CONSTRAINT ACT_FK_EXE_PROCDEF FOREIGN KEY (PROC_DEF_ID_) REFERENCES act_re_procdef (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_FK_EXE_PROCDEF ON act_ru_execution (PROC_DEF_ID_);

ALTER TABLE act_ru_execution
    ADD CONSTRAINT ACT_FK_EXE_PROCINST FOREIGN KEY (PROC_INST_ID_) REFERENCES act_ru_execution (ID_) ON DELETE CASCADE;

CREATE INDEX ACT_FK_EXE_PROCINST ON act_ru_execution (PROC_INST_ID_);

ALTER TABLE act_ru_execution
    ADD CONSTRAINT ACT_FK_EXE_SUPER FOREIGN KEY (SUPER_EXEC_) REFERENCES act_ru_execution (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_FK_EXE_SUPER ON act_ru_execution (SUPER_EXEC_);

ALTER TABLE act_ru_ext_task
    ADD CONSTRAINT ACT_FK_EXT_TASK_ERROR_DETAILS FOREIGN KEY (ERROR_DETAILS_ID_) REFERENCES act_ge_bytearray (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_IDX_EXT_TASK_ERR_DETAILS ON act_ru_ext_task (ERROR_DETAILS_ID_);

ALTER TABLE act_ru_ext_task
    ADD CONSTRAINT ACT_FK_EXT_TASK_EXE FOREIGN KEY (EXECUTION_ID_) REFERENCES act_ru_execution (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_IDX_EXT_TASK_EXEC ON act_ru_ext_task (EXECUTION_ID_);

ALTER TABLE act_ru_incident
    ADD CONSTRAINT ACT_FK_INC_CAUSE FOREIGN KEY (CAUSE_INCIDENT_ID_) REFERENCES act_ru_incident (ID_) ON DELETE CASCADE;

CREATE INDEX ACT_IDX_INC_CAUSEINCID ON act_ru_incident (CAUSE_INCIDENT_ID_);

ALTER TABLE act_ru_incident
    ADD CONSTRAINT ACT_FK_INC_EXE FOREIGN KEY (EXECUTION_ID_) REFERENCES act_ru_execution (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_IDX_INC_EXID ON act_ru_incident (EXECUTION_ID_);

ALTER TABLE act_ru_incident
    ADD CONSTRAINT ACT_FK_INC_JOB_DEF FOREIGN KEY (JOB_DEF_ID_) REFERENCES act_ru_jobdef (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_IDX_INC_JOB_DEF ON act_ru_incident (JOB_DEF_ID_);

ALTER TABLE act_ru_incident
    ADD CONSTRAINT ACT_FK_INC_PROCDEF FOREIGN KEY (PROC_DEF_ID_) REFERENCES act_re_procdef (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_IDX_INC_PROCDEFID ON act_ru_incident (PROC_DEF_ID_);

ALTER TABLE act_ru_incident
    ADD CONSTRAINT ACT_FK_INC_PROCINST FOREIGN KEY (PROC_INST_ID_) REFERENCES act_ru_execution (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_IDX_INC_PROCINSTID ON act_ru_incident (PROC_INST_ID_);

ALTER TABLE act_ru_incident
    ADD CONSTRAINT ACT_FK_INC_RCAUSE FOREIGN KEY (ROOT_CAUSE_INCIDENT_ID_) REFERENCES act_ru_incident (ID_) ON DELETE CASCADE;

CREATE INDEX ACT_IDX_INC_ROOTCAUSEINCID ON act_ru_incident (ROOT_CAUSE_INCIDENT_ID_);

ALTER TABLE act_ru_job
    ADD CONSTRAINT ACT_FK_JOB_EXCEPTION FOREIGN KEY (EXCEPTION_STACK_ID_) REFERENCES act_ge_bytearray (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_FK_JOB_EXCEPTION ON act_ru_job (EXCEPTION_STACK_ID_);

ALTER TABLE act_id_membership
    ADD CONSTRAINT ACT_FK_MEMB_GROUP FOREIGN KEY (GROUP_ID_) REFERENCES act_id_group (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_FK_MEMB_GROUP ON act_id_membership (GROUP_ID_);

ALTER TABLE act_id_membership
    ADD CONSTRAINT ACT_FK_MEMB_USER FOREIGN KEY (USER_ID_) REFERENCES act_id_user (ID_) ON DELETE NO ACTION;

ALTER TABLE act_ru_task
    ADD CONSTRAINT ACT_FK_TASK_CASE_DEF FOREIGN KEY (CASE_DEF_ID_) REFERENCES act_re_case_def (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_FK_TASK_CASE_DEF ON act_ru_task (CASE_DEF_ID_);

ALTER TABLE act_ru_task
    ADD CONSTRAINT ACT_FK_TASK_CASE_EXE FOREIGN KEY (CASE_EXECUTION_ID_) REFERENCES act_ru_case_execution (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_FK_TASK_CASE_EXE ON act_ru_task (CASE_EXECUTION_ID_);

ALTER TABLE act_ru_task
    ADD CONSTRAINT ACT_FK_TASK_EXE FOREIGN KEY (EXECUTION_ID_) REFERENCES act_ru_execution (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_FK_TASK_EXE ON act_ru_task (EXECUTION_ID_);

ALTER TABLE act_ru_task
    ADD CONSTRAINT ACT_FK_TASK_PROCDEF FOREIGN KEY (PROC_DEF_ID_) REFERENCES act_re_procdef (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_FK_TASK_PROCDEF ON act_ru_task (PROC_DEF_ID_);

ALTER TABLE act_ru_task
    ADD CONSTRAINT ACT_FK_TASK_PROCINST FOREIGN KEY (PROC_INST_ID_) REFERENCES act_ru_execution (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_FK_TASK_PROCINST ON act_ru_task (PROC_INST_ID_);

ALTER TABLE act_id_tenant_member
    ADD CONSTRAINT ACT_FK_TENANT_MEMB FOREIGN KEY (TENANT_ID_) REFERENCES act_id_tenant (ID_) ON DELETE NO ACTION;

ALTER TABLE act_id_tenant_member
    ADD CONSTRAINT ACT_FK_TENANT_MEMB_GROUP FOREIGN KEY (GROUP_ID_) REFERENCES act_id_group (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_FK_TENANT_MEMB_GROUP ON act_id_tenant_member (GROUP_ID_);

ALTER TABLE act_id_tenant_member
    ADD CONSTRAINT ACT_FK_TENANT_MEMB_USER FOREIGN KEY (USER_ID_) REFERENCES act_id_user (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_FK_TENANT_MEMB_USER ON act_id_tenant_member (USER_ID_);

ALTER TABLE act_ru_identitylink
    ADD CONSTRAINT ACT_FK_TSKASS_TASK FOREIGN KEY (TASK_ID_) REFERENCES act_ru_task (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_FK_TSKASS_TASK ON act_ru_identitylink (TASK_ID_);

ALTER TABLE act_ru_variable
    ADD CONSTRAINT ACT_FK_VAR_BATCH FOREIGN KEY (BATCH_ID_) REFERENCES act_ru_batch (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_IDX_BATCH_ID ON act_ru_variable (BATCH_ID_);

ALTER TABLE act_ru_variable
    ADD CONSTRAINT ACT_FK_VAR_BYTEARRAY FOREIGN KEY (BYTEARRAY_ID_) REFERENCES act_ge_bytearray (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_FK_VAR_BYTEARRAY ON act_ru_variable (BYTEARRAY_ID_);

ALTER TABLE act_ru_variable
    ADD CONSTRAINT ACT_FK_VAR_CASE_EXE FOREIGN KEY (CASE_EXECUTION_ID_) REFERENCES act_ru_case_execution (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_FK_VAR_CASE_EXE ON act_ru_variable (CASE_EXECUTION_ID_);

ALTER TABLE act_ru_variable
    ADD CONSTRAINT ACT_FK_VAR_CASE_INST FOREIGN KEY (CASE_INST_ID_) REFERENCES act_ru_case_execution (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_FK_VAR_CASE_INST ON act_ru_variable (CASE_INST_ID_);

ALTER TABLE act_ru_variable
    ADD CONSTRAINT ACT_FK_VAR_EXE FOREIGN KEY (EXECUTION_ID_) REFERENCES act_ru_execution (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_FK_VAR_EXE ON act_ru_variable (EXECUTION_ID_);

ALTER TABLE act_ru_variable
    ADD CONSTRAINT ACT_FK_VAR_PROCINST FOREIGN KEY (PROC_INST_ID_) REFERENCES act_ru_execution (ID_) ON DELETE NO ACTION;

CREATE INDEX ACT_FK_VAR_PROCINST ON act_ru_variable (PROC_INST_ID_);

DROP TABLE columns_priv;

DROP TABLE `component`;

DROP TABLE db;

DROP TABLE default_roles;

DROP TABLE engine_cost;

DROP TABLE func;

DROP TABLE general_log;

DROP TABLE global_grants;

DROP TABLE gtid_executed;

DROP TABLE help_category;

DROP TABLE help_keyword;

DROP TABLE help_relation;

DROP TABLE help_topic;

DROP TABLE innodb_index_stats;

DROP TABLE innodb_table_stats;

DROP TABLE ndb_binlog_index;

DROP TABLE password_history;

DROP TABLE plugin;

DROP TABLE procs_priv;

DROP TABLE proxies_priv;

DROP TABLE replication_asynchronous_connection_failover;

DROP TABLE replication_asynchronous_connection_failover_managed;

DROP TABLE replication_group_configuration_version;

DROP TABLE replication_group_member_actions;

DROP TABLE role_edges;

DROP TABLE server_cost;

DROP TABLE servers;

DROP TABLE slave_master_info;

DROP TABLE slave_relay_log_info;

DROP TABLE slave_worker_info;

DROP TABLE slow_log;

DROP TABLE tables_priv;

DROP TABLE time_zone;

DROP TABLE time_zone_leap_second;

DROP TABLE time_zone_name;

DROP TABLE time_zone_transition;

DROP TABLE time_zone_transition_type;

DROP TABLE user;