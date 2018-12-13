package me.cxj.ifc.deserializer;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

/**
 * Created by vipcxj on 2018/11/21.
 */
public class CodePointStream implements Closeable {

    private Reader reader;

    public CodePointStream(Reader reader) {
        this.reader = reader;
    }

    public int read() throws IOException {
        int unit0 = reader.read();
        if (unit0 < 0)
            return unit0; // EOF

        if (!Character.isHighSurrogate((char)unit0))
            return unit0;

        int unit1 = reader.read();
        if (unit1 < 0)
            return unit1; // EOF

        if (!Character.isLowSurrogate((char)unit1))
            throw new RuntimeException("Invalid surrogate pair");

        return Character.toCodePoint((char)unit0, (char)unit1);
    }

    @Override
    public void close() throws IOException {
        reader.close();
        reader = null;
    }
}
