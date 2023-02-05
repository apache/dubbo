package org.apache.dubbo.annotation.util;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Info here.
 *
 * @author Andy Cheung
 */
public class MessagerPrintStreamWrapper extends PrintStream {
    private final ProcessingEnvironment processingEnvironment;

    public MessagerPrintStreamWrapper(ProcessingEnvironment processingEnvironment) {
        super((OutputStream) null);
        this.processingEnvironment = processingEnvironment;
    }

    @Override
    public void println(String x) {
        processingEnvironment.getMessager().printMessage(
            Diagnostic.Kind.WARNING,
            x
        );
    }

    @Override
    public void print(String x) {
        processingEnvironment.getMessager().printMessage(
            Diagnostic.Kind.WARNING,
            x
        );
    }
}
