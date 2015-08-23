package computersimulator.gui;

import computersimulator.components.*;
import computersimulator.cpu.Computer;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * OperatorConsole should include:
 Display all registers
 Display machine status and condition registers
      Displays:
          * Current Memory Address
          * Various Registers (as mentioned above)
          * Sense Switches (?) to inmainWindoworm the program  (relates to I/O). One DEVID accesses one sense switch.
 * An IPL button (to start the simulation)  @TODO: IPL will be added once we're in part 2
 * Switches (simulated as checkboxes) to load data into registers, select displays, and initiate certain conditions in the machine.
 * Numeric pad 
 */

public class OperatorConsole implements Runnable {

    private Computer computer;

    // HashMap of Visual Components
    private HashMap<String, DataDisplayComposite> displayComponents;

    // Data Entry Widget
    private DataEntryComposite input;

    private JFrame mainWindow;

    private JPanel leftPanel;
    private JPanel rightPanel;

    public void setComputer(Computer computer) {
        this.computer = computer;
    }

    public void createComponent(String name, boolean edit) throws Exception {
        DataDisplayComposite widget = new DataDisplayComposite(this.computer, name, edit);
        this.displayComponents.put(name, widget);
        //mainWindow.add(widget.getGUI());
        leftPanel.add((widget.getGUI()));

    }

    @Override
    public void run() {
        displayComponents = new HashMap<>();

        // Create the window
        mainWindow = new JFrame("Group 3 Computer Simulator: Operator Console");
        mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create the left pane
        leftPanel = new JPanel();

        // Create the right pane
        rightPanel = new JPanel();

        // create panel to hold all components
        JPanel labelHolder = new JPanel();
        // create title
        JLabel title = new JLabel("Operator Console");
        Font font = new Font("Verdana", Font.BOLD, 14);
        title.setFont(font);
        title.setForeground(Color.BLACK);
        labelHolder.add(title);
        //mainWindow.add(labelHolder);
        leftPanel.add(labelHolder);

        // Create grid layout for main window - main layout will include 2 columns: 
        // left column will hold components and right column will hold the numeric pad
        //GridLayout layout = new GridLayout(15, 1, 15, 5);
        GridLayout layout = new GridLayout(1, 2, 10, 5);
        //layout.setVgap(1);
        mainWindow.setLayout(layout);

        // create grid layout - each component/register will be placed as a separate line
        GridLayout leftLayout = new GridLayout(15, 1, 15, 5);
        leftPanel.setLayout(leftLayout);

        // create grid layout - it will hold the numberic pad
        GridLayout rightLayout = new GridLayout(4, 1);
        rightPanel.setLayout(rightLayout);

        // Create simulator components and initialize the initial state         
        try {
            createComponent("R0", true);
            createComponent("R1", true);
            createComponent("R2", true);
            createComponent("R3", true);

            createComponent("X1", true);
            createComponent("X2", true);
            createComponent("X3", true);

            createComponent("MAR", true);
            createComponent("MBR", true);

            createComponent("PC", true);
            createComponent("CC", false);

            createComponent("IR", true);

        } catch (Exception err) {
            System.out.println("Error: " + err);
        }
        // add left panel to main window
        mainWindow.add(leftPanel);

        input = new DataEntryComposite(20, "Input");
        //mainWindow.add(input.getGUI());
        leftPanel.add(input.getGUI());

        // create button panel with all buttons
        JPanel buttonPanel = new JPanel();
        // create buttons
        JButton start = new JButton("IPL");
        JButton load = new JButton("Load");
        JButton deposit = new JButton("Deposit");
        JButton step = new JButton("Step");
        //JButton stop = new JButton("Stop");

        // buttonPanel.add(start);
        buttonPanel.add(load);
        buttonPanel.add(deposit);
        buttonPanel.add(step);
        //buttonPanel.add(stop);

        // add button panel to frame
        //mainWindow.add(buttonPanel);
        leftPanel.add(buttonPanel);

        // add title to right pane
        JPanel rightLabelHolder = new JPanel();
        // create title
        JLabel rightTitle = new JLabel("Numeric Pad");
        rightTitle.setFont(new Font("Verdana", Font.BOLD, 14));
        rightTitle.setForeground(Color.BLACK);
        rightLabelHolder.add(rightTitle);
        rightPanel.add(rightLabelHolder);
        
        // add text area
        JTextArea textArea = new JTextArea(5, 20);
        JScrollPane scrollPane = new JScrollPane(textArea);
        rightPanel.add(scrollPane);

        // add numeric pad component to right pane
        PadComposite pad = new PadComposite();
        pad.createComposite();
        rightPanel.add(pad.getGUI());
        
        // add empty area
        JLabel rightEmpty = new JLabel("");
        rightPanel.add(rightEmpty);

        //mainWindow.add(pad.getGUI());
        mainWindow.add(rightPanel);

        // add listeners
        final OperatorConsole opconsole = this;

        // Start Button
        start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                opconsole.updateDisplay();
            }
        });

        // deposit
        deposit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                String valueToDeposit = input.getValueAsBinaryString();
                Unit unitToDeposit = Unit.UnitFromBinaryString(valueToDeposit);
                input.resetToZero();

                for (Map.Entry<String, DataDisplayComposite> el : displayComponents.entrySet()) {
                    DataDisplayComposite widget = el.getValue();

                    // If Widget is checked, it is receiving the deposit
                    if (widget.isChecked()) {
                        computer.setComponentValueByName(widget.getName(), unitToDeposit);
                        System.out.println("Deposit s" + unitToDeposit.getSignedValue() + "/u" + unitToDeposit.getUnsignedValue() + " to " + widget.getName());
                        widget.uncheck();
                        widget.updateDisplay();
                    }
                }
            }
        });

        // load
        load.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                input.resetToZero();

                String loadStr = "";
                for (Map.Entry<String, DataDisplayComposite> el : displayComponents.entrySet()) {
                    DataDisplayComposite widget = el.getValue();

                    // If Widget is checked, it is receiving the deposit
                    if (widget.isChecked()) {
                        loadStr = widget.getSource().getBinaryString();
                        widget.uncheck();
                    }
                }
                input.setFromBinaryString(loadStr);

                System.out.println("Load Requested: " + loadStr);
            }
        });

        // step
        step.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    computer.clockCycle();
                    opconsole.updateDisplay();
                } catch (HaltSystemException eHalt) {
                    System.out.println("System HALT.");
                } catch (Exception err) {
                    System.out.println("Error: " + err);
                }
            }
        });

        this.updateDisplay();

        mainWindow.pack();
        mainWindow.setVisible(true);
    }

    public void updateDisplay() {
        for (Map.Entry<String, DataDisplayComposite> el : displayComponents.entrySet()) {
            DataDisplayComposite widget = el.getValue();
            widget.updateDisplay();
        }
    }

}
