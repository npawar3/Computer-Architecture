/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package implementation;

import baseclasses.PipelineRegister;
import baseclasses.PipelineStageBase;
import baseclasses.CpuCore;
import examples.FloatAddSubUnit;
import examples.FloatMul;
import examples.IntMul;
import examples.MemUnit;
import tools.InstructionSequence;
import utilitytypes.EnumOpcode;
import utilitytypes.IGlobals;
import utilitytypes.IPipeReg;
import utilitytypes.IPipeStage;
import utilitytypes.IRegFile;

import static utilitytypes.IProperties.*;
import utilitytypes.Logger;
import utilitytypes.RegisterFile;
import voidtypes.VoidRegister;

/**
 * This is an example of a class that builds a specific CPU simulator out of
 * pipeline stages and pipeline registers.
 * 
 * @author 
 */
public class MyCpuCore extends CpuCore {
    static final String[] producer_props = {RESULT_VALUE};
    
    public void initProperties() {
        properties = new GlobalData();
        IRegFile regfile = new RegisterFile(256);
        regfile.markPhysical();
        for(int i=0;i<32;i++) 
        	regfile.markUsed(i, true);
        for(int i=0;i<32;i++) {
        	GlobalData.rat[i]=i;
        }
        for(int i=0;i<256;i++) {
        	GlobalData.Table[i]="";
        }
       
        properties.setProperty("reg_file", regfile);
    }
    
    public void loadProgram(InstructionSequence program) {
        getGlobals().loadProgram(program);
    }
    
    public void runProgram() {
        properties.setProperty("running", true);
        int i=0;
        int j=0;
        while (properties.getPropertyBoolean("running")) {
        //  while(j<70) {
        	Logger.out.println("## Cycle number: " + cycle_number);
        	for(i=0;i<256;i++) {
        		if((!getGlobals().getRegisterFile().isInvalid(i))&&getGlobals().getRegisterFile().isUsed(i)&&getGlobals().getRegisterFile().isRenamed(i)) {
        			getGlobals().getRegisterFile().markUsed(i, false);
        			Logger.out.println("freeing:P"+i);
        		}
        	}            
            advanceClock();
            j++;
        }
    }

    @Override
    public void createPipelineRegisters() {
        createPipeReg("FetchToDecode");
        createPipeReg("DecodeToIQ");
        createPipeReg("IQToExecute");
        createPipeReg("IQToIntDiv");
        createPipeReg("IQToFloatDiv");
        createPipeReg("IQToMemory");
        //createPipeReg("DecodeToMemUnit");
        createPipeReg("IQToIntMul");
        createPipeReg("IQToFloatAddSub");//new
        createPipeReg("IQToFloatMul");
        createPipeReg("IntDivToWriteback");//new
        createPipeReg("FloatDivToWriteback");//new
        createPipeReg("ExecuteToWriteback");
        //createPipeReg("MemoryToWriteback");
    }

    @Override
    public void createPipelineStages() {
        addPipeStage(new AllMyStages.Fetch(this));
        addPipeStage(new AllMyStages.Decode(this));
        addPipeStage(new AllMyStages.IssueQueue(this));
        addPipeStage(new AllMyStages.Execute(this));
        addPipeStage(new IntDiv(this));
        addPipeStage(new FloatDiv(this));
        
        addPipeStage(new AllMyStages.Writeback(this));
    }

    @Override
    public void createChildModules() {
        // MSFU is an example multistage functional unit.  Use this as a
        // basis for FMul, IMul, and FAddSub functional units.
    	addChildUnit(new MemUnit(this, "MemUnit"));
        addChildUnit(new IntMul(this, "IntMul"));
        addChildUnit(new FloatAddSubUnit(this, "FloatAddSub"));
        addChildUnit(new FloatMul(this, "FloatMul"));
    }

    @Override
    public void createConnections() {
        // Connect pipeline elements by name.  Notice that 
        // Decode has multiple outputs, able to send to Memory, Execute,
        // or any other compute stages or functional units.
        // Writeback also has multiple inputs, able to receive from 
        // any of the compute units.
        // NOTE: Memory no longer connects to Execute.  It is now a fully 
        // independent functional unit, parallel to Execute.
        
        // Connect two stages through a pipelin register
        connect("Fetch", "FetchToDecode", "Decode");
        
        // Decode has multiple output registers, connecting to different
        // execute units.  
        // "MSFU" is an example multistage functional unit.  Those that
        // follow the convention of having a single input stage and single
        // output register can be connected simply my naming the functional
        // unit.  The input to MSFU is really called "MSFU.in".
        connect("Decode", "DecodeToIQ", "IssueQueue");//sequence
        connect("IssueQueue", "IQToExecute", "Execute");//sequence
        connect("IssueQueue", "IQToIntDiv", "IntDiv");
        //connect("Decode", "DecodeToMemUnit", "MemUnit");
        connect("IssueQueue", "IQToMemory", "MemUnit");
        connect("IssueQueue", "IQToIntMul", "IntMul");
        
        connect("IssueQueue", "IQToFloatDiv", "FloatDiv");
             
       
        
        connect("IssueQueue", "IQToFloatAddSub", "FloatAddSub");//new
        
        connect("IssueQueue", "IQToFloatMul", "FloatMul");//new
        
        // Writeback has multiple input connections from different execute
        // units.  The output from MSFU is really called "MSFU.Delay.out",
        // which was aliased to "MSFU.out" so that it would be automatically
        // identified as an output from MSFU.
        connect("IntDiv", "IntDivToWriteback","Writeback");
        connect("FloatDiv", "FloatDivToWriteback","Writeback");
        connect("Execute","ExecuteToWriteback", "Writeback");
        connect("IntMul", "Writeback");
        connect("FloatMul","Writeback");
        
        connect("FloatAddSub","Writeback");//new
        connect("MemUnit", "Writeback");
        //new
        //connect("Memory", "MemoryToWriteback", "Writeback");
        
    }

    @Override
    public void specifyForwardingSources() {
        addForwardingSource("ExecuteToWriteback");
       // addForwardingSource("MemoryToWriteback");
        
        addForwardingSource("IntDivToWriteback");
        addForwardingSource("FloatDivToWriteback");
        //addForwardingSource("MemoryToWriteback");
        // MSFU.specifyForwardingSources is where this forwarding source is added
        // addForwardingSource("MSFU.out");
        addForwardingSource("FloatAddSub.out");//new
        addForwardingSource("IntMul.out");
        addForwardingSource("FloatMul.out");
        addForwardingSource("MemUnit.out");
    }

    @Override
    public void specifyForwardingTargets() {
        // Not really used for anything yet
    }

    @Override
    public IPipeStage getFirstStage() {
        // CpuCore will sort stages into an optimal ordering.  This provides
        // the starting point.
        return getPipeStage("Fetch");
    }
    
    public MyCpuCore() {
        super(null, "core");
        initModule();
        printHierarchy();
        Logger.out.println("");
    }
}
