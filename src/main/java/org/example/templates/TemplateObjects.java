package org.example.templates;

import java.util.Map;

public interface TemplateObjects {

    // namespaces

    String getNamespacesAsString(); // needed for templates

    Map<String, String> getNamespaces();

    void setNamespaces(Map<String, String> namespaces);

    // params

    void setParams();

    // helper methods

    void clear();

}
