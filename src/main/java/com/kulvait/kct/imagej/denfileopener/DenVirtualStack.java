/*******************************************************************************
 * Project : KCT ImageJ plugin to open in house den format
 * Author: Vojtěch Kulvait
 * Licence: GNU GPL3
 * Description : Implementation of memory mapped views to DEN files
 * So called virtual stack is created.
 * Memory representation is always float independent of type.
 * Date: 2022
 ******************************************************************************/
package com.kulvait.kct.imagej.denfileopener;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

// Ideas based on
// https://github.com/tischi/imagej-open-stacks-as-virtualstack/blob/master/open_stacks_as_virtual_stack_maven/src/main/java/bigDataTools/VirtualStackOfStacks.java
// ImageJ processes just these four bit depths and corresponding types 8=byte, 16=short, 24=RGB,
// 32=float, see https://forum.image.sc/t/how-to-obtain-an-integer-image/1401

public class DenVirtualStack extends ImageStack
{
    File f;
    DenFileInfo inf;
    RandomAccessFile df;
    FileChannel inChannel;
    int dimx, dimy, dimz;
    int dimImg;
    DenDataType typ;
    float[] pixelArray;

    DenVirtualStack(File f) throws IOException
    {
        this.f = f;
        this.inf = new DenFileInfo(f);
        if(!inf.isValidDEN())
        {
            throw new RuntimeException(String.format("File %s is not valid DEN!", f.getName()));
        }

        this.df = new RandomAccessFile(f, "r");
        inChannel = df.getChannel();

        // Supports fast access, if from undefined dimension these are ones
        dimx = (int)inf.getDim(0);
        dimy = (int)inf.getDim(1);
        dimz = (int)inf.getDim(2);
        dimImg = dimx * dimy;
        typ = inf.getElementType();
        pixelArray = new float[dimImg];
    }

    /**
     * The following methods are intentionally overriden to do nothing as
     * VirtualStack do not support such functionality
     */
    public void addSlice(String sliceLabel, Object pixels) {}

    public void addSlice(String sliceLabel, ImageProcessor ip) {}

    public void addSlice(String sliceLabel, ImageProcessor ip, int n) {}

    public void deleteSlice(int n) {}

    public void deleteLastSlice() {}

    public void setPixels(Object pixels, int n) {}

    public void setSliceLabel(String label, int n) {}

    public void trim() {}

    public Object[] getImageArray() { return null; }

    // Theoretically I can derive it from ImagePlus with given offset
    // 1 based n
    public ImageProcessor getProcessor(int n)
    {
        FloatProcessor fp = new FloatProcessor(dimx, dimy);
        getPixels(n);
        int index;
        for(int y = 0; y != dimy; y++)
        {
            for(int x = 0; x != dimx; x++)
            {
                index = y * dimx + x;
                fp.setf(x, y, pixelArray[index]);
            }
        }
        return fp;
    }

    public Object getPixels(int n)
    {
        if(n > dimz)
        {
            throw new RuntimeException(
                String.format("Illegal acces to the slice %d/%d", n - 1, dimz));
        }
        long pos = inf.getDataByteOffset() + inf.getElementSize() * (long)(n - 1) * (long)dimImg;
        MappedByteBuffer buf;
        try
        {
            buf = inChannel.map(FileChannel.MapMode.READ_ONLY, pos, inf.getElementSize() * dimImg);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.load();
            float f;
            long val_lng;
            int val_int;
            int index;
            boolean xmajor = inf.isXmajor();
            if(xmajor)
            {
                for(int y = 0; y != dimy; y++)
                {
                    for(int x = 0; x != dimx; x++)
                    {
                        index = y * dimx + x;

                        if(typ == DenDataType.UINT8)
                        {
                            val_int = buf.get() & 0xff;
                            f = (float)val_int;
                        } else if(typ == DenDataType.UINT16)
                        {
                            val_int = buf.getShort() & 0xffff;
                            f = (float)val_int;
                        } else if(typ == DenDataType.FLOAT32)
                        {
                            f = buf.getFloat();
                        } else if(typ == DenDataType.FLOAT64)
                        {
                            f = (float)buf.getDouble();
                        } else if(typ == DenDataType.UINT32)
                        {
                            val_lng = buf.getInt() & 0xffffffffL;
                            f = (float)val_lng;
                        } else
                        {
                            throw new RuntimeException(
                                String.format("The type %s is not implemented yet!", typ.name()));
                        }
                        pixelArray[index] = f;
                    }
                }
            } else
            {
                for(int x = 0; x != dimx; x++)
                {
                    for(int y = 0; y != dimy; y++)
                    {
                        index = y * dimx + x; // Transpose to use row major array
                        if(typ == DenDataType.UINT8)
                        {
                            val_int = buf.get() & 0xff;
                            f = (float)val_int;
                        } else if(typ == DenDataType.UINT16)
                        {
                            val_int = buf.getShort() & 0xffff;
                            f = (float)val_int;
                        } else if(typ == DenDataType.FLOAT32)
                        {
                            f = buf.getFloat();
                        } else if(typ == DenDataType.FLOAT64)
                        {
                            f = (float)buf.getDouble();
                        } else if(typ == DenDataType.UINT32)
                        {
                            val_lng = buf.getInt() & 0xffffffffL;
                            f = (float)val_lng;
                        } else
                        {
                            throw new RuntimeException(
                                String.format("The type %s is not implemented yet!", typ.name()));
                        }
                        pixelArray[index] = f;
                    }
                }
            }

            return pixelArray;
        } catch(IOException e)
        {
            e.printStackTrace();
            throw new RuntimeException(
                String.format("Can not map buffer of the slice %d/%d", n - 1, dimz));
        }
    }

    /**
     * 8=byte, 16=short, 24=RGB, 32=float
     *
     */
    public int getBitDepth()
    {
        if(typ == DenDataType.UINT8)
        {
            return 8;
        } else if(typ == DenDataType.UINT16)
        {
            return 16;
        } else if(typ == DenDataType.FLOAT32)
        {
            return 32;
        } else if(typ == DenDataType.FLOAT64)
        {
            return 32;
        } else if(typ == DenDataType.UINT32)
        {
            return 32;
        } else
        {
            throw new RuntimeException(
                String.format("The type %s is not implemented yet!", typ.name()));
        }
    }

    public int getSize() { return (int)inf.getDim(2); }

    public int getWidth() { return (int)inf.getDim(0); }

    public int getHeight() { return (int)inf.getDim(1); }

    public String getSliceLabel(int n) { return String.format("z=%d", n - 1, (int)inf.getDim(2)); }

    public boolean isVirtual() { return true; }
}
