/*******************************************************************************
 * Project : KCT ImageJ plugin to open DEN files
 * Author: Vojtěch Kulvait
 * Licence: GNU GPL3
 * Description : Class to get information about legacy or extended DEN
 * Date: 2023
 ******************************************************************************/
package com.kulvait.kct.imagej.denfileopener.dat;

import com.kulvait.kct.imagej.denfileopener.DenDataType;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class DatFileInfo
{
    // The DEX format is inspired by DEN
    // It is format to store n-dimensional arrays
    // In 2-byte headers is encoded the following HEADER1(YMAJOR,TYPE) HEADER2(DIM,ELMLEN)

    File f;
    long byteSize;
    long elementCount;
    long elementSize;
    DenDataType elementType;
    boolean validDAT;
    long dataByteOffset;
    int DIMCOUNT;
    long[] dim;
    long dimx, dimy, dimz;
    boolean xmajor;
    String firstLine;

    DatFileInfo(File f)
    {
        byteSize = f.length();
        dim = new long[16];
        try
        {
            RandomAccessFile df = new RandomAccessFile(f, "r");
            // ISO_8859_1 is 8bit encoding LATIN1
            CharsetDecoder decoder = StandardCharsets.ISO_8859_1.newDecoder()
                                         .onMalformedInput(CodingErrorAction.REPLACE)
                                         .onUnmappableCharacter(CodingErrorAction.REPLACE);
            FileChannel inChannel = df.getChannel();
            MappedByteBuffer buffer
                = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, Math.min(1024, inChannel.size()));
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.load();
            CharBuffer decodedBuffer = decoder.decode(buffer);
            char a = decodedBuffer.get();
            firstLine = "";
            dataByteOffset = 1;
            while(a != '\n' && dataByteOffset < 2 * decodedBuffer.limit())
            {
                firstLine += Character.toString(a);
                a = decodedBuffer.get();
                dataByteOffset++;
            }
            if(firstLine == "")
            {
                System.out.printf("Invalid DAT: no newline in a first 1024 bytes.\n");
                validDAT = false;
                return;
            }
            String[] fld = firstLine.split("_");
            if(fld.length < 3)
            {
                System.out.printf(
                    "Invalid DAT: firstLineFields shall be of minimally 3 components.\n");
                validDAT = false;
                return;
            }
            DIMCOUNT = Integer.parseInt(fld[1]);
            if(fld.length != 3 + DIMCOUNT + 1) // Empty field at the end
            {
                System.out.printf("Invalid DAT: firstLine=%s shall have %d elements but has %d\n",
                                  firstLine, 3 + DIMCOUNT, fld.length);
                validDAT = false;
                return;
            }
            if(DIMCOUNT > 16)
            {
                System.out.printf("Invalid DAT: DIMCOUNT=%d shall not be more than 16\n", DIMCOUNT);
                validDAT = false;
                return;
            }
            switch(fld[2])
            {
            case "F":
                elementType = DenDataType.FLOAT32;
                break;
            case "D":
                elementType = DenDataType.FLOAT64;
                break;
            case "U":
                elementType = DenDataType.UINT16;
                break;
            case "L":
                elementType = DenDataType.UINT32;
                break;
            case "I":
                elementType = DenDataType.INT16;
                break;
            default:
                System.out.printf("Invalid DAT: elementType=%s is not supported or known", fld[2]);
                validDAT = false;
                return;
            }
            System.out.printf("Openning DAT: first line %s type %s.\n", firstLine,
                              elementType.toString());
            elementSize = elementType.getSize();
            for(int i = 0; i != DIMCOUNT; i++)
            {
                dim[i] = Integer.parseInt(fld[3 + i]);
            }
            if(DIMCOUNT == 0)
            {
                dimx = 0;
                dimy = 0;
                dimz = 0;
            } else if(DIMCOUNT == 1)
            {
                dimx = dim[0]; // Height
                dimy = 1;
                dimz = 1;
            } else if(DIMCOUNT == 2)
            {
                dimx = dim[0]; // Height
                dimy = dim[1]; // Width
                dimz = 1;
            } else
            {
                dimx = dim[0]; // Height
                dimy = dim[1]; // Width
                dimz = dim[2];
                System.out.printf("dimx=%d dimy=%d dimz=%d\n", dimx, dimy, dimz);
                // Flat indexing for more than 3D arrays
                for(int i = 3; i < DIMCOUNT; i++)
                {
                    dimz = dimz * dim[i];
                    System.out.printf("dimz=%d\n", dimz);
                }
            }
            xmajor = true;
            // Problem
            // https://stackoverflow.com/questions/48693695/java-nio-buffer-not-loading-clear-method-on-runtime
            ((Buffer)buffer).clear();
            inChannel.close();
            df.close();
            elementCount = dimx * dimy * dimz;
            if(dataByteOffset + elementSize * elementCount == byteSize)
            {
                validDAT = true;
            } else
            {
                System.out.printf(
                    "Invalid DAT: size check firstLine=%s dataByteOffset=%d elementSize=%d elementCount=%d byteSize=%d != expectedByteSize=%d\n",
                    firstLine, dataByteOffset, elementSize, elementCount, byteSize,
                    dataByteOffset + elementSize * elementCount);
                validDAT = false;
            }
        } catch(IOException e)
        {
            System.out.printf("Invalid DAT: IOException %s\n", e.toString());
            validDAT = false;
        }
    }

    public int getDIMCOUNT() { return DIMCOUNT; }

    public long getByteSize() { return byteSize; }

    public long getElementCount() { return elementCount; }

    public long getElementSize() { return elementSize; }

    public DenDataType getElementType() { return elementType; }

    public boolean isValidDAT() { return validDAT; }

    public long getDataByteOffset() { return dataByteOffset; }

    public long getDim(int i) { return dim[i]; }

    public long getDimx() { return dimx; }

    public long getDimy() { return dimy; }

    public long getDimz() { return dimz; }

    public String getFirstLine() { return firstLine; }

    public boolean isXmajor() { return xmajor; }
}
