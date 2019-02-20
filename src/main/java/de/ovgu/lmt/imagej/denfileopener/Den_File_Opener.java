/*******************************************************************************
 * Project : Stimulate, OpenJ plugin to open in house den format
 * Author: Vojtěch Kulvait
 * Licence: GNU GPL3
 * Description : Modification of original file
 *https://imagej.nih.gov/ij/plugins/raw-file-opener.html.
 *Date: 2019
 ******************************************************************************/
package de.ovgu.lmt.imagej.denfileopener;

import ij.plugin.*;
import ij.*;
import ij.io.*;
import java.awt.EventQueue;
import java.io.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.nio.ByteOrder;

/**	Uses the JFileChooser from Swing to open one or more raw images.
	 The "Open All Files in Folder" check box in the dialog is ignored. */
public class Den_File_Opener implements PlugIn {
	static private String directory;
	private File file;

	public void run(String arg) {
		openFiles();
	}

	public void openFiles() {
		try {
			EventQueue.invokeAndWait(new Runnable() {
				public void run() {
					JFileChooser fc = new JFileChooser();
					fc.setMultiSelectionEnabled(false);
					File dir = null;
					if (directory == null) {
						directory = Prefs.getString(".options.denlastdir");
					}
					if (directory == null) {
						directory = OpenDialog.getLastDirectory();
					}
					if (directory == null) {
						directory = OpenDialog.getDefaultDirectory();
					}
					if (directory != null) {
						fc.setCurrentDirectory(new File(directory));
						System.out.println(
						    String.format("Directory is %s", directory));
					} else {
						System.out.println("Directory is null");
					}
					int returnVal = fc.showOpenDialog(IJ.getInstance());
					if (returnVal != JFileChooser.APPROVE_OPTION)
						return;
					file = fc.getSelectedFile();
					directory =
					    fc.getCurrentDirectory().getPath() + File.separator;
					OpenDialog.setLastDirectory(directory);
					Prefs.set("options.denlastdir", directory);
					Prefs.savePreferences();
					System.out.println(String.format("Storing directory %s.",
					    Prefs.getString(".options.denlastdir")));
				}
			});
		} catch (Exception e) {
		}
		if (file == null) {
			return;
		}
		List<Integer> dim = analyzeDenFile(file);
		long fileSize = file.length();
		FileInfo fi = new FileInfo();
		fi.fileFormat = fi.RAW;
		fi.fileName = file.getName();
		fi.directory = directory;
		fi.width = dim.get(0);
		fi.height = dim.get(1);
		fi.offset = 6;
		fi.nImages = dim.get(2);
		fi.gapBetweenImages = 0;
		fi.intelByteOrder = true; // little endian
		fi.whiteIsZero = false; // can be adjusted
		long totalunits = (long) dim.get(0) * dim.get(1) * dim.get(2);
		if (totalunits == 0)
			throw new RuntimeException(
			    String.format("One or more dimensions are zero x=%d, y=%d, z=%d",
				dim.get(0), dim.get(1), dim.get(2)));
		if ((fileSize - 6) % totalunits != 0)
			throw new RuntimeException(String.format(
			    "Dimensions do not match actual size of file. The file %s is not in den format or is corrupted.",
			    file.getPath()));
		long unitsize = (fileSize - 6) / totalunits;
		String typ;
		if (unitsize == 1) {
			fi.fileType = FileInfo.GRAY8;
			typ = "uint8";
		} else if (unitsize == 2) {
			fi.fileType = FileInfo.GRAY16_UNSIGNED;
			typ = "uint16";
		} else if (unitsize == 4) {
			fi.fileType = FileInfo.GRAY32_FLOAT;
			typ = "float32";
		} else if (unitsize == 8) {
			fi.fileType = FileInfo.GRAY64_FLOAT;
			typ = "float64";
		} else {
			throw new RuntimeException(String.format(
			    "The unit size of %d of the file %s does not match any number storage format used in den files.",
			    unitsize, file.getPath()));
		}
		System.out.println(String.format("%s with dimensions x=%d, y=%d, z=%d and type %s.",
		    file.getPath(), dim.get(0), dim.get(1), dim.get(2), typ));
		FileOpener fo = new FileOpener(fi);
		ImagePlus img = fo.open(false);
		if (img != null) {
			if (IJ.getVersion().compareTo("1.50e") >= 0)
				img.setIJMenuBar(true);
			img.setZ(dim.get(2) / 2);
			img.resetDisplayRange();
			img.show();
		}
	}

	public static List<Integer> analyzeDenFile(File f) {
		try {
			RandomAccessFile df = new RandomAccessFile(f, "r");
			FileChannel inChannel = df.getChannel();
			MappedByteBuffer buffer = inChannel.map(
			    FileChannel.MapMode.READ_ONLY, 0, Math.min(1024, inChannel.size()));
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
		} catch (IOException e) {
			return null;
		}
	}
}
