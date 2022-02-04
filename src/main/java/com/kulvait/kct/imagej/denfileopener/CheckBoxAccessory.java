/*******************************************************************************
 * Project : KCT ImageJ plugin to open DEN files
 * Author: Vojtěch Kulvait
 * Licence: GNU GPL3
 * Description : Class to add preview of DEN file to JFileChooser
 * Date: 2022
 ******************************************************************************/
package com.kulvait.kct.imagej.denfileopener;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextArea;

public class CheckBoxAccessory extends JComponent implements PropertyChangeListener
{

    private static final long serialVersionUID = 1L;

    File selectedFile = null;

    JLabel nameInfo;
    JLabel typeInfo;
    JLabel dimInfo;
    JLabel noInfo;
    JLabel debugInfo;
    JCheckBox virtualCheckBox;
    boolean checkBoxInit = false;
    int preferredWidth = 150;
    int preferredHeight = 100; // Mostly ignored as it is
    int checkBoxPosX = 5;
    int checkBoxPosY = 20;
    int checkBoxWidth = preferredWidth;
    int checkBoxHeight = 20;

    public CheckBoxAccessory()
    {
        setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        setLayout(new GridBagLayout());
        // Component initialization
        nameInfo = new JLabel();
        typeInfo = new JLabel();
        dimInfo = new JLabel();
        debugInfo = new JLabel();
        noInfo = new JLabel();
        virtualCheckBox = new JCheckBox("Virtual stack", checkBoxInit);
        virtualCheckBox.setBounds(checkBoxPosX, checkBoxPosY, checkBoxWidth, checkBoxHeight);

        JTextArea abc = new JTextArea();
        abc.setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        abc.setLineWrap(true);
        abc.setWrapStyleWord(true);
        abc.setEditable(false);
        abc.setText("DEN.");

        // Positioning
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.ipady = 10;
        // gbc.insets = new Insets(0, 0, 0, 0);
        // c.gridwidth = GridBagConstraints.REMAINDER;
        // c.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        this.add(nameInfo, gbc);
        gbc.gridy = 1;
        gbc.ipady = 0;
        this.add(typeInfo, gbc);
        gbc.gridy = 2;
        this.add(dimInfo, gbc);
        gbc.gridy = 3;
        gbc.ipady = 10;
        this.add(virtualCheckBox, gbc);
        gbc.gridy = 4;
        gbc.weighty = 1;
        this.add(noInfo, gbc);
        gbc.gridy = 5;
        gbc.weighty = 0;
        this.add(debugInfo, gbc);
        debugInfo.setVisible(false);
        this.setVisible(false);
    }

    public CheckBoxAccessory(JFileChooser fc)
    {
        this();
        fc.addPropertyChangeListener(this);
    }

    public boolean isBoxSelected() { return virtualCheckBox.isSelected(); }

    public void propertyChange(PropertyChangeEvent e)
    {
        boolean update = false;
        String prop = e.getPropertyName();

        // If the directory changed, don't show a preview
        if(JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(prop))
        {
            selectedFile = null;
            nameInfo.setText("N/A");
            this.setVisible(false);
            update = true;
            // If a file became selected, find out which one.
        } else if(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(prop))
        {
            this.setVisible(true);
            selectedFile = (File)e.getNewValue();
            if(selectedFile != null)
            {
                updateInfo(selectedFile);
                update = true;
            } else
            {
                this.setVisible(false);
            }
        } else
        {
            // JOptionPane.showMessageDialog(null, String.format("property=%s", prop));
        }

        // Update the preview accordingly.
        if(update && isShowing())
        {
            repaint();
        }
    }

    public void updateInfo(File f)
    {
        DenFileInfo inf = new DenFileInfo(f);
        if(inf.isValidDEN())
        {
            if(inf.isExtendedDEN())
            {
                nameInfo.setText("Extended DEN.");
            } else
            {
                nameInfo.setText("Legacy DEN.");
            }
            int DIMCOUNT = inf.getDIMCOUNT();
            typeInfo.setText(String.format("%dD %s", DIMCOUNT, inf.getElementType().name()));
            String dimString;
            if(DIMCOUNT == 0)
            {
                dimString = "Empty dim";
            } else
            {
                dimString = String.format("%d", inf.getDim(0));
                for(int i = 1; i < DIMCOUNT; i++)
                {
                    dimString = String.format("%sx%d", dimString, inf.getDim(i));
                }
            }
            if(DIMCOUNT > 2)
            {
                virtualCheckBox.setVisible(true);
            }
            dimInfo.setText(dimString);
        } else
        {
            nameInfo.setText("Not DEN");
            typeInfo.setText("");
            dimInfo.setText("");
            virtualCheckBox.setVisible(false);
        }
    }

    protected void paintComponent(Graphics g)
    {
        int componentWidth = getWidth();
        int componentHeight = getHeight();
        String debugStr = String.format("w=%d, h=%d", componentWidth, componentHeight);
        // g.drawString(debugStr, 5, 10);
        debugInfo.setText(debugStr);
    }
}
