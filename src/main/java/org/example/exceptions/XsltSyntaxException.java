package org.example.exceptions;

public class XsltSyntaxException extends Exception {
    String receiverName;
    boolean isInterfaceDetermination;

    public XsltSyntaxException(String receiverName, boolean isInterfaceDetermination) {
        this.receiverName = receiverName;
        this.isInterfaceDetermination = isInterfaceDetermination;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public boolean isInterfaceDetermination() {
        return isInterfaceDetermination;
    }
}
