/*******************************************************************************
 * Project : KCT ImageJ plugin to open DEN files
 * Author: Vojtěch Kulvait
 * Licence: GNU GPL3
 * Description : Class to get information about legacy or extended DEN
 * Date: 2023
 ******************************************************************************/
package com.kulvait.kct.imagej.denfileopener;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class DenFileInfo
{
    // The DEX format is inspired by DEN
    // It is format to store n-dimensional arrays
    // In 2-byte headers is encoded the following HEADER1(YMAJOR,TYPE) HEADER2(DIM,ELMLEN)

    final int DIM_EXTHEADER1_MASK = 0xf;
    final long ELMLEN_EXTHEADER1_MASK = 0xffff0000L;
    final int FILETYPE_EXTHEADER2_MASK = 0xff; // For extended DEN
    final short UINT16_EXTHEADER2 = 0;
    final short INT16_EXTHEADER2 = 1;
    final short UINT32_EXTHEADER2 = 2;
    final short INT32_EXTHEADER2 = 3;
    final short UINT64_EXTHEADER2 = 4;
    final short INT64_EXTHEADER2 = 5;
    final short FLOAT32_EXTHEADER2 = 6;
    final short FLOAT64_EXTHEADER2 = 7;
    final int YMAJOR_EXTHEADER2_MASK = 0x100;

    File f;
    long byteSize;
    long elementCount;
    long elementSize;
    DenDataType elementType;
    boolean validDEN;
    boolean extendedDEN;
    long dataByteOffset;
    int header0, header1, header2;
    int DIMCOUNT;
    long[] dim;
    long dimx, dimy, dimz;
    boolean xmajor;

    DenFileInfo(File f)
    {
        byteSize = f.length();
        dim = new long[16];
        Arrays.fill(dim, 1);
        if(byteSize < 6)
        {
            System.out.printf("Invalid DEN: byteSize < 6");
            validDEN = false;
            return;
        }
        try
        {
            RandomAccessFile df = new RandomAccessFile(f, "r");
            FileChannel inChannel = df.getChannel();
            MappedByteBuffer buffer
                = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, Math.min(1024, inChannel.size()));
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.load();
            header0 = buffer.getShort() & 0xffff;
            header1 = buffer.getShort() & 0xffff;
            header2 = buffer.getShort() & 0xffff;
            // This is to convert uint16 representation to int as Java does not have signed types
            // int is able to represent uint16s > 32768 as positive numbers, short not
            // It is working due to two's complement representation, see
            // https://en.wikipedia.org/wiki/Two%27s_complement
            if(header0 == 0 && byteSize > 6)
            {
                int header3 = buffer.getShort() & 0xffff;
                int header4 = buffer.getShort() & 0xffff;
                extendedDEN = true;
                DIMCOUNT = header1;
                elementSize = header2;
                xmajor = (header3 == 0);
                elementType = DenDataType.values()[header4];
                dataByteOffset = 4096;
                for(int i = 0; i != DIMCOUNT; i++)
                {
                    dim[i] = (buffer.getInt() & 0xffffffffL);
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
            } else
            {
                extendedDEN = false;
                dataByteOffset = 6;
                DIMCOUNT = 3;
                dim[0] = header1;
                dim[1] = header0;
                dim[2] = header2;
                dimx = header1;
                dimy = header0;
                dimz = header2;
                xmajor = true;
            }
//Problem https://stackoverflow.com/questions/48693695/java-nio-buffer-not-loading-clear-method-on-runtime
            ((Buffer)buffer).clear();
            inChannel.close();
            df.close();
            elementCount = dimx * dimy * dimz;
            if(elementCount == 0 && byteSize - dataByteOffset == 0)
            {
                validDEN = true;
                return;
            }

            if(!extendedDEN)
            {
                if((byteSize - dataByteOffset) % elementCount != 0)
                {
                    System.out.printf(
                        "Invalid legacy DEN: (byteSize - dataByteOffset) % elementCount != 0");
                    validDEN = false;
                    return;
                }
                elementSize = (int)((byteSize - dataByteOffset) / elementCount);
                if(elementSize == 2)
                {
                    elementType = DenDataType.UINT16;
                } else if(elementSize == 4)
                {
                    elementType = DenDataType.FLOAT32;
                } else if(elementSize == 8)
                {
                    elementType = DenDataType.FLOAT64;
                } else
                {
                    System.out.printf("Invalid legacy DEN: elementSize=%d", elementSize);
                    validDEN = false;
                    return;
                }
            }
            elementSize = elementType.getSize();
            if(dataByteOffset + elementSize * elementCount == byteSize)
            {
                validDEN = true;
            } else
            {
                System.out.printf(
                    "Invalid DEN: size check dataByteOffset=%d elementSize=%d elementCount=%d byteSize=%d\n",
                    dataByteOffset, elementSize, elementCount, byteSize);
                validDEN = false;
            }
        } catch(IOException e)
        {
            System.out.printf("Invalid DEN: IOException");
            validDEN = false;
        }
    }

    public int getDIMCOUNT() { return DIMCOUNT; }

    public long getByteSize() { return byteSize; }

    public long getElementCount() { return elementCount; }

    public long getElementSize() { return elementSize; }

    public DenDataType getElementType() { return elementType; }

    public boolean isValidDEN() { return validDEN; }

    public boolean isExtendedDEN() { return extendedDEN; }

    public long getDataByteOffset() { return dataByteOffset; }

    public long getDim(int i) { return dim[i]; }

    public long getDimx() { return dimx; }

    public long getDimy() { return dimy; }

    public long getDimz() { return dimz; }

    public boolean isXmajor() { return xmajor; }
}
