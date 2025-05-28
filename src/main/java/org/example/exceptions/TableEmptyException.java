package org.example.exceptions;

public class TableEmptyException extends Exception{
    String tableName;

    public TableEmptyException(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }
}
