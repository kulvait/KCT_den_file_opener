/*******************************************************************************
 * Project : KCT ImageJ plugin to open DEN files
 * Author: Vojtěch Kulvait
 * Licence: GNU GPL3
 * Description : Class to get information about legacy or extended DEN
 * Date: 2022
 ******************************************************************************/
package com.kulvait.kct.imagej.denfileopener;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
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
                extendedDEN = true;
                DIMCOUNT = (header1 & DIM_EXTHEADER1_MASK);
                elementSize = ((header1 & ELMLEN_EXTHEADER1_MASK) >> 4);
                dataByteOffset = 6 + 4 * DIMCOUNT;
                for(int i = 0; i != DIMCOUNT; i++)
                {
                    dim[i] = (buffer.getInt() & 0xffffffffL);
                }
                dimy = buffer.getInt() & 0xffffffffL; // Width
                dimx = buffer.getInt() & 0xffffffffL; // Height
                dimz = buffer.getInt() & 0xffffffffL;
                xmajor = ((header1 & YMAJOR_EXTHEADER2_MASK) == 0);
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
            buffer.clear();
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
                    validDEN = false;
                    return;
                }
            } else
            {
                int typebyte = header1 & 0xff;
                elementType = DenDataType.values()[typebyte];
            }
            elementSize = elementType.getSize();
            if(dataByteOffset + elementSize * elementCount == byteSize)
            {
                validDEN = true;
            } else
            {
                validDEN = false;
            }
        } catch(IOException e)
        {
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
