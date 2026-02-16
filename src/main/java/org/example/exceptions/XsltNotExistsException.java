package org.example.exceptions;

public class XsltNotExistsException extends Exception {
    String receiverName;

    public XsltNotExistsException(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverName() {
        return receiverName;
    }
}
