package com.alibaba.com.caucho.hessian.io.base;

import com.alibaba.com.caucho.hessian.io.Hessian2Input;
import com.alibaba.com.caucho.hessian.io.Hessian2Output;
import com.alibaba.com.caucho.hessian.io.HessianInput;
import com.alibaba.com.caucho.hessian.io.HessianOutput;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * hession base serialize utils
 *
 */
public class SerializeTestBase {
    /**
     * hession serialize util
     *
     * @param data
     * @param <T>
     * @return
     * @throws IOException
     */
    protected <T> T baseHessionSerialize(T data) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        HessianOutput out = new HessianOutput(bout);

        out.writeObject(data);
        out.flush();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        HessianInput input = new HessianInput(bin);
        return (T) input.readObject();
    }

    /**
     * hession2 serialize util
     *
     * @param data
     * @param <T>
     * @return
     * @throws IOException
     */
    protected <T> T baseHession2Serialize(T data) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        Hessian2Output out = new Hessian2Output(bout);

        out.writeObject(data);
        out.flush();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        Hessian2Input input = new Hessian2Input(bin);
        return (T) input.readObject();
    }
}
