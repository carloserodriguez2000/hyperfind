/*
 *  HyperFind, an search application for the OpenDiamond platform
 *
 *  Copyright (c) 2008-2011 Carnegie Mellon University
 *  All rights reserved.
 *
 *  HyperFind is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 2.
 *
 *  HyperFind is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with HyperFind. If not, see <http://www.gnu.org/licenses/>.
 *
 *  Linking HyperFind statically or dynamically with other modules is
 *  making a combined work based on HyperFind. Thus, the terms and
 *  conditions of the GNU General Public License cover the whole
 *  combination.
 *
 *  In addition, as a special exception, the copyright holders of
 *  HyperFind give you permission to combine HyperFind with free software
 *  programs or libraries that are released under the GNU LGPL or the
 *  Eclipse Public License 1.0. You may copy and distribute such a system
 *  following the terms of the GNU GPL for HyperFind and the licenses of
 *  the other code concerned, provided that you include the source code of
 *  that other code when and as the GNU GPL requires distribution of source
 *  code.
 *
 *  Note that people who make modified versions of HyperFind are not
 *  obligated to grant this special exception for their modified versions;
 *  it is their choice whether to do so. The GNU General Public License
 *  gives permission to release a modified version without this exception;
 *  this exception also makes it possible to release a modified version
 *  which carries forward this exception.
 */

package edu.cmu.cs.diamond.hyperfind;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import edu.cmu.cs.diamond.opendiamond.Util;

public class SearchSettingsFrame extends JFrame {

    private List<ChangeListener> listeners = new ArrayList<ChangeListener>();

    private JComponent content;

    private StringField instanceNameField;

    private SpinnerField thresholdField;

    private ArrayList<JComponent> arguments = new ArrayList<JComponent>();

    private int currentRow;

