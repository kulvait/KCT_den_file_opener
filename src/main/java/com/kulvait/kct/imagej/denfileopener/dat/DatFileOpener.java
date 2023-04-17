/****************************************************************************
 * Project : KCT ImageJ plugin to open DEN files
 * Author: Vojtěch Kulvait
 * Licence: GNU GPL3
 * Description : Based on the implementation of the raw file opener for ImageJ
 * https://imagej.nih.gov/ij/plugins/raw-file-opener.html.
 * Date: 2019-2023
 ******************************************************************************/
package com.kulvait.kct.imagej.denfileopener.dat;

import com.kulvait.kct.imagej.denfileopener.DenDataType;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;

/**	Uses the JFileChooser from Swing to open one or more raw images.
         The "Open All Files in Folder" check box in the dialog is ignored. */
public class DatFileOpener implements PlugIn
{
    static private String directory;
    private File file;
    private CheckBoxDat cba;

    public void run(String arg)
    {
        try
        {
            boolean useVirtualStack;
            boolean fileSelected = true;
            if(arg.equals(""))
            {
                if(openFilesDialog() == false)
                {
                    return;
                }
                useVirtualStack = cba.isBoxSelected();
            } else
            {
                file = new File(arg);
                useVirtualStack = true;
            }
            openDat(useVirtualStack);
        } catch(IOException e)
        {
            System.out.printf("%s ERROR", e.toString());
        }
    }

    public boolean openFilesDialog() throws IOException
    {
        try
        {
            EventQueue.invokeAndWait(new Runnable() {
                public void run()
                {
                    JFileChooser fc = new JFileChooser();
                    fc.setDialogTitle("Open DAT file...");
                    fc.setAccessory(new CheckBoxDat(fc));
                    fc.setMultiSelectionEnabled(false);
                    if(directory == null)
                    {
                        directory = Prefs.getString(".options.datlastdir");
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
                    {
                        return;
                    }
                    file = fc.getSelectedFile();
                    cba = (CheckBoxDat)fc.getAccessory();
                    directory = fc.getCurrentDirectory().getPath() + File.separator;
                }
            });
        } catch(Exception e)
        {
            return false;
        }
        if(file == null || cba == null)
        {
            return false;
        }
        return true;
    }

    private void openDat(boolean useVirtualStack) throws IOException
    {
        DatFileInfo inf = new DatFileInfo(file);
        if(!inf.isValidDAT())
        {
            throw new RuntimeException(String.format("File %s is not valid DAT!", file.getName()));
        } else
        {
            // OpenDialog.setLastDirectory(directory);
            Prefs.set("options.datlastdir", directory);
            Prefs.savePreferences();
            System.out.println(
                String.format("Storing directory %s.", Prefs.getString(".options.datlastdir")));
        }
        FileInfo fi = new FileInfo();
        fi.fileFormat = FileInfo.RAW;
        fi.fileName = file.getName();
        fi.directory = directory;
        fi.width = (int)inf.getDimx();
        fi.height = (int)inf.getDimy();
        fi.offset = (int)inf.getDataByteOffset();
        fi.nImages = (int)inf.getDimz();
        fi.gapBetweenImages = 0;
        fi.intelByteOrder = true; // little endian
        fi.whiteIsZero = false; // can be adjusted
        DenDataType typ = inf.getElementType();
        if(typ == DenDataType.UINT8)
        {
            fi.fileType = FileInfo.GRAY8;
        } else if(typ == DenDataType.UINT16)
        {
            fi.fileType = FileInfo.GRAY16_UNSIGNED;
        } else if(typ == DenDataType.FLOAT32)
        {
            fi.fileType = FileInfo.GRAY32_FLOAT;
        } else if(typ == DenDataType.FLOAT64)
        {
            fi.fileType = FileInfo.GRAY64_FLOAT;
        } else if(typ == DenDataType.UINT32)
        {
            fi.fileType = FileInfo.GRAY32_UNSIGNED;
        } else
        {
            throw new RuntimeException(
                String.format("The type %s is not implemented yet!", typ.name()));
        }
        ImagePlus img;
        if(useVirtualStack)
        {
            img = new ImagePlus(file.getName(), new DatVirtualStack(file));
        } else
        {
            FileOpener fo = new FileOpener(fi);
            img = fo.open(false);
        }
        if(img != null)
        {
            if(IJ.getVersion().compareTo("1.50e") >= 0)
                img.setIJMenuBar(true);
            img.show();
            img.setZ((int)((inf.getDim(2) + 1) / 2));
            img.updateAndDraw();
        }
    }
}
