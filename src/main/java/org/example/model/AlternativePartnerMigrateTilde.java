package org.example.model;

import static org.example.utils.SharedData.TILDE;

public class AlternativePartnerMigrateTilde {
    String oldPid;
    String newPid;

    private AlternativePartnerMigrateTilde(String oldPid, String newPid) {
        this.oldPid = oldPid;
        this.newPid = newPid;
    }

    public AlternativePartnerMigrateTilde(String oldPid) {
        this(oldPid, null);
    }

    public String getOldPid() {
        return oldPid == null ? "" : oldPid;
    }

    public String getAgency() {
        if (oldPid == null) return "";
        int idx = oldPid.indexOf(TILDE);
        if (idx >= 0) {
            return oldPid.substring(0, idx).trim();
        } else {
            return "";
        }
    }

    public String getId() {
        if (oldPid == null) return "";
        int idx = oldPid.indexOf(TILDE);
        if (idx >= 0) {
            return oldPid.substring(idx + 1).trim();
        } else {
            return oldPid.trim();
        }
    }

    public String getNewPid() {
        return newPid == null ? "" : newPid;
    }

    public void setNewPid(String newPid) {
        this.newPid = newPid;
    }
}

