package computersimulator.gui;

import computersimulator.components.Unit;
import computersimulator.cpu.Computer;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Composite GUI Element for Displaying Data. This also contains an optional 
 * input element that allows it to be a target of a deposit or load.  Each 
 * DataDisplayComposite maintains a reference to computer for querying for the 
 * most recent data.  Since this reference can become outdated, it must continually
 * query for the latest.
 */
public class DataDisplayComposite {
    private Computer computer;
    private JPanel p;
    private JLabel [] bits;
    private JCheckBox checkBox;  
    private String name;

    /**
     * Constructor
     * @param computer Used for lookups
     * @param name composite name
     * @param edit
     */
    public DataDisplayComposite(Computer computer, String name, boolean edit) {
        this.name = name;
        this.computer = computer;
                
        int size = computer.getComponentValueByName(name).getSize();
        
        p = new JPanel();
        p.setBackground(Color.WHITE);
        
        this.checkBox = new JCheckBox();
        if(edit){ // checkbox for use with Deposit, only visible if edited                        
            p.add(this.checkBox);
            p.add(new JLabel(" ")); // add empty placeholder between checkbox and bits
        }
        
        // component name
        p.add(new JLabel(name));    
        
        // Labels used for Display of Bits
        bits = new JLabel[size];
        for (int i = 0; i < size; i++) {
            bits[i] = new JLabel();
            bits[i].setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            bits[i].setBackground(Color.gray);
            bits[i].setOpaque(true);
            bits[i].setPreferredSize(new Dimension(15, 15));
            p.add(bits[i]);
        }                
    }
    
    /**
     * Updates display
     */
    public void updateDisplay(){
        Integer[] data = this.getSource().getBinaryArray();
        
        for(int i = 0; i<data.length; i++){
            bits[i].setBackground((data[i]==1) ? Color.red : Color.gray);
        }
    }   
    
    /**
     * @return state of checkbox
     */
    public boolean isChecked(){
        return this.checkBox.isSelected();
    }
    
    /**
     * Unselect check box
     */
    public void uncheck(){
        this.checkBox.setSelected(false);
    }
    
    /**
     * Select check box 
     */
    public void check(){
        this.checkBox.setSelected(true);
    }
    
    /**
     * @return name
     */
    public String getName() {
        return name;
    }
    
    /**
     * 
     * @return source
     */
    public Unit getSource() {
        return this.computer.getComponentValueByName(this.name);
    }
    
    /**
     * @return panel instance
     */ 
    public JComponent getGUI() {
        return p;
    }    
}
