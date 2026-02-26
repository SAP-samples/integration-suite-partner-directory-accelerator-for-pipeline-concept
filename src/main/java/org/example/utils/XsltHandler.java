package org.example.utils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.example.templates.TemplateObjects;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class XsltHandler {
    private final Configuration configuration;

    public XsltHandler() {
        configuration = new Configuration(Configuration.VERSION_2_3_33);
        configuration.setClassForTemplateLoading(this.getClass(), "/templates");
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }

    public String handleXslt(String typeOfDetermination, TemplateObjects object) throws IOException, TemplateException {
        Map<String, Object> content = new HashMap<>();
        content.put("content", object);

        StringWriter stringWriter = new StringWriter();

        Template template = this.configuration.getTemplate(typeOfDetermination + ".ftlh");
        template.process(content, stringWriter);

        return stringWriter.toString();
    }
}