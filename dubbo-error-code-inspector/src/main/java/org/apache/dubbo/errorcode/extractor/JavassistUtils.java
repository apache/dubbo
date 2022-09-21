package org.apache.dubbo.errorcode.extractor;

import org.apache.dubbo.errorcode.util.FileUtils;

import javassist.bytecode.ClassFile;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Info here.
 *
 * @author Andy Cheung
 */
class JavassistUtils {
    private JavassistUtils() {
        throw new UnsupportedOperationException("No instance of JavassistUtils for you! ");
    }

    static ClassFile openClassFile(String classFilePath) {
        try {
            byte[] clsB = FileUtils.openFileAsByteArray(classFilePath);
            return new ClassFile(new DataInputStream(new ByteArrayInputStream(clsB)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
