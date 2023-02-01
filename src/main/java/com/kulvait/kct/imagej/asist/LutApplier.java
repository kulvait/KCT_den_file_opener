/*******************************************************************************
 * Project : KCT ImageJ plugin to open DEN files
 * Author: Vojtěch Kulvait
 * Licence: GNU GPL3
 * Description : Based on the implementation of the raw file opener for ImageJ
 * https://imagej.nih.gov/ij/plugins/raw-file-opener.html.
 * Date: 2019-2022
 ******************************************************************************/
package com.kulvait.kct.imagej.asist;

import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.OpenDialog;
import ij.plugin.LutLoader;
import ij.plugin.PlugIn;
import ij.process.LUT;
import java.awt.EventQueue;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class LutApplier implements PlugIn
{

    public void run(String arg)
    {

        if(arg.equals("asist"))
        {
            applyAsist();
        }
        if(arg.equals("grayscale"))
        {
            applyGrayscale();
        }
    }

    public void applyAsist()
    {
        try
        {
            byte[] r = new byte[256];
            byte[] g = new byte[256];
            byte[] b = new byte[256];
            InputStream in = getClass().getResourceAsStream("/asist.lut");
            IndexColorModel lf = LutLoader.open(in);
            lf.getReds(r);
            lf.getGreens(g);
            lf.getBlues(b);
            LUT lut = new LUT(r, g, b);
            WindowManager.getCurrentImage().setLut(lut);
        } catch(IOException e)
        {
            System.out.printf("%s ERROR", e.toString());
        }
    }

    public void applyGrayscale()
    {
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];
        for(int i = 0; i != 256; i++)
        {
            r[i] = (byte)i;
            g[i] = (byte)i;
            b[i] = (byte)i;
        }
        LUT lut = new LUT(r, g, b);
        WindowManager.getCurrentImage().setLut(lut);
    }
}
