package org.example.utils;

import net.sf.saxon.s9api.*;

import javax.xml.transform.stream.StreamSource;

import java.io.StringReader;

public class XsltSyntaxValidator {
    private final XsltCompiler compiler;

    public XsltSyntaxValidator() {
        Processor processor = new Processor();
        this.compiler = processor.newXsltCompiler();
    }

    public String validateXsltSyntax(String xsltString) {
        StringBuilder errorMessages = new StringBuilder();

        try {
            compiler.setErrorReporter(xmlProcessingError -> {
                StringBuilder errorMessageBuilder = new StringBuilder();

                Location location = xmlProcessingError.getLocation();
                if (location != null) {
                    errorMessageBuilder.append("Error at line ").append(location.getLineNumber());
                    errorMessageBuilder.append(" column ").append(location.getColumnNumber());

                    try {
                        Object locator = xmlProcessingError.getLocation();
                        Class<?> locatorClass = locator.getClass();

                        java.lang.reflect.Field nearbyTextField = locatorClass.getDeclaredField("nearbyText");
                        nearbyTextField.setAccessible(true);
                        String nearbyText = (String) nearbyTextField.get(locator);

                        errorMessageBuilder.append(" near text: \"").append(nearbyText).append("...\"");
                    } catch (Exception e) {
                        // System.out.println("Error accessing nearby text: " + e.getMessage());
                    }
                }

                errorMessages.append(errorMessageBuilder).append("\n");
            });

            StreamSource xsltStream = new StreamSource(new StringReader(xsltString));
            compiler.compile(xsltStream);
        } catch (SaxonApiException e) {
            // errorMessages.add("SaxonApiException: " + e.getMessage());
        }

        return errorMessages.toString();
    }
}
