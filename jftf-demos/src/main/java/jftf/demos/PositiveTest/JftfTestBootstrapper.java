package jftf.demos.PositiveTest;

import jftf.lib.core.JftfVirtualMachineBuilder;

public class JftfTestBootstrapper {
    public static void main(String[] args){
        JftfVirtualMachineBuilder jftfVirtualMachineBuilder = new JftfVirtualMachineBuilder();
        jftfVirtualMachineBuilder.VirtualMachineFactory(args, JftfDemosPositiveTest.class);
    }
}