    public SearchSettingsFrame(String filterName, String instanceName,
            boolean instanceEditable, int threshold,
            boolean thresholdEditable) {
        super("Edit " + filterName);

        setResizable(false);
        content = (JComponent) getContentPane();
        content.setLayout(new GridBagLayout());

        final SearchSettingsFrame frame = this;

        // Close button
        JButton close_button = new JButton("Close");
        close_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.setVisible(false);
            }
        });
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = currentRow++;
        c.anchor = GridBagConstraints.LINE_START;
        c.insets = new Insets(2, 2, 2, 2);
        content.add(close_button, c);

        // Filter name.  Always create the field, sometimes display it.
        instanceNameField = new StringField(this, instanceName);
        if (instanceEditable) {
            addField("Filter name", instanceNameField);
        }

        // Threshold.  Always create the field, sometimes display it.
        thresholdField = new SpinnerField(this, new Double(threshold),
                new Double(0), null, 1);
        if (thresholdEditable) {
            addField("Threshold", thresholdField);
        }

        pack();
    }

    private void addField(String label, JComponent field) {
        // Add label
        JLabel l = new JLabel(label + ":");
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = currentRow;
        c.insets = new Insets(2, 2, 2, 2);
        c.anchor = GridBagConstraints.LINE_START;
        content.add(l, c);

        // Add field
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = currentRow;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(2, 2, 2, 2);
        c.anchor = GridBagConstraints.LINE_START;
        content.add(field, c);

        // Update state
        currentRow++;
        pack();
    }

    private void addArgumentField(String label, JComponent field) {
        // If this is the first argument field, we need a separator
        if (arguments.size() == 0) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = currentRow++;
            c.gridwidth = 2;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(3, 0, 3, 0);
            content.add(new JSeparator(), c);
        }

        addField(label, field);
        arguments.add(field);
    }

    private class BooleanField extends JCheckBox {

        public BooleanField(final SearchSettingsFrame settings, boolean defl) {
            setSelected(defl);
            addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    settings.fireChangeEvent();
                }
            });
        }

        @Override
        public String toString() {
            return (getSelectedObjects() != null) ? "true" : "false";
        }
    }

    public void addBoolean(String label, boolean defl) {
        addArgumentField(label, new BooleanField(this, defl));
    }

    private class StringField extends JTextField {
        private final int FIELD_WIDTH = 15;

        public StringField(final SearchSettingsFrame settings, String defl) {
            super(defl);
            setColumns(FIELD_WIDTH);
            getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    settings.fireChangeEvent();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    settings.fireChangeEvent();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    settings.fireChangeEvent();
                }
            });
        }

        @Override
        public String toString() {
            // Base64-encode the string to get around fspec parser problems.
            // Zero-length strings become "*" for the same reason.
            if (getText().equals("")) {
                return "*";
            } else {
                return Util.base64Encode(getText().getBytes());
            }
        }
    }

    public void addString(String label, String defl) {
        addArgumentField(label, new StringField(this, defl));
    }

    private class SpinnerField extends JSpinner {

        public SpinnerField(final SearchSettingsFrame settings, Double defl,
                Double min, Double max, double increment) {

            if (defl == null) {
                defl = new Double(0);
                if (min != null && defl.compareTo(min) < 0) {
                    defl = min;
                } else if (max != null && defl.compareTo(max) > 0) {
                    defl = max;
                }
            }

            SpinnerNumberModel model = new SpinnerNumberModel(defl, min,
                    max, new Double(increment));
            setModel(model);

            addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    settings.fireChangeEvent();
                }
            });
        }

        @Override
        public String toString() {
            double d = ((Double) getValue()).doubleValue();
            int i = (int) d;
            if (d == i) {
                // Avoid trailing .0 if possible
                return Integer.toString(i);
            } else {
                return Double.toString(d);
            }
        }
    }

    public void addNumber(String label, Double defl, Double min, Double max,
            double increment) {
        addArgumentField(label, new SpinnerField(this, defl, min, max,
                increment));
    }

    private class ChoiceField extends JComboBox {
        public ChoiceField(final SearchSettingsFrame settings,
                List<String> choices, Integer defl) {
            super(choices.toArray());
            if (defl != null) {
                setSelectedIndex(defl.intValue());
            }
            addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    settings.fireChangeEvent();
                }
            });
        }

        @Override
        public String toString() {
            return Integer.toString(getSelectedIndex());
        }
    }

    public void addChoice(String label, List<String> choices, Integer defl) {
        if (choices.size() == 0) {
            throw new IllegalArgumentException("No choices");
        }
        if (defl != null && defl.intValue() >= choices.size()) {
            throw new IllegalArgumentException("Default out of range");
        }
        addArgumentField(label, new ChoiceField(this, choices, defl));
    }

    public List<String> getFilterArguments() {
        List<String> ret = new ArrayList<String>();
        for (JComponent arg : arguments) {
            ret.add(arg.toString());
        }
        return ret;
    }

    public String getInstanceName() {
        return instanceNameField.getText();
    }

    public int getThreshold() {
        Double val = (Double) thresholdField.getValue();
        return val.intValue();
    }

    public boolean isEditable() {
        // we are editable if the instance name or threshold is, or if
        // we have editable arguments
        return instanceNameField.isDisplayable() ||
                thresholdField.isDisplayable() ||
                arguments.size() > 0;
    }

    public void addChangeListener(ChangeListener l) {
        listeners.add(l);
    }

    public void removeChangeListener(ChangeListener l) {
        listeners.remove(l);
    }

    private void fireChangeEvent() {
        ChangeEvent ev = new ChangeEvent(this);
        for (ChangeListener l : listeners) {
            l.stateChanged(ev);
        }
    }

    private static String getProperty(Properties p, String label, int i) {
        String key = new Formatter().format("%s-%d", label, i).toString();
        return p.getProperty(key);
    }

    private static String getProperty(Properties p, String label, int i,
            int j) {
        String key = new Formatter().format("%s-%d-%d", label, j,
                i).toString();
        return p.getProperty(key);
    }

    /* Accepted properties:
       Instance: the default filter instance name (optional)
       Instance-Editable: "false" if the instance name should not be editable
           (optional, and should usually be omitted)
       Threshold: the Diamond drop threshold
       Threshold-Editable: "true" if threshold should be editable (optional)

       In addition, there can be parameter descriptions, arranged in a
       zero-indexed array.  Each parameter corresponds to a single argument
       to the server-side filter code.  Let II be the array index.
       Properties:
       Type-II: {boolean, string, number, choice}
       Label-II: field label for this argument
       Default-II: default value (optional)

       boolean produces a filter argument of "true" or "false".  The Default
       value, if any, should be "true" or "false".

       string produces a filter argument consisting of a Base64-encoded string
       (to get around fspec parsing issues).  The Default value, if any,
       should not be Base64-encoded.  Empty strings are encoded into a
       filter argument of "*", again to work around the fspec parser.

       number produces a filter argument consisting of the text
       representation of the selected number.  Additional properties:
       Minimum-II: minimum value (optional)
       Maximum-II: maximum value (optional)
       Increment-II: distance between acceptable values (optional, defaults
           to 1)

       choice displays a drop-down menu of options.  There are additional
       properties listing these options as a zero-indexed array.  Let the
       index be JJ.  Properties:
       Choice-JJ-II: text label of the menu entry
       The filter argument consists of the text representation of JJ.
    */
    public static SearchSettingsFrame createFromProperties(String filterName,
            Properties p) {
        String instanceName = p.getProperty("Instance");
        if (instanceName == null) {
            instanceName = "filter";
        }

        String instanceEditableStr = p.getProperty("Instance-Editable");
        boolean instanceEditable = instanceEditableStr == null ||
                ! instanceEditableStr.equals("false");

        int threshold = Integer.parseInt(p.getProperty("Threshold"));

        String threshEditableStr = p.getProperty("Threshold-Editable");
        boolean threshEditable = threshEditableStr != null &&
                threshEditableStr.equals("true");

        SearchSettingsFrame fr = new SearchSettingsFrame(filterName,
                instanceName, instanceEditable, threshold, threshEditable);

        for (int i = 0; ; i++) {
            String type = getProperty(p, "Type", i);
            if (type == null) {
                break;
            }

            String label = getProperty(p, "Label", i);
            if (label == null) {
                throw new IllegalArgumentException(
                        "Missing label for option " + i);
            }

            String defl = getProperty(p, "Default", i);

            if (type.equals("boolean")) {
                fr.addBoolean(label, defl != null && defl.equals("true"));
            } else if (type.equals("string")) {
                fr.addString(label, defl != null ? defl : "");
            } else if (type.equals("number")) {
                String min = getProperty(p, "Minimum", i);
                String max = getProperty(p, "Maximum", i);
                String increment = getProperty(p, "Increment", i);
                Double f_defl = (defl != null) ? new Double(defl) : null;
                Double f_min = (min != null) ? new Double(min) : null;
                Double f_max = (max != null) ? new Double(max) : null;
                double f_increment = (increment != null) ?
                        Double.parseDouble(increment) : 1;
                fr.addNumber(label, f_defl, f_min, f_max, f_increment);
            } else if (type.equals("choice")) {
                List<String> choices = new ArrayList<String>();
                Integer i_defl = (defl != null) ? new Integer(defl) : null;
                for (int j = 0; ; j++) {
                    String cur = getProperty(p, "Choice", i, j);
                    if (cur == null) {
                        break;
                    }
                    choices.add(cur);
                }
                fr.addChoice(label, choices, i_defl);
            } else {
                throw new IllegalArgumentException("Unknown type " + type);
            }
        }

        return fr;
    }

    public static void main(final String args[]) throws FileNotFoundException,
            UnsupportedEncodingException, IOException {
        if (args.length != 1) {
            System.out.println("Usage: " +
                    SearchSettingsFrame.class.getName() + " manifest-file");
            System.exit(1);
        }

        final Properties p = new Properties();
        FileInputStream in = new FileInputStream(args[0]);
        Reader r = new InputStreamReader(in, "UTF-8");
        p.load(r);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                String name = p.getProperty("Filter");
                SearchSettingsFrame fr = createFromProperties(name, p);
                fr.addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentHidden(ComponentEvent e) {
                        SearchSettingsFrame fr = (SearchSettingsFrame)
                                e.getSource();
                        System.out.println("Instance: " +
                                fr.getInstanceName());
                        System.out.println("Threshold: " + fr.getThreshold());
                        System.out.println("Arguments:");
                        for (String arg : fr.getFilterArguments()) {
                            System.out.println(arg);
                        }
                        System.exit(0);
                    }
                });
                fr.setVisible(true);
            }
        });
    }
}