package jftf.demos.ProductionExample;

import jftf.lib.core.JftfVirtualMachineBuilder;

public class JftfTestBootstrapper {
    public static void main(String[] args){
        JftfVirtualMachineBuilder jftfVirtualMachineBuilder = new JftfVirtualMachineBuilder();
        jftfVirtualMachineBuilder.VirtualMachineFactory(args, JftfDemosProductionExample.class);
    }
}
