/*******************************************************************************
 * Project : KCT ImageJ plugin to open in house den format
 * Author: Vojtěch Kulvait
 * Licence: GNU GPL3
 * Description : Inspirred by
 * https://imagej.nih.gov/ij/plugins/download/Jpeg_Writer.java
 * https://imagej.nih.gov/ij/plugins/download/Jpeg_Writer.java
 * Date: 2019
 ******************************************************************************/
package com.kulvait.kct.imagej.denfileopener;

import java.awt.EventQueue;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.plugin.PlugIn;

/**	Uses the JFileChooser from Swing to open one or more raw images.
         The "Open All Files in Folder" check box in the dialog is ignored. */
public class DenFileWritter implements PlugIn
{
    static private String directory;
    private File file;
    private FileInfo fi;

    public void run(String arg)
    {
        String path = arg;
        ImagePlus imp = WindowManager.getCurrentImage();
        if(imp == null)
        {
            IJ.noImage();
            return;
        }
        if(arg.equals(""))
        {
            SaveDialog sd = new SaveDialog("Save as DEN ...", imp.getTitle(), ".den");
            String dir = sd.getDirectory();
            String name = sd.getFileName();
            if(name == null)
                return;
            path = dir + name;
        }
        imp.startTiming();
        saveAsDEN(imp, path);
        IJ.showTime(imp, imp.getStartTime(), "Den Writter");
    }

    void saveAsDEN(ImagePlus imp, String path)
    {
        try
        {
            fi = imp.getFileInfo();
            if(fi.fileType == FileInfo.GRAY8 || fi.fileType == FileInfo.GRAY16_UNSIGNED
               || fi.fileType == FileInfo.GRAY32_FLOAT || fi.fileType == FileInfo.GRAY64_FLOAT)
            {
                OutputStream output = new BufferedOutputStream(new FileOutputStream(path));
                writeImageHeader(output);
                Object[] stack = (Object[])fi.pixels;
                for(int i = 0; i < fi.nImages; i++)
                {
                    IJ.showStatus("Writing: " + (i + 1) + "/" + fi.nImages);
                    switch(fi.fileType)
                    {
                    case FileInfo.GRAY16_UNSIGNED:
                        write16Image(output, (short[])stack[i]);
                        break;
                    case FileInfo.GRAY32_FLOAT:
                        writeFloatImage(output, (float[])stack[i]);
                        break;
                    case FileInfo.GRAY64_FLOAT:
                        writeDoubleImage(output, (double[])stack[i]);
                        break;
                    }
                    IJ.showProgress((double)(i + 1) / fi.nImages);
                }
                output.close();

            } else
            {
                IJ.error("Unrecognized file format!\n");
            }
        } catch(IOException e)
        {
            IJ.error("An error occured writing the file.\n \n" + e);
        }
    }

    void writeImageHeader(OutputStream out) throws IOException
    {
        byte[] buffer = new byte[6];
        int val;
        val = fi.width;
        buffer[0] = (byte)val;
        buffer[1] = (byte)(val >>> 8);
        val = fi.height;
        buffer[2] = (byte)val;
        buffer[3] = (byte)(val >>> 8);
        val = fi.nImages;
        buffer[4] = (byte)val;
        buffer[5] = (byte)(val >>> 8);
        out.write(buffer, 0, 6);
    }

    void write16Image(OutputStream out, short[] pixels) throws IOException
    {
        int bytesWritten = 0;
        int size = fi.width * fi.height * 2;
        int count = 8192;
        byte[] buffer = new byte[count];

        while(bytesWritten < size)
        {
            if((bytesWritten + count) > size)
                count = size - bytesWritten;
            int j = bytesWritten / 2;
            int value;
            // Little endian
            for(int i = 0; i < count; i += 2)
            {
                value = pixels[j];
                buffer[i] = (byte)value;
                buffer[i + 1] = (byte)(value >>> 8);
                j++;
            }
            out.write(buffer, 0, count);
            bytesWritten += count;
        }
    }

    void writeFloatImage(OutputStream out, float[] pixels) throws IOException
    {
        long bytesWritten = 0L;
        long size = 4L * fi.width * fi.height;
        int count = 8192;
        byte[] buffer = new byte[count];
        int tmp;
        while(bytesWritten < size)
        {
            if((bytesWritten + count) > size)
                count = (int)(size - bytesWritten);
            int j = (int)(bytesWritten / 4L);
            // Little endian
            for(int i = 0; i < count; i += 4)
            {
                tmp = Float.floatToRawIntBits(pixels[j]);
                buffer[i] = (byte)tmp;
                buffer[i + 1] = (byte)(tmp >> 8);
                buffer[i + 2] = (byte)(tmp >> 16);
                buffer[i + 3] = (byte)(tmp >> 24);
                j++;
            }
            out.write(buffer, 0, count);
            bytesWritten += count;
        }
    }

