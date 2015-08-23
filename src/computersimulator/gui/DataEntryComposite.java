package computersimulator.gui;

import java.awt.Color;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * GUI element for inputting data or receiving load data. Configurable in size
 * for reuse.
 */
public class DataEntryComposite {
    private JPanel p;
    private JCheckBox[] inputBits; 
    private JLabel componentName;
    private int size;
    
    /**
     * Constructor
     * @param size number of inputBits
     * @param name Name of Field
     */
    public DataEntryComposite(int size, String name) {
        this.size = size;
        createDataEntryComposite(size, name);
    }
    
     /**
     * Create Data Entry Component with n elements
     * @param size  number of inputBits
     * @return returns inputBits
     */
    private JCheckBox[] createDataEntryComposite(int size, String name) {        
        p = new JPanel();           
        p.setBackground(Color.LIGHT_GRAY);
        
        componentName = new JLabel(name);
        p.add(componentName);
        
        // create inputBits
        inputBits = new JCheckBox[size];
        for (int i = 0; i < size; i++) {
            inputBits[i] = new JCheckBox("");
            p.add(inputBits[i]);
        }
        
        return inputBits;
    }
    
    /**
     * Get status of inputBits
     * @return the status of inputBits
     */
    public String getValueAsBinaryString() {  
        StringBuilder code = new StringBuilder();
        
        for (JCheckBox inputBit : inputBits) {
            if (inputBit.isSelected()) { 
                code.append("1");
            } else { 
                code.append("0");
            }
        }
        return new String(code);
    }
    
    /**
     * Update input to bin input
     * @param bin Binary String
     */
    public void setFromBinaryString(String bin) {    
        String binaryStr = bin;
        // Pad left with zeros
        while(binaryStr.length() < inputBits.length){
            binaryStr = "0" + binaryStr;
        }
        for(int i =0; i<binaryStr.length(); i++){
            char c = binaryStr.charAt(i);
            inputBits[i].setSelected(c=='1');
        }
    }
    
    /**
     * Reset state of checkboxes
     */
    public void resetToZero(){
         for (JCheckBox inputBit : inputBits) {
             inputBit.setSelected(false);
         }
    }

    /**
     * @return panel instance
     */
    public JComponent getGUI() {
        return p;
    }
    
   /**
     * Get the size of the component
     * @return size of the component
     */
    public int getSize() {
        return size;
    }
    
    /**
     * Set the size of the component
     * @param size 
     */
    public void setSize(int size) {
        this.size = size;
    }
}
