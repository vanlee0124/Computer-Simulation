package computersimulator.cpu;

/**
 * The CPU class primarily represents a placeholder for ALU and the ControlUnit.
 * It also passes the reference to memory to symbolize that communication buffer.
 */
public class CentralProcessingUnit implements IClockCycle {
    
    private ControlUnit controlUnit;
    private ArithmeticLogicUnit alu;
    private MemoryControlUnit memory;
    

    public CentralProcessingUnit(MemoryControlUnit mem) {        
        this.memory = mem;
        alu = new ArithmeticLogicUnit();
        
        controlUnit = new ControlUnit(this.memory, this.alu);   
        alu.setControlUnit(controlUnit); // exchange reference
        
        
    }
    
    /**
     * Clock cycle. This is the main function which causes the CPU to do work.
     *  This serves as a publicly accessible method, but delegates
     * to the ALU/ControlUnit.
     * @throws java.lang.Exception
     */
    @Override
    public void clockCycle() throws Exception{
        this.controlUnit.clockCycle();
        this.alu.clockCycle();
    }           

    public ControlUnit getControlUnit() {
        return controlUnit;
    }

    public ArithmeticLogicUnit getALU() {
        return alu;
    }
    
   
    
    
}