    void writeDoubleImage(OutputStream out, double[] pixels) throws IOException
    {
        long bytesWritten = 0L;
        long size = 8L * fi.width * fi.height;
        int count = 8192;
        byte[] buffer = new byte[count];
        long tmp;
        while(bytesWritten < size)
        {
            if((bytesWritten + count) > size)
                count = (int)(size - bytesWritten);
            int j = (int)(bytesWritten / 4L);
            // Little endian
            for(int i = 0; i < count; i += 8)
            {
                tmp = Double.doubleToRawLongBits(pixels[j]);
                buffer[i] = (byte)tmp;
                buffer[i + 1] = (byte)(tmp >> 8);
                buffer[i + 2] = (byte)(tmp >> 16);
                buffer[i + 3] = (byte)(tmp >> 24);
                buffer[i + 4] = (byte)(tmp >> 32);
                buffer[i + 5] = (byte)(tmp >> 40);
                buffer[i + 6] = (byte)(tmp >> 48);
                buffer[i + 7] = (byte)(tmp >> 56);
                j++;
            }
            out.write(buffer, 0, count);
            bytesWritten += count;
        }
    }

    public void openFiles()
    {
        try
        {
            EventQueue.invokeAndWait(new Runnable() {
                public void run()
                {
                    JFileChooser fc = new JFileChooser();
                    fc.setMultiSelectionEnabled(false);
                    if(directory == null)
                    {
                        directory = Prefs.getString(".options.denlastdir");
                    }
                    if(directory == null)
                    {
                        directory = OpenDialog.getLastDirectory();
                    }
                    if(directory == null)
                    {
                        directory = OpenDialog.getDefaultDirectory();
                    }
                    if(directory != null)
                    {
                        fc.setCurrentDirectory(new File(directory));
                        System.out.println(String.format("Directory is %s", directory));
                    } else
                    {
                        System.out.println("Directory is null");
                    }
                    int returnVal = fc.showOpenDialog(IJ.getInstance());
                    if(returnVal != JFileChooser.APPROVE_OPTION)
                        return;
                    file = fc.getSelectedFile();
                    directory = fc.getCurrentDirectory().getPath() + File.separator;
                    OpenDialog.setLastDirectory(directory);
                    Prefs.set("options.denlastdir", directory);
                    Prefs.savePreferences();
                    System.out.println(String.format("Storing directory %s.",
                                                     Prefs.getString(".options.denlastdir")));
                }
            });
        } catch(Exception e)
        {
        }
        if(file == null)
        {
            return;
        }
        List<Integer> dim = analyzeDenFile(file);
        long fileSize = file.length();
        FileInfo fi = new FileInfo();
        fi.fileFormat = FileInfo.RAW;
        fi.fileName = file.getName();
        fi.directory = directory;
        fi.width = dim.get(0);
        fi.height = dim.get(1);
        fi.offset = 6;
        fi.nImages = dim.get(2);
        fi.gapBetweenImages = 0;
        fi.intelByteOrder = true; // little endian
        fi.whiteIsZero = false; // can be adjusted
        long totalunits = (long)dim.get(0) * dim.get(1) * dim.get(2);
        if(totalunits == 0)
            throw new RuntimeException(
                String.format("One or more dimensions are zero x=%d, y=%d, z=%d", dim.get(0),
                              dim.get(1), dim.get(2)));
        if((fileSize - 6) % totalunits != 0)
            throw new RuntimeException(String.format(
                "Dimensions do not match actual size of file. The file %s is not in den format or is corrupted.",
                file.getPath()));
        long unitsize = (fileSize - 6) / totalunits;
        String typ;
        if(unitsize == 1)
        {
            fi.fileType = FileInfo.GRAY8;
            typ = "uint8";
        } else if(unitsize == 2)
        {
            fi.fileType = FileInfo.GRAY16_UNSIGNED;
            typ = "uint16";
        } else if(unitsize == 4)
        {
            fi.fileType = FileInfo.GRAY32_FLOAT;
            typ = "float32";
        } else if(unitsize == 8)
        {
            fi.fileType = FileInfo.GRAY64_FLOAT;
            typ = "float64";
        } else
        {
            throw new RuntimeException(String.format(
                "The unit size of %d of the file %s does not match any number storage format used in den files.",
                unitsize, file.getPath()));
        }
        System.out.println(String.format("%s with dimensions x=%d, y=%d, z=%d and type %s.",
                                         file.getPath(), dim.get(0), dim.get(1), dim.get(2), typ));
        FileOpener fo = new FileOpener(fi);
        ImagePlus img = fo.open(false);
        if(img != null)
        {
            if(IJ.getVersion().compareTo("1.50e") >= 0)
                img.setIJMenuBar(true);
            img.setZ(dim.get(2) / 2);
            img.resetDisplayRange();
            img.show();
        }
    }

    public static List<Integer> analyzeDenFile(File f)
    {
        try
        {
            RandomAccessFile df = new RandomAccessFile(f, "r");
            FileChannel inChannel = df.getChannel();
            MappedByteBuffer buffer
                = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, Math.min(1024, inChannel.size()));
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.load();
            int ysize = buffer.getShort();
            int xsize = buffer.getShort();
            int zsize = buffer.getShort();
            buffer.clear();
            inChannel.close();
            df.close();
            List<Integer> dimensions = new ArrayList<Integer>();
            dimensions.add(xsize);
            dimensions.add(ysize);
            dimensions.add(zsize);
            return dimensions;
        } catch(IOException e)
        {
            return null;
        }
    }
}
