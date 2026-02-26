package org.example.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.api.HttpRequestHandler;
import org.example.api.JsonApiHandler;
import org.example.model.AlternativePartner;
import org.example.model.BinaryParameter;
import org.example.model.StringParameter;
import org.example.ui.MainFrame;
import org.example.ui.pages.AlternativePartnersPage;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SharedData {
    public static AlternativePartnersPage alternativePartnersPage;

    public static final String UI_TITLE = "Partner Directory Accelerator UI (version 1.0.1 dev)"; // todo: change version for new release
    public static final int UI_PADDING = 5;
    public static final int UI_TEXT_FIELD_COLUMNS = 30;
    public static final int UI_SEARCH_FIELD_WIDTH = 15;
    public static final int UI_DIALOG_WIDTH = 800;
    public static final int UI_DIALOG_HEIGHT = 500;

    public static final String STRING_PARAMETER_PID_SAP_INTEGRATION_SUITE_LANDSCAPE = "SAP_Integration_Suite_Landscape";

    public static final String LABEL_ADD_ALTERNATIVE_PARTNER = "Add new Alternative Partner";
    public static final String LABEL_ADD_ENTRY = "Add new entry";
    public static final String LABEL_ADD_NEW_TENANT = "Add new tenant";
    public static final String LABEL_ADD_ROW = "Add row";
    public static final String LABEL_ALTERNATIVE = "Alternative";
    public static final String LABEL_AGENCY = "Sender System";
    public static final String LABEL_BACK = "Back to Alternative Partners";
    public static final String LABEL_BACK_TO_ALL = "Back to all Alternative Partners";
    public static final String LABEL_BACKUP_CREATED = "Backup was created and ";
    public static final String LABEL_BACKUP_QUESTION = "Save files locally as backup?";
    public static final String LABEL_BUTTON_DESELECT_ALL = "Deselect all shown entries";
    public static final String LABEL_BUTTON_SELECT_ALL = "Select all shown entries";
    public static final String LABEL_CANCEL = "Cancel";
    public static final String LABEL_CLIENT_ID = "Client ID";
    public static final String LABEL_CLIENT_SECRET = "Client Secret";
    public static final String LABEL_COMBINED_XSLT = "Combined XSLT";
    public static final String LABEL_CONFIRM_MIGRATION_1 = "Confirm migration of ";
    public static final String LABEL_CONFIRM_MIGRATION_2 = " scenario(s) using old partner ID with tilde to new partner ID.";
    public static final String LABEL_CONFIRMATION = "Confirmation";
    public static final String LABEL_CONDITION = "XPath Condition";
    public static final String LABEL_CONTAINS_SYNTAX_ERRORS = " contains syntax errors.";
    public static final String LABEL_CRITICAL = "Critical";
    public static final String LABEL_DEFAULT = "Default";
    public static final String LABEL_DELETE = "Delete";
    public static final String LABEL_DELETE_ENTRY = "Delete this entry";
    public static final String LABEL_DELETE_OLD_ENTRIES = "Delete entries with old Pid";
    public static final String LABEL_DELETE_ROW = "Delete row";
    public static final String LABEL_DELETE_SELECTED_TENANT = "Delete selected tenant";
    public static final String LABEL_DOES_NOT_EXIST = " does not exist.";
    public static final String LABEL_EDIT_SELECTED_TENANT = "Edit selected tenant";
    public static final String LABEL_ENTRIES = "Entries";
    public static final String LABEL_ERROR = "Error";
    public static final String LABEL_ERROR_ADD_AT_LEAST_ONE_NEW_PARTNER_ID_TO_MIGRATE = "Add at least one new partner id to migrate.";
    public static final String LABEL_ERROR_EMPTY_INPUT = "Input field(s) may not be empty. Please provide all values and send again.";
    public static final String LABEL_ERROR_READING_JSON_FILE = "Error when reading JSON file. Make sure the JSON file is in the right format.";
    public static final String LABEL_ERROR_MERGING_XSLT = "Error when merging XSLT";
    public static final String LABEL_ERROR_SELECT_AT_LEAST_ONE_ENTRY = "Select at least one alternative partner to replicate.";
    public static final String LABEL_ERROR_SELECTED_TENANT_NOT_FOUND = "Selected tenant not found in list of tenants.";
    public static final String LABEL_ERROR_TENANT_NAME_ALREADY_EXISTS = "Tenant name already exists. Choose a unique tenant name.";
    public static final String LABEL_ERROR_TRANSPORT_TRY_AGAIN = "Error when replicating. Please try again.";
    public static final String LABEL_ERROR_WHEN_CONFIGURING = "Error when configuring API: ";
    public static final String LABEL_ERROR_WHEN_GENERATING = "Error when generating XSLT from input data. ";
    public static final String LABEL_ERROR_WHEN_GENERATING_INPUT = LABEL_ERROR_WHEN_GENERATING + "Make sure that all tables and the receiver not found section are filled properly.";
    public static final String LABEL_ERROR_WHEN_GENERATING_DEFAULT_RECEIVER = LABEL_ERROR_WHEN_GENERATING + "If \"Default\" is selected as the behavior when no receiver is found, the Default Receiver System must be specified in the corresponding input field.";
    public static final String LABEL_ERROR_WHEN_GENERATING_TABLE = LABEL_ERROR_WHEN_GENERATING + "Table which is missing data: ";
    public static final String LABEL_ERROR_WHEN_OPENING_BROWSER = "Error when opening documentation in browser. Please use this link to open the documentation manually: ";
    public static final String LABEL_FILL_OUT_ALL_FIELDS = "All fields must be filled out.";
    public static final String LABEL_GENERATE_XSLT = "Generate resulting XSLT";
    public static final String LABEL_GENERATED_XSLT_INVALID_SYNTAX = "The generated XSLT might contain invalid syntax";
    public static final String LABEL_HTTP_ERROR = "ERROR";
    public static final String LABEL_HTTP_SUCCESS = "SUCCESS";
    public static final String LABEL_HTTP_WARNING = "WARNING";
    public static final String LABEL_ID = "ID";
    public static final String LABEL_ID_ALTERNATIVE_PARTNERS = "Sender Interface";
    public static final String LABEL_ID_ALTERNATIVE_PARTNERS_XI = "Namespace";
    public static final String LABEL_IGNORE = "Ignore";
    public static final String LABEL_INTERFACE_DETERMINATION = "Interface Determination";
    public static final String LABEL_LANDSCAPE_STAGES = "Landscape Stages";
    public static final String LABEL_LAST_TENANT = "Last tenant cannot be deleted. You can still edit this tenant.";
    public static final String LABEL_MAINTAIN_NAMESPACES = "Maintain Namespaces";
    public static final String LABEL_MAINTAIN_LANDSCAPE_FIRST = "To setup landscape stages for a scenario, first maintain String Parameter \"" + STRING_PARAMETER_PID_SAP_INTEGRATION_SUITE_LANDSCAPE + "\" (on bottom of Alternative Partners Page).";
    public static final String LABEL_MAINTAIN_STRING_PARAMETER = "Maintain String Parameter ";
    public static final String LABEL_MERGE_XSLTS = "Merge XSLTs";
    public static final String LABEL_MIGRATE_DEPRECATED_ENTRIES = "Migrate deprecated entries";
    public static final String LABEL_MIGRATE_DEPRECATED_ENTRIES_ID = "migrateDeprecatedEntries";
    public static final String LABEL_MIGRATE_TILDE = "Migrate Pids with tilde";
    public static final String LABEL_MOVE_ROW_DOWN = "Move row down";
    public static final String LABEL_MOVE_ROW_UP = "Move row up";
    public static final String LABEL_MULTIPLE_XSLTS = "Multiple XSLTs";
    public static final String LABEL_NAMESPACE_PREFIX = "Namespace Prefix";
    public static final String LABEL_NAMESPACE_URI = "Namespace URI";
    public static final String LABEL_OVERWRITE_EXISTING_ENTRIES = "Overwrite existing entries";
    public static final String LABEL_PID = "Partner ID";
    public static final String LABEL_PID_NEW = "New " + LABEL_PID + " for alternative partner";
    public static final String LABEL_PID_OLD = "Old " + LABEL_PID + " with tilde";
    public static final String LABEL_PLACEHOLDER_EMPTY_DROPDOWN = "Add new tenant first...";
    public static final String LABEL_POINT_TO_POINT = "Point to Point";
    public static final String LABEL_POINT_TO_POINT_DETERMINATION = "Point to Point Determination";
    public static final String LABEL_PREVIEW_MERGED_XSLT = "Preview of merged XSLT";
    public static final String LABEL_RECEIVER_COMPONENT = "Receiver System";
    public static final String LABEL_RECEIVER_DETERMINATION = "Receiver Determination";
    public static final String LABEL_RECEIVER_INTERFACE = "Receiver Interface";
    public static final String LABEL_RECEIVER_INTERFACE_DETERMINATION = "Receiver and Interface Determination";
    public static final String LABEL_RECEIVER_NOT_FOUND = "If no receiver is found, proceed as follows";
    public static final String LABEL_RELOAD = "Reload data from API";
    public static final String LABEL_RESET = "Reset";
    public static final String LABEL_SAVE = "Save";
    public static final String LABEL_SCHEME = "Scheme";
    public static final String LABEL_SCHEME_XI = "Sender Interface";
    public static final String LABEL_SELECT_TENANT_TO_TRANSPORT = "Select tenant to replicate";
    public static final String LABEL_SEND_ANYWAY = "Send anyway";
    public static final String LABEL_SEND_ANYWAY_QUESTION = "Do you want to send the shown XSLT to the API anyway?";
    public static final String LABEL_SEND_CHANGES_TO_API = "Send changes to API";
    public static final String LABEL_SEND_NEW_TO_API = "Send new entry to API";
    public static final String LABEL_SEND_XSLT_TO_API = "Send shown XSLT to API";
    public static final String LABEL_SENDER_DEFAULT = "Default";
    public static final String LABEL_SENDER_TYPE = "Sender Type (Default/XI)";
    public static final String LABEL_SENDER_XI = "XI Sender";
    public static final String LABEL_SEARCH = "Search";
    public static final String LABEL_SELECT_DETERMINATION_TYPE = "Select type of Receiver / Interface Determination";
    public static final String LABEL_SELECT_LOCAL_JSON_FILE = "Select local JSON file";
    public static final String LABEL_SHOWN_XSLT_INVALID_SYNTAX = "The shown XSLT might contain invalid syntax";
    public static final String LABEL_STRING_PARAMETERS = "String Parameters";
    public static final String LABEL_SUCCESS = "Success";
    public static final String LABEL_SURE_TO_DELETE = "Are you sure that you want to delete the selected tenant named \"";
    public static final String LABEL_TENANT_NAME = "Tenant Name";
    public static final String LABEL_TOKEN_URL = "Token URL";
    public static final String LABEL_TRANSPORT = "Replicate?";
    public static final String LABEL_TRANSPORT_1 = "Replicate ";
    public static final String LABEL_TRANSPORT_2 = " alternative partner(s) with binary / string parameters to selected tenant.";
    public static final String LABEL_TRANSPORT_ALTERNATIVE_PARTNERS = "Replicate to another Tenant";
    public static final String LABEL_TRANSPORT_ERROR_ADD_TENANT = "Please add at least one more tenant to use the replication feature.";
    public static final String LABEL_TRANSPORT_ID = "replicate";
    public static final String LABEL_TRANSPORT_START_1 = "Start replication of ";
    public static final String LABEL_TRANSPORT_START_2 = " alternative partners with binary / string parameters to selected tenant ";
    public static final String LABEL_TRANSPORT_SUCCESSFUL = "Successfully replicated all selected entries.";
    public static final String LABEL_TRANSPORT_FAILED_1 = "During replication, ";
    public static final String LABEL_TRANSPORT_FAILED_2 = " request(s) failed. Please check the logs for details.";
    public static final String LABEL_TRANSPORT_FINISHED = "Finished replication. ";
    public static final String LABEL_TRANSPORT_FOUND_X = "Found ";
    public static final String LABEL_TRANSPORT_FOUND_BINARY = " binary parameters to replicate.";
    public static final String LABEL_TRANSPORT_FOUND_STRING = " string parameters to replicate.";
    public static final String LABEL_TRANSPORT_NOT_FOUND_BINARY = "No binary parameters found for selected Pids.";
    public static final String LABEL_TRANSPORT_NOT_FOUND_STRING = "No string parameters found for selected Pids.";
    public static final String LABEL_UPDATE_RECEIVERS = "Update receivers";
    public static final String LABEL_URL = "URL";
    public static final String LABEL_VALUE = "Value";
    public static final String LABEL_WARNING = "Warning";
    public static final String LABEL_WARNING_DELETE_LAST_ROW = "Cannot delete the last row.";
    public static final String LABEL_XSLTS_MERGED_SUCCESSFULLY = "XSLTs were merged successfully.";

    public static final String LOGGER_INFO_ALTERNATIVE_PARTNERS_PAGE = "Alternative Partners Page selected";

    public static final String STRING_PARAMETER_ID_INBOUND_CONVERSION_ENDPOINT = "InboundConversionEndpoint";
    public static final String STRING_PARAMETER_ID_MAX_JMS_RETRIES = "MaxJMSRetries";
    public static final String STRING_PARAMETER_ID_REUSE_XRD_ENDPOINT = "ReuseXRDEndpoint";
    public static final String STRING_PARAMETER_ID_RECEIVER_NOT_DETERMINED_TYPE = "ReceiverNotDeterminedType";
    public static final String STRING_PARAMETER_ID_RECEIVER_NOT_DETERMINED_DEFAULT = "ReceiverNotDeterminedDefault";
    public static final String STRING_PARAMETER_ID_CUSTOM_XRD_ENDPOINT = "CustomXRDEndpoint";
    public static final String STRING_PARAMETER_ID_CUSTOM_XID_ENDPOINT = "CustomXIDEndpoint";
    public static final String STRING_PARAMETER_ID_INBOUND_QUEUE = "InboundQueue";
    public static final String STRING_PARAMETER_ID_KEEP_BULK = "KeepBulk";
    public static final String STRING_PARAMETER_ID_CUSTOM_X_PRE_ENABLED = "CustomXPreEnabled";
    public static final String STRING_PARAMETER_ID_CUSTOM_X_PRE_ENDPOINT = "CustomXPreEndpoint";
    public static final String STRING_PARAMETER_ID_RECEIVER_SPECIFIC_QUEUE = "ReceiverSpecificQueue";
    public static final String STRING_PARAMETER_LABEL_DATA_STORE = "Date Store Extension - ";
    public static final String STRING_PARAMETER_ID_RETRY_DATA_STORE = "RetryDataStore";
    public static final String STRING_PARAMETER_ID_RESTART_MODE = "restartMode";
    public static final String STRING_PARAMETER_ID_MAX_DATA_STORE_RETRIES = "MaxDataStoreRetries";
    public static final String[] STRING_PARAMETER_IDS_ARRAY = {
            STRING_PARAMETER_ID_INBOUND_CONVERSION_ENDPOINT,
            STRING_PARAMETER_ID_MAX_JMS_RETRIES,
            STRING_PARAMETER_ID_REUSE_XRD_ENDPOINT,
            STRING_PARAMETER_ID_RECEIVER_NOT_DETERMINED_TYPE,
            STRING_PARAMETER_ID_RECEIVER_NOT_DETERMINED_DEFAULT,
            STRING_PARAMETER_ID_CUSTOM_XRD_ENDPOINT,
            STRING_PARAMETER_ID_CUSTOM_XID_ENDPOINT,
            STRING_PARAMETER_ID_INBOUND_QUEUE,
            STRING_PARAMETER_ID_KEEP_BULK,
            STRING_PARAMETER_ID_CUSTOM_X_PRE_ENABLED,
            STRING_PARAMETER_ID_CUSTOM_X_PRE_ENDPOINT,
            STRING_PARAMETER_LABEL_DATA_STORE + STRING_PARAMETER_ID_RETRY_DATA_STORE,
            STRING_PARAMETER_LABEL_DATA_STORE + STRING_PARAMETER_ID_RESTART_MODE,
            STRING_PARAMETER_LABEL_DATA_STORE + STRING_PARAMETER_ID_MAX_DATA_STORE_RETRIES,
    };

    public static final String LINK_DOCUMENTATION_STRING_PARAMETERS = "https://help.sap.com/docs/migration-guide-po/migration-guide-for-sap-process-orchestration/using-partner-directory-in-pipeline-concept#message-processing-behavior";

    public static final String[] LABELS_DETERMINATION_TYPES = {LABEL_COMBINED_XSLT, LABEL_MULTIPLE_XSLTS, LABEL_POINT_TO_POINT};
    public static final String[] LABELS_SENDER_TYPES = {LABEL_SENDER_DEFAULT, LABEL_SENDER_XI};

    public static final String COMPONENT_RECEIVER_NOT_FOUND = "C Rec not found";
    public static final String COMPONENT_RECEIVER_DEFAULT = "C Rec default";
    public static final String COMPONENT_RECEIVER_TABLE = "C Rec table";
    public static final String COMPONENT_RECEIVER_TABLE_MODEL = "C Rec table model";
    public static final String COMPONENT_INTERFACE_TABLE = "C Int table";

    public static final String COMPONENT_SUFFIX_HTTP_LABEL = "-httpLabel";

    public static final String TENANTS_FILE_NAME = "tenants.json";
    public static final String BACKUP_DIRECTORY_NAME = "backup";

    public static final String JSON_KEY_ACCESS_TOKEN = "access_token";
    public static final String JSON_KEY_AGENCY = "Agency";
    public static final String JSON_KEY_CLIENT_ID = "clientid";
    public static final String JSON_KEY_CLIENT_SECRET = "clientsecret";
    public static final String JSON_KEY_CREATED_TIME = "CreatedTime";
    public static final String JSON_KEY_CRITICAL = "critical";
    public static final String JSON_KEY_CONTENT_TYPE = "ContentType";
    public static final String JSON_KEY_D = "d";
    public static final String JSON_KEY_EXPIRES_IN = "expires_in";
    public static final String JSON_KEY_HEXAGENCY = "Hexagency";
    public static final String JSON_KEY_HEXID = "Hexid";
    public static final String JSON_KEY_HEXSCHEME = "Hexscheme";
    public static final String JSON_KEY_ID = "Id";
    public static final String JSON_KEY_IMPORTANT = "important";
    public static final String JSON_KEY_NAME = "name";
    public static final String JSON_KEY_OAUTH = "oauth";
    public static final String JSON_KEY_PID = "Pid";
    public static final String JSON_KEY_RESULTS = "results";
    public static final String JSON_KEY_SCHEME = "Scheme";
    public static final String JSON_KEY_TENANTS = "tenants";
    public static final String JSON_KEY_TOKEN_EXPIRATION_DATE_TIME = "token_expiration_datetime";
    public static final String JSON_KEY_TOKEN_URL = "tokenurl";
    public static final String JSON_KEY_URL = "url";
    public static final String JSON_KEY_VALUE = "Value";
    public static final String JSON_KEY___NEXT = "__next";

    public static final String JSON_VALUE_GENERATED = "This file is automatically generated and contains sensitive data. Please do not share this file and please do not change the content in this file.";
    public static final String JSON_VALUE_XSL = "xsl";

    public static final String SCHEME_BUSINESS_SYSTEM_NAME = "BusinessSystemName";
    public static final String SCHEME_LOGICAL_SYSTEM_NAME = "LogicalSystemName";
    public static final String SCHEME_SENDER_INTERFACE = "SenderInterface";

    public static final String PATH_TO_API = "/api/v1/";
    public static final String API_ALTERNATIVE_PARTNERS = "AlternativePartners";
    public static final String API_BINARY_PARAMETERS = "BinaryParameters";
    public static final String API_STRING_PARTNERS = "StringParameters";

    public static final String ID_RECEIVER_DETERMINATION = "receiverDetermination";
    public static final String ID_INTERFACE_DETERMINATION = "interfaceDetermination";
    public static final String ID_INTERFACE_DETERMINATION_ = "interfaceDetermination_";
    public static final String ID_COMBINED_DETERMINATION = "combinedDetermination";

    public static final String XSLT_NOT_NULL = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="3.0">
            <!-- Placeholder for XSLT -->
            </xsl:stylesheet>""";
    public static final String TILDE = "~";

    public static final String DATE_TIME_FORMATTER_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static final int DEFAULT_COLUMNS_TEXT_FIELD = 30;

    public static final Color GREEN = new Color(0, 153, 0);
    public static final Color ORANGE = new Color(255, 153, 0);
    public static final Color RED = new Color(204, 0, 0);

    public static MainFrame mainFrame;
    public static HttpRequestHandler httpRequestHandler;
    public static JsonApiHandler jsonApiHandler;
    public static XsltHandler xsltHandler;
    public static CardLayout cardLayout;
    public static JPanel panelContainer;
    public static JsonFileHandler jsonFileHandler;
    public static JLabel httpResponseLabelHeader;

    public static final Logger LOGGER = LogManager.getRootLogger();

    public static List<AlternativePartner> currentAlternativePartnersList = new ArrayList<>();
    public static BinaryParameter currentReceiverDetermination = new BinaryParameter(ID_RECEIVER_DETERMINATION);
    public static Map<String, BinaryParameter> currentInterfaceDeterminationsList = new LinkedHashMap<>();
    public static Map<String, StringParameter> currentStringParametersList = new LinkedHashMap<>();
    public static Map<String, String> currentLandscapeTenantParameters = new LinkedHashMap<>();
    public static List<AlternativePartner> currentLandscapeScenarioParameters = new ArrayList<>();

    public static List<TenantCredentials> tenantCredentialsList = new ArrayList<>();
    public static String currentTenantName;

    private SharedData() {

    }

    public static String colon(String string) {
        return string + ":";
    }

    public static String space(String string) {
        return string + " ";
    }

    public static String colonAsterisk(String string) {
        return space(colon(string)) + "*";
    }

    public static String colonSpace(String string) {
        return space(colon(string));
    }
}