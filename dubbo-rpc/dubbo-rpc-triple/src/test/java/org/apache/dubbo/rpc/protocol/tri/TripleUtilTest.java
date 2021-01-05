package org.apache.dubbo.rpc.protocol.tri;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TripleUtilTest {

    @Test
    void percentEncoding() {
        String content="\t\ntest with whitespace\r\nand Unicode BMP ☺ and non-BMP 😈\t\n";
        final String encoded = TripleUtil.percentEncode(content);
        final String decoded = TripleUtil.percentDecode(encoded);
        Assertions.assertEquals(content,decoded);
    }
